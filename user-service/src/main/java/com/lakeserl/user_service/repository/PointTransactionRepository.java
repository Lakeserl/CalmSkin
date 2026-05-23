package com.lakeserl.user_service.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lakeserl.user_service.model.entity.PointTransaction;
import com.lakeserl.user_service.model.enums.PointTransactionType;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, UUID> {

    Page<PointTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    boolean existsByUserIdAndReferenceIdAndReferenceTypeAndType(
            UUID userId, String referenceId, String referenceType, PointTransactionType type);
}
