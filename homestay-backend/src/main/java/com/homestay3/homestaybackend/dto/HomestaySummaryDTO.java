package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomestaySummaryDTO {

    private Long id;
    private String title;
    private String type;
    private String propertyTypeName;
    private String price;
    private String status;
    private Integer maxGuests;
    private String provinceText;
    private String cityText;
    private String districtText;
    private String addressDetail;
    private String coverImage;

    @Builder.Default
    private List<String> images = new ArrayList<>();

    private Long ownerId;
    private String ownerUsername;
    private String ownerName;
    private String ownerAvatar;
    private Double ownerRating;
    private Boolean featured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double latitude;
    private Double longitude;

    // 房源分组信息
    private Long groupId;
    private String groupName;
    private String groupCode;
    private String groupColor;
}
