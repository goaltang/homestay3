package com.homestay3.homestaybackend.service;

import java.time.LocalDateTime;
import java.util.Map;

public interface CouponAnalyticsService {

    /**
     * 记录优惠券分析事件
     */
    void track(Long templateId, Long campaignId, String channel, String eventType,
               Long userId, Long homestayId, Long orderId);

    /**
     * 获取优惠券转化漏斗
     */
    Map<String, Object> getCouponFunnel(Long templateId, LocalDateTime start, LocalDateTime end);

    /**
     * 获取渠道归因统计
     */
    Map<String, Object> getChannelStats(Long templateId, LocalDateTime start, LocalDateTime end);
}
