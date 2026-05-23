package com.lakeserl.review_service.dto.response;

import com.lakeserl.review_service.enums.MediaType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewMediaDTO {
    private Long id;
    private MediaType mediaType;
    private String url;
    private String thumbnailUrl;
    private short sortOrder;
}
