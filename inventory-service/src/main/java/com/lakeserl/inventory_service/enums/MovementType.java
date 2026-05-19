package com.lakeserl.inventory_service.enums;

public enum MovementType {
    IN,         // Nhập kho
    OUT,        // Xuất kho / bán
    RESERVE,    // Giữ hàng chờ thanh toán
    RELEASE,    // Giải phóng hàng (hủy đơn)
    ADJUST,     // Điều chỉnh kiểm kho
    RETURN,     // Trả lại hàng
    EXPIRE      // Reservation expired
}
