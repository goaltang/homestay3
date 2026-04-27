package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 已应用的定价规则DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppliedPricingRuleDTO {
    private Long ruleId;
    private String ruleName;
    private String ruleType;
    private String adjustmentType;
    private BigDecimal adjustmentValue;
    private BigDecimal deltaAmount;
}
