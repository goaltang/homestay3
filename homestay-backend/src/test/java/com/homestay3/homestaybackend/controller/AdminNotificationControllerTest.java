package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.NotificationBroadcastJobDTO;
import com.homestay3.homestaybackend.entity.NotificationBroadcastJob;
import com.homestay3.homestaybackend.service.NotificationBroadcastService;
import com.homestay3.homestaybackend.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminNotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationBroadcastService notificationBroadcastService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminNotificationController(notificationService, notificationBroadcastService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                "7",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void broadcastReturnsAcceptedJobStatus() throws Exception {
        when(notificationBroadcastService.submitBroadcast("system notice", 7L, "7"))
                .thenReturn(job(99L, NotificationBroadcastJob.Status.PENDING, null));

        mockMvc.perform(post("/api/admin/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\" system notice \"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(99))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(notificationBroadcastService).submitBroadcast("system notice", 7L, "7");
    }

    @Test
    void broadcastReturnsTooManyRequestsForRateLimitedJob() throws Exception {
        when(notificationBroadcastService.submitBroadcast("system notice", 7L, "7"))
                .thenReturn(job(100L, NotificationBroadcastJob.Status.RATE_LIMITED, "rate limited"));

        mockMvc.perform(post("/api/admin/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"system notice\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.jobId").value(100))
                .andExpect(jsonPath("$.status").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.failureReason").value("rate limited"));
    }

    @Test
    void broadcastRejectsBlankContent() throws Exception {
        mockMvc.perform(post("/api/admin/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"  \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(notificationBroadcastService);
    }

    @Test
    void getBroadcastJobsReturnsPagedResultWithStatusFilter() throws Exception {
        NotificationBroadcastJobDTO dto = job(101L, NotificationBroadcastJob.Status.SUCCEEDED, null);
        when(notificationBroadcastService.getBroadcastJobs(
                eq(NotificationBroadcastJob.Status.SUCCEEDED),
                any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 5), 1));

        mockMvc.perform(get("/api/admin/notifications/broadcast-jobs")
                        .param("status", "succeeded")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].jobId").value(101))
                .andExpect(jsonPath("$.content[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(notificationBroadcastService).getBroadcastJobs(eq(NotificationBroadcastJob.Status.SUCCEEDED), any());
    }

    @Test
    void getBroadcastJobReturnsDetail() throws Exception {
        when(notificationBroadcastService.getBroadcastJob(101L))
                .thenReturn(job(101L, NotificationBroadcastJob.Status.FAILED, "failed"));

        mockMvc.perform(get("/api/admin/notifications/broadcast-jobs/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(101))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("failed"));

        verify(notificationBroadcastService).getBroadcastJob(101L);
    }

    private NotificationBroadcastJobDTO job(Long jobId,
                                            NotificationBroadcastJob.Status status,
                                            String failureReason) {
        return NotificationBroadcastJobDTO.builder()
                .jobId(jobId)
                .status(status)
                .contentSummary("system notice")
                .contentLength(13)
                .failureReason(failureReason)
                .submittedAt(LocalDateTime.now())
                .build();
    }
}
