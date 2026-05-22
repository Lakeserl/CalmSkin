package com.lakeserl.promotion_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lakeserl.promotion_service.entity.FlashSaleReservation;

@Repository
public interface FlashSaleReservationRepository extends JpaRepository<FlashSaleReservation, Long> {

    List<FlashSaleReservation> findByOrderId(String orderId);
}
