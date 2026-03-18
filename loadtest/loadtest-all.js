import http from "k6/http";
import exec from "k6/execution";
import { check, fail, sleep } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "https://api.commit-me.com";
const LOADTEST_BASE_PATH = __ENV.LOADTEST_BASE_PATH || "/internal/loadtest";
const TARGET_SCENARIO = __ENV.TARGET_SCENARIO || "positions_cache";

const RESUME_CREATE_POSITION_ID = Number(__ENV.RESUME_CREATE_POSITION_ID || 1);
const RESUME_CREATE_REPO_URLS = (
    __ENV.RESUME_CREATE_REPO_URLS || "https://github.com/openai/openai-openapi"
)
    .split(",")
    .map((value) => value.trim())
    .filter(Boolean);
const RESUME_EDIT_MESSAGE = __ENV.RESUME_EDIT_MESSAGE || "loadtest resume edit request";
const RESUME_CONFLICT_OPERATION = (__ENV.RESUME_CONFLICT_OPERATION || "create").toLowerCase();
const API_P95_THRESHOLD_MS = Number(__ENV.API_P95_THRESHOLD_MS || 10000);
const API_P99_THRESHOLD_MS = Number(__ENV.API_P99_THRESHOLD_MS || 15000);

const positionsColdTrend = new Trend("positions_cold_req_duration", true);
const positionsWarmTrend = new Trend("positions_warm_req_duration", true);
const refreshReissueTrend = new Trend("refresh_reissue_req_duration", true);
const refreshPenetrationTrend = new Trend("refresh_penetration_req_duration", true);
const refreshStampedeTrend = new Trend("refresh_stampede_req_duration", true);
const sseConnectTrend = new Trend("sse_connect_req_duration", true);
const e2eFlowTrend = new Trend("e2e_flow_duration", true);

const refreshReissueSuccessRate = new Rate("refresh_reissue_success_rate");
const refreshPenetrationExpectedRate = new Rate("refresh_penetration_expected_rate");
const stampedeSuccessRate = new Rate("stampede_success_rate");
const sseConnectedRate = new Rate("sse_connected_rate");
const e2eSuccessRate = new Rate("e2e_success_rate");
const resumeConflict403Rate = new Rate("resume_conflict_403_rate");

const resumeConflictStatusCounter = new Counter("resume_conflict_status_count");
const penetrationStatusCounter = new Counter("refresh_penetration_status_count");
const sseTimeoutCounter = new Counter("sse_timeout_count");

