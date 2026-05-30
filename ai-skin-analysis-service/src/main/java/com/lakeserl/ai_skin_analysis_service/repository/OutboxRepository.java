package com.lakeserl.ai_skin_analysis_service.repository;

import com.lakeserl.ai_skin_analysis_service.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc();
}
