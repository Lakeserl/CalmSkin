package com.lakeserl.ai_chatbot_service.repository;

import com.lakeserl.ai_chatbot_service.entity.AIUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIUsageLogRepository extends JpaRepository<AIUsageLog, Long> {
}
