package com.lakeserl.promotion_service.service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.lakeserl.promotion_service.dto.request.AssignBulkRequest;
import com.lakeserl.promotion_service.dto.request.GenerateVoucherCodesRequest;
import com.lakeserl.promotion_service.dto.request.VoucherValidationRequest;
import com.lakeserl.promotion_service.dto.response.MyVoucherResponse;
import com.lakeserl.promotion_service.dto.response.VoucherCodeResponse;
import com.lakeserl.promotion_service.dto.response.VoucherInfoResponse;
import com.lakeserl.promotion_service.dto.response.VoucherValidationResponse;

/** Voucher validation, lookup, claiming, assignment and campaign codes. */
public interface VoucherService {

    VoucherValidationResponse validate(VoucherValidationRequest request);

    VoucherInfoResponse codeInfo(String code);

    List<MyVoucherResponse> myVouchers(UUID userId, boolean usableOnly);

    void claim(UUID userId, String code);

    int assignBulk(AssignBulkRequest request);

    void assignSignupBonus(UUID userId);

    /** Bulk-generates single-use campaign codes for a promotion. */
    List<VoucherCodeResponse> generateCodes(Long promotionId, GenerateVoucherCodesRequest request);

    /** Lists every campaign code generated for a promotion. */
    List<VoucherCodeResponse> listCodes(Long promotionId);

    /** Assigns the voucher with the given code to each user, skipping duplicates. */
    int grantByCode(String code, Collection<UUID> userIds, String source);
}
