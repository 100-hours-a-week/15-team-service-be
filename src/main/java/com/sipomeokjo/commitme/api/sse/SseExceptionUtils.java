package com.sipomeokjo.commitme.api.sse;

import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

public final class SseExceptionUtils {
    private static final String BROKEN_PIPE = "broken pipe";
    private static final String CONNECTION_RESET = "connection reset";
    private static final String DISCONNECTED_CLIENT = "disconnected client";

    private SseExceptionUtils() {}

    public static boolean isClientDisconnected(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof AsyncRequestNotUsableException) {
                return true;
            }

            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains(BROKEN_PIPE)
                        || lower.contains(CONNECTION_RESET)
                        || lower.contains(DISCONNECTED_CLIENT)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
