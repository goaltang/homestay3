package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 统一计价服务
 * 所有价格计算的唯一入口，确保实时报价和下单价格一致
 */
public interface PricingService {

    /**
     * 统一报价接口
     * 根据房源、日期、人数、优惠券等计算完整价格明细
     *
     * @param request 报价请求
     * @param userId  当前用户ID（可为null，用于活动匹配）
     * @return 报价响应
     */
    PricingQuoteResponse quote(PricingQuoteRequest request, Long userId);

    /**
     * 内部计价接口（供Service层使用）
     *
     * @param homestayId   房源ID
     * @param checkInDate  入住日期
     * @param checkOutDate 退房日期
     * @param guestCount   入住人数
     * @param couponIds    优惠券ID列表
     * @param userId       用户ID
     * @return 计价结果
     */
    PricingResult calculate(Long homestayId, LocalDate checkInDate, LocalDate checkOutDate,
                            Integer guestCount, List<Long> couponIds, Long userId);

    /**
     * 保存订单价格快照
     *
     * @param orderId 订单ID
     * @param result  计价结果
     * @param quoteToken 报价令牌
     */
    void savePriceSnapshot(Long orderId, PricingResult result, String quoteToken);

    /**
     * 获取订单价格快照
     */
    PricingResult getPriceSnapshot(Long orderId);

    /**
     * 生成报价令牌
     */
    String generateQuoteToken();

    /**
     * 校验报价令牌
     * @param quoteToken 报价令牌
     * @param orderDTO 订单数据（用于比对参数和金额）
     * @param userId 当前用户ID
     * @return 如果 token 有效且金额一致返回 true；否则抛出 PriceChangedException
     */
    boolean validateQuoteToken(String quoteToken, com.homestay3.homestaybackend.dto.OrderDTO orderDTO, Long userId);
}
