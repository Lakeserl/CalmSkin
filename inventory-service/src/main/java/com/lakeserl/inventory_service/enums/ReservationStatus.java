package com.lakeserl.inventory_service.enums;

public enum ReservationStatus {
    PENDING,      // Chờ xác nhận
    CONFIRMED,    // Đã xác nhận
    RELEASED,     // Đã giải phóng (hủy đơn)
    EXPIRED       // Hết hạn
}
