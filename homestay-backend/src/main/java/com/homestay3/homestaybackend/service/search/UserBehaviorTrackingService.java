package com.homestay3.homestaybackend.service.search;

import com.homestay3.homestaybackend.entity.UserBehaviorEvent;

import java.math.BigDecimal;
import java.util.List;

public interface UserBehaviorTrackingService {

    /**
     * 记录房源浏览事件
     */
    void trackView(Long userId, String sessionId, Long homestayId,
                   String cityCode, String type, BigDecimal price);

    /**
     * 记录搜索事件
     */
    void trackSearch(Long userId, String sessionId, String keyword,
                     String cityCode, String type);

    /**
     * 记录点击事件
     */
    void trackClick(Long userId, String sessionId, Long homestayId);

    /**
     * 记录收藏事件
     */
    void trackFavorite(Long userId, String sessionId, Long homestayId,
                       String cityCode, String type, BigDecimal price);

    /**
     * 记录预订事件
     */
    void trackBooking(Long userId, String sessionId, Long homestayId,
                      String cityCode, String type, BigDecimal price);

    /**
     * 记录分享事件
     */
    void trackShare(Long userId, String sessionId, Long homestayId);

    /**
     * 查询用户最近事件
     */
    List<UserBehaviorEvent> getRecentEvents(Long userId, int days);
}
