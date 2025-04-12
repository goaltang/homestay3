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
public class HomestayDTO {
    private Long id;
    private String title;
    private String type;
    private String price;
    private String status;
    private Integer maxGuests;
    private Integer minNights;
    private String province;
    private String city;
    private String district;
    private String address;
    
    @Builder.Default
    private List<String> amenities = new ArrayList<>();
    
    private String description;
    private String coverImage;
    
    @Builder.Default
    private List<String> images = new ArrayList<>();
    
    private String ownerUsername;
    private String ownerName;
    private boolean featured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 