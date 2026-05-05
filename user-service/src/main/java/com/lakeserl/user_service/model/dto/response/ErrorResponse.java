package com.lakeserl.user_service.model.dto.response;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import lombok.Builder;
import lombok.Data;

@Data 
@Builder
public class ErrorResponse {
    private boolean success = false;
    private String code;
    private String message;
    private LocalDateTime timestamp;
    private String path;

    public static ErrorResponse of(String code, String message, String path) {
        return ErrorResponse.builder()
                .code(code).message(message).path(path)
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .build();
    }
}