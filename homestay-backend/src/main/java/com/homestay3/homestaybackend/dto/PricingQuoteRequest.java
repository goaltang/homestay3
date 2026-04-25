package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 统一报价请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingQuoteRequest {
    private Long homestayId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guestCount;
    private List<Long> couponIds;
}
