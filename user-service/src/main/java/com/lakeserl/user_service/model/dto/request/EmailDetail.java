package com.lakeserl.user_service.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailDetail {
    private String recipient;
    private String message;
    private String subject;
    private String attachment;
}
