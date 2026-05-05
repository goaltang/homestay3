package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.NotificationDTO;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.service.NotificationPreferenceService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationPreferenceService notificationPreferenceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationController(notificationService, notificationPreferenceService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                "42",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyNotificationsReturnsUnknownLegacyValuesWithoutFailing() throws Exception {
        NotificationDTO notification = new NotificationDTO();
        notification.setId(100L);
        notification.setUserId(42L);
        notification.setType(NotificationType.UNKNOWN);
        notification.setRawType("FUTURE_ORDER_STATUS");
        notification.setEntityType(EntityType.UNKNOWN);
        notification.setRawEntityType("FUTURE_ENTITY");
        notification.setEntityId("9001");
        notification.setContent("legacy notification");

        when(notificationService.getNotificationsForUser(eq(42L), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(notification), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "10")
                        .param("type", "FUTURE_ORDER_STATUS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].type").value("UNKNOWN"))
                .andExpect(jsonPath("$.content[0].rawType").value("FUTURE_ORDER_STATUS"))
                .andExpect(jsonPath("$.content[0].entityType").value("UNKNOWN"))
                .andExpect(jsonPath("$.content[0].rawEntityType").value("FUTURE_ENTITY"))
                .andExpect(jsonPath("$.content[0].content").value("legacy notification"));

        verify(notificationService).getNotificationsForUser(eq(42L), isNull(), isNull(), any());
    }

    @Test
    void getMyNotificationsIgnoresInvalidTypeFilter() throws Exception {
        when(notificationService.getNotificationsForUser(eq(42L), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/notifications")
                        .param("type", "NOT_A_NOTIFICATION_TYPE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));

        verify(notificationService).getNotificationsForUser(eq(42L), isNull(), isNull(), any());
    }
}
