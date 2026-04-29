package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.CouponTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface CouponTemplateRepository extends JpaRepository<CouponTemplate, Long>, JpaSpecificationExecutor<CouponTemplate> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ct FROM CouponTemplate ct WHERE ct.id = :id")
    Optional<CouponTemplate> findByIdForUpdate(@Param("id") Long id);

    List<CouponTemplate> findByStatusAndValidStartAtLessThanEqualAndValidEndAtGreaterThanEqual(
            String status, LocalDateTime now1, LocalDateTime now2);

    @Modifying
    @Query("UPDATE CouponTemplate ct SET ct.issuedCount = ct.issuedCount + 1 WHERE ct.id = :id AND ct.issuedCount < ct.totalStock")
    int incrementIssuedCount(@Param("id") Long id);

    List<CouponTemplate> findByIsNewUserCouponTrueAndStatusAndValidStartAtLessThanEqualAndValidEndAtGreaterThanEqual(
            String status, LocalDateTime now1, LocalDateTime now2);

    /**
     * 查询新人券模板，支持 validEndAt 为 null（领取后N天有效类型）
     */
    @Query("SELECT ct FROM CouponTemplate ct WHERE ct.isNewUserCoupon = true AND ct.status = :status " +
           "AND (ct.validStartAt IS NULL OR ct.validStartAt <= :now) " +
           "AND (ct.validEndAt IS NULL OR ct.validEndAt >= :now)")
    List<CouponTemplate> findActiveNewUserTemplates(@Param("status") String status, @Param("now") LocalDateTime now);

    List<CouponTemplate> findByAutoIssueTriggerAndStatus(String autoIssueTrigger, String status);
}
