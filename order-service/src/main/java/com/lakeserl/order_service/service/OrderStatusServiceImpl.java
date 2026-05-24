package com.lakeserl.order_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.order_service.entity.Order;
import com.lakeserl.order_service.entity.OrderItem;
import com.lakeserl.order_service.entity.OrderStatusHistory;
import com.lakeserl.order_service.entity.OutboxEvent;
import com.lakeserl.order_service.enums.OrderStatus;
import com.lakeserl.order_service.enums.OutboxStatus;
import com.lakeserl.order_service.exception.InvalidOrderStatusTransitionException;
import com.lakeserl.order_service.repository.OrderRepository;
import com.lakeserl.order_service.repository.OrderStatusHistoryRepository;
import com.lakeserl.order_service.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatusServiceImpl implements OrderStatusService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.order.points-earn-rate:1000}")
    private int pointsEarnRate;

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),
            OrderStatus.PAID, Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
            OrderStatus.PREPARING, Set.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED),
            OrderStatus.SHIPPING, Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED),
            OrderStatus.DELIVERED, Set.of(OrderStatus.RETURN_REQUESTED),
            OrderStatus.RETURN_REQUESTED, Set.of(OrderStatus.RETURNED, OrderStatus.DELIVERED),
            OrderStatus.CANCELLED, Set.of(),
            OrderStatus.RETURNED, Set.of()
    );

    @Override
    @Transactional
    public void transitionTo(Order order, OrderStatus nextStatus, String changedBy, String reason, String metadata) {
        OrderStatus currentStatus = order.getStatus();
        validateTransition(currentStatus, nextStatus);

        log.info("Transitioning orderNumber={} from {} to {} (changedBy={}, reason={})",
                order.getOrderNumber(), currentStatus, nextStatus, changedBy, reason);

        order.setStatus(nextStatus);
        updateOrderTimestamp(order, nextStatus);
        orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .fromStatus(currentStatus.name())
                .toStatus(nextStatus.name())
                .changedBy(changedBy)
                .reason(reason)
                .metadata(metadata)
                .build();
        statusHistoryRepository.save(history);

        if (nextStatus == OrderStatus.DELIVERED) {
            publishOrderCompleted(order);
        }
    }

    private void publishOrderCompleted(Order order) {
        List<Map<String, Object>> itemPayloads = order.getItems().stream()
                .map(this::toItemPayload)
                .collect(Collectors.toList());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.getId().toString());
        payload.put("orderNumber", order.getOrderNumber());
        payload.put("userId", order.getUserId());
        payload.put("pointsEarned", order.getTotalAmount()
                .divide(BigDecimal.valueOf(pointsEarnRate)).intValue());
        payload.put("items", itemPayloads);

        try {
            String body = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(order.getId().toString())
                    .eventType("order.completed")
                    .payload(body)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();
            outboxRepository.save(event);
        } catch (JsonProcessingException e) {
            // Surface as runtime so the surrounding @Transactional rolls back
            // the status change rather than silently losing the downstream event.
            throw new IllegalStateException("Failed to serialize order.completed payload for orderId=" + order.getId(), e);
        }
    }

    private Map<String, Object> toItemPayload(OrderItem item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("orderItemId", item.getId());
        map.put("productId", item.getProductId());
        map.put("variantId", item.getVariantId() == null ? "" : item.getVariantId().toString());
        map.put("quantity", item.getQuantity());
        return map;
    }

    @Override
    @Transactional
    public void transitionTo(Order order, OrderStatus nextStatus, String changedBy, String reason) {
        transitionTo(order, nextStatus, changedBy, reason, null);
    }

    @Override
    public void validateTransition(OrderStatus current, OrderStatus next) {
        if (current == next) {
            return;
        }
        Set<OrderStatus> allowed = VALID_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(next)) {
            throw new InvalidOrderStatusTransitionException(current.name(), next.name());
        }
    }

    private void updateOrderTimestamp(Order order, OrderStatus status) {
        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case CONFIRMED -> order.setConfirmedAt(now);
            case PAID -> order.setPaidAt(now);
            case PREPARING -> order.setPreparingAt(now);
            case SHIPPING -> order.setShippedAt(now);
            case DELIVERED -> order.setDeliveredAt(now);
            case CANCELLED -> order.setCancelledAt(now);
            default -> {}
        }
    }
}
