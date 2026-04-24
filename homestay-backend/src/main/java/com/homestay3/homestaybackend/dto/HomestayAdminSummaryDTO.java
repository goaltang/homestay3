package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomestayAdminSummaryDTO {

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
    private Long ownerId;
    private String ownerUsername;
    private String ownerName;
    private Boolean featured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double latitude;
    private Double longitude;
}
