import http from "k6/http";
import ws from "k6/ws";
import exec from "k6/execution";
import { check, fail, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "https://api.commit-me.com";
const WS_BASE_URL = __ENV.WS_BASE_URL || BASE_URL.replace(/^http/, "ws");
const LOADTEST_BASE_PATH = __ENV.LOADTEST_BASE_PATH || "/internal/loadtest";
const CHAT_WS_PATH = __ENV.CHAT_WS_PATH || "/ws";
const CHAT_ROOM_ID = __ENV.CHAT_ROOM_ID || "8";
const CHAT_MESSAGE_COUNT = Number(__ENV.CHAT_MESSAGE_COUNT || 90);
const CHAT_MESSAGE_INTERVAL_SEC = Number(__ENV.CHAT_MESSAGE_INTERVAL_SEC || 0.5);
const CHAT_STOMP_HOST = __ENV.CHAT_STOMP_HOST || "api.commit-me.com";
const RESUME_CREATE_POSITION_ID = Number(__ENV.RESUME_CREATE_POSITION_ID || 1);
const RESUME_CREATE_REPO_URLS = (__ENV.RESUME_CREATE_REPO_URLS || "https://github.com/openai/openai-openapi").split(",")
  .map((value) => value.trim())
  .filter(Boolean);
const RESUME_EDIT_MESSAGE = __ENV.RESUME_EDIT_MESSAGE || "loadtest resume edit request";

const BULK_CREATE_USER_COUNT = 320; // 시나리오 중 최대 VU 수로 설정
const RESUME_CREATE_REQUESTS_PER_MINUTE = 18;
const RESUME_EDIT_REQUESTS_PER_MINUTE = 50;
const RESUME_CREATE_DURATION_MS = 60 * 1000;
const RESUME_EDIT_START_DELAY_MS = 60 * 1000;
const RESUME_EDIT_DURATION_MS = 60 * 1000;
const RESUME_PIPELINE_DURATION_MS = RESUME_EDIT_START_DELAY_MS + RESUME_EDIT_DURATION_MS;
const RESUME_CREATE_INTERVAL_MS = RESUME_CREATE_DURATION_MS / RESUME_CREATE_REQUESTS_PER_MINUTE;
const RESUME_EDIT_INTERVAL_MS = RESUME_EDIT_DURATION_MS / RESUME_EDIT_REQUESTS_PER_MINUTE;
const INTERVIEW_CREATE_POSITION_ID = Number(__ENV.INTERVIEW_CREATE_POSITION_ID || RESUME_CREATE_POSITION_ID);
const INTERVIEW_CREATE_TYPE = __ENV.INTERVIEW_CREATE_TYPE || "TECHNICAL";
const INTERVIEW_CREATE_REQUESTS_PER_MINUTE = 18;
const INTERVIEW_MESSAGE_REQUESTS_PER_MINUTE = 50;
const INTERVIEW_CREATE_DURATION_MS = 60 * 1000;
const INTERVIEW_MESSAGE_START_DELAY_MS = 15 * 1000;
const INTERVIEW_MESSAGE_DURATION_MS = 60 * 1000;
const INTERVIEW_PIPELINE_DURATION_MS = INTERVIEW_MESSAGE_START_DELAY_MS + INTERVIEW_MESSAGE_DURATION_MS;
const INTERVIEW_CREATE_INTERVAL_MS = INTERVIEW_CREATE_DURATION_MS / INTERVIEW_CREATE_REQUESTS_PER_MINUTE;
const INTERVIEW_MESSAGE_INTERVAL_MS = INTERVIEW_MESSAGE_DURATION_MS / INTERVIEW_MESSAGE_REQUESTS_PER_MINUTE;
const INTERVIEW_ANSWER_TEXT = __ENV.INTERVIEW_ANSWER_TEXT || "loadtest interview answer";

export const options = {
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)"],
  summaryTimeUnit: "ms",
  thresholds: {
    "http_req_duration{name:POST loadtest bulk create}": ["p(95)<5000"],
    "http_req_duration{name:GET resumes list}": ["p(95)<5000"],
    "http_req_duration{name:GET resumes list (cache for detail)}": ["p(95)<5000"],
    "http_req_duration{name:GET resume detail}": ["p(95)<5000"],
    "http_req_duration{name:GET auth github loginUrl}": ["p(95)<5000"],
    "http_req_duration{name:POST auth token}": ["p(95)<5000"],
    "http_req_duration{name:POST resumes create}": ["p(95)<5000"],
    "http_req_duration{name:PATCH resumes edit}": ["p(95)<5000"],
    "http_req_duration{name:POST interviews create}": ["p(95)<5000"],
    "http_req_duration{name:GET interview messages}": ["p(95)<5000"],
    "http_req_duration{name:POST interview answer}": ["p(95)<5000"],
    "ws_connecting{name:WS chat connect}": ["p(95)<5000"],
  },
  scenarios: {
    resume_create_edit_pipeline: {
      executor: "constant-vus",
      vus: 1,
      duration: `${Math.ceil(RESUME_PIPELINE_DURATION_MS / 1000)}s`,
      exec: "resumeCreateEditPipeline",
    },
    interview_create_message_pipeline: {
      executor: "constant-vus",
      vus: 1,
      duration: `${Math.ceil(INTERVIEW_PIPELINE_DURATION_MS / 1000)}s`,
      exec: "interviewCreateMessagePipeline",
    },
    resume_list: {
      executor: "constant-vus",
      vus: 50,
      duration: "3m",
      exec: "resumeList",
    },
    resume_detail: {
      executor: "constant-vus",
      vus: 50,
      duration: "3m",
      exec: "resumeDetail",
    },
    multi_chat: {
      executor: "constant-vus",
      vus: 320,
      duration: "3m",
      exec: "multiChat",
    },
    auth_login_url: {
      executor: "constant-vus",
      vus: 50,
      duration: "3m",
      exec: "authLoginUrl",
    },
    auth_token_reissue: {
      executor: "constant-vus",
      vus: 50,
      duration: "3m",
      exec: "authTokenReissue",
    },
  },
};

