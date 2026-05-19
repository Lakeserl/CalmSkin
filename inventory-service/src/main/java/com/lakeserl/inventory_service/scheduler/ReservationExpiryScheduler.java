package com.lakeserl.inventory_service.scheduler;

import com.lakeserl.inventory_service.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpiryScheduler {
    private final StockReservationService stockReservationService;

    @Scheduled(fixedDelay = 60000)
    public void expireReservations() {
        int expired = stockReservationService.expireReservations();
        if (expired > 0) {
            log.info("Expired {} reservations", expired);
        }
    }
}
