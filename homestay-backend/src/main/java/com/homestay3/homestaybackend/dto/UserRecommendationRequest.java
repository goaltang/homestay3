package com.homestay3.homestaybackend.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRecommendationRequest {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 偏好的省份代码
     */
    private String preferredProvinceCode;
    
    /**
     * 偏好的城市代码
     */
    private String preferredCityCode;
    
    /**
     * 预算范围 - 最低价格
     */
    private Integer minPrice;
    
    /**
     * 预算范围 - 最高价格
     */
    private Integer maxPrice;
    
    /**
     * 客人数量
     */
    private Integer guestCount;
    
    /**
     * 偏好的房型类型
     */
    private List<String> preferredPropertyTypes;
    
    /**
     * 必需设施
     */
    private List<String> requiredAmenities;
    
    /**
     * 偏好设施
     */
    private List<String> preferredAmenities;
    
    /**
     * 入住日期
     */
    private LocalDate checkInDate;
    
    /**
     * 退房日期
     */
    private LocalDate checkOutDate;
    
    /**
     * 最低评分要求
     */
    private Double minRating;
    
    /**
     * 是否只要求即时预订
     */
    private Boolean instantBookingOnly;
    
    /**
     * 推荐类型
     */
    private RecommendationType recommendationType;
    
    /**
     * 排序方式
     */
    private SortType sortType;
    
    public enum RecommendationType {
        POPULAR,        // 热门推荐
        PERSONALIZED,   // 个性化推荐  
        LOCATION_BASED, // 基于位置推荐
        SIMILAR,        // 相似推荐
        TRENDING,       // 趋势推荐
        VALUE_FOR_MONEY // 性价比推荐
    }
    
    public enum SortType {
        POPULARITY,     // 按热门度排序
        PRICE_LOW_HIGH, // 价格从低到高
        PRICE_HIGH_LOW, // 价格从高到低
        RATING,         // 按评分排序
        NEWEST,         // 最新上架
        DISTANCE        // 按距离排序
    }
} 