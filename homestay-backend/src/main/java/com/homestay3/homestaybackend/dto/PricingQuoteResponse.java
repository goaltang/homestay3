package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一报价响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingQuoteResponse {
    private String quoteToken;
    private LocalDateTime expiresAt;
    private Long homestayId;
    private Integer nights;
    private BigDecimal roomOriginalAmount;
    private BigDecimal activityDiscountAmount;
    private BigDecimal fullReductionAmount;
    private BigDecimal couponDiscountAmount;
    private BigDecimal platformDiscountAmount;
    private BigDecimal hostDiscountAmount;
    private BigDecimal cleaningFee;
    private BigDecimal serviceFee;
    private BigDecimal payableAmount;
    private BigDecimal hostReceivableAmount;
    private List<PriceCalculationResponse.DailyPrice> dailyPrices;
    private List<AppliedPromotionDTO> appliedPromotions;
    private List<AvailableCouponDTO> availableCoupons;
    private PriceCalculationResponse.PriceDetails priceDetails;
}
