package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.homestay3.homestaybackend.dto.AmenityDTO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomestayDTO {
    private Long id;
    private String title;
    private String type;
    private String price;
    private String status;
    private Integer maxGuests;
    private Integer minNights;
    private String provinceText;
    private String cityText;
    private String districtText;
    private String addressDetail;
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    
    @Builder.Default
    private List<AmenityDTO> amenities = new ArrayList<>();
    
    private String description;
    private String coverImage;
    
    @Builder.Default
    private List<String> images = new ArrayList<>();
    
    private String ownerUsername;
    private String ownerName;
    private String ownerAvatar;
    private Double ownerRating;
    private Boolean featured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 