package com.lakeserl.inventory_service.repository;

import com.lakeserl.inventory_service.entity.StockReservation;
import com.lakeserl.inventory_service.enums.ReservationStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    List<StockReservation> findByOrderId(String orderId);
    List<StockReservation> findByOrderIdAndStatus(String orderId, ReservationStatus status);
    List<StockReservation> findByStatus(ReservationStatus status);
    List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime expiresAt);
}
