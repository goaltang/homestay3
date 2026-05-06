package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.NotificationPreference;
import com.homestay3.homestaybackend.model.enums.NotificationDomain;
import com.homestay3.homestaybackend.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceImplTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Test
    void getEnabledMapDefaultsMissingPreferencesToEnabledAndAppliesStoredValues() {
        NotificationPreference disabledPreference = NotificationPreference.builder()
                .userId(2L)
                .domain(NotificationDomain.SYSTEM.name())
                .enabled(false)
                .build();
        NotificationPreference enabledPreference = NotificationPreference.builder()
                .userId(3L)
                .domain(NotificationDomain.SYSTEM.name())
                .enabled(true)
                .build();

        when(preferenceRepository.findByDomainAndUserIdIn(eq(NotificationDomain.SYSTEM.name()), any()))
                .thenReturn(List.of(disabledPreference, enabledPreference));

        NotificationPreferenceServiceImpl service = new NotificationPreferenceServiceImpl(preferenceRepository);

        Map<Long, Boolean> result = service.getEnabledMap(List.of(1L, 2L, 3L), NotificationDomain.SYSTEM);

        assertEquals(Map.of(1L, true, 2L, false, 3L, true), result);
        verify(preferenceRepository).findByDomainAndUserIdIn(eq(NotificationDomain.SYSTEM.name()), any());
    }
}
