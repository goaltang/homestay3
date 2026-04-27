package com.homestay3.homestaybackend.service.search.impl;

import com.homestay3.homestaybackend.entity.UserBehaviorEvent;
import com.homestay3.homestaybackend.repository.UserBehaviorEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserBehaviorTrackingServiceImplTest {

    @Mock
    private UserBehaviorEventRepository userBehaviorEventRepository;

    @InjectMocks
    private UserBehaviorTrackingServiceImpl trackingService;

    @Test
    void trackView_shouldSaveEventWithCorrectType() {
        trackingService.trackView(1L, "session-1", 100L, "330100", "ENTIRE", new BigDecimal("500"));

        ArgumentCaptor<UserBehaviorEvent> captor = ArgumentCaptor.forClass(UserBehaviorEvent.class);
        verify(userBehaviorEventRepository, times(1)).save(captor.capture());

        UserBehaviorEvent event = captor.getValue();
        assertEquals("VIEW", event.getEventType());
        assertEquals(1L, event.getUserId());
        assertEquals(100L, event.getHomestayId());
        assertEquals("330100", event.getCityCode());
        assertEquals("ENTIRE", event.getType());
        assertEquals(new BigDecimal("500"), event.getPrice());
    }

    @Test
    void trackSearch_shouldSaveEventWithKeyword() {
        trackingService.trackSearch(1L, "session-1", "杭州民宿", "330100", "ENTIRE");

        ArgumentCaptor<UserBehaviorEvent> captor = ArgumentCaptor.forClass(UserBehaviorEvent.class);
        verify(userBehaviorEventRepository).save(captor.capture());

        UserBehaviorEvent event = captor.getValue();
        assertEquals("SEARCH", event.getEventType());
        assertEquals("杭州民宿", event.getKeyword());
    }

    @Test
    void trackFavorite_shouldSaveEventWithHomestayInfo() {
        trackingService.trackFavorite(2L, "session-2", 200L, "310000", "PRIVATE", new BigDecimal("800"));

        ArgumentCaptor<UserBehaviorEvent> captor = ArgumentCaptor.forClass(UserBehaviorEvent.class);
        verify(userBehaviorEventRepository).save(captor.capture());

        UserBehaviorEvent event = captor.getValue();
        assertEquals("FAVORITE", event.getEventType());
        assertEquals(2L, event.getUserId());
        assertEquals(200L, event.getHomestayId());
    }

    @Test
    void trackBooking_shouldSaveEventWithPrice() {
        trackingService.trackBooking(3L, "session-3", 300L, "440300", "ENTIRE", new BigDecimal("1200"));

        ArgumentCaptor<UserBehaviorEvent> captor = ArgumentCaptor.forClass(UserBehaviorEvent.class);
        verify(userBehaviorEventRepository).save(captor.capture());

        UserBehaviorEvent event = captor.getValue();
        assertEquals("BOOKING", event.getEventType());
        assertEquals(new BigDecimal("1200"), event.getPrice());
    }

    @Test
    void trackView_shouldSwallowException() {
        doThrow(new RuntimeException("db error")).when(userBehaviorEventRepository).save(any());

        assertDoesNotThrow(() ->
                trackingService.trackView(1L, "session-1", 100L, "330100", "ENTIRE", new BigDecimal("500")));
    }

    @Test
    void getRecentEvents_shouldReturnEventsOrderedByTime() {
        when(userBehaviorEventRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of());

        List<UserBehaviorEvent> result = trackingService.getRecentEvents(1L, 7);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userBehaviorEventRepository).findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(any(), any());
    }
}
