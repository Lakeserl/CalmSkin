package com.lakeserl.order_service.service;

import com.lakeserl.order_service.entity.Order;
import com.lakeserl.order_service.entity.OrderStatusHistory;
import com.lakeserl.order_service.enums.OrderStatus;
import com.lakeserl.order_service.exception.InvalidOrderStatusTransitionException;
import com.lakeserl.order_service.repository.OrderRepository;
import com.lakeserl.order_service.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatusServiceImpl implements OrderStatusService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;

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
