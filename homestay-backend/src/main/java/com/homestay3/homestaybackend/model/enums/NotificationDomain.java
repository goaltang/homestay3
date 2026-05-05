package com.homestay3.homestaybackend.model.enums;

/**
 * 通知业务域枚举
 */
public enum NotificationDomain {
    ORDER("order"),
    MESSAGE("message"),
    REVIEW("review"),
    HOMESTAY("homestay"),
    COUPON("coupon"),
    SYSTEM("system");

    private final String category;

    NotificationDomain(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }
}
