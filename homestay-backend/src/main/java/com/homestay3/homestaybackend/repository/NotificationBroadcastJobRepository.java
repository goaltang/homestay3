package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.NotificationBroadcastJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface NotificationBroadcastJobRepository extends JpaRepository<NotificationBroadcastJob, Long> {
    Page<NotificationBroadcastJob> findByStatus(NotificationBroadcastJob.Status status, Pageable pageable);

    boolean existsByInitiatedByAndStatusNotAndSubmittedAtAfter(
            Long initiatedBy,
            NotificationBroadcastJob.Status excludedStatus,
            LocalDateTime submittedAfter);

    boolean existsByStatusNotAndSubmittedAtAfter(
            NotificationBroadcastJob.Status excludedStatus,
            LocalDateTime submittedAfter);
}
