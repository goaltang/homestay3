package com.homestay3.homestaybackend.service.search.impl;

import com.homestay3.homestaybackend.dto.AmenityDTO;
import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.service.search.SearchPersonalizationService;
import com.homestay3.homestaybackend.service.search.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchPersonalizationServiceImpl implements SearchPersonalizationService {

    private final UserProfileService userProfileService;

    // 个性化维度权重
    private static final double WEIGHT_CITY = 0.30;
    private static final double WEIGHT_TYPE = 0.25;
    private static final double WEIGHT_AMENITY = 0.15;
    private static final double WEIGHT_PRICE = 0.20;
    private static final double WEIGHT_RATING = 0.10;

    @Override
    public List<HomestayDTO> boost(List<HomestayDTO> results, Long userId) {
        if (results == null || results.isEmpty() || userId == null) {
            return results;
        }

        try {
            Map<String, Double> preferredCities = userProfileService.getPreferredCities(userId);
            Map<String, Double> preferredTypes = userProfileService.getPreferredTypes(userId);
            Map<String, Double> preferredAmenities = userProfileService.getPreferredAmenities(userId);
            BigDecimal[] priceRange = userProfileService.getPriceRange(userId);

            boolean hasProfileData = !preferredCities.isEmpty()
                    || !preferredTypes.isEmpty()
                    || !preferredAmenities.isEmpty()
                    || (priceRange[0] != null && priceRange[1] != null);

            if (!hasProfileData) {
                return results;
            }

            List<HomestayDTO> boosted = results.stream()
                    .map(h -> new ScoredHomestay(h, computeScore(h, preferredCities, preferredTypes, preferredAmenities, priceRange)))
                    .sorted((a, b) -> Double.compare(b.score, a.score))
                    .map(s -> s.homestay)
                    .collect(Collectors.toList());

            log.debug("Applied personalization boost for user {}, results: {}", userId, boosted.size());
            return boosted;
        } catch (Exception e) {
            log.warn("Failed to apply search personalization for user {}: {}", userId, e.getMessage());
            return results;
        }
    }

    private double computeScore(HomestayDTO h,
                                 Map<String, Double> preferredCities,
                                 Map<String, Double> preferredTypes,
                                 Map<String, Double> preferredAmenities,
                                 BigDecimal[] priceRange) {
        double score = 0.0;

        // 城市匹配
        if (preferredCities != null && !preferredCities.isEmpty() && h.getCityCode() != null) {
            Double cityWeight = preferredCities.get(h.getCityCode());
            if (cityWeight != null) {
                score += WEIGHT_CITY * cityWeight;
            }
        }

        // 房型匹配
        if (preferredTypes != null && !preferredTypes.isEmpty() && h.getType() != null) {
            Double typeWeight = preferredTypes.get(h.getType());
            if (typeWeight != null) {
                score += WEIGHT_TYPE * typeWeight;
            }
        }

        // 设施重叠度
        if (preferredAmenities != null && !preferredAmenities.isEmpty()
                && h.getAmenities() != null && !h.getAmenities().isEmpty()) {
            List<String> amenityValues = h.getAmenities().stream()
                    .map(AmenityDTO::getValue)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!amenityValues.isEmpty()) {
                double overlapScore = 0.0;
                for (String av : amenityValues) {
                    Double aw = preferredAmenities.get(av);
                    if (aw != null) {
                        overlapScore += aw;
                    }
                }
                score += WEIGHT_AMENITY * (overlapScore / Math.max(preferredAmenities.size(), amenityValues.size()));
            }
        }

        // 价格匹配
        if (priceRange[0] != null && priceRange[1] != null && h.getPrice() != null) {
            try {
                BigDecimal price = new BigDecimal(h.getPrice());
                BigDecimal minPrice = priceRange[0];
                BigDecimal maxPrice = priceRange[1];
                if (price.compareTo(minPrice) >= 0 && price.compareTo(maxPrice) <= 0) {
                    score += WEIGHT_PRICE;
                } else {
                    double range = maxPrice.subtract(minPrice).doubleValue();
                    if (range > 0) {
                        double distance = price.compareTo(maxPrice) > 0
                                ? price.subtract(maxPrice).doubleValue()
                                : minPrice.subtract(price).doubleValue();
                        score += WEIGHT_PRICE * Math.max(0, 1 - distance / range);
                    }
                }
            } catch (NumberFormatException e) {
                // ignore invalid price
            }
        }

        // 评分基础分
        if (h.getRating() != null && h.getRating() > 0) {
            score += WEIGHT_RATING * (h.getRating() / 5.0);
        }

        return score;
    }

    private record ScoredHomestay(HomestayDTO homestay, double score) {
    }
}
