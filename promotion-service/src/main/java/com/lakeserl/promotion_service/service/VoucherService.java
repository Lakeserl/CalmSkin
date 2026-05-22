package com.lakeserl.promotion_service.service;

import java.util.List;
import java.util.UUID;

import com.lakeserl.promotion_service.dto.request.AssignBulkRequest;
import com.lakeserl.promotion_service.dto.request.VoucherValidationRequest;
import com.lakeserl.promotion_service.dto.response.MyVoucherResponse;
import com.lakeserl.promotion_service.dto.response.VoucherInfoResponse;
import com.lakeserl.promotion_service.dto.response.VoucherValidationResponse;

/** Voucher validation, lookup, claiming and assignment. */
public interface VoucherService {

    VoucherValidationResponse validate(VoucherValidationRequest request);

    VoucherInfoResponse codeInfo(String code);

    List<MyVoucherResponse> myVouchers(UUID userId, boolean usableOnly);

    void claim(UUID userId, String code);

    int assignBulk(AssignBulkRequest request);

    void assignSignupBonus(UUID userId);
}
