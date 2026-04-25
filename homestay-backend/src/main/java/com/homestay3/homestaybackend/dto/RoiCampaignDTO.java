package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 单个活动的 ROI 分析 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoiCampaignDTO {

    private Long campaignId;
    private String campaignName;
    private String campaignType;
    private String subsidyBearer;

    /** 活动带动的 GMV */
    private BigDecimal gmv;

    /** 活动优惠成本 */
    private BigDecimal discountCost;

    /** 带动订单数 */
    private Long orderCount;

    /** 优惠使用次数 */
    private Long usageCount;

    /** 已核销次数 */
    private Long usedCount;

    /** ROI */
    private BigDecimal roi;

    /** 核销率 % */
    private BigDecimal usageRate;

    /** 预算使用率 % */
    private BigDecimal budgetUsageRate;

    /** 预算总额 */
    private BigDecimal budgetTotal;

    /** 已用预算 */
    private BigDecimal budgetUsed;
}
