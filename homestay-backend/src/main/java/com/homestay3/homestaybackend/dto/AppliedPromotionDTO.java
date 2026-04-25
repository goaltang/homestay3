package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 已应用的优惠信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppliedPromotionDTO {
    private String type;
    private String name;
    private BigDecimal discountAmount;
    private String bearer;
    private Long campaignId;
    private Long couponId;
}
