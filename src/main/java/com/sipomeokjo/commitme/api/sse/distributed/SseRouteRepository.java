package com.sipomeokjo.commitme.api.sse.distributed;

import java.time.Duration;
import java.util.Set;

public interface SseRouteRepository {
    void upsertRoute(SseRouteKey routeKey, String instanceId, Duration ttl);

    void removeRoute(SseRouteKey routeKey, String instanceId);

    Set<String> findInstanceIds(SseRouteKey routeKey);
}
