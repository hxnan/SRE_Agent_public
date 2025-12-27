package com.flashsale.backend.model;

public enum OrderStatus {
    PENDING_PAYMENT(1),
    PAID(2),
    CANCELLED(3);

    private final int code;
    OrderStatus(int code) { this.code = code; }
    public int getCode() { return code; }
    public static OrderStatus from(int code) {
        for (OrderStatus s : values()) {
            if (s.code == code) return s;
        }
        return PENDING_PAYMENT;
    }
}

