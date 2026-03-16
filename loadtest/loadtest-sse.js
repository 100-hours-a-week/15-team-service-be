import http from "k6/http";
import exec from "k6/execution";
import { check, fail, sleep } from "k6";
import { Counter, Rate } from "k6/metrics";
import sse from "k6/x/sse";

const BASE_URL = __ENV.BASE_URL || "https://api.commit-me.com";
const LOADTEST_BASE_PATH = __ENV.LOADTEST_BASE_PATH || "/internal/loadtest";
const USER_COUNT = Number(__ENV.SSE_USER_COUNT || 60);
const STREAM_VUS = Number(__ENV.SSE_STREAM_VUS || 30);
const TRIGGER_VUS = Number(__ENV.SSE_TRIGGER_VUS || 6);
const STREAM_DURATION = __ENV.SSE_STREAM_DURATION || "3m";
const TRIGGER_DURATION = __ENV.SSE_TRIGGER_DURATION || "3m";
const STREAM_TIMEOUT = __ENV.SSE_STREAM_TIMEOUT || "35s";
const STREAM_IDLE_SLEEP_SEC = Number(__ENV.SSE_STREAM_IDLE_SLEEP_SEC || 1);
const RESUME_CREATE_POSITION_ID = Number(__ENV.RESUME_CREATE_POSITION_ID || 1);
const RESUME_CREATE_REPO_URLS = (__ENV.RESUME_CREATE_REPO_URLS || "https://github.com/openai/openai-openapi")
  .split(",")
  .map((value) => value.trim())
  .filter(Boolean);
const RESUME_EDIT_MESSAGE = __ENV.RESUME_EDIT_MESSAGE || "loadtest sse resume edit request";
const EXPECT_HEARTBEAT = String(__ENV.SSE_EXPECT_HEARTBEAT || "true").toLowerCase() !== "false";
const EXPECT_RESUME_REFRESH = String(__ENV.SSE_EXPECT_RESUME_REFRESH || "true").toLowerCase() !== "false";
const LAST_EVENT_ID = __ENV.SSE_LAST_EVENT_ID || "";

const connectedEventRate = new Rate("sse_connected_event_rate");
const heartbeatEventRate = new Rate("sse_heartbeat_event_rate");
const resumeRefreshEventRate = new Rate("sse_resume_refresh_event_rate");
const streamOpenRate = new Rate("sse_stream_open_rate");
const streamErrorRate = new Rate("sse_stream_error_rate");
const streamEventCount = new Counter("sse_stream_events_total");
const streamMessageCount = new Counter("sse_stream_messages_total");

export const options = {
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)"],
  summaryTimeUnit: "ms",
  thresholds: {
    "http_req_duration{name:POST loadtest bulk create}": ["p(95)<5000"],
    "http_req_duration{name:POST resumes create (seed)}": ["p(95)<5000"],
    "http_req_duration{name:PATCH resumes edit (sse trigger)}": ["p(95)<5000"],
    sse_stream_open_rate: ["rate>0.95"],
    sse_connected_event_rate: ["rate>0.9"],
  },
  scenarios: {
    notification_stream: {
      executor: "constant-vus",
      vus: STREAM_VUS,
      duration: STREAM_DURATION,
      exec: "notificationStream",
    },
    resume_stream: {
      executor: "constant-vus",
      vus: STREAM_VUS,
      duration: STREAM_DURATION,
      exec: "resumeStream",
    },
    resume_edit_trigger: {
      executor: "constant-vus",
      vus: TRIGGER_VUS,
      duration: TRIGGER_DURATION,
      exec: "resumeEditTrigger",
    },
  },
};

function buildRunId() {
  if (__ENV.LOADTEST_RUN_ID) {
    return __ENV.LOADTEST_RUN_ID;
  }

  return `ltsse-${new Date().toISOString().replace(/[^0-9A-Za-z]/g, "").slice(0, 16)}`;
}

function getUserIndex(items) {
  const vuId = exec.vu.idInTest || __VU;
  return (vuId - 1) % items.length;
}

