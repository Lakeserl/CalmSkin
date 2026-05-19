package com.lakeserl.inventory_service.service;

import com.lakeserl.inventory_service.dto.request.ReserveStockRequest;
import com.lakeserl.inventory_service.dto.request.ReturnStockRequest;
import com.lakeserl.inventory_service.dto.response.ReservationResponse;
import com.lakeserl.inventory_service.entity.Inventory;
import com.lakeserl.inventory_service.entity.StockMovement;
import com.lakeserl.inventory_service.entity.StockReservation;
import com.lakeserl.inventory_service.enums.MovementType;
import com.lakeserl.inventory_service.enums.ReferenceType;
import com.lakeserl.inventory_service.enums.ReservationStatus;
import com.lakeserl.inventory_service.event.producer.InventoryEventProducer;
import com.lakeserl.inventory_service.exception.InsufficientStockException;
import com.lakeserl.inventory_service.exception.InventoryNotFoundException;
import com.lakeserl.inventory_service.exception.ReservationAlreadyProcessedException;
import com.lakeserl.inventory_service.exception.ReservationNotFoundException;
import com.lakeserl.inventory_service.repository.InventoryRepository;
import com.lakeserl.inventory_service.repository.StockMovementRepository;
import com.lakeserl.inventory_service.repository.StockReservationRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockReservationServiceImpl implements StockReservationService {
    private static final int RESERVATION_TTL_MINUTES = 15;

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository stockReservationRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryEventProducer inventoryEventProducer;

    @Override
    @Transactional(noRollbackFor = InsufficientStockException.class)
    public ReservationResponse reserveStock(ReserveStockRequest request) {
        List<ReservationTarget> targets = new ArrayList<>();

        for (ReserveStockRequest.ReserveItem item : request.items()) {
            Inventory inventory = inventoryRepository
                    .findByProductIdAndVariantIdForUpdate(item.productId(), item.variantId())
                    .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));

            if (inventory.getQuantityAvailable() < item.quantity()) {
                throw new InsufficientStockException("Insufficient stock for product " + item.productId());
            }

            targets.add(new ReservationTarget(inventory, item));
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(RESERVATION_TTL_MINUTES);
        List<Long> reservationIds = new ArrayList<>();

        for (ReservationTarget target : targets) {
            Inventory inventory = target.inventory();
            int previousAvailable = inventory.getQuantityAvailable();

            inventory.setQuantityAvailable(inventory.getQuantityAvailable() - target.item().quantity());
            inventory.setQuantityReserved(inventory.getQuantityReserved() + target.item().quantity());
            inventoryRepository.save(inventory);

            StockReservation reservation = StockReservation.builder()
                    .inventoryId(inventory.getId())
                    .orderId(request.orderId())
                    .quantity(target.item().quantity())
                    .status(ReservationStatus.PENDING)
                    .expiresAt(expiresAt)
                    .build();
            StockReservation saved = stockReservationRepository.save(reservation);
            reservationIds.add(saved.getId());

            StockMovement movement = StockMovement.builder()
                    .inventoryId(inventory.getId())
                    .movementType(MovementType.RESERVE)
                    .quantity(target.item().quantity())
                    .referenceId(request.orderId())
                    .referenceType(ReferenceType.ORDER)
                    .createdBy("SYSTEM")
                    .build();
            stockMovementRepository.save(movement);

            inventoryEventProducer.publishStockAlerts(inventory, previousAvailable);
        }

        ReservationResponse response = ReservationResponse.builder()
                .orderId(request.orderId())
                .reservationIds(reservationIds)
                .expiresAt(toInstant(expiresAt))
                .build();

        inventoryEventProducer.publish("inventory.reserved", request.orderId(), response);

        return response;
    }

    @Override
    @Transactional
    public ReservationResponse releaseStock(String orderId) {
        List<StockReservation> pendingReservations = stockReservationRepository
                .findByOrderIdAndStatus(orderId, ReservationStatus.PENDING);

        if (pendingReservations.isEmpty()) {
            List<StockReservation> existing = stockReservationRepository.findByOrderId(orderId);
            if (existing.isEmpty()) {
                throw new ReservationNotFoundException("Reservation not found for order " + orderId);
            }
            throw new ReservationAlreadyProcessedException("Reservation already processed for order " + orderId);
        }

        for (StockReservation reservation : pendingReservations) {
            Inventory inventory = inventoryRepository.findByIdForUpdate(reservation.getInventoryId())
                    .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));

            int previousAvailable = inventory.getQuantityAvailable();
            inventory.setQuantityAvailable(inventory.getQuantityAvailable() + reservation.getQuantity());
            inventory.setQuantityReserved(inventory.getQuantityReserved() - reservation.getQuantity());

            if (inventory.getQuantityReserved() < 0) {
                throw new InsufficientStockException("Reserved quantity cannot be negative");
            }

            inventoryRepository.save(inventory);

            StockMovement movement = StockMovement.builder()
                    .inventoryId(inventory.getId())
                    .movementType(MovementType.RELEASE)
                    .quantity(reservation.getQuantity())
                    .referenceId(orderId)
                    .referenceType(ReferenceType.ORDER)
                    .createdBy("SYSTEM")
                    .build();
            stockMovementRepository.save(movement);

            reservation.setStatus(ReservationStatus.RELEASED);
            stockReservationRepository.save(reservation);

            inventoryEventProducer.publishStockAlerts(inventory, previousAvailable);
        }

        return ReservationResponse.builder()
                .orderId(orderId)
                .released(true)
                .build();
    }

    @Override
    @Transactional
    public ReservationResponse confirmStock(String orderId) {
        List<StockReservation> pendingReservations = stockReservationRepository
                .findByOrderIdAndStatus(orderId, ReservationStatus.PENDING);

        if (pendingReservations.isEmpty()) {
            List<StockReservation> existing = stockReservationRepository.findByOrderId(orderId);
            if (existing.isEmpty()) {
                throw new ReservationNotFoundException("Reservation not found for order " + orderId);
            }
            throw new ReservationAlreadyProcessedException("Reservation already processed for order " + orderId);
        }

        for (StockReservation reservation : pendingReservations) {
            Inventory inventory = inventoryRepository.findByIdForUpdate(reservation.getInventoryId())
                    .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));

            inventory.setQuantityReserved(inventory.getQuantityReserved() - reservation.getQuantity());
            inventory.setQuantitySold(inventory.getQuantitySold() + reservation.getQuantity());

            if (inventory.getQuantityReserved() < 0) {
                throw new InsufficientStockException("Reserved quantity cannot be negative");
            }

            inventoryRepository.save(inventory);

            StockMovement movement = StockMovement.builder()
                    .inventoryId(inventory.getId())
                    .movementType(MovementType.OUT)
                    .quantity(reservation.getQuantity())
                    .referenceId(orderId)
                    .referenceType(ReferenceType.ORDER)
                    .createdBy("SYSTEM")
                    .build();
            stockMovementRepository.save(movement);

            reservation.setStatus(ReservationStatus.CONFIRMED);
            stockReservationRepository.save(reservation);
        }

        return ReservationResponse.builder()
                .orderId(orderId)
                .confirmed(true)
                .build();
    }

    @Override
    @Transactional
    public ReservationResponse returnStock(ReturnStockRequest request) {
        for (ReturnStockRequest.ReturnItem item : request.items()) {
            Inventory inventory = inventoryRepository
                    .findByProductIdAndVariantIdForUpdate(item.productId(), item.variantId())
                    .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));

            if (inventory.getQuantitySold() < item.quantity()) {
                throw new InsufficientStockException("Return quantity exceeds sold quantity");
            }

            int previousAvailable = inventory.getQuantityAvailable();
            inventory.setQuantityAvailable(inventory.getQuantityAvailable() + item.quantity());
            inventory.setQuantitySold(inventory.getQuantitySold() - item.quantity());

            inventoryRepository.save(inventory);

            StockMovement movement = StockMovement.builder()
                    .inventoryId(inventory.getId())
                    .movementType(MovementType.RETURN)
                    .quantity(item.quantity())
                    .referenceId(request.orderId())
                    .referenceType(ReferenceType.RETURN)
                    .createdBy("SYSTEM")
                    .build();
            stockMovementRepository.save(movement);

            inventoryEventProducer.publishStockAlerts(inventory, previousAvailable);
        }

        return ReservationResponse.builder()
                .orderId(request.orderId())
                .returned(true)
                .build();
    }

    @Override
    @Transactional
    public int expireReservations() {
        List<StockReservation> expired = stockReservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.PENDING, LocalDateTime.now());

        for (StockReservation reservation : expired) {
            Inventory inventory = inventoryRepository.findByIdForUpdate(reservation.getInventoryId())
                    .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));

            int previousAvailable = inventory.getQuantityAvailable();
            inventory.setQuantityAvailable(inventory.getQuantityAvailable() + reservation.getQuantity());
            inventory.setQuantityReserved(inventory.getQuantityReserved() - reservation.getQuantity());

            if (inventory.getQuantityReserved() < 0) {
                throw new InsufficientStockException("Reserved quantity cannot be negative");
            }

            inventoryRepository.save(inventory);

            StockMovement movement = StockMovement.builder()
                    .inventoryId(inventory.getId())
                    .movementType(MovementType.EXPIRE)
                    .quantity(reservation.getQuantity())
                    .referenceId(reservation.getOrderId())
                    .referenceType(ReferenceType.ORDER)
                    .createdBy("SYSTEM")
                    .build();
            stockMovementRepository.save(movement);

            reservation.setStatus(ReservationStatus.EXPIRED);
            stockReservationRepository.save(reservation);

            inventoryEventProducer.publishStockAlerts(inventory, previousAvailable);
            inventoryEventProducer.publish("inventory.reservation-expired", reservation.getOrderId(),
                    Map.of("orderId", reservation.getOrderId()));
        }

        return expired.size();
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC).toInstant();
    }

    private record ReservationTarget(Inventory inventory, ReserveStockRequest.ReserveItem item) {
    }
}
