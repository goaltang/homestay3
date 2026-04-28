package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.CouponTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CouponTemplateRepository extends JpaRepository<CouponTemplate, Long>, JpaSpecificationExecutor<CouponTemplate> {

    List<CouponTemplate> findByStatusAndValidStartAtLessThanEqualAndValidEndAtGreaterThanEqual(
            String status, LocalDateTime now1, LocalDateTime now2);

    @Modifying
    @Query("UPDATE CouponTemplate ct SET ct.issuedCount = ct.issuedCount + 1 WHERE ct.id = :id AND ct.issuedCount < ct.totalStock")
    int incrementIssuedCount(@Param("id") Long id);

    List<CouponTemplate> findByIsNewUserCouponTrueAndStatusAndValidStartAtLessThanEqualAndValidEndAtGreaterThanEqual(
            String status, LocalDateTime now1, LocalDateTime now2);

    List<CouponTemplate> findByAutoIssueTriggerAndStatus(String autoIssueTrigger, String status);
}
