package com.lakeserl.payment_service.models.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.lakeserl.payment_service.models.dto.response.PaymentDTO;
import com.lakeserl.payment_service.models.dto.response.PaymentInitResponse;
import com.lakeserl.payment_service.models.dto.response.RefundDTO;
import com.lakeserl.payment_service.models.entity.Payment;
import com.lakeserl.payment_service.models.entity.Refund;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    PaymentDTO toDTO(Payment payment);

    @Mapping(target = "paymentNumber", source = "paymentNumber")
    @Mapping(target = "paymentUrl", source = "paymentUrl")
    @Mapping(target = "status", source = "status")
    PaymentInitResponse toInitResponse(Payment payment);

    RefundDTO toRefundDTO(Refund refund);
}