function buildRunId() {
  if (__ENV.LOADTEST_RUN_ID) {
    return __ENV.LOADTEST_RUN_ID;
  }

  return `ltv2-${new Date().toISOString().replace(/[^0-9A-Za-z]/g, "").slice(0, 16)}`;
}

function getUserIndex(users) {
  const vuId = exec.vu.idInTest || __VU;
  return (vuId - 1) % users.length;
}

function getVuUser(data) {
  const users = data?.users || [];
  if (!users.length) {
    fail("No loadtest users available. Check /internal/loadtest/auth/users/bulk-create.");
  }
  return users[getUserIndex(users)];
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

function authHeaders(accessToken) {
  return {
    Cookie: buildAuthCookie(accessToken),
  };
}

function extractCookieValue(response, name) {
  const cookies = response?.cookies?.[name];
  if (!Array.isArray(cookies) || !cookies.length) {
    return null;
  }

  const latest = cookies[cookies.length - 1];
  return latest?.value || null;
}

function ensureResponseOk(response, label) {
  const ok = check(response, {
    [`${label} status is 2xx`]: (r) => r.status >= 200 && r.status < 300,
  });

  if (!ok) {
    fail(`${label} failed with status=${response.status} body=${response.body}`);
  }
}

function createResume(user, ordinal) {
  const response = http.post(
    `${BASE_URL}/resumes`,
    JSON.stringify({
      repoUrls: RESUME_CREATE_REPO_URLS,
      positionId: RESUME_CREATE_POSITION_ID,
      name: `lt-resume-${ordinal}`,
    }),
    {
      headers: {
        ...authHeaders(user.accessToken),
        "Content-Type": "application/json",
      },
      tags: { name: "POST resumes create" },
    },
  );

  const ok = check(response, {
    "resume create 200": (r) => r.status === 200,
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

function editResume(user, resumeId) {
  const response = http.patch(
    `${BASE_URL}/resumes/${encodeURIComponent(String(resumeId))}`,
    JSON.stringify({ message: RESUME_EDIT_MESSAGE }),
    {
      headers: {
        ...authHeaders(user.accessToken),
        "Content-Type": "application/json",
      },
      tags: { name: "PATCH resumes edit" },
    },
  );

  check(response, {
    "resume edit 200": (r) => r.status === 200,
  });

  return response;
}

function createInterview(user, resumeId) {
  const response = http.post(
    `${BASE_URL}/interviews`,
    JSON.stringify({
      interviewType: INTERVIEW_CREATE_TYPE,
      positionId: INTERVIEW_CREATE_POSITION_ID,
      resumeId,
    }),
    {
      headers: {
        ...authHeaders(user.accessToken),
        "Content-Type": "application/json",
      },
      tags: { name: "POST interviews create" },
    },
  );

  const ok = check(response, {
    "interview create 200": (r) => r.status === 200,
  });

  if (!ok) {
    return null;
  }

  try {
    return response.json("data.id");
  } catch (e) {
    return null;
  }
}

function getInterviewMessages(user, interviewId) {
  const response = http.get(`${BASE_URL}/interviews/${encodeURIComponent(String(interviewId))}/messages`, {
    headers: authHeaders(user.accessToken),
    tags: { name: "GET interview messages" },
  });

  const ok = check(response, {
    "interview messages 200": (r) => r.status === 200,
  });

  if (!ok) {
    return [];
  }

  try {
    return response.json("data") || [];
  } catch (e) {
    return [];
  }
}

function answerInterview(user, interviewId, turnNo) {
  const response = http.post(
    `${BASE_URL}/interviews/${encodeURIComponent(String(interviewId))}/messages`,
    JSON.stringify({
      turnNo,
      answer: INTERVIEW_ANSWER_TEXT,
      answerInputType: "TEXT",
      audioUrl: null,
    }),
    {
      headers: {
        ...authHeaders(user.accessToken),
        "Content-Type": "application/json",
      },
      tags: { name: "POST interview answer" },
    },
  );

  check(response, {
    "interview answer 200": (r) => r.status === 200,
  });

  return response;
}

export function setup() {
  const runId = buildRunId();
  const payload = JSON.stringify({
    runId,
    count: BULK_CREATE_USER_COUNT,
    startIndex: 1,
    status: "ACTIVE",
    returnToken: true,
  });

  const response = http.post(
    `${BASE_URL}${LOADTEST_BASE_PATH}/auth/users/bulk-create`,
    payload,
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

  if (!Array.isArray(items) || items.length < BULK_CREATE_USER_COUNT) {
    fail(
      `loadtest bulk create returned ${Array.isArray(items) ? items.length : 0} users, expected ${BULK_CREATE_USER_COUNT}`,
    );
  }

  const users = items.map((item) => ({
    userId: item.userId,
    providerUserId: item.providerUserId,
    providerUsername: item.providerUsername,
    accessToken: item.accessToken,
    refreshToken: item.refreshToken,
  }));

  return { runId, users };
}

let resumePipelineState = null;

export function resumeCreateEditPipeline(data) {
  if (!resumePipelineState) {
    resumePipelineState = {
      startedAt: Date.now(),
      createdCount: 0,
      editedCount: 0,
      resumeIds: [],
      nextResumeIndex: 0,
    };
  }

  const user = getVuUser(data);
  const elapsedMs = Date.now() - resumePipelineState.startedAt;

  while (
    resumePipelineState.createdCount < RESUME_CREATE_REQUESTS_PER_MINUTE &&
    elapsedMs >= resumePipelineState.createdCount * RESUME_CREATE_INTERVAL_MS
  ) {
    const ordinal = resumePipelineState.createdCount + 1;
    const resumeId = createResume(user, ordinal);
    resumePipelineState.createdCount += 1;
    if (resumeId !== null && resumeId !== undefined) {
      resumePipelineState.resumeIds.push(resumeId);
    }
  }

  while (
    resumePipelineState.editedCount < RESUME_EDIT_REQUESTS_PER_MINUTE &&
    elapsedMs >= RESUME_EDIT_START_DELAY_MS + (resumePipelineState.editedCount * RESUME_EDIT_INTERVAL_MS) &&
    resumePipelineState.resumeIds.length > 0
  ) {
    const index = resumePipelineState.nextResumeIndex % resumePipelineState.resumeIds.length;
    const resumeId = resumePipelineState.resumeIds[index];
    editResume(user, resumeId);
    resumePipelineState.editedCount += 1;
    resumePipelineState.nextResumeIndex += 1;
  }

  sleep(0.1);
}

let interviewPipelineState = null;

export function interviewCreateMessagePipeline(data) {
  if (!interviewPipelineState) {
    interviewPipelineState = {
      startedAt: Date.now(),
      createdCount: 0,
      answeredCount: 0,
      interviews: [],
      nextInterviewIndex: 0,
    };
  }

  const user = getVuUser(data);
  const elapsedMs = Date.now() - interviewPipelineState.startedAt;

  while (
    interviewPipelineState.createdCount < INTERVIEW_CREATE_REQUESTS_PER_MINUTE &&
    elapsedMs >= interviewPipelineState.createdCount * INTERVIEW_CREATE_INTERVAL_MS
  ) {
    const resumeIds = resumePipelineState?.resumeIds || [];
    const resumeId = resumeIds.length > 0
      ? resumeIds[interviewPipelineState.createdCount % resumeIds.length]
      : null;

    interviewPipelineState.createdCount += 1;

    if (!resumeId) {
      continue;
    }

    const interviewId = createInterview(user, resumeId);
    if (interviewId !== null && interviewId !== undefined) {
      interviewPipelineState.interviews.push({
        interviewId,
        nextTurnNo: 1,
      });
    }
  }

  while (
    interviewPipelineState.answeredCount < INTERVIEW_MESSAGE_REQUESTS_PER_MINUTE &&
    elapsedMs >= INTERVIEW_MESSAGE_START_DELAY_MS + (interviewPipelineState.answeredCount * INTERVIEW_MESSAGE_INTERVAL_MS) &&
    interviewPipelineState.interviews.length > 0
  ) {
    const index = interviewPipelineState.nextInterviewIndex % interviewPipelineState.interviews.length;
    const interviewState = interviewPipelineState.interviews[index];
    const messages = getInterviewMessages(user, interviewState.interviewId);
    const unansweredMessage = messages.find(
      (message) => message?.turnNo === interviewState.nextTurnNo && !message?.answer,
    );

    interviewPipelineState.nextInterviewIndex += 1;

    if (!unansweredMessage?.turnNo) {
      break;
    }

    answerInterview(user, interviewState.interviewId, unansweredMessage.turnNo);
    interviewPipelineState.answeredCount += 1;
    interviewState.nextTurnNo = unansweredMessage.turnNo + 1;
  }

  sleep(0.1);
}

export function resumeList(data) {
  const user = getVuUser(data);
  const response = http.get(`${BASE_URL}/resumes?page=0&size=10`, {
    headers: authHeaders(user.accessToken),
    tags: { name: "GET resumes list" },
  });

  check(response, {
    "resume list 200": (r) => r.status === 200,
  });

  sleep(0.2);
}

let cachedResumeIds = null;

export function resumeDetail(data) {
  const user = getVuUser(data);

  if (!cachedResumeIds) {
    const listResponse = http.get(`${BASE_URL}/resumes?page=0&size=10`, {
      headers: authHeaders(user.accessToken),
      tags: { name: "GET resumes list (cache for detail)" },
    });

    const ok = check(listResponse, {
      "resume list(cache) 200": (r) => r.status === 200,
    });

    if (!ok) {
      sleep(0.2);
      return;
    }

    try {
      const items = listResponse.json("data.items");
      cachedResumeIds = Array.isArray(items)
        ? items.map((item) => item?.resumeId).filter((id) => id !== null && id !== undefined)
        : [];
    } catch (e) {
      cachedResumeIds = [];
    }
  }

  if (!Array.isArray(cachedResumeIds) || !cachedResumeIds.length) {
    sleep(0.2);
    return;
  }

  const resumeId = String(cachedResumeIds[(__ITER + __VU) % cachedResumeIds.length]);
  const response = http.get(`${BASE_URL}/resumes/${encodeURIComponent(resumeId)}`, {
    headers: authHeaders(user.accessToken),
    tags: { name: "GET resume detail" },
  });

  check(response, {
    "resume detail 200": (r) => r.status === 200,
  });

  sleep(0.2);
}

export function multiChat(data) {
  if (!CHAT_ROOM_ID) {
    sleep(0.2);
    return;
  }

  const user = getVuUser(data);
  const response = ws.connect(
    `${WS_BASE_URL}${CHAT_WS_PATH}`,
    {
      headers: authHeaders(user.accessToken),
      tags: { name: "WS chat connect" },
    },
    (socket) => {
      socket.on("open", () => {
        const connectFrame =
          `CONNECT\naccept-version:1.2\nheart-beat:10000,10000\nhost:${CHAT_STOMP_HOST}\n\n\0`;
        socket.send(connectFrame);
      });

      socket.on("message", (message) => {
        const dataText = String(message);
        if (!dataText.startsWith("CONNECTED")) {
          return;
        }

        const subscribeFrame =
          `SUBSCRIBE\nid:sub-${__VU}\ndestination:/topic/chats/${CHAT_ROOM_ID}\n\n\0`;
        socket.send(subscribeFrame);

        for (let i = 0; i < CHAT_MESSAGE_COUNT; i += 1) {
          const body = JSON.stringify({
            message: `loadtest-msg-${__VU}-${__ITER}-${i}`,
            attachmentUploadIds: [],
            mentionUserId: null,
          });
          const sendFrame =
            `SEND\ndestination:/app/chats/${CHAT_ROOM_ID}\ncontent-type:application/json\ncontent-length:${body.length}\n\n${body}\0`;
          socket.send(sendFrame);
          sleep(CHAT_MESSAGE_INTERVAL_SEC);
        }

        socket.close();
      });

      socket.on("error", () => {});
    },
  );

  check(response, {
    "ws connected": (r) => r && r.status === 101,
  });

  sleep(0.2);
}

export function authLoginUrl() {
  const response = http.get(`${BASE_URL}/auth/github/loginUrl`, {
    tags: { name: "GET auth github loginUrl" },
  });

  check(response, {
    "auth loginUrl 200": (r) => r.status === 200,
    "auth loginUrl has data": (r) => Boolean(r.json("data.loginUrl")),
  });

  sleep(0.2);
}

let tokenSession = null;

export function authTokenReissue(data) {
  if (!tokenSession) {
    const user = getVuUser(data);
    tokenSession = {
      accessToken: user.accessToken,
      refreshToken: user.refreshToken,
    };
  }

  const csrfResponse = http.get(`${BASE_URL}/auth/github/loginUrl`, {
    headers: {
      Cookie: buildAuthCookie(tokenSession.accessToken, tokenSession.refreshToken),
    },
    tags: { name: "GET auth github loginUrl" },
  });

  const xsrfToken = extractCookieValue(csrfResponse, "XSRF-TOKEN");
  const csrfOk = check(csrfResponse, {
    "auth token csrf seed 200": (r) => r.status === 200,
    "auth token csrf seed has xsrf": () => Boolean(xsrfToken),
  });

  if (!csrfOk || !xsrfToken) {
    sleep(0.2);
    return;
  }

  const response = http.post(`${BASE_URL}/auth/token`, null, {
    headers: {
      Cookie: buildAuthCookie(null, tokenSession.refreshToken, xsrfToken),
      "X-XSRF-TOKEN": xsrfToken,
    },
    tags: { name: "POST auth token" },
  });

  const nextAccessToken = extractCookieValue(response, "access_token");
  const nextRefreshToken = extractCookieValue(response, "refresh_token");

  const ok = check(response, {
    "auth token 200": (r) => r.status === 200,
    "auth token issued access cookie": () => Boolean(nextAccessToken),
    "auth token issued refresh cookie": () => Boolean(nextRefreshToken),
  });

  if (ok && nextAccessToken && nextRefreshToken) {
    tokenSession.accessToken = nextAccessToken;
    tokenSession.refreshToken = nextRefreshToken;
  }

  sleep(0.2);
}
