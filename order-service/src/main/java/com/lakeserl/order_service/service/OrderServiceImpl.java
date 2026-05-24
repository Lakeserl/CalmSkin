package com.lakeserl.order_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.order_service.client.*;
import com.lakeserl.order_service.dto.request.CreateOrderRequest;
import com.lakeserl.order_service.dto.request.ReturnOrderRequest;
import com.lakeserl.order_service.dto.response.OrderDTO;
import com.lakeserl.order_service.dto.response.OrderStatsDTO;
import com.lakeserl.order_service.dto.response.OrderSummaryDTO;
import com.lakeserl.order_service.entity.*;
import com.lakeserl.order_service.enums.OrderStatus;
import com.lakeserl.order_service.enums.OutboxStatus;
import com.lakeserl.order_service.enums.PaymentMethod;
import com.lakeserl.order_service.enums.PaymentStatus;
import com.lakeserl.order_service.exception.*;
import com.lakeserl.order_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderPaymentInfoRepository paymentInfoRepository;
    private final OrderShippingInfoRepository shippingInfoRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final OutboxRepository outboxRepository;

    private final OrderStatusService orderStatusService;
    private final OrderPricingService orderPricingService;

    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final PromotionServiceClient promotionServiceClient;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.order.default-shipping-fee:30000}")
    private double defaultShippingFee;

    @Override
    @Transactional
    public OrderDTO createOrder(UUID userId, CreateOrderRequest request) {
        // 1. Redis lock
        String lockKey = "lock:order:user:" + userId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(5));
        if (Boolean.FALSE.equals(acquired)) {
            throw new DuplicateOrderException("Please wait 5 seconds between placing orders.");
        }

        try {
            // 2. Fetch shipping details
            UserServiceClient.AddressResponse address = userServiceClient.getAddress(userId, request.addressId());

            // 3. Validate products via product-service
            List<ProductServiceClient.ProductValidationRequestItem> productReqItems = request.items().stream()
                    .map(item -> ProductServiceClient.ProductValidationRequestItem.builder()
                            .productId(item.productId())
                            .variantId(item.variantId())
                            .quantity(item.quantity())
                            .build())
                    .toList();

            List<ProductServiceClient.ProductValidationItem> validatedProducts = productServiceClient.validateProducts(productReqItems);
            
            // Map validation results for quick lookup and verify availability
            Map<String, ProductServiceClient.ProductValidationItem> validatedMap = new HashMap<>();
            for (ProductServiceClient.ProductValidationItem item : validatedProducts) {
                if (!item.isAvailable()) {
                    throw new ProductNotAvailableException("Product " + item.getProductName() + " is currently unavailable.");
                }
                String key = item.getProductId() + ":" + (item.getVariantId() == null ? "default" : item.getVariantId());
                validatedMap.put(key, item);
            }

            // 4. Synchronous precheck inventory
            List<InventoryServiceClient.StockCheckRequestItem> stockReqItems = request.items().stream()
                    .map(item -> InventoryServiceClient.StockCheckRequestItem.builder()
                            .productId(item.productId())
                            .variantId(item.variantId())
                            .quantity(item.quantity())
                            .build())
                    .toList();

            List<InventoryServiceClient.StockCheckItem> stockStatusList = inventoryServiceClient.checkStockBatch(stockReqItems);
            for (InventoryServiceClient.StockCheckItem stockItem : stockStatusList) {
                if (!stockItem.isAvailable()) {
                    throw new InsufficientStockException("Insufficient stock for product id " + stockItem.getProductId() 
                            + " (available: " + stockItem.getQuantityAvailable() + ")");
                }
            }

            // 5. Build order items and calculate subtotal
            List<OrderItem> orderItems = new ArrayList<>();
            for (CreateOrderRequest.OrderItemRequest itemReq : request.items()) {
                String key = itemReq.productId() + ":" + (itemReq.variantId() == null ? "default" : itemReq.variantId());
                ProductServiceClient.ProductValidationItem valItem = validatedMap.get(key);
                if (valItem == null) {
                    throw new ProductNotAvailableException("Failed to validate product/variant configurations.");
                }

                OrderItem orderItem = OrderItem.builder()
                        .productId(itemReq.productId())
                        .variantId(itemReq.variantId())
                        .productName(valItem.getProductName())
                        .productSku(valItem.getProductSku())
                        .variantName(valItem.getVariantName())
                        .productImageUrl(valItem.getProductImageUrl())
                        .brandName(valItem.getBrandName())
                        .unitPrice(valItem.getUnitPrice())
                        .quantity(itemReq.quantity())
                        .subtotal(valItem.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.quantity())))
                        .build();
                orderItems.add(orderItem);
            }

            BigDecimal subtotal = orderPricingService.calculateSubtotal(orderItems);

            // 6. Validate promotion voucher
            BigDecimal voucherDiscount = BigDecimal.ZERO;
            String voucherCode = request.voucherCode();
            if (voucherCode != null && !voucherCode.isBlank()) {
                PromotionServiceClient.VoucherValidationResponse voucherResp = promotionServiceClient.validateVoucher(
                        new PromotionServiceClient.VoucherValidationRequest(voucherCode, subtotal, userId));
                if (voucherResp.isValid()) {
                    voucherDiscount = voucherResp.getDiscountAmount();
                } else {
                    throw new InvalidVoucherException("Voucher is invalid: " + voucherResp.getReason());
                }
            }

            // 7. Validate points
            int pointsUsed = 0;
            BigDecimal pointsAmount = BigDecimal.ZERO;
            if (request.pointsToUse() != null && request.pointsToUse() > 0) {
                UserServiceClient.PointsResponse pointsResp = userServiceClient.getPoints(userId);
                if (request.pointsToUse() > pointsResp.getPoints()) {
                    throw new InsufficientStockException("User only has " + pointsResp.getPoints() + " points, but requested to use " + request.pointsToUse());
                }
                pointsUsed = request.pointsToUse();
                pointsAmount = orderPricingService.calculatePointsAmount(pointsUsed);
                if (pointsAmount.compareTo(subtotal) > 0) {
                    pointsAmount = subtotal; // capped at subtotal
                }
            }

            // 8. Shipping fee
            BigDecimal shippingFee = BigDecimal.valueOf(defaultShippingFee);

            // 9. Total amount
            BigDecimal totalAmount = orderPricingService.calculateTotal(subtotal, shippingFee, voucherDiscount, pointsAmount);

            // 10. Generate order number
            String orderNumber = generateOrderNumber();

            PaymentMethod paymentMethod = PaymentMethod.valueOf(request.paymentMethod().toUpperCase());

            // 11. Save Order entity
            Order order = Order.builder()
                    .orderNumber(orderNumber)
                    .userId(userId)
                    .shippingName(address.getRecipientName())
                    .shippingPhone(address.getPhone())
                    .shippingProvince(address.getProvince())
                    .shippingDistrict(address.getDistrict())
                    .shippingWard(address.getWard())
                    .shippingStreet(address.getStreet())
                    .subtotal(subtotal)
                    .discountAmount(voucherDiscount)
                    .shippingFee(shippingFee)
                    .pointsUsed(pointsUsed)
                    .pointsAmount(pointsAmount)
                    .totalAmount(totalAmount)
                    .voucherCode(voucherCode)
                    .voucherDiscount(voucherDiscount)
                    .status(OrderStatus.PENDING)
                    .paymentMethod(paymentMethod)
                    .note(request.note())
                    .build();

            for (OrderItem item : orderItems) {
                order.addItem(item);
            }

            orderRepository.save(order);

            // 12. Save Order Payment Info
            OrderPaymentInfo paymentInfo = OrderPaymentInfo.builder()
                    .order(order)
                    .paymentMethod(paymentMethod.name())
                    .paymentStatus(PaymentStatus.PENDING)
                    .amount(totalAmount)
                    .build();
            paymentInfoRepository.save(paymentInfo);

            // 13. Save Order Shipping Info
            OrderShippingInfo shippingInfo = OrderShippingInfo.builder()
                    .order(order)
                    .shippingFee(shippingFee)
                    .shippingStatus("PENDING")
                    .build();
            shippingInfoRepository.save(shippingInfo);

            // 14. Save initial state audit
            OrderStatusHistory history = OrderStatusHistory.builder()
                    .order(order)
                    .fromStatus(null)
                    .toStatus(OrderStatus.PENDING.name())
                    .changedBy("system")
                    .reason("Order created by customer")
                    .build();
            statusHistoryRepository.save(history);

            // 15. Transactional Outbox Event for Saga orchestration (order.created)
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("orderId", order.getId().toString());
            eventPayload.put("orderNumber", orderNumber);
            eventPayload.put("userId", userId);
            eventPayload.put("totalAmount", totalAmount);
            eventPayload.put("paymentMethod", paymentMethod.name());
            eventPayload.put("pointsUsed", pointsUsed);
            eventPayload.put("items", orderItems.stream()
                    .map(item -> Map.of(
                            "productId", item.getProductId(),
                            "variantId", item.getVariantId() == null ? "" : item.getVariantId().toString(),
                            "sku", item.getProductSku(),
                            "quantity", item.getQuantity()
                    )).toList());

            publishOutboxEvent("Order", order.getId().toString(), "order.created", eventPayload);

            // 16. Generate paymentUrl if online method
            String paymentUrl = null;
            if (paymentMethod != PaymentMethod.COD && paymentMethod != PaymentMethod.FREE && paymentMethod != PaymentMethod.POINTS) {
                paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?order=" + orderNumber + "&amount=" + totalAmount;
            }

            return mapToDTO(order, paymentInfo, shippingInfo, List.of(history), paymentUrl);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload", e);
            throw new IllegalStateException("Failed to place order due to serialization failure");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderDetail(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order with number " + orderNumber + " not found."));

        OrderPaymentInfo paymentInfo = paymentInfoRepository.findByOrderId(order.getId()).orElse(null);
        OrderShippingInfo shippingInfo = shippingInfoRepository.findByOrderId(order.getId()).orElse(null);
        List<OrderStatusHistory> history = statusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(order.getId());

        return mapToDTO(order, paymentInfo, shippingInfo, history, null);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderDetail(String orderNumber, UUID userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order with number " + orderNumber + " not found."));

        if (!order.getUserId().equals(userId)) {
            throw new OrderNotBelongToUserException("You do not have permission to access order " + orderNumber);
        }

        OrderPaymentInfo paymentInfo = paymentInfoRepository.findByOrderId(order.getId()).orElse(null);
        OrderShippingInfo shippingInfo = shippingInfoRepository.findByOrderId(order.getId()).orElse(null);
        List<OrderStatusHistory> history = statusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(order.getId());

        return mapToDTO(order, paymentInfo, shippingInfo, history, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> getUserOrders(UUID userId, OrderStatus status, Pageable pageable) {
        Page<Order> orders;
        if (status == null) {
            orders = orderRepository.findByUserId(userId, pageable);
        } else {
            orders = orderRepository.findByUserIdAndStatus(userId, status, pageable);
        }
        return orders.map(this::mapToSummaryDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> getAllOrdersAdmin(OrderStatus status, UUID userId, String orderNumber, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        Page<Order> orders = orderRepository.findAllWithFilters(status, userId, orderNumber, fromDate, toDate, pageable);
        return orders.map(this::mapToSummaryDTO);
    }

    @Override
    @Transactional
    public void cancelOrder(String orderNumber, UUID userId, String reason) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order with number " + orderNumber + " not found."));

        if (!order.getUserId().equals(userId)) {
            throw new OrderNotBelongToUserException("You do not have permission to cancel order " + orderNumber);
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new OrderNotCancellableException("Order in " + order.getStatus() + " status cannot be cancelled.");
        }

        try {
            orderStatusService.transitionTo(order, OrderStatus.CANCELLED, "user-" + userId, reason);
            order.setCancelReason(reason);
            orderRepository.save(order);

            // Update payment info if exists
            paymentInfoRepository.findByOrderId(order.getId()).ifPresent(payment -> {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                paymentInfoRepository.save(payment);
            });

            // Publish cancellation outbox event
            publishOutboxEvent("Order", order.getId().toString(), "order.cancelled", Map.of(
                    "orderId", order.getId().toString(),
                    "orderNumber", order.getOrderNumber(),
                    "userId", userId,
                    "pointsUsed", order.getPointsUsed(),
                    "reason", reason
            ));

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload", e);
            throw new IllegalStateException("Failed to cancel order due to event serialization failure");
        }
    }

    @Override
    @Transactional
    public void cancelOrderSystem(String orderNumber, String reason) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order with number " + orderNumber + " not found."));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return; // already cancelled
        }

        try {
            orderStatusService.transitionTo(order, OrderStatus.CANCELLED, "system", reason);
            order.setCancelReason(reason);
            orderRepository.save(order);

            paymentInfoRepository.findByOrderId(order.getId()).ifPresent(payment -> {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                paymentInfoRepository.save(payment);
            });

            publishOutboxEvent("Order", order.getId().toString(), "order.cancelled", Map.of(
                    "orderId", order.getId().toString(),
                    "orderNumber", order.getOrderNumber(),
                    "userId", order.getUserId(),
                    "pointsUsed", order.getPointsUsed(),
                    "reason", reason
            ));

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload", e);
            throw new IllegalStateException("Failed to cancel order due to event serialization failure");
        }
    }

    @Override
    @Transactional
    public void updateOrderStatusAdmin(String orderNumber, String status, String note, String adminUsername) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order with number " + orderNumber + " not found."));

        OrderStatus nextStatus = OrderStatus.valueOf(status.toUpperCase());
        orderStatusService.transitionTo(order, nextStatus, adminUsername, note);

        // Sync payment status if DELIVERED. The order.completed Kafka event is
        // emitted from OrderStatusServiceImpl.transitionTo so every DELIVERED path
        // (admin manual + shipping webhook) publishes exactly once.
        if (nextStatus == OrderStatus.DELIVERED) {
            paymentInfoRepository.findByOrderId(order.getId()).ifPresent(payment -> {
                if (payment.getPaymentStatus() == PaymentStatus.PENDING) {
                    payment.setPaymentStatus(PaymentStatus.COMPLETED);
                    payment.setPaidAt(LocalDateTime.now());
                    paymentInfoRepository.save(payment);
                }
            });
        }
    }

    @Override
    @Transactional
    public void requestReturn(String orderNumber, UUID userId, ReturnOrderRequest request) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order with number " + orderNumber + " not found."));

        if (!order.getUserId().equals(userId)) {
            throw new OrderNotBelongToUserException("You do not have permission to return order " + orderNumber);
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new OrderNotCancellableException("Only delivered orders can be returned.");
        }

        if (order.getDeliveredAt() != null && order.getDeliveredAt().isBefore(LocalDateTime.now().minusDays(7))) {
            throw new OrderNotCancellableException("Return policy expired. Returns are only allowed within 7 days of delivery.");
        }

        orderStatusService.transitionTo(order, OrderStatus.RETURN_REQUESTED, "user-" + userId, request.reason());
    }

    @Override
    @Transactional
    public void confirmReturnAdmin(String orderNumber, String adminUsername) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order with number " + orderNumber + " not found."));

        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new OrderNotCancellableException("Order must be in RETURN_REQUESTED status to be returned.");
        }

        try {
            orderStatusService.transitionTo(order, OrderStatus.RETURNED, adminUsername, "Return approved by Admin");

            paymentInfoRepository.findByOrderId(order.getId()).ifPresent(payment -> {
                payment.setPaymentStatus(PaymentStatus.REFUNDED);
                payment.setRefundAmount(payment.getAmount());
                payment.setRefundedAt(LocalDateTime.now());
                paymentInfoRepository.save(payment);
            });

            publishOutboxEvent("Order", order.getId().toString(), "order.returned", Map.of(
                    "orderId", order.getId().toString(),
                    "orderNumber", order.getOrderNumber(),
                    "userId", order.getUserId(),
                    "items", order.getItems().stream()
                            .map(item -> Map.of(
                                    "productId", item.getProductId(),
                                    "variantId", item.getVariantId() == null ? "" : item.getVariantId().toString(),
                                    "quantity", item.getQuantity()
                            )).toList()
            ));

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload", e);
            throw new IllegalStateException("Failed to return order due to event serialization failure");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getPurchasedProductIds(UUID userId, LocalDateTime since) {
        return orderRepository.findDeliveredProductIdsByUserSince(userId, since);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderStatsDTO getOrderStatsAdmin() {
        long totalOrders = orderRepository.count();
        
        BigDecimal totalRevenue = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.SHIPPING || o.getStatus() == OrderStatus.PAID || o.getStatus() == OrderStatus.PREPARING)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> statsMap = new HashMap<>();
        List<Object[]> statusCounts = orderRepository.countOrdersByStatus();
        for (Object[] statusCount : statusCounts) {
            OrderStatus status = (OrderStatus) statusCount[0];
            Long count = (Long) statusCount[1];
            statsMap.put(status.name(), count);
        }

        BigDecimal averageOrderValue = totalOrders == 0 ? BigDecimal.ZERO : totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, BigDecimal.ROUND_HALF_UP);

        return OrderStatsDTO.builder()
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .byStatus(statsMap)
                .averageOrderValue(averageOrderValue)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportOrdersCsv(OrderStatus status, UUID userId, String orderNumber, LocalDateTime fromDate, LocalDateTime toDate) {
        List<Order> orders = orderRepository.findAllWithFilters(status, userId, orderNumber, fromDate, toDate, Pageable.unpaged()).getContent();
        
        StringBuilder csv = new StringBuilder();
        csv.append("Order Number,User ID,Recipient Name,Phone,Address,Subtotal,Discount,Shipping Fee,Points Used,Points Amount,Total Amount,Voucher Code,Status,Payment Method,Created At\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Order order : orders) {
            String address = String.format("%s, %s, %s, %s, VN",
                    order.getShippingStreet(), order.getShippingWard(),
                    order.getShippingDistrict(), order.getShippingProvince());

            csv.append(csvField(order.getOrderNumber())).append(",")
                    .append(csvField(order.getUserId().toString())).append(",")
                    .append(csvField(order.getShippingName())).append(",")
                    .append(csvField(order.getShippingPhone())).append(",")
                    .append(csvField(address)).append(",")
                    .append(order.getSubtotal()).append(",")
                    .append(order.getDiscountAmount()).append(",")
                    .append(order.getShippingFee()).append(",")
                    .append(order.getPointsUsed()).append(",")
                    .append(order.getPointsAmount()).append(",")
                    .append(order.getTotalAmount()).append(",")
                    .append(csvField(order.getVoucherCode() == null ? "" : order.getVoucherCode())).append(",")
                    .append(order.getStatus().name()).append(",")
                    .append(order.getPaymentMethod().name()).append(",")
                    .append(order.getCreatedAt().format(formatter)).append("\n");
        }
        return csv.toString();
    }

    // RFC 4180: quote the field and escape any internal double-quotes.
    // Also prefix formula-trigger characters (=, +, -, @) to prevent CSV injection in spreadsheets.
    private String csvField(String value) {
        if (value == null) return "\"\"";
        String safe = value;
        if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    // Helper mappings
    private String generateOrderNumber() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randCode = RandomStringUtils.randomNumeric(6);
        return "CS-" + dateStr + "-" + randCode;
    }

    private void publishOutboxEvent(String aggregateType, String aggregateId, String eventType, Object payload) throws JsonProcessingException {
        String body = objectMapper.writeValueAsString(payload);
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(body)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
        outboxRepository.save(event);
    }

    private OrderSummaryDTO mapToSummaryDTO(Order order) {
        int totalItems = order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        return OrderSummaryDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .shippingName(order.getShippingName())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .createdAt(order.getCreatedAt())
                .totalItems(totalItems)
                .build();
    }

    private OrderDTO mapToDTO(Order order, OrderPaymentInfo payment, OrderShippingInfo shipping, List<OrderStatusHistory> history, String paymentUrl) {
        List<com.lakeserl.order_service.dto.response.OrderItemDTO> itemsDTO = order.getItems().stream()
                .map(item -> com.lakeserl.order_service.dto.response.OrderItemDTO.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .variantId(item.getVariantId())
                        .productName(item.getProductName())
                        .productSku(item.getProductSku())
                        .variantName(item.getVariantName())
                        .productImageUrl(item.getProductImageUrl())
                        .brandName(item.getBrandName())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();

        List<com.lakeserl.order_service.dto.response.OrderStatusHistoryDTO> historyDTO = history.stream()
                .map(h -> com.lakeserl.order_service.dto.response.OrderStatusHistoryDTO.builder()
                        .id(h.getId())
                        .fromStatus(h.getFromStatus())
                        .toStatus(h.getToStatus())
                        .changedBy(h.getChangedBy())
                        .reason(h.getReason())
                        .metadata(h.getMetadata())
                        .createdAt(h.getCreatedAt())
                        .build())
                .toList();

        OrderDTO.PaymentInfoDTO pInfo = null;
        if (payment != null) {
            pInfo = OrderDTO.PaymentInfoDTO.builder()
                    .paymentMethod(payment.getPaymentMethod())
                    .paymentStatus(payment.getPaymentStatus())
                    .transactionId(payment.getTransactionId())
                    .amount(payment.getAmount())
                    .refundAmount(payment.getRefundAmount())
                    .paidAt(payment.getPaidAt())
                    .refundedAt(payment.getRefundedAt())
                    .build();
        }

        OrderDTO.ShippingInfoDTO sInfo = null;
        if (shipping != null) {
            sInfo = OrderDTO.ShippingInfoDTO.builder()
                    .shippingProvider(shipping.getShippingProvider())
                    .trackingNumber(shipping.getTrackingNumber())
                    .shippingStatus(shipping.getShippingStatus())
                    .estimatedDelivery(shipping.getEstimatedDelivery())
                    .actualDelivery(shipping.getActualDelivery())
                    .shippingFee(shipping.getShippingFee())
                    .providerOrderId(shipping.getProviderOrderId())
                    .build();
        }

        return OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .shippingName(order.getShippingName())
                .shippingPhone(order.getShippingPhone())
                .shippingProvince(order.getShippingProvince())
                .shippingDistrict(order.getShippingDistrict())
                .shippingWard(order.getShippingWard())
                .shippingStreet(order.getShippingStreet())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .pointsUsed(order.getPointsUsed())
                .pointsAmount(order.getPointsAmount())
                .totalAmount(order.getTotalAmount())
                .voucherCode(order.getVoucherCode())
                .voucherDiscount(order.getVoucherDiscount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .note(order.getNote())
                .cancelReason(order.getCancelReason())
                .confirmedAt(order.getConfirmedAt())
                .paidAt(order.getPaidAt())
                .preparingAt(order.getPreparingAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemsDTO)
                .statusHistory(historyDTO)
                .paymentInfo(pInfo)
                .shippingInfo(sInfo)
                .paymentUrl(paymentUrl)
                .build();
    }
}