const scenarioBuilders = {
    positions_cache: () => ({
        positions_cold_start: {
            executor: "shared-iterations",
            vus: 1,
            iterations: 1,
            exec: "positionsColdStart",
        },
        positions_warm_cache: {
            executor: "constant-vus",
            vus: Number(__ENV.POSITIONS_WARM_VUS || 20),
            duration: __ENV.POSITIONS_WARM_DURATION || "2m",
            startTime: __ENV.POSITIONS_WARM_START_TIME || "10s",
            exec: "positionsWarmCache",
        },
    }),
    refresh_cache: () => ({
        refresh_token_rtr: {
            executor: "constant-vus",
            vus: Number(__ENV.REFRESH_RTR_VUS || 200),
            duration: __ENV.REFRESH_RTR_DURATION || "3m",
            exec: "refreshTokenRotation",
        },
    }),
    cache_penetration: () => ({
        cache_penetration_spike_200: {
            executor: "per-vu-iterations",
            vus: Number(__ENV.PENETRATION_SPIKE_200 || 200),
            iterations: Number(__ENV.PENETRATION_ITERATIONS || 1),
            maxDuration: __ENV.PENETRATION_MAX_DURATION || "30s",
            exec: "refreshTokenPenetration",
        },
        cache_penetration_spike_500: {
            executor: "per-vu-iterations",
            vus: Number(__ENV.PENETRATION_SPIKE_500 || 500),
            iterations: Number(__ENV.PENETRATION_ITERATIONS || 1),
            maxDuration: __ENV.PENETRATION_MAX_DURATION || "30s",
            startTime: __ENV.PENETRATION_SPIKE_500_START || "20s",
            exec: "refreshTokenPenetration",
        },
        cache_penetration_spike_1000: {
            executor: "per-vu-iterations",
            vus: Number(__ENV.PENETRATION_SPIKE_1000 || 1000),
            iterations: Number(__ENV.PENETRATION_ITERATIONS || 1),
            maxDuration: __ENV.PENETRATION_MAX_DURATION || "30s",
            startTime: __ENV.PENETRATION_SPIKE_1000_START || "40s",
            exec: "refreshTokenPenetration",
        },
    }),
    cache_stampede: () => ({
        cache_stampede_200: {
            executor: "per-vu-iterations",
            vus: Number(__ENV.STAMPEDE_USERS_200 || 200),
            iterations: 1,
            maxDuration: __ENV.STAMPEDE_MAX_DURATION || "30s",
            exec: "refreshTokenStampede",
        },
        cache_stampede_500: {
            executor: "per-vu-iterations",
            vus: Number(__ENV.STAMPEDE_USERS_500 || 500),
            iterations: 1,
            maxDuration: __ENV.STAMPEDE_MAX_DURATION || "30s",
            startTime: __ENV.STAMPEDE_500_START || "20s",
            exec: "refreshTokenStampede",
        },
    }),
    sse_heartbeat: () => ({
        sse_connections_200: {
            executor: "per-vu-iterations",
            vus: Number(__ENV.SSE_CONNECTIONS_200 || 200),
            iterations: 1,
            maxDuration: __ENV.SSE_MAX_DURATION || "90s",
            exec: "sseMassConnection",
        },
        sse_connections_500: {
            executor: "per-vu-iterations",
            vus: Number(__ENV.SSE_CONNECTIONS_500 || 500),
            iterations: 1,
            maxDuration: __ENV.SSE_MAX_DURATION || "90s",
            startTime: __ENV.SSE_500_START || "100s",
            exec: "sseMassConnection",
        },
        sse_connections_1000: {
            executor: "per-vu-iterations",
            vus: Number(__ENV.SSE_CONNECTIONS_1000 || 1000),
            iterations: 1,
            maxDuration: __ENV.SSE_MAX_DURATION || "90s",
            startTime: __ENV.SSE_1000_START || "200s",
            exec: "sseMassConnection",
        },
    }),
    e2e_mix: () => ({
        e2e_mixed_flow: {
            executor: "constant-vus",
            vus: Number(__ENV.E2E_VUS || 200),
            duration: __ENV.E2E_DURATION || "3m",
            exec: "e2eMixedFlow",
        },
    }),
    resume_conflict: () => ({
        resume_conflict_same_user_5: {
            executor: "per-vu-iterations",
            vus: Number(__ENV.CONFLICT_SAME_USER_5 || 5),
            iterations: 1,
            maxDuration: "20s",
            exec: "resumeConflictSameUser",
        },
        resume_conflict_same_user_20: {
            executor: "per-vu-iterations",
            vus: Number(__ENV.CONFLICT_SAME_USER_20 || 20),
            iterations: 1,
            maxDuration: "20s",
            startTime: "15s",
            exec: "resumeConflictSameUser",
        },
        resume_conflict_same_user_50: {
            executor: "per-vu-iterations",
            vus: Number(__ENV.CONFLICT_SAME_USER_50 || 50),
            iterations: 1,
            maxDuration: "20s",
            startTime: "30s",
            exec: "resumeConflictSameUser",
        },
        resume_conflict_multi_user_50: {
            executor: "per-vu-iterations",
            vus: Number(__ENV.CONFLICT_MULTI_USER_50 || 50),
            iterations: 1,
            maxDuration: "20s",
            startTime: "60s",
            exec: "resumeConflictMultiUser",
        },
        resume_conflict_multi_user_200: {
            executor: "per-vu-iterations",
            vus: Number(__ENV.CONFLICT_MULTI_USER_200 || 200),
            iterations: 1,
            maxDuration: "20s",
            startTime: "75s",
            exec: "resumeConflictMultiUser",
        },
        resume_conflict_multi_user_500: {
            executor: "per-vu-iterations",
            vus: Number(__ENV.CONFLICT_MULTI_USER_500 || 500),
            iterations: 1,
            maxDuration: "20s",
            startTime: "90s",
            exec: "resumeConflictMultiUser",
        },
    }),
};

