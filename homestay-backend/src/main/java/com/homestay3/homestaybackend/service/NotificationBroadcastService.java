package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.NotificationBroadcastJobDTO;

public interface NotificationBroadcastService {
    NotificationBroadcastJobDTO submitBroadcast(String content, Long initiatedBy, String initiatedByUsername);
}
