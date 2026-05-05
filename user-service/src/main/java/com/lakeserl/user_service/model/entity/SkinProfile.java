package com.lakeserl.user_service.model.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.lakeserl.user_service.model.enums.SkinType;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "skin_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkinProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "skin_type")
    private SkinType skinType;

    @Column(name = "skin_concerns", columnDefinition = "jsonb")
    private String skinConcerns;

    @Column(name = "allergies", columnDefinition = "jsonb")
    private String allergies;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
