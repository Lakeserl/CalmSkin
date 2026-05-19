package com.lakeserl.inventory_service.service;

import com.lakeserl.inventory_service.dto.request.ReserveStockRequest;
import com.lakeserl.inventory_service.dto.request.ReturnStockRequest;
import com.lakeserl.inventory_service.dto.response.ReservationResponse;

public interface StockReservationService {
    ReservationResponse reserveStock(ReserveStockRequest request);

    ReservationResponse releaseStock(String orderId);

    ReservationResponse confirmStock(String orderId);

    ReservationResponse returnStock(ReturnStockRequest request);

    int expireReservations();
}
