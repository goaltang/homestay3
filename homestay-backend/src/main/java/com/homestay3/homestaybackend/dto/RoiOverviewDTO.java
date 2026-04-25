package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * ROI 分析概览 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoiOverviewDTO {

    /** 优惠带动的 GMV（订单实付金额） */
    private BigDecimal totalGmv;

    /** 优惠总成本 */
    private BigDecimal totalDiscountCost;

    /** 优惠带动的订单数（去重） */
    private Long totalOrders;

    /** 总优惠使用次数 */
    private Long totalUsageCount;

    /** 已核销次数 */
    private Long usedCount;

    /** 平台承担金额 */
    private BigDecimal platformCost;

    /** 房东承担金额 */
    private BigDecimal hostCost;

    /** ROI = GMV / 优惠成本（保留2位小数） */
    private BigDecimal roi;

    /** 核销率 % */
    private BigDecimal usageRate;

    /** 平均每单优惠金额 */
    private BigDecimal avgDiscountPerOrder;

    /** 平均客单价 */
    private BigDecimal avgOrderValue;
}
