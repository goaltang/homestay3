package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.NotificationBroadcastJobDTO;
import com.homestay3.homestaybackend.entity.NotificationBroadcastJob;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.repository.NotificationBroadcastJobRepository;
import com.homestay3.homestaybackend.service.NotificationBroadcastService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class NotificationBroadcastServiceImpl implements NotificationBroadcastService {

    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    private static final int CONTENT_SUMMARY_LIMIT = 120;
    private static final int USERNAME_LIMIT = 100;
    private static final String RATE_LIMIT_REASON =
            "Broadcast rate limit exceeded: only one broadcast is allowed per minute.";

    private final NotificationBroadcastJobRepository jobRepository;
    private final NotificationBroadcastJobProcessor processor;

    public NotificationBroadcastServiceImpl(NotificationBroadcastJobRepository jobRepository,
                                            NotificationBroadcastJobProcessor processor) {
        this.jobRepository = jobRepository;
        this.processor = processor;
    }

    @Override
    @Transactional
    public NotificationBroadcastJobDTO submitBroadcast(String content, Long initiatedBy, String initiatedByUsername) {
        String normalizedContent = normalizeContent(content);
        if (normalizedContent.isBlank()) {
            throw new IllegalArgumentException("Broadcast content must not be blank");
        }

        LocalDateTime now = LocalDateTime.now();
        if (isRateLimited(initiatedBy, now.minus(RATE_LIMIT_WINDOW))) {
            NotificationBroadcastJob limitedJob = buildJob(
                    normalizedContent,
                    initiatedBy,
                    initiatedByUsername,
                    NotificationBroadcastJob.Status.RATE_LIMITED,
                    now);
            limitedJob.setFailureReason(RATE_LIMIT_REASON);
            limitedJob.setCompletedAt(now);
            return NotificationBroadcastJobDTO.fromEntity(jobRepository.save(limitedJob));
        }

        NotificationBroadcastJob job = buildJob(
                normalizedContent,
                initiatedBy,
                initiatedByUsername,
                NotificationBroadcastJob.Status.PENDING,
                now);
        NotificationBroadcastJob savedJob = jobRepository.save(job);
        runAfterCommit(() -> processor.process(savedJob.getId(), normalizedContent));
        return NotificationBroadcastJobDTO.fromEntity(savedJob);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationBroadcastJobDTO> getBroadcastJobs(NotificationBroadcastJob.Status status,
                                                              Pageable pageable) {
        Page<NotificationBroadcastJob> jobs = status == null
                ? jobRepository.findAll(pageable)
                : jobRepository.findByStatus(status, pageable);
        return jobs.map(NotificationBroadcastJobDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationBroadcastJobDTO getBroadcastJob(Long jobId) {
        return jobRepository.findById(jobId)
                .map(NotificationBroadcastJobDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification broadcast job not found with id: " + jobId));
    }

    private boolean isRateLimited(Long initiatedBy, LocalDateTime submittedAfter) {
        if (initiatedBy != null) {
            return !jobRepository.findRecentByInitiatedByAndStatusNotAndSubmittedAtAfter(
                    initiatedBy,
                    NotificationBroadcastJob.Status.RATE_LIMITED,
                    submittedAfter).isEmpty();
        }
        return !jobRepository.findRecentByStatusNotAndSubmittedAtAfter(
                NotificationBroadcastJob.Status.RATE_LIMITED,
                submittedAfter).isEmpty();
    }

    private NotificationBroadcastJob buildJob(String content,
                                              Long initiatedBy,
                                              String initiatedByUsername,
                                              NotificationBroadcastJob.Status status,
                                              LocalDateTime now) {
        return NotificationBroadcastJob.builder()
                .status(status)
                .initiatedBy(initiatedBy)
                .initiatedByUsername(truncate(blankToNull(initiatedByUsername), USERNAME_LIMIT))
                .contentSummary(summarize(content))
                .contentLength(content.length())
                .submittedAt(now)
                .updatedAt(now)
                .build();
    }

    private String normalizeContent(String content) {
        return content == null ? "" : content.trim();
    }

    private String summarize(String content) {
        String oneLine = content.replaceAll("\\s+", " ");
        return truncate(oneLine, CONTENT_SUMMARY_LIMIT);
    }

    private String truncate(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void runAfterCommit(Runnable runnable) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runnable.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runnable.run();
            }
        });
    }
}