function getVuUser(data) {
  const users = data?.users || [];
  if (!users.length) {
    fail("No loadtest users available. Check /internal/loadtest/auth/users/bulk-create.");
  }
  return users[getUserIndex(users)];
}

function getVuResumeId(data, user) {
  const resumeIdsByUserId = data?.resumeIdsByUserId || {};
  return resumeIdsByUserId[String(user.userId)] || null;
}

function buildAuthCookie(accessToken, refreshToken, xsrfToken) {
  const parts = [];

  if (accessToken) {
    parts.push(`access_token=${accessToken}`);
  }
  if (refreshToken) {
    parts.push(`refresh_token=${refreshToken}`);
  }
  if (xsrfToken) {
    parts.push(`XSRF-TOKEN=${xsrfToken}`);
  }

  return parts.join("; ");
}

function authHeaders(user, extraHeaders = {}) {
  return {
    Cookie: buildAuthCookie(user.accessToken, user.refreshToken),
    ...extraHeaders,
  };
}

function ensureResponseOk(response, label) {
  const ok = check(response, {
    [`${label} status is 2xx`]: (r) => r.status >= 200 && r.status < 300,
  });

  if (!ok) {
    fail(`${label} failed with status=${response.status} body=${response.body}`);
  }
}

function createSeedResume(user, ordinal) {
  const response = http.post(
    `${BASE_URL}/resumes`,
    JSON.stringify({
      repoUrls: RESUME_CREATE_REPO_URLS,
      positionId: RESUME_CREATE_POSITION_ID,
      name: `lt-sse-resume-${ordinal}`,
    }),
    {
      headers: {
        ...authHeaders(user),
        "Content-Type": "application/json",
      },
      tags: { name: "POST resumes create (seed)" },
    },
  );

  const ok = check(response, {
    "seed resume create 200": (r) => r.status === 200,
  });

  if (!ok) {
    return null;
  }

  try {
    return response.json("data");
  } catch (e) {
    return null;
  }
}

function buildSseParams(user, tagName, lastEventId = "") {
  const headers = {
    Accept: "text/event-stream",
    "Cache-Control": "no-cache",
  };

  if (lastEventId) {
    headers["Last-Event-ID"] = String(lastEventId);
  }

  return {
    headers: authHeaders(user, headers),
    tags: { name: tagName },
    timeout: STREAM_TIMEOUT,
  };
}

function openSseStream(url, user, tagName, expectations) {
  const state = {
    opened: false,
    errored: false,
    connected: false,
    heartbeat: false,
    resumeRefresh: false,
    lastEventId: null,
    eventCount: 0,
    messageCount: 0,
  };

  const response = sse.open(url, buildSseParams(user, tagName, LAST_EVENT_ID), (client) => {
    client.on("open", () => {
      state.opened = true;
    });

    client.on("error", () => {
      state.errored = true;
    });

    client.on("event", (event) => {
      state.eventCount += 1;
      streamEventCount.add(1);

      const eventName = event?.name || event?.event || "";
      const eventId = event?.id || null;
      if (eventId) {
        state.lastEventId = eventId;
      }

      if (eventName === "connected") {
        state.connected = true;
      } else if (eventName === "heartbeat") {
        state.heartbeat = true;
      } else if (eventName === "resume-refresh-required") {
        state.resumeRefresh = true;
      }

      if (
        state.connected &&
        (!expectations.expectHeartbeat || state.heartbeat) &&
        (!expectations.expectResumeRefresh || state.resumeRefresh)
      ) {
        client.close();
      }
    });

    client.on("message", () => {
      state.messageCount += 1;
      streamMessageCount.add(1);
    });
  });

  streamOpenRate.add(state.opened);
  streamErrorRate.add(state.errored);
  connectedEventRate.add(state.connected);
  heartbeatEventRate.add(state.heartbeat);
  resumeRefreshEventRate.add(state.resumeRefresh);

  check(response, {
    [`${tagName} open succeeded`]: () => state.opened && !state.errored,
    [`${tagName} received connected event`]: () => state.connected,
    [`${tagName} status is 200`]: (r) => r && r.status === 200,
  });

  if (expectations.expectHeartbeat) {
    check(response, {
      [`${tagName} received heartbeat event`]: () => state.heartbeat,
    });
  }

  if (expectations.expectResumeRefresh) {
    check(response, {
      [`${tagName} received resume-refresh-required event`]: () => state.resumeRefresh,
    });
  }

  return state;
}

