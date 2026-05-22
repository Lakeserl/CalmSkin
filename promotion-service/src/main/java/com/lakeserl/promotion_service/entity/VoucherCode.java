package com.lakeserl.promotion_service.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.lakeserl.promotion_service.enums.VoucherCodeStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single-use campaign code. Many codes map to one promotion; each is mailed
 * to a customer and consumed once when claimed (status moves ACTIVE -> USED).
 */
@Entity
@Table(name = "voucher_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /** Null means the code is from a public pool any user may claim. */
    @Column(name = "assigned_user_id")
    private UUID assignedUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VoucherCodeStatus status;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = VoucherCodeStatus.ACTIVE;
        }
    }
}
