package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestedFeatureDTO {
    private String featureId; // e.g., "TYPE_SPACIOUS_VILLA", "AMENITY_KITCHEN_WIFI"
    private String iconName;  // Element Plus icon name (e.g., "House", "KnifeFork", "Location")
    private String title;     // Feature title (e.g., "宽敞别墅", "设备齐全的厨房", "黄金地理位置")
    private String description; // Detailed description of the feature
    private Integer priority; // Optional: for sorting or emphasizing certain features
} 