package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.CouponAnalytics;
import com.homestay3.homestaybackend.repository.CouponAnalyticsRepository;
import com.homestay3.homestaybackend.service.CouponAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponAnalyticsServiceImpl implements CouponAnalyticsService {

    private final CouponAnalyticsRepository analyticsRepository;

    @Override
    @Transactional
    public void track(Long templateId, Long campaignId, String channel, String eventType,
                      Long userId, Long homestayId, Long orderId) {
        try {
            CouponAnalytics analytics = CouponAnalytics.builder()
                    .templateId(templateId)
                    .campaignId(campaignId)
                    .channel(channel)
                    .eventType(eventType)
                    .userId(userId)
                    .homestayId(homestayId)
                    .orderId(orderId)
                    .build();
            analyticsRepository.save(analytics);
        } catch (Exception e) {
            log.warn("优惠券埋点失败: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCouponFunnel(Long templateId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = analyticsRepository.countByTemplateIdAndEventType(templateId, start, end);

        Map<String, Long> eventCounts = new HashMap<>();
        for (Object[] row : rows) {
            String eventType = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            eventCounts.put(eventType, count);
        }

        long impression = eventCounts.getOrDefault("IMPRESSION", 0L);
        long click = eventCounts.getOrDefault("CLICK", 0L);
        long claim = eventCounts.getOrDefault("CLAIM", 0L);
        long lock = eventCounts.getOrDefault("LOCK", 0L);
        long use = eventCounts.getOrDefault("USE", 0L);

        Map<String, Object> funnel = new LinkedHashMap<>();
        funnel.put("曝光", Map.of("count", impression, "conversionRate", calcRate(click, impression)));
        funnel.put("点击", Map.of("count", click, "conversionRate", calcRate(claim, click)));
        funnel.put("领取", Map.of("count", claim, "conversionRate", calcRate(lock, claim)));
        funnel.put("锁定(下单)", Map.of("count", lock, "conversionRate", calcRate(use, lock)));
        funnel.put("核销", Map.of("count", use, "conversionRate", 100));

        Map<String, Object> result = new HashMap<>();
        result.put("templateId", templateId);
        result.put("start", start);
        result.put("end", end);
        result.put("funnel", funnel);
        result.put("totalClaim", claim);
        result.put("totalUse", use);
        result.put("usageRate", calcRate(use, claim));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getChannelStats(Long templateId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = analyticsRepository.countByTemplateIdAndChannelAndEventType(templateId, start, end);

        Map<String, Map<String, Long>> channelData = new HashMap<>();
        for (Object[] row : rows) {
            String channel = (String) row[0];
            String eventType = (String) row[1];
            Long count = ((Number) row[2]).longValue();
            channelData.computeIfAbsent(channel, k -> new HashMap<>()).put(eventType, count);
        }

        List<Map<String, Object>> channelList = new ArrayList<>();
        for (Map.Entry<String, Map<String, Long>> entry : channelData.entrySet()) {
            Map<String, Long> events = entry.getValue();
            long claim = events.getOrDefault("CLAIM", 0L);
            long use = events.getOrDefault("USE", 0L);
            Map<String, Object> item = new HashMap<>();
            item.put("channel", entry.getKey());
            item.put("impression", events.getOrDefault("IMPRESSION", 0L));
            item.put("click", events.getOrDefault("CLICK", 0L));
            item.put("claim", claim);
            item.put("use", use);
            item.put("usageRate", calcRate(use, claim));
            channelList.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("templateId", templateId);
        result.put("channels", channelList);
        return result;
    }

    private BigDecimal calcRate(long numerator, long denominator) {
        if (denominator <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}
