package com.lakeserl.shipping_service.repository;

import com.lakeserl.shipping_service.entity.ShipmentTrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentTrackingEventRepository extends JpaRepository<ShipmentTrackingEvent, Long> {
    List<ShipmentTrackingEvent> findByShipmentIdOrderByOccurredAtDesc(Long shipmentId);
}
