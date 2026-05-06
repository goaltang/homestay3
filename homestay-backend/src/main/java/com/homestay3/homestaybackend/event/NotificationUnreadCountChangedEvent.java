package com.homestay3.homestaybackend.event;

public record NotificationUnreadCountChangedEvent(Long userId, long unreadCount) {
}
