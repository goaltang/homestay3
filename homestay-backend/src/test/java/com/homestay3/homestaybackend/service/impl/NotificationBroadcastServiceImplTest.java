package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.NotificationBroadcastJobDTO;
import com.homestay3.homestaybackend.entity.NotificationBroadcastJob;
import com.homestay3.homestaybackend.repository.NotificationBroadcastJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

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
}
