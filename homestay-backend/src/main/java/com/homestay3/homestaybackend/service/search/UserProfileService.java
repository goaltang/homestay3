package com.homestay3.homestaybackend.service.search;

import com.homestay3.homestaybackend.entity.UserPreferenceProfile;

import java.util.Map;
import java.util.Optional;

public interface UserProfileService {

    /**
     * 获取用户画像
     */
    Optional<UserPreferenceProfile> getProfile(Long userId);

    /**
     * 聚合用户画像（按行为事件重新计算）
     */
    void aggregateProfile(Long userId);

    /**
     * 批量聚合所有活跃用户画像
     */
    void aggregateAllActiveProfiles();

    /**
     * 获取用户偏好城市权重
     */
    Map<String, Double> getPreferredCities(Long userId);

    /**
     * 获取用户偏好房型权重
     */
    Map<String, Double> getPreferredTypes(Long userId);

    /**
     * 获取用户偏好设施权重
     */
    Map<String, Double> getPreferredAmenities(Long userId);

    /**
     * 获取用户价格范围
     */
    java.math.BigDecimal[] getPriceRange(Long userId);
}
