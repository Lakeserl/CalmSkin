package com.lakeserl.shipping_service.repository;

import com.lakeserl.shipping_service.entity.Shipment;
import com.lakeserl.shipping_service.enums.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByOrderId(Long orderId);
    Optional<Shipment> findByOrderNumber(String orderNumber);
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    boolean existsByOrderId(Long orderId);
    Page<Shipment> findByUserId(UUID userId, Pageable pageable);
    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);
}
