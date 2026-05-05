package com.lakeserl.user_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lakeserl.user_service.model.entity.OtpToken;
import com.lakeserl.user_service.model.enums.OtpType;

public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {

    Optional<OtpToken> findTopByUserIdAndTypeAndUsedFalseOrderByCreatedAtDesc(UUID userId, OtpType type);
}
