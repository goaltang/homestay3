package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.CouponAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CouponAnalyticsRepository extends JpaRepository<CouponAnalytics, Long> {

    @Query("SELECT ca.eventType, COUNT(*) FROM CouponAnalytics ca WHERE ca.templateId = :templateId AND ca.createdAt BETWEEN :start AND :end GROUP BY ca.eventType")
    List<Object[]> countByTemplateIdAndEventType(@Param("templateId") Long templateId,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);

    @Query("SELECT ca.channel, ca.eventType, COUNT(*) FROM CouponAnalytics ca WHERE ca.templateId = :templateId AND ca.createdAt BETWEEN :start AND :end GROUP BY ca.channel, ca.eventType")
    List<Object[]> countByTemplateIdAndChannelAndEventType(@Param("templateId") Long templateId,
                                                            @Param("start") LocalDateTime start,
                                                            @Param("end") LocalDateTime end);

    @Query("SELECT ca.eventType, COUNT(DISTINCT ca.userId) FROM CouponAnalytics ca WHERE ca.templateId = :templateId AND ca.createdAt BETWEEN :start AND :end GROUP BY ca.eventType")
    List<Object[]> countDistinctUserByTemplateIdAndEventType(@Param("templateId") Long templateId,
                                                              @Param("start") LocalDateTime start,
                                                              @Param("end") LocalDateTime end);
}
