package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 内部统一计价结果（供Service层使用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingResult {
    private Long homestayId;
    private Integer nights;
    private List<PriceCalculationResponse.DailyPrice> dailyPrices;
    private BigDecimal roomOriginalAmount;
    private BigDecimal activityDiscountAmount;
    private BigDecimal fullReductionAmount;
    private BigDecimal couponDiscountAmount;
    private BigDecimal platformDiscountAmount;
    private BigDecimal hostDiscountAmount;
    private BigDecimal discountedRoomAmount;
    private BigDecimal cleaningFee;
    private BigDecimal serviceFee;
    private BigDecimal payableAmount;
    private BigDecimal hostReceivableAmount;
    private List<AppliedPromotionDTO> appliedPromotions;
    private PriceCalculationResponse.PriceDetails priceDetails;
}
