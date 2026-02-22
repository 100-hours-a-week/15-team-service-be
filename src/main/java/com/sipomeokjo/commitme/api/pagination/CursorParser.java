package com.sipomeokjo.commitme.api.pagination;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class CursorParser {

    public Cursor parseCompositeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new Cursor(null, null);
        }
        String[] parts = cursor.split("\\|", -1);
        if (parts.length != 2) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR_VALUE);
        }
        try {
            Instant createdAt = Instant.parse(parts[0]);
            Long id = Long.parseLong(parts[1]);
            return new Cursor(createdAt, id);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR_VALUE);
        }
    }

    public Long parseIdCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            long value = Long.parseLong(cursor);
            if (value < 1) {
                throw new NumberFormatException();
            }
            return value;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR_VALUE);
        }
    }

    public record Cursor(Instant createdAt, Long id) {}
}
