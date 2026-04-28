package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.UserBehaviorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserBehaviorEventRepository extends JpaRepository<UserBehaviorEvent, Long> {

    /**
     * 查询用户最近的行为事件
     */
    List<UserBehaviorEvent> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long userId, LocalDateTime since);

    /**
     * 查询指定时间后有行为的登录用户ID
     */
    @Query("SELECT DISTINCT e.userId FROM UserBehaviorEvent e " +
           "WHERE e.userId IS NOT NULL AND e.createdAt > :since")
    List<Long> findActiveUserIdsSince(@Param("since") LocalDateTime since);

    /**
     * 查询会话最近的行为事件
     */
    List<UserBehaviorEvent> findBySessionIdAndCreatedAtAfterOrderByCreatedAtDesc(
            String sessionId, LocalDateTime since);

    /**
     * 统计用户某类事件数量
     */
    long countByUserIdAndEventTypeAndCreatedAtAfter(
            Long userId, String eventType, LocalDateTime since);

    /**
     * 查询用户最近浏览/收藏的房源ID
     */
    @Query("SELECT DISTINCT e.homestayId FROM UserBehaviorEvent e " +
           "WHERE e.userId = :userId AND e.eventType IN (:eventTypes) AND e.createdAt > :since " +
           "ORDER BY e.createdAt DESC")
    List<Long> findRecentHomestayIdsByUserIdAndEventTypes(
            @Param("userId") Long userId,
            @Param("eventTypes") List<String> eventTypes,
            @Param("since") LocalDateTime since);

    /**
     * 聚合用户偏好数据（城市）
     */
    @Query("SELECT e.cityCode, COUNT(e) as cnt FROM UserBehaviorEvent e " +
           "WHERE e.userId = :userId AND e.cityCode IS NOT NULL AND e.createdAt > :since " +
           "GROUP BY e.cityCode ORDER BY cnt DESC")
    List<Object[]> aggregateCityPreferences(
            @Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * 聚合用户偏好数据（房型）
     */
    @Query("SELECT e.type, COUNT(e) as cnt FROM UserBehaviorEvent e " +
           "WHERE e.userId = :userId AND e.type IS NOT NULL AND e.createdAt > :since " +
           "GROUP BY e.type ORDER BY cnt DESC")
    List<Object[]> aggregateTypePreferences(
            @Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * 聚合用户偏好数据（设施）
     */
    @Query("SELECT a.value, COUNT(e) as cnt FROM UserBehaviorEvent e, Homestay h " +
           "JOIN h.amenities a " +
           "WHERE e.userId = :userId AND e.homestayId = h.id AND e.createdAt > :since " +
           "GROUP BY a.value ORDER BY cnt DESC")
    List<Object[]> aggregateAmenityPreferences(
            @Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * 获取用户价格范围
     */
    @Query("SELECT MIN(e.price), MAX(e.price) FROM UserBehaviorEvent e " +
           "WHERE e.userId = :userId AND e.price IS NOT NULL AND e.createdAt > :since")
    List<Object[]> findPriceRangeByUserId(
            @Param("userId") Long userId, @Param("since") LocalDateTime since);
}