export function setup() {
  const runId = buildRunId();
  const response = http.post(
    `${BASE_URL}${LOADTEST_BASE_PATH}/auth/users/bulk-create`,
    JSON.stringify({
      runId,
      count: USER_COUNT,
      startIndex: 1,
      status: "ACTIVE",
      returnToken: true,
    }),
    {
      headers: { "Content-Type": "application/json" },
      tags: { name: "POST loadtest bulk create" },
    },
  );

  ensureResponseOk(response, "loadtest bulk create");

  let items = [];
  try {
    items = response.json("data.items") || [];
  } catch (e) {
    fail(`loadtest bulk create returned invalid JSON: ${e}`);
  }

  if (!Array.isArray(items) || items.length < USER_COUNT) {
    fail(
      `loadtest bulk create returned ${Array.isArray(items) ? items.length : 0} users, expected ${USER_COUNT}`,
    );
  }

  const users = items.map((item) => ({
    userId: item.userId,
    providerUserId: item.providerUserId,
    providerUsername: item.providerUsername,
    accessToken: item.accessToken,
    refreshToken: item.refreshToken,
  }));

  const resumeIdsByUserId = {};
  for (let i = 0; i < users.length; i += 1) {
    const user = users[i];
    const resumeId = createSeedResume(user, i + 1);
    if (resumeId !== null && resumeId !== undefined) {
      resumeIdsByUserId[String(user.userId)] = resumeId;
    }
  }

  return {
    runId,
    users,
    resumeIdsByUserId,
  };
}

export function teardown(data) {
  if (!data?.runId) {
    return;
  }

  http.post(
    `${BASE_URL}${LOADTEST_BASE_PATH}/auth/reset`,
    JSON.stringify({
      runId: data.runId,
      limit: USER_COUNT,
    }),
    {
      headers: { "Content-Type": "application/json" },
      tags: { name: "POST loadtest auth reset" },
    },
  );
}

export function notificationStream(data) {
  const user = getVuUser(data);

  openSseStream(`${BASE_URL}/notifications/stream`, user, "GET notifications stream", {
    expectHeartbeat: EXPECT_HEARTBEAT,
    expectResumeRefresh: false,
  });

  sleep(STREAM_IDLE_SLEEP_SEC);
}

export function resumeStream(data) {
  const user = getVuUser(data);
  const resumeId = getVuResumeId(data, user);

  if (!resumeId) {
    sleep(STREAM_IDLE_SLEEP_SEC);
    return;
  }

  openSseStream(`${BASE_URL}/resumes/${encodeURIComponent(String(resumeId))}/stream`, user, "GET resumes stream", {
    expectHeartbeat: EXPECT_HEARTBEAT,
    expectResumeRefresh: EXPECT_RESUME_REFRESH,
  });

  sleep(STREAM_IDLE_SLEEP_SEC);
}

export function resumeEditTrigger(data) {
  const user = getVuUser(data);
  const resumeId = getVuResumeId(data, user);

  if (!resumeId) {
    sleep(0.2);
    return;
  }

  const response = http.patch(
    `${BASE_URL}/resumes/${encodeURIComponent(String(resumeId))}`,
    JSON.stringify({ message: `${RESUME_EDIT_MESSAGE}-${__VU}-${__ITER}` }),
    {
      headers: {
        ...authHeaders(user),
        "Content-Type": "application/json",
      },
      tags: { name: "PATCH resumes edit (sse trigger)" },
    },
  );

  check(response, {
    "resume edit trigger 200": (r) => r.status === 200,
  });

  sleep(1);
}
