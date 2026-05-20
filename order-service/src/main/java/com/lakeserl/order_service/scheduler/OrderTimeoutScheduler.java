package com.lakeserl.order_service.scheduler;

import com.lakeserl.order_service.entity.Order;
import com.lakeserl.order_service.enums.OrderStatus;
import com.lakeserl.order_service.repository.OrderRepository;
import com.lakeserl.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Value("${app.order.timeout-minutes:15}")
    private int timeoutMinutes;

    @Scheduled(fixedDelay = 60000) // Every 60 seconds
    public void timeoutPendingOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, threshold);

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("Found {} pending orders that exceeded timeout of {} minutes", expiredOrders.size(), timeoutMinutes);
        for (Order order : expiredOrders) {
            try {
                log.info("Timing out order orderNumber={} (created={})", order.getOrderNumber(), order.getCreatedAt());
                orderService.cancelOrderSystem(order.getOrderNumber(), "Payment timeout. Exceeded limit of " + timeoutMinutes + " minutes.");
            } catch (Exception e) {
                log.error("Failed to timeout order orderNumber={} due to: {}", order.getOrderNumber(), e.getMessage());
            }
        }
    }
}
