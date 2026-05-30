package com.lakeserl.ai_skin_analysis_service.repository;

import com.lakeserl.ai_skin_analysis_service.entity.SkinAnalysisSession;
import com.lakeserl.ai_skin_analysis_service.enums.AnalysisStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SkinAnalysisSessionRepository extends JpaRepository<SkinAnalysisSession, Long> {

    Optional<SkinAnalysisSession> findBySessionId(String sessionId);

    Page<SkinAnalysisSession> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<SkinAnalysisSession> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<SkinAnalysisSession> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    long countByStatus(AnalysisStatus status);

    @Query("SELECT COUNT(s) FROM SkinAnalysisSession s")
    long countAllSessions();

    @Query("SELECT COALESCE(AVG(s.tokensUsed), 0) FROM SkinAnalysisSession s WHERE s.tokensUsed IS NOT NULL")
    double avgTokensUsed();

    List<SkinAnalysisSession> findByProcessedImageUrlIsNotNullAndCreatedAtBefore(LocalDateTime cutoff);
}
