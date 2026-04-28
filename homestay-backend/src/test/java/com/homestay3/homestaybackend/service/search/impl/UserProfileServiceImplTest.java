package com.homestay3.homestaybackend.service.search.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay3.homestaybackend.entity.UserPreferenceProfile;
import com.homestay3.homestaybackend.repository.UserBehaviorEventRepository;
import com.homestay3.homestaybackend.repository.UserPreferenceProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserBehaviorEventRepository userBehaviorEventRepository;

    @Mock
    private UserPreferenceProfileRepository userPreferenceProfileRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    private UserProfileServiceImpl userProfileService;

    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileServiceImpl(
                userBehaviorEventRepository,
                userPreferenceProfileRepository,
                objectMapper
        );
    }

    @Test
    void getProfile_shouldReturnEmpty_whenNotExists() {
        when(userPreferenceProfileRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());

        Optional<UserPreferenceProfile> result = userProfileService.getProfile(TEST_USER_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void getProfile_shouldReturnProfile_whenExists() {
        UserPreferenceProfile profile = UserPreferenceProfile.builder()
                .userId(TEST_USER_ID)
                .preferredCityJson("{\"330100\":0.7}")
                .build();
        when(userPreferenceProfileRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(profile));

        Optional<UserPreferenceProfile> result = userProfileService.getProfile(TEST_USER_ID);

        assertTrue(result.isPresent());
        assertEquals(TEST_USER_ID, result.get().getUserId());
    }

    @Test
    void aggregateProfile_shouldCreateNewProfile_whenNotExists() {
        when(userPreferenceProfileRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());
        java.util.List<Object[]> cityAgg = new java.util.ArrayList<>();
        cityAgg.add(new Object[]{"330100", 5L});
        cityAgg.add(new Object[]{"310000", 3L});
        java.util.List<Object[]> typeAgg = new java.util.ArrayList<>();
        typeAgg.add(new Object[]{"ENTIRE", 4L});
        java.util.List<Object[]> amenityAgg = new java.util.ArrayList<>();
        amenityAgg.add(new Object[]{"WIFI", 4L});
        amenityAgg.add(new Object[]{"AIR_CONDITIONING", 2L});
        java.util.List<Object[]> priceAgg = new java.util.ArrayList<>();
        priceAgg.add(new Object[]{new BigDecimal("300"), new BigDecimal("800")});

        when(userBehaviorEventRepository.aggregateCityPreferences(eq(TEST_USER_ID), any()))
                .thenReturn(cityAgg);
        when(userBehaviorEventRepository.aggregateTypePreferences(eq(TEST_USER_ID), any()))
                .thenReturn(typeAgg);
        when(userBehaviorEventRepository.aggregateAmenityPreferences(eq(TEST_USER_ID), any()))
                .thenReturn(amenityAgg);
        when(userBehaviorEventRepository.findPriceRangeByUserId(eq(TEST_USER_ID), any()))
                .thenReturn(priceAgg);

        userProfileService.aggregateProfile(TEST_USER_ID);

        ArgumentCaptor<UserPreferenceProfile> captor = ArgumentCaptor.forClass(UserPreferenceProfile.class);
        verify(userPreferenceProfileRepository).save(captor.capture());

        UserPreferenceProfile saved = captor.getValue();
        assertEquals(TEST_USER_ID, saved.getUserId());
        assertEquals(new BigDecimal("300"), saved.getMinPrice());
        assertEquals(new BigDecimal("800"), saved.getMaxPrice());
        assertTrue(saved.getPreferredAmenityJson().contains("WIFI"));
        assertNotNull(saved.getLastActiveAt());
    }

    @Test
    void aggregateAllActiveProfiles_shouldAggregateOnlyActiveUserIds() {
        when(userBehaviorEventRepository.findActiveUserIdsSince(any()))
                .thenReturn(List.of(TEST_USER_ID, 2L));
        when(userPreferenceProfileRepository.findByUserId(any()))
                .thenReturn(Optional.empty());
        when(userBehaviorEventRepository.aggregateCityPreferences(any(), any()))
                .thenReturn(List.of());
        when(userBehaviorEventRepository.aggregateTypePreferences(any(), any()))
                .thenReturn(List.of());
        when(userBehaviorEventRepository.aggregateAmenityPreferences(any(), any()))
                .thenReturn(List.of());
        when(userBehaviorEventRepository.findPriceRangeByUserId(any(), any()))
                .thenReturn(List.of());

        userProfileService.aggregateAllActiveProfiles();

        verify(userBehaviorEventRepository).findActiveUserIdsSince(any());
        verify(userPreferenceProfileRepository, times(2)).save(any(UserPreferenceProfile.class));
    }

    @Test
    void getPreferredCities_shouldParseJsonWeights() {
        UserPreferenceProfile profile = UserPreferenceProfile.builder()
                .userId(TEST_USER_ID)
                .preferredCityJson("{\"330100\":0.7,\"310000\":0.3}")
                .build();
        when(userPreferenceProfileRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(profile));

        Map<String, Double> cities = userProfileService.getPreferredCities(TEST_USER_ID);

        assertEquals(2, cities.size());
        assertEquals(0.7, cities.get("330100"));
        assertEquals(0.3, cities.get("310000"));
    }

    @Test
    void getPreferredCities_shouldReturnEmpty_whenNoProfile() {
        when(userPreferenceProfileRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());

        Map<String, Double> cities = userProfileService.getPreferredCities(TEST_USER_ID);

        assertTrue(cities.isEmpty());
    }

    @Test
    void getPriceRange_shouldReturnRange_whenProfileExists() {
        UserPreferenceProfile profile = UserPreferenceProfile.builder()
                .userId(TEST_USER_ID)
                .minPrice(new BigDecimal("200"))
                .maxPrice(new BigDecimal("1000"))
                .build();
        when(userPreferenceProfileRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(profile));

        BigDecimal[] range = userProfileService.getPriceRange(TEST_USER_ID);

        assertEquals(new BigDecimal("200"), range[0]);
        assertEquals(new BigDecimal("1000"), range[1]);
    }

    @Test
    void getPriceRange_shouldReturnNulls_whenNoProfile() {
        when(userPreferenceProfileRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());

        BigDecimal[] range = userProfileService.getPriceRange(TEST_USER_ID);

        assertNull(range[0]);
        assertNull(range[1]);
    }
}
