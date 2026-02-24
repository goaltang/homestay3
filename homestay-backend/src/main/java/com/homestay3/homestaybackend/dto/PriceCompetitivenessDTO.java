package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 价格竞争力分析结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceCompetitivenessDTO {
    
    // 当前房源价格
    private BigDecimal currentPrice;
    
    // 比较范围信息
    private String comparisonScope; // "同区域同类型", "同城市同类型", "同类型"
    private int comparableHomestaysCount; // 参与比较的房源数量
    
    // 市场价格数据
    private BigDecimal averagePrice; // 平均价格
    private BigDecimal medianPrice; // 中位数价格
    private BigDecimal minPrice; // 最低价格
    private BigDecimal maxPrice; // 最高价格
    
    // 竞争力指标
    private double percentileBelowCurrentPrice; // 低于当前价格的房源百分比
    private double priceDifferenceFromAverage; // 与平均价格的差异百分比
    private double priceDifferenceFromMedian; // 与中位数的差异百分比
    
    // 竞争力等级
    private PriceCompetitivenessLevel competitivenessLevel;
    
    // 季节性调整
    private boolean isSeasonalPeak; // 是否为旺季
    private BigDecimal seasonalAdjustedAverage; // 季节性调整后的平均价格
    
    public enum PriceCompetitivenessLevel {
        EXTREMELY_COMPETITIVE, // 极具竞争力 (低于市场30%+)
        HIGHLY_COMPETITIVE,    // 高度竞争力 (低于市场15-30%)
        COMPETITIVE,           // 有竞争力 (低于市场5-15%)
        MARKET_RATE,           // 市场价格 (±5%)
        ABOVE_MARKET,          // 高于市场 (高于市场5-20%)
        PREMIUM                // 溢价定位 (高于市场20%+)
    }
} 