package com.homestay3.homestaybackend.event;

import com.homestay3.homestaybackend.dto.NotificationDTO;

public record NotificationCreatedEvent(Long userId, NotificationDTO notification) {
}
