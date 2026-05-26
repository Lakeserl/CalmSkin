package com.lakeserl.subscription_service.controller;

import com.lakeserl.subscription_service.dto.response.ApiResponse;
import com.lakeserl.subscription_service.dto.response.ApiResponse.PageInfo;
import com.lakeserl.subscription_service.dto.response.SubscriptionDTO;
import com.lakeserl.subscription_service.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/subscriptions")
@RequiredArgsConstructor
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public ApiResponse<List<SubscriptionDTO>> listAll(Pageable pageable) {
        Page<SubscriptionDTO> page = subscriptionService.adminListAll(pageable);
        return ApiResponse.ok("OK", page.getContent(), PageInfo.builder()
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build());
    }

    @GetMapping("/{id}")
    public ApiResponse<SubscriptionDTO> getById(@PathVariable UUID id) {
        return ApiResponse.ok(subscriptionService.adminGetById(id));
    }
}
