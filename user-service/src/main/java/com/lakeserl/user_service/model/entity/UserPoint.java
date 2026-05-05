package com.lakeserl.user_service.model.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.lakeserl.user_service.model.enums.LoyaltyTier;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_points")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "total_points", nullable = false)
    private int totalPoints = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoyaltyTier tier = LoyaltyTier.BRONZE;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