const endpointTagsByScenario = {
    positions_cache: ["GET positions cold", "GET positions warm"],
    refresh_cache: ["GET auth github loginUrl", "POST auth token"],
    cache_penetration: ["GET auth github loginUrl (anonymous)", "POST auth token invalid"],
    cache_stampede: ["GET auth github loginUrl", "POST auth token"],
    sse_heartbeat: ["GET notifications SSE stream"],
    e2e_mix: [
        "POST loadtest bulk create",
        "POST loadtest auth login",
        "GET resumes list (e2e)",
        "POST resumes create",
        "PATCH resumes name",
        "GET notifications list (e2e)",
        "PATCH user profile",
        "POST auth logout",
        "GET auth github loginUrl",
    ],
    resume_conflict: [
        "POST loadtest bulk create",
        "POST resumes create",
        "PATCH resumes edit conflict",
        "GET resumes list (for edit)",
        "GET auth github loginUrl",
    ],
};

function buildApiDurationThresholds() {
    const names = new Set(endpointTagsByScenario[TARGET_SCENARIO] || []);
    const thresholds = {};
    names.forEach((name) => {
        thresholds[`http_req_duration{name:${name}}`] = [
            `p(95)<${API_P95_THRESHOLD_MS}`,
            `p(99)<${API_P99_THRESHOLD_MS}`,
        ];
    });
    return thresholds;
}

const selectedScenarioBuilder = scenarioBuilders[TARGET_SCENARIO];
if (!selectedScenarioBuilder) {
    fail(
        `Unsupported TARGET_SCENARIO=${TARGET_SCENARIO}. ` +
            `Use one of: ${Object.keys(scenarioBuilders).join(", ")}`,
    );
}

export const options = {
    summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)"],
    summaryTimeUnit: "ms",
    scenarios: selectedScenarioBuilder(),
    thresholds: {
        "http_req_failed": ["rate<0.2"],
        "refresh_reissue_success_rate": ["rate>0.9"],
        "stampede_success_rate": ["rate>0.8"],
        "sse_connected_rate": ["rate>0.7"],
        "e2e_success_rate": ["rate>0.8"],
        ...buildApiDurationThresholds(),
    },
};

const sessionByUserId = {};
let anonymousXsrfToken = null;

function buildRunId() {
    if (__ENV.LOADTEST_RUN_ID) {
        return __ENV.LOADTEST_RUN_ID;
    }
    return `ltv3-${new Date()
        .toISOString()
        .replace(/[^0-9A-Za-z]/g, "")
        .slice(0, 16)}`;
}

function expectedUserCount() {
    switch (TARGET_SCENARIO) {
        case "refresh_cache":
            return Number(__ENV.REFRESH_RTR_VUS || 200);
        case "cache_stampede":
            return Number(__ENV.STAMPEDE_USERS_500 || 500);
        case "sse_heartbeat":
            return Number(__ENV.SSE_CONNECTIONS_1000 || 1000);
        case "e2e_mix":
            return Number(__ENV.E2E_VUS || 100);
        case "resume_conflict":
            return Number(__ENV.CONFLICT_MULTI_USER_500 || 500);
        default:
            return Number(__ENV.BULK_CREATE_USER_COUNT || 0);
    }
}

function requiresLoadtestUsers() {
    return ["refresh_cache", "cache_stampede", "sse_heartbeat", "e2e_mix", "resume_conflict"].includes(
        TARGET_SCENARIO,
    );
}

