package com.lakeserl.user_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lakeserl.user_service.model.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String tokenHash);

    List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId);

    void deleteByUserId(UUID userId);
}
