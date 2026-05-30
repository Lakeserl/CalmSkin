package com.lakeserl.ai_skin_analysis_service.repository;

import com.lakeserl.ai_skin_analysis_service.entity.ProcessedKafkaEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedKafkaEventRepository extends JpaRepository<ProcessedKafkaEvent, String> {
}
