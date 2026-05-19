package com.lakeserl.inventory_service.enums;

public enum ReferenceType {
    ORDER,          // Đơn hàng
    MANUAL_IMPORT,  // Nhập manual
    RETURN,         // Trả lại hàng
    ADJUSTMENT,     // Điều chỉnh kiểm kho
    SYSTEM          // Hệ thống (auto)
}
