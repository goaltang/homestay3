package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {

    List<PromotionUsage> findByOrderId(Long orderId);

    @Modifying
    @Query("UPDATE PromotionUsage pu SET pu.status = :status WHERE pu.orderId = :orderId")
    int updateStatusByOrderId(@Param("orderId") Long orderId, @Param("status") String status);

    List<PromotionUsage> findByUserIdAndCampaignId(Long userId, Long campaignId);
}
