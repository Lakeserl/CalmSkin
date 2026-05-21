package com.lakeserl.notification_service.support;

import org.springframework.data.domain.Page;

import com.lakeserl.notification_service.dto.response.ApiResponse;

/** Builds the {@link ApiResponse.PageInfo} envelope from a Spring Data page. */
public final class Pages {

    private Pages() {
    }

    public static ApiResponse.PageInfo info(Page<?> page) {
        return ApiResponse.PageInfo.builder()
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
