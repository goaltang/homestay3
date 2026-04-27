package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.AppliedPricingRuleDTO;
import com.homestay3.homestaybackend.dto.PriceCalculationResponse;
import com.homestay3.homestaybackend.entity.Homestay;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 定价引擎服务
 * 负责房源基础价 -> 日期级调价 -> 入住级折扣 的计算
 * 与营销活动/优惠券解耦
 */
public interface PricingEngineService {

    /**
     * 计算每日价格明细（步骤 1~2：日期级调价）
     */
    List<PriceCalculationResponse.DailyPrice> calculateDailyPrices(
            Homestay homestay, LocalDate checkInDate, LocalDate checkOutDate);

    /**
     * 计算入住级折扣（步骤 3：早鸟、连住）
     * 返回折扣乘数（如 0.90 表示九折）和应用的规则列表
     */
    StayDiscountResult calculateStayDiscounts(
            Homestay homestay, LocalDate checkInDate, LocalDate checkOutDate,
            int nights, int advanceDays);

    /**
     * 入住级折扣结果
     */
    record StayDiscountResult(
            BigDecimal multiplier,
            List<AppliedPricingRuleDTO> appliedRules
    ) {}
}
