package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 可用优惠券DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableCouponDTO {
    private Long id;
    private String name;
    private String couponType;
    private BigDecimal faceValue;
    private BigDecimal discountRate;
    private BigDecimal thresholdAmount;
    private BigDecimal maxDiscount;
    private LocalDateTime expireAt;
    private String scopeType;

    /**
     * 承担方: PLATFORM(平台), HOST(房东), MIXED(混合)
     */
    private String subsidyBearer;
}
