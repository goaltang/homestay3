package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomestaySearchRequest {
    private String keyword;
    private String location;
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String propertyType;
    private Integer minGuests;
    private Integer maxGuests;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private List<String> requiredAmenities;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
} 