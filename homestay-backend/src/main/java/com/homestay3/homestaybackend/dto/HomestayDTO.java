package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomestayDTO {
    private Long id;
    private String title;
    private String description;
    private String location;
    private String city;
    private String country;
    
    private BigDecimal pricePerNight;
    private Integer maxGuests;
    private Integer bedrooms;
    private Integer beds;
    private Integer bathrooms;
    
    private List<String> amenities;
    private List<String> images;
    
    private Double rating;
    private Integer reviewCount;
    
    private Double latitude;
    private Double longitude;
    
    private String hostName;
    private Long hostId;
    
    private boolean featured;
    private String propertyType;
    
    private Double distanceFromCenter;
} 