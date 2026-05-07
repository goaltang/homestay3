package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.NotificationBroadcastJobDTO;
import com.homestay3.homestaybackend.entity.NotificationBroadcastJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationBroadcastService {
    NotificationBroadcastJobDTO submitBroadcast(String content, Long initiatedBy, String initiatedByUsername);

    Page<NotificationBroadcastJobDTO> getBroadcastJobs(NotificationBroadcastJob.Status status, Pageable pageable);

    NotificationBroadcastJobDTO getBroadcastJob(Long jobId);
}
