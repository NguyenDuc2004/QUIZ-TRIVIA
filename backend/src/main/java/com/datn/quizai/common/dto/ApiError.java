package com.datn.quizai.common.dto;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Response lỗi chuẩn của toàn hệ thống (docs/api.md §10).
 *
 * <pre>
 * { "timestamp": "...", "status": 400, "error": "Bad Request",
 *   "message": "...", "path": "...", "traceId": "..." }
 * </pre>
 *
 * @param fieldErrors chỉ có khi lỗi validate: tên field → thông báo
 */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        String traceId,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, String message, String path, String traceId) {
        return new ApiError(OffsetDateTime.now(), status, error, message, path, traceId, null);
    }

    public static ApiError validation(String message, String path, String traceId, Map<String, String> fieldErrors) {
        return new ApiError(OffsetDateTime.now(), 400, "Bad Request", message, path, traceId, fieldErrors);
    }
}
