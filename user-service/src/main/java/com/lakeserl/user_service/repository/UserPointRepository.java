package com.lakeserl.user_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lakeserl.user_service.model.entity.UserPoint;

public interface UserPointRepository extends JpaRepository<UserPoint, UUID> {

    Optional<UserPoint> findByUserId(UUID userId);
}
