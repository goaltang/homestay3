package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomestaySearchRequest {
    private String keyword;
    private String location;
    private String propertyType;
    private Integer minGuests;
    private Integer maxGuests;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Boolean hasWifi;
    private Boolean hasAirConditioning;
    private Boolean hasKitchen;
    private Boolean hasWasher;
    private Boolean hasParking;
    private Boolean hasPool;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
} 