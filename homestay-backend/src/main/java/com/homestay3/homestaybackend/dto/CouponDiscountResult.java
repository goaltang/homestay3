package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 优惠券折扣计算结果，包含优惠金额和承担方信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponDiscountResult {

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 承担方: PLATFORM(平台), HOST(房东), MIXED(混合)
     */
    private String subsidyBearer;

    @Builder.Default
    private BigDecimal platformDiscountAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal hostDiscountAmount = BigDecimal.ZERO;

    @Builder.Default
    private List<Long> effectiveCouponIds = new ArrayList<>();
}