function createLoadtestUsers(runId, count) {
    const response = http.post(
        `${BASE_URL}${LOADTEST_BASE_PATH}/auth/users/bulk-create`,
        JSON.stringify({
            runId,
            count,
            startIndex: 1,
            status: "ACTIVE",
            returnToken: true,
        }),
        {
            headers: { "Content-Type": "application/json" },
            tags: { name: "POST loadtest bulk create" },
        },
    );

    const ok = check(response, {
        "loadtest bulk create 2xx": (r) => r.status >= 200 && r.status < 300,
    });

    if (!ok) {
        fail(`loadtest bulk create failed status=${response.status} body=${response.body}`);
    }

    const users = response.json("data.items") || [];
    if (!Array.isArray(users) || users.length < count) {
        fail(`insufficient loadtest users. expected=${count}, actual=${Array.isArray(users) ? users.length : 0}`);
    }

    return users.map((item) => ({
        userId: item.userId,
        providerUserId: item.providerUserId,
        providerUsername: item.providerUsername,
        accessToken: item.accessToken,
        refreshToken: item.refreshToken,
    }));
}

export function setup() {
    const runId = buildRunId();
    const userCount = expectedUserCount();

    const users = requiresLoadtestUsers() ? createLoadtestUsers(runId, userCount) : [];

    if (String(__ENV.ENABLE_CACHE_CLEAR_HOOK || "false") === "true") {
        runOptionalCacheClearHook();
    }

    return {
        runId,
        users,
    };
}

function runOptionalCacheClearHook() {
    const url = __ENV.CACHE_CLEAR_URL;
    if (!url) {
        return;
    }

    const method = (__ENV.CACHE_CLEAR_METHOD || "POST").toUpperCase();
    const body = __ENV.CACHE_CLEAR_BODY || null;
    const response = http.request(method, url, body, {
        headers: {
            "Content-Type": __ENV.CACHE_CLEAR_CONTENT_TYPE || "application/json",
            ...(String(__ENV.CACHE_CLEAR_BEARER || "").trim()
                ? { Authorization: `Bearer ${String(__ENV.CACHE_CLEAR_BEARER).trim()}` }
                : {}),
        },
        tags: { name: "CACHE CLEAR HOOK" },
    });

    check(response, {
        "cache clear hook status 2xx": (r) => r.status >= 200 && r.status < 300,
    });
}

function getVuUser(data) {
    const users = data?.users || [];
    if (!users.length) {
        fail(`No loadtest users available for scenario=${TARGET_SCENARIO}`);
    }
    const vuId = exec.vu.idInTest || __VU;
    const index = (vuId - 1) % users.length;
    return users[index];
}

function getSharedUser(data) {
    const users = data?.users || [];
    if (!users.length) {
        fail(`No loadtest users available for scenario=${TARGET_SCENARIO}`);
    }
    return users[0];
}

function getUserSession(user) {
    if (!sessionByUserId[user.userId]) {
        sessionByUserId[user.userId] = {
            accessToken: user.accessToken,
            refreshToken: user.refreshToken,
            xsrfToken: null,
            providerUserId: user.providerUserId,
        };
    }
    return sessionByUserId[user.userId];
}

