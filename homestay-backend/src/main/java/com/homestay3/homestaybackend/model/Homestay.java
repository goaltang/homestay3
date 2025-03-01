package com.homestay3.homestaybackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Homestay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    
    @ElementCollection
    private List<String> amenities = new ArrayList<>();
    
    @ElementCollection
    private List<String> images = new ArrayList<>();
    
    private Double rating;
    private Integer reviewCount;
    
    private Double latitude;
    private Double longitude;
    
    private String hostName;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User host;
    
    private boolean featured;
    private String propertyType; // 树屋、小木屋、海景房等
    
    private Double distanceFromCenter; // 距离市中心的距离（公里）
} 