package com.lakeserl.user_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lakeserl.user_service.model.entity.UserAddress;

public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {

    List<UserAddress> findByUserId(UUID userId);

    int countByUserId(UUID userId);

    Optional<UserAddress> findByIdAndUserId(UUID id, UUID userId);

    Optional<UserAddress> findByUserIdAndIsDefaultTrue(UUID userId);
}
