package com.lakeserl.promotion_service.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.lakeserl.promotion_service.dto.request.CreatePromotionRequest;
import com.lakeserl.promotion_service.dto.request.LockRequest;
import com.lakeserl.promotion_service.dto.request.PreviewRequest;
import com.lakeserl.promotion_service.dto.request.UpdatePromotionRequest;
import com.lakeserl.promotion_service.dto.response.ConfirmResponse;
import com.lakeserl.promotion_service.dto.response.LockResponse;
import com.lakeserl.promotion_service.dto.response.PreviewResponse;
import com.lakeserl.promotion_service.dto.response.PromotionResponse;
import com.lakeserl.promotion_service.dto.response.PromotionStatsResponse;
import com.lakeserl.promotion_service.dto.response.PromotionSummaryResponse;
import com.lakeserl.promotion_service.dto.response.ReleaseResponse;
import com.lakeserl.promotion_service.entity.PromotionUsage;
import com.lakeserl.promotion_service.enums.PromotionStatus;
import com.lakeserl.promotion_service.enums.PromotionType;

/** Promotion lifecycle, admin management and the order-facing apply flow. */
public interface PromotionService {

    PromotionResponse create(CreatePromotionRequest request, UUID createdBy);

    PromotionResponse update(Long id, UpdatePromotionRequest request);

    PromotionResponse updateStatus(Long id, PromotionStatus status);

    PromotionResponse getById(Long id);

    Page<PromotionResponse> search(PromotionType type, PromotionStatus status, Pageable pageable);

    List<PromotionSummaryResponse> activePromotions();

    PreviewResponse preview(PreviewRequest request);

    LockResponse lock(LockRequest request);

    ConfirmResponse confirm(String orderId);

    ReleaseResponse release(String orderId);

    PromotionStatsResponse stats(Long promotionId);

    Page<PromotionUsage> usages(Long promotionId, Pageable pageable);
}
