package com.lakeserl.promotion_service.dto.response;

import java.time.Instant;

import org.slf4j.MDC;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard response envelope for every promotion-service API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;
    private PageInfo page;
    private Instant timestamp;
    private String requestId;
    private String path;

    public static <T> ApiResponse<T> ok(T data) {
        return ok("OK", data, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ok(message, data, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data, PageInfo page) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message(message)
                .data(data)
                .page(page)
                .timestamp(Instant.now())
                .requestId(MDC.get("requestId"))
                .path(MDC.get("path"))
                .build();
    }

    public static ApiResponse<Void> error(String code, String message) {
        return ApiResponse.<Void>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(null)
                .page(null)
                .timestamp(Instant.now())
                .requestId(MDC.get("requestId"))
                .path(MDC.get("path"))
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo {
        private int currentPage;
        private int pageSize;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }
}
