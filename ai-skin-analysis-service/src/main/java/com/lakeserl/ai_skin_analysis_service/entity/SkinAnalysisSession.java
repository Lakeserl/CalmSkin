package com.lakeserl.ai_skin_analysis_service.entity;

import com.lakeserl.ai_skin_analysis_service.enums.AnalysisStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "skin_analysis_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkinAnalysisSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 50)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "age")
    private Integer age;

    @Column(name = "self_skin_type", length = 30)
    private String selfSkinType;

    @Column(name = "self_concerns", columnDefinition = "TEXT")
    private String selfConcerns;

    @Column(name = "allergies", columnDefinition = "TEXT")
    private String allergies;

    @Column(name = "original_image_url", length = 500)
    private String originalImageUrl;

    @Column(name = "processed_image_url", length = 500)
    private String processedImageUrl;

    @Column(name = "image_hash", length = 64)
    private String imageHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AnalysisStatus status = AnalysisStatus.PROCESSING;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "cv_features", columnDefinition = "TEXT")
    private String cvFeatures;

    @Column(name = "detected_skin_type", length = 30)
    private String detectedSkinType;

    @Column(name = "detected_concerns", columnDefinition = "TEXT")
    private String detectedConcerns;

    @Column(name = "skin_condition_report", columnDefinition = "TEXT")
    private String skinConditionReport;

    @Column(name = "recommended_product_ids", columnDefinition = "TEXT")
    private String recommendedProductIds;

    @Column(name = "morning_routine", columnDefinition = "TEXT")
    private String morningRoutine;

    @Column(name = "evening_routine", columnDefinition = "TEXT")
    private String eveningRoutine;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @Column(name = "consent_given", nullable = false)
    @Builder.Default
    private boolean consentGiven = false;

    @Column(name = "consent_at")
    private LocalDateTime consentAt;

    @Column(name = "consent_version", length = 20)
    private String consentVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = AnalysisStatus.PROCESSING;
        }
    }
}
