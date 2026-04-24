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
public class HomestayAdminDetailDTO {

    private Long id;
    private String title;
    private String type;
    private String propertyTypeName;
    private String price;
    private String status;
    private Integer maxGuests;
    private Integer minNights;
    private Integer maxNights;
    private String provinceText;
    private String cityText;
    private String districtText;
    private String addressDetail;
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String description;
    private String coverImage;

    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Builder.Default
    private List<AmenityDTO> amenities = new ArrayList<>();

    private Long ownerId;
    private String ownerUsername;
    private String ownerName;
    private String ownerAvatar;
    private Double ownerRating;
    private String ownerPhone;
    private String ownerEmail;
    private String ownerRealName;
    private String ownerNickname;
    private String ownerOccupation;
    private String ownerIntroduction;
    private LocalDateTime ownerJoinDate;
    private LocalDateTime ownerHostSince;
    private Long ownerHomestayCount;
    private Double ownerHostRating;
    private Boolean featured;
    private Boolean autoConfirm;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double latitude;
    private Double longitude;
}
