package com.homestay3.homestaybackend.service.search.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay3.homestaybackend.entity.UserBehaviorEvent;
import com.homestay3.homestaybackend.entity.UserPreferenceProfile;
import com.homestay3.homestaybackend.repository.UserBehaviorEventRepository;
import com.homestay3.homestaybackend.repository.UserPreferenceProfileRepository;
import com.homestay3.homestaybackend.service.search.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserBehaviorEventRepository userBehaviorEventRepository;
    private final UserPreferenceProfileRepository userPreferenceProfileRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<UserPreferenceProfile> getProfile(Long userId) {
        return userPreferenceProfileRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public void aggregateProfile(Long userId) {
        LocalDateTime since = LocalDateTime.now().minus(90, ChronoUnit.DAYS);

        // 聚合城市偏好
        Map<String, Double> cityWeights = aggregateWeights(
                userBehaviorEventRepository.aggregateCityPreferences(userId, since));

        // 聚合房型偏好
        Map<String, Double> typeWeights = aggregateWeights(
                userBehaviorEventRepository.aggregateTypePreferences(userId, since));

        // 价格范围
        BigDecimal[] priceRange = resolvePriceRange(
                userBehaviorEventRepository.findPriceRangeByUserId(userId, since));

        UserPreferenceProfile profile = userPreferenceProfileRepository.findByUserId(userId)
                .orElse(UserPreferenceProfile.builder()
                        .userId(userId)
                        .build());

        try {
            profile.setPreferredCityJson(objectMapper.writeValueAsString(cityWeights));
            profile.setPreferredTypeJson(objectMapper.writeValueAsString(typeWeights));
            profile.setMinPrice(priceRange[0]);
            profile.setMaxPrice(priceRange[1]);
            profile.setLastActiveAt(LocalDateTime.now());
            userPreferenceProfileRepository.save(profile);
            log.info("Aggregated preference profile for user {}", userId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize preference profile for user {}", userId, e);
        }
    }

    @Override
    @Transactional
    public void aggregateAllActiveProfiles() {
        LocalDateTime since = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        List<UserBehaviorEvent> recentEvents = userBehaviorEventRepository
                .findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(null, since);

        Set<Long> activeUserIds = recentEvents.stream()
                .map(UserBehaviorEvent::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Long userId : activeUserIds) {
            try {
                aggregateProfile(userId);
            } catch (Exception e) {
                log.warn("Failed to aggregate profile for user {}: {}", userId, e.getMessage());
            }
        }
    }

    @Override
    public Map<String, Double> getPreferredCities(Long userId) {
        return userPreferenceProfileRepository.findByUserId(userId)
                .map(p -> parseJsonWeights(p.getPreferredCityJson()))
                .orElse(Collections.emptyMap());
    }

    @Override
    public Map<String, Double> getPreferredTypes(Long userId) {
        return userPreferenceProfileRepository.findByUserId(userId)
                .map(p -> parseJsonWeights(p.getPreferredTypeJson()))
                .orElse(Collections.emptyMap());
    }

    @Override
    public Map<String, Double> getPreferredAmenities(Long userId) {
        return userPreferenceProfileRepository.findByUserId(userId)
                .map(p -> parseJsonWeights(p.getPreferredAmenityJson()))
                .orElse(Collections.emptyMap());
    }

    @Override
    public BigDecimal[] getPriceRange(Long userId) {
        return userPreferenceProfileRepository.findByUserId(userId)
                .map(p -> new BigDecimal[]{p.getMinPrice(), p.getMaxPrice()})
                .orElse(new BigDecimal[]{null, null});
    }

    private Map<String, Double> aggregateWeights(List<Object[]> aggregates) {
        if (aggregates == null || aggregates.isEmpty()) {
            return Collections.emptyMap();
        }
        long total = aggregates.stream().mapToLong(a -> (Long) a[1]).sum();
        if (total == 0) {
            return Collections.emptyMap();
        }
        Map<String, Double> weights = new LinkedHashMap<>();
        for (Object[] agg : aggregates) {
            String key = (String) agg[0];
            Long count = (Long) agg[1];
            weights.put(key, count.doubleValue() / total);
        }
        return weights;
    }

    private BigDecimal[] resolvePriceRange(List<Object[]> results) {
        if (results == null || results.isEmpty() || results.get(0)[0] == null) {
            return new BigDecimal[]{null, null};
        }
        Object[] result = results.get(0);
        BigDecimal min = result[0] instanceof BigDecimal ? (BigDecimal) result[0]
                : BigDecimal.valueOf(((Number) result[0]).doubleValue());
        BigDecimal max = result[1] instanceof BigDecimal ? (BigDecimal) result[1]
                : BigDecimal.valueOf(((Number) result[1]).doubleValue());
        return new BigDecimal[]{min, max};
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> parseJsonWeights(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse preference json: {}", json);
            return Collections.emptyMap();
        }
    }
}
