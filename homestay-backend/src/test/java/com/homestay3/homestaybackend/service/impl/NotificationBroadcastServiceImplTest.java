package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.NotificationBroadcastJobDTO;
import com.homestay3.homestaybackend.entity.NotificationBroadcastJob;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.repository.NotificationBroadcastJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationBroadcastServiceImplTest {

    @Mock
    private NotificationBroadcastJobRepository jobRepository;

    @Mock
    private NotificationBroadcastJobProcessor processor;

    @InjectMocks
    private NotificationBroadcastServiceImpl service;

    @Test
    void submitBroadcastCreatesPendingJobAndStartsProcessor() {
        when(jobRepository.existsByInitiatedByAndStatusNotAndSubmittedAtAfter(
                eq(7L), eq(NotificationBroadcastJob.Status.RATE_LIMITED), any(LocalDateTime.class)))
                .thenReturn(false);
        when(jobRepository.save(any(NotificationBroadcastJob.class))).thenAnswer(invocation -> {
            NotificationBroadcastJob job = invocation.getArgument(0);
            job.setId(10L);
            return job;
        });

        NotificationBroadcastJobDTO result = service.submitBroadcast("  first line\nsecond line  ", 7L, "admin");

        assertEquals(10L, result.getJobId());
        assertEquals(NotificationBroadcastJob.Status.PENDING, result.getStatus());
        assertEquals(7L, result.getInitiatedBy());
        assertEquals("admin", result.getInitiatedByUsername());
        assertEquals("first line second line", result.getContentSummary());
        assertEquals("first line\nsecond line".length(), result.getContentLength());
        verify(processor).process(10L, "first line\nsecond line");
    }

    @Test
    void submitBroadcastCreatesRateLimitedAuditJobWithoutStartingProcessor() {
        when(jobRepository.existsByInitiatedByAndStatusNotAndSubmittedAtAfter(
                eq(7L), eq(NotificationBroadcastJob.Status.RATE_LIMITED), any(LocalDateTime.class)))
                .thenReturn(true);
        when(jobRepository.save(any(NotificationBroadcastJob.class))).thenAnswer(invocation -> {
            NotificationBroadcastJob job = invocation.getArgument(0);
            job.setId(11L);
            return job;
        });

        NotificationBroadcastJobDTO result = service.submitBroadcast("system notice", 7L, "admin");

        assertEquals(11L, result.getJobId());
        assertEquals(NotificationBroadcastJob.Status.RATE_LIMITED, result.getStatus());
        assertTrue(result.getFailureReason().contains("rate limit"));
        verify(processor, never()).process(any(), any());

        ArgumentCaptor<NotificationBroadcastJob> jobCaptor = ArgumentCaptor.forClass(NotificationBroadcastJob.class);
        verify(jobRepository).save(jobCaptor.capture());
        assertEquals(NotificationBroadcastJob.Status.RATE_LIMITED, jobCaptor.getValue().getStatus());
        assertEquals(0, jobCaptor.getValue().getTargetCount());
        assertEquals(0, jobCaptor.getValue().getSuccessCount());
    }

    @Test
    void submitBroadcastRejectsBlankContent() {
        assertThrows(IllegalArgumentException.class, () -> service.submitBroadcast(" ", 7L, "admin"));
        verify(jobRepository, never()).save(any());
        verify(processor, never()).process(any(), any());
    }

    @Test
    void getBroadcastJobsReturnsAllJobsWhenStatusIsMissing() {
        Pageable pageable = PageRequest.of(0, 10);
        NotificationBroadcastJob entity = jobEntity(20L, NotificationBroadcastJob.Status.PENDING);
        when(jobRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        Page<NotificationBroadcastJobDTO> result = service.getBroadcastJobs(null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(20L, result.getContent().get(0).getJobId());
        assertEquals(NotificationBroadcastJob.Status.PENDING, result.getContent().get(0).getStatus());
        verify(jobRepository).findAll(pageable);
        verify(jobRepository, never()).findByStatus(any(), any());
    }

    @Test
    void getBroadcastJobsFiltersByStatus() {
        Pageable pageable = PageRequest.of(1, 5);
        NotificationBroadcastJob entity = jobEntity(21L, NotificationBroadcastJob.Status.FAILED);
        when(jobRepository.findByStatus(NotificationBroadcastJob.Status.FAILED, pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 6));

        Page<NotificationBroadcastJobDTO> result = service.getBroadcastJobs(
                NotificationBroadcastJob.Status.FAILED,
                pageable);

        assertEquals(6, result.getTotalElements());
        assertEquals(21L, result.getContent().get(0).getJobId());
        assertEquals(NotificationBroadcastJob.Status.FAILED, result.getContent().get(0).getStatus());
        verify(jobRepository).findByStatus(NotificationBroadcastJob.Status.FAILED, pageable);
    }

    @Test
    void getBroadcastJobReturnsDetail() {
        NotificationBroadcastJob entity = jobEntity(22L, NotificationBroadcastJob.Status.SUCCEEDED);
        when(jobRepository.findById(22L)).thenReturn(Optional.of(entity));

        NotificationBroadcastJobDTO result = service.getBroadcastJob(22L);

        assertEquals(22L, result.getJobId());
        assertEquals(NotificationBroadcastJob.Status.SUCCEEDED, result.getStatus());
        assertEquals(3, result.getTargetCount());
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
    }

    @Test
    void getBroadcastJobThrowsWhenMissing() {
        when(jobRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getBroadcastJob(404L));
    }

    private NotificationBroadcastJob jobEntity(Long id, NotificationBroadcastJob.Status status) {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 1, 2, 3, 4);
        return NotificationBroadcastJob.builder()
                .id(id)
                .status(status)
                .initiatedBy(7L)
                .initiatedByUsername("admin")
                .contentSummary("system notice")
                .contentLength(13)
                .targetCount(3)
                .successCount(2)
                .failureCount(1)
                .failureReason(status == NotificationBroadcastJob.Status.FAILED ? "failed" : null)
                .submittedAt(submittedAt)
                .startedAt(submittedAt.plusSeconds(1))
                .completedAt(submittedAt.plusSeconds(2))
                .updatedAt(submittedAt.plusSeconds(2))
                .build();
    }
}