function extractCookieValue(response, name) {
    const cookies = response?.cookies?.[name];
    if (!Array.isArray(cookies) || !cookies.length) {
        return null;
    }
    const latest = cookies[cookies.length - 1];
    return latest?.value || null;
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

function seedXsrfTokenForSession(session) {
    const response = http.get(`${BASE_URL}/auth/github/loginUrl`, {
        headers: {
            Cookie: buildAuthCookie(session.accessToken, session.refreshToken),
        },
        tags: { name: "GET auth github loginUrl" },
    });

    const xsrfToken = extractCookieValue(response, "XSRF-TOKEN");
    const ok = check(response, {
        "csrf seed 200": (r) => r.status === 200,
        "csrf seed exists": () => Boolean(xsrfToken),
    });

    if (!ok || !xsrfToken) {
        return null;
    }

    session.xsrfToken = xsrfToken;
    return xsrfToken;
}

function ensureXsrfToken(user) {
    const session = getUserSession(user);
    if (session.xsrfToken) {
        return session.xsrfToken;
    }
    return seedXsrfTokenForSession(session);
}

function ensureAnonymousXsrfToken() {
    if (anonymousXsrfToken) {
        return anonymousXsrfToken;
    }

    const response = http.get(`${BASE_URL}/auth/github/loginUrl`, {
        tags: { name: "GET auth github loginUrl (anonymous)" },
    });
    const xsrfToken = extractCookieValue(response, "XSRF-TOKEN");

    if (!xsrfToken) {
        return null;
    }

    anonymousXsrfToken = xsrfToken;
    return xsrfToken;
}

function readHeaders(user) {
    const session = getUserSession(user);
    return {
        Cookie: buildAuthCookie(session.accessToken, session.refreshToken),
    };
}

function mutationHeaders(user) {
    const session = getUserSession(user);
    const xsrfToken = ensureXsrfToken(user);

    if (!xsrfToken) {
        fail(`Failed to seed xsrf for user=${user.userId}`);
    }

    return {
        Cookie: buildAuthCookie(session.accessToken, session.refreshToken, xsrfToken),
        "X-XSRF-TOKEN": xsrfToken,
        "Content-Type": "application/json",
    };
}

function reissueTokenWithSession(user) {
    const session = getUserSession(user);
    const xsrfToken = ensureXsrfToken(user);

    if (!xsrfToken) {
        fail(`Failed to seed xsrf for token reissue user=${user.userId}`);
    }

    const response = http.post(`${BASE_URL}/auth/token`, null, {
        headers: {
            Cookie: buildAuthCookie(null, session.refreshToken, xsrfToken),
            "X-XSRF-TOKEN": xsrfToken,
        },
        tags: { name: "POST auth token" },
    });

    const nextAccessToken = extractCookieValue(response, "access_token");
    const nextRefreshToken = extractCookieValue(response, "refresh_token");

    if (response.status === 200 && nextAccessToken && nextRefreshToken) {
        session.accessToken = nextAccessToken;
        session.refreshToken = nextRefreshToken;
    }

    return response;
}

function listResumeSummaries(user, tagName = "GET resumes list") {
    const response = http.get(`${BASE_URL}/resumes?page=0&size=10`, {
        headers: readHeaders(user),
        tags: { name: tagName },
    });

    const ok = check(response, {
        [`${tagName} 200`]: (r) => r.status === 200,
    });

    if (!ok) {
        return [];
    }

    return response.json("data.data") || [];
}

function createResume(user, suffix) {
    const resumeName = buildSafeResumeName("lt-v3-", suffix);
    const response = http.post(
        `${BASE_URL}/resumes`,
        JSON.stringify({
            repoUrls: RESUME_CREATE_REPO_URLS,
            positionId: RESUME_CREATE_POSITION_ID,
            name: resumeName,
        }),
        {
            headers: mutationHeaders(user),
            tags: { name: "POST resumes create" },
        },
    );

    return response;
}

function pickResumeId(user) {
    const items = listResumeSummaries(user, "GET resumes list (for edit)");
    if (!Array.isArray(items) || !items.length) {
        return null;
    }
    const first = items.find((item) => item?.resumeId != null);
    return first?.resumeId ?? null;
}

function renameResume(user, resumeId, suffix) {
    const rename = buildSafeResumeName("lt-name-", suffix);
    return http.patch(
        `${BASE_URL}/resumes/${encodeURIComponent(String(resumeId))}/name`,
        JSON.stringify({ name: rename }),
        {
            headers: mutationHeaders(user),
            tags: { name: "PATCH resumes name" },
        },
    );
}

function updateUserProfile(user, suffix) {
    const safeName = buildSafeUserName(`${user.userId}-${suffix}`);
    return http.patch(
        `${BASE_URL}/user`,
        JSON.stringify({
            name: safeName,
            phonePolicyAgreed: false,
        }),
        {
            headers: mutationHeaders(user),
            tags: { name: "PATCH user profile" },
        },
    );
}

function logout(user) {
    return http.post(`${BASE_URL}/auth/logout`, null, {
        headers: mutationHeaders(user),
        tags: { name: "POST auth logout" },
    });
}

function loadtestLogin(user) {
    const response = http.post(
        `${BASE_URL}${LOADTEST_BASE_PATH}/auth/login`,
        JSON.stringify({ providerUserId: user.providerUserId, returnToken: true }),
        {
            headers: { "Content-Type": "application/json" },
            tags: { name: "POST loadtest auth login" },
        },
    );

    if (response.status !== 200) {
        return response;
    }

    const data = response.json("data") || {};
    const session = getUserSession(user);
    session.accessToken =
        extractCookieValue(response, "access_token") ||
        data.cookieAccessToken ||
        data.accessToken ||
        session.accessToken;
    session.refreshToken =
        extractCookieValue(response, "refresh_token") ||
        data.cookieRefreshToken ||
        data.refreshToken ||
        session.refreshToken;
    session.xsrfToken = null;

    return response;
}

function buildSafeResumeName(prefix, suffix) {
    const normalizedPrefix = String(prefix || "lt-").replace(/\s+/g, "");
    const base = `${normalizedPrefix}${String(suffix ?? "")}`.trim();
    if (!base) {
        return "lt-resume";
    }
    return base.length <= 30 ? base : base.slice(0, 30);
}

function buildSafeUserName(seed) {
    const alnum = String(seed ?? "").replace(/[^0-9A-Za-z]/g, "");
    const candidate = `u${alnum}`.slice(0, 10);
    if (candidate.length >= 2) {
        return candidate;
    }
    return `u${String(__VU || 0)}${String(__ITER || 0)}`.slice(0, 10);
}

export function positionsColdStart() {
    const response = http.get(`${BASE_URL}/positions`, {
        tags: { name: "GET positions cold" },
    });

    positionsColdTrend.add(response.timings.duration);

    check(response, {
        "positions cold 200": (r) => r.status === 200,
    });
}

export function positionsWarmCache() {
    const response = http.get(`${BASE_URL}/positions`, {
        tags: { name: "GET positions warm" },
    });

    positionsWarmTrend.add(response.timings.duration);

    check(response, {
        "positions warm 200": (r) => r.status === 200,
    });

    sleep(Number(__ENV.POSITIONS_WARM_SLEEP_SEC || 0.1));
}

export function refreshTokenRotation(data) {
    const user = getVuUser(data);
    const response = reissueTokenWithSession(user);

    refreshReissueTrend.add(response.timings.duration);

    const success = check(response, {
        "refresh token reissue 200": (r) => r.status === 200,
        "refresh token issued access cookie": (r) => Boolean(extractCookieValue(r, "access_token")),
        "refresh token issued refresh cookie": (r) => Boolean(extractCookieValue(r, "refresh_token")),
    });

    refreshReissueSuccessRate.add(success);

    sleep(Number(__ENV.REFRESH_RTR_SLEEP_SEC || 0.2));
}

export function refreshTokenPenetration() {
    const xsrfToken = ensureAnonymousXsrfToken();

    if (!xsrfToken) {
        fail("Failed to seed anonymous XSRF token for penetration scenario");
    }

    const fakeRefreshToken = `rt-invalid-${TARGET_SCENARIO}-${__VU}-${__ITER}-${Date.now()}`;
    const response = http.post(`${BASE_URL}/auth/token`, null, {
        headers: {
            Cookie: buildAuthCookie(null, fakeRefreshToken, xsrfToken),
            "X-XSRF-TOKEN": xsrfToken,
        },
        tags: { name: "POST auth token invalid" },
    });

    refreshPenetrationTrend.add(response.timings.duration);

    const expected = check(response, {
        "refresh penetration expected error": (r) => [400, 401, 403].includes(r.status),
    });

    penetrationStatusCounter.add(1, { status: String(response.status) });
    refreshPenetrationExpectedRate.add(expected);
}

export function refreshTokenStampede(data) {
    const user = getVuUser(data);
    const response = reissueTokenWithSession(user);

    refreshStampedeTrend.add(response.timings.duration);

    const success = check(response, {
        "stampede refresh 200": (r) => r.status === 200,
    });

    stampedeSuccessRate.add(success);
}

export function sseMassConnection(data) {
    const user = getVuUser(data);
    const timeoutSeconds = Number(__ENV.SSE_TIMEOUT_SEC || 70);

    const response = http.get(`${BASE_URL}/notifications/stream`, {
        headers: {
            ...readHeaders(user),
            Accept: "text/event-stream",
            Connection: "keep-alive",
        },
        timeout: `${timeoutSeconds}s`,
        tags: { name: "GET notifications SSE stream" },
    });

    if (response.status === 0) {
        sseTimeoutCounter.add(1);
    }

    sseConnectTrend.add(response.timings.duration);

    const connected = check(response, {
        "sse connected response": (r) => r.status === 200 || r.status === 0,
    });

    sseConnectedRate.add(connected);
}

export function e2eMixedFlow(data) {
    const startedAt = Date.now();
    const user = getVuUser(data);

    const loginResponse = loadtestLogin(user);
    const loginOk = check(loginResponse, {
        "e2e login 200": (r) => r.status === 200,
    });

    const resumesResponse = http.get(`${BASE_URL}/resumes?page=0&size=10`, {
        headers: readHeaders(user),
        tags: { name: "GET resumes list (e2e)" },
    });
    const resumesOk = check(resumesResponse, {
        "e2e resumes list 200": (r) => r.status === 200,
    });

    let resumeId = null;
    if (resumesOk) {
        const items = resumesResponse.json("data.data") || [];
        resumeId = items?.[0]?.resumeId ?? null;
    }

    if (!resumeId) {
        const createResponse = createResume(user, `${user.userId}-${__ITER}`);
        if (createResponse.status === 201) {
            resumeId = createResponse.json("data");
        }
    }

    let renameOk = true;
    if (resumeId) {
        const renameResponse = renameResume(user, resumeId, `${user.userId}-${__ITER}`);
        renameOk = check(renameResponse, {
            "e2e resume rename 200": (r) => r.status === 200,
        });
    }

    const notificationsResponse = http.get(`${BASE_URL}/notifications?size=10`, {
        headers: readHeaders(user),
        tags: { name: "GET notifications list (e2e)" },
    });
    const notificationsOk = check(notificationsResponse, {
        "e2e notifications 200": (r) => r.status === 200,
    });

    const profileResponse = updateUserProfile(user, `${user.userId}-${__ITER}`);
    const profileOk = check(profileResponse, {
        "e2e user patch 200": (r) => r.status === 200,
    });

    const logoutResponse = logout(user);
    const logoutOk = check(logoutResponse, {
        "e2e logout 200": (r) => r.status === 200,
    });

    const success = loginOk && resumesOk && renameOk && notificationsOk && profileOk && logoutOk;
    e2eSuccessRate.add(success);
    e2eFlowTrend.add(Date.now() - startedAt);

    sleep(Number(__ENV.E2E_SLEEP_SEC || 0.2));
}

function recordResumeConflict(response) {
    resumeConflictStatusCounter.add(1, { status: String(response.status) });
    resumeConflict403Rate.add(response.status === 403);

    check(response, {
        "resume conflict status expected": (r) => [200, 201, 403, 409].includes(r.status),
    });
}

function executeResumeConflict(user) {
    if (RESUME_CONFLICT_OPERATION === "edit") {
        const resumeId = pickResumeId(user);
        if (!resumeId) {
            const fallbackCreate = createResume(user, `${user.userId}-${__ITER}`);
            recordResumeConflict(fallbackCreate);
            return;
        }

        const response = http.patch(
            `${BASE_URL}/resumes/${encodeURIComponent(String(resumeId))}`,
            JSON.stringify({ message: RESUME_EDIT_MESSAGE }),
            {
                headers: mutationHeaders(user),
                tags: { name: "PATCH resumes edit conflict" },
            },
        );
        recordResumeConflict(response);
        return;
    }

    const response = createResume(user, `${user.userId}-${__ITER}`);
    recordResumeConflict(response);
}

export function resumeConflictSameUser(data) {
    const user = getSharedUser(data);
    executeResumeConflict(user);
}

export function resumeConflictMultiUser(data) {
    const user = getVuUser(data);
    executeResumeConflict(user);
}
