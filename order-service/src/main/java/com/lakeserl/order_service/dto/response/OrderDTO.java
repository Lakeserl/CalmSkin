package com.lakeserl.order_service.dto.response;

import com.lakeserl.order_service.enums.OrderStatus;
import com.lakeserl.order_service.enums.PaymentMethod;
import com.lakeserl.order_service.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private String orderNumber;
    private UUID userId;

    // Address
    private String shippingName;
    private String shippingPhone;
    private String shippingProvince;
    private String shippingDistrict;
    private String shippingWard;
    private String shippingStreet;

    // Pricing
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private Integer pointsUsed;
    private BigDecimal pointsAmount;
    private BigDecimal totalAmount;

    // Voucher
    private String voucherCode;
    private BigDecimal voucherDiscount;

    // Status / Method
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private String note;
    private String cancelReason;

    // Timestamps
    private LocalDateTime confirmedAt;
    private LocalDateTime paidAt;
    private LocalDateTime preparingAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Sub-objects
    private List<OrderItemDTO> items;
    private List<OrderStatusHistoryDTO> statusHistory;
    
    private PaymentInfoDTO paymentInfo;
    private ShippingInfoDTO shippingInfo;

    // For online payment flows
    private String paymentUrl;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfoDTO {
        private String paymentMethod;
        private PaymentStatus paymentStatus;
        private String transactionId;
        private BigDecimal amount;
        private BigDecimal refundAmount;
        private LocalDateTime paidAt;
        private LocalDateTime refundedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingInfoDTO {
        private String shippingProvider;
        private String trackingNumber;
        private String shippingStatus;
        private LocalDateTime estimatedDelivery;
        private LocalDateTime actualDelivery;
        private BigDecimal shippingFee;
        private String providerOrderId;
    }
}
