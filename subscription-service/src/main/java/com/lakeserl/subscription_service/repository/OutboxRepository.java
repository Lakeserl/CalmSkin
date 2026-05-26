package com.lakeserl.subscription_service.repository;

import com.lakeserl.subscription_service.entity.OutboxEvent;
import com.lakeserl.subscription_service.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
