package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.PromotionCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionCampaignRepository extends JpaRepository<PromotionCampaign, Long>, JpaSpecificationExecutor<PromotionCampaign> {

    @Query("SELECT pc FROM PromotionCampaign pc LEFT JOIN FETCH pc.rules WHERE pc.status = 'ACTIVE' " +
            "AND pc.startAt <= :now AND pc.endAt >= :now ORDER BY pc.priority DESC")
    List<PromotionCampaign> findActiveCampaigns(@Param("now") LocalDateTime now);

    List<PromotionCampaign> findByStatusOrderByPriorityDesc(String status);

    List<PromotionCampaign> findByHostIdAndStatus(Long hostId, String status);

    List<PromotionCampaign> findByHostIdOrderByCreatedAtDesc(Long hostId);

    /**
     * 扣减活动预算（乐观锁）
     * @return 影响行数，0表示扣减失败（预算不足）
     */
    @Modifying
    @Query("UPDATE PromotionCampaign c SET c.budgetUsed = c.budgetUsed + :amount WHERE c.id = :id AND (c.budgetTotal IS NULL OR c.budgetUsed + :amount <= c.budgetTotal)")
    int deductBudget(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * 回退活动预算（乐观锁）
     * @return 影响行数，0表示回退失败
     */
    @Modifying
    @Query("UPDATE PromotionCampaign c SET c.budgetUsed = c.budgetUsed - :amount WHERE c.id = :id AND c.budgetUsed >= :amount")
    int refundBudget(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * 查询待自动启用的活动：状态为DRAFT，且已到达开始时间
     */
    @Query("SELECT pc FROM PromotionCampaign pc LEFT JOIN FETCH pc.rules WHERE pc.status = 'DRAFT' AND pc.startAt <= :now AND pc.endAt >= :now")
    List<PromotionCampaign> findDraftCampaignsReadyToActivate(@Param("now") LocalDateTime now);

    /**
     * 查询待自动结束的活动：状态为ACTIVE，且结束时间已过
     */
    @Query("SELECT pc FROM PromotionCampaign pc WHERE pc.status = 'ACTIVE' AND pc.endAt < :now")
    List<PromotionCampaign> findActiveCampaignsPastEndTime(@Param("now") LocalDateTime now);

    /**
     * 原子更新预算预警标志（避免竞态条件）
     * @return 影响行数
     */
    @Modifying
    @Query("UPDATE PromotionCampaign c SET c.budgetAlertTriggered = true " +
           "WHERE c.id = :id AND c.budgetTotal IS NOT NULL AND c.budgetTotal > 0 " +
           "AND c.budgetUsed >= c.budgetTotal * :threshold AND c.budgetAlertTriggered = false")
    int triggerBudgetAlert(@Param("id") Long id, @Param("threshold") BigDecimal threshold);

    /**
     * 原子更新活动状态为ENDED（预算耗尽时）
     * @return 影响行数
     */
    @Modifying
    @Query("UPDATE PromotionCampaign c SET c.status = 'ENDED' " +
           "WHERE c.id = :id AND c.budgetTotal IS NOT NULL AND c.budgetUsed >= c.budgetTotal AND c.status != 'ENDED'")
    int endCampaignByBudgetExhaustion(@Param("id") Long id);
}
