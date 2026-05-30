package com.lakeserl.ai_skin_analysis_service.repository;

import com.lakeserl.ai_skin_analysis_service.entity.AIUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface AIUsageLogRepository extends JpaRepository<AIUsageLog, Long> {

    @Query("SELECT COALESCE(SUM(l.costUsd), 0) FROM AIUsageLog l WHERE l.success = true")
    BigDecimal sumTotalCostUsd();
}
