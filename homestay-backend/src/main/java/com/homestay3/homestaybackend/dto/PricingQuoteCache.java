package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 报价缓存对象（存储于 Redis）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingQuoteCache {
    private String quoteToken;
    private String requestHash;
    private Long homestayId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guestCount;
    private String couponIdsJson;
    private BigDecimal payableAmount;
    private LocalDateTime expiresAt;
}
