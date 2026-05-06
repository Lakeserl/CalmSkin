package com.lakeserl.product_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Data
@Builder
public class ErrorResponse {
    @Builder.Default
    private boolean success = false;
    private String code;
    private String message;
    private LocalDateTime timestamp;
    private String path;

    public static ErrorResponse of(String code, String message, String path) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .build();
    }
}
