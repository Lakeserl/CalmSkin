package com.lakeserl.user_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lakeserl.user_service.model.entity.SkinProfile;

public interface SkinProfileRepository extends JpaRepository<SkinProfile, UUID> {

    Optional<SkinProfile> findByUserId(UUID userId);
}
