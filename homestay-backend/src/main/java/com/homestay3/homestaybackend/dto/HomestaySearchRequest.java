package com.homestay3.homestaybackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class HomestaySearchRequest {
    private String location;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer guestCount;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String propertyType;
    private List<String> amenities;
    private Integer bedrooms;
    private Integer beds;
    private Integer bathrooms;
    private Double minRating;
} 