package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.NotificationBroadcastJob;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationBroadcastJobRepository extends JpaRepository<NotificationBroadcastJob, Long> {
    Page<NotificationBroadcastJob> findByStatus(NotificationBroadcastJob.Status status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM NotificationBroadcastJob j WHERE j.initiatedBy = :initiatedBy AND j.status <> :excludedStatus AND j.submittedAt > :submittedAfter ORDER BY j.submittedAt DESC")
    List<NotificationBroadcastJob> findRecentByInitiatedByAndStatusNotAndSubmittedAtAfter(
            @Param("initiatedBy") Long initiatedBy,
            @Param("excludedStatus") NotificationBroadcastJob.Status excludedStatus,
            @Param("submittedAfter") LocalDateTime submittedAfter);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM NotificationBroadcastJob j WHERE j.status <> :excludedStatus AND j.submittedAt > :submittedAfter ORDER BY j.submittedAt DESC")
    List<NotificationBroadcastJob> findRecentByStatusNotAndSubmittedAtAfter(
            @Param("excludedStatus") NotificationBroadcastJob.Status excludedStatus,
            @Param("submittedAfter") LocalDateTime submittedAfter);
}
