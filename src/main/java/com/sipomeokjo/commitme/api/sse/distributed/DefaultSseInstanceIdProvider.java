package com.sipomeokjo.commitme.api.sse.distributed;

import java.net.InetAddress;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class DefaultSseInstanceIdProvider implements SseInstanceIdProvider {
    private final String instanceId;

    public DefaultSseInstanceIdProvider(
            @Value("${app.sse.instance-id:}") String configuredInstanceId) {
        if (StringUtils.hasText(configuredInstanceId)) {
            this.instanceId = configuredInstanceId.trim();
            log.info("[SSE_INSTANCE] configured_instance_id instanceId={}", this.instanceId);
            return;
        }

        this.instanceId = buildFallbackInstanceId();
        log.info("[SSE_INSTANCE] generated_instance_id instanceId={}", this.instanceId);
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    private String buildFallbackInstanceId() {
        String hostName = "unknown-host";
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            log.debug("[SSE_INSTANCE] hostname_lookup_failed", ex);
        }
        long pid = ProcessHandle.current().pid();
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        return hostName + "-" + pid + "-" + randomSuffix;
    }
}
