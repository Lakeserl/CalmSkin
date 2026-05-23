package com.lakeserl.review_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PresignResponse {
    private String uploadUrl;
    private String mediaUrl;
}
