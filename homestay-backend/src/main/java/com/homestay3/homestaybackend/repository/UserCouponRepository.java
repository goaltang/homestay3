package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    List<UserCoupon> findByUserIdAndStatusAndExpireAtAfterOrderByExpireAtAsc(
            Long userId, String status, LocalDateTime now);

    List<UserCoupon> findByUserIdAndStatusOrderByExpireAtAsc(Long userId, String status);

    Optional<UserCoupon> findByCouponCode(String couponCode);

    @Query("SELECT COUNT(uc) FROM UserCoupon uc WHERE uc.userId = :userId AND uc.template.id = :templateId")
    long countByUserIdAndTemplateId(@Param("userId") Long userId, @Param("templateId") Long templateId);

    @Modifying
    @Query("UPDATE UserCoupon uc SET uc.status = 'EXPIRED' WHERE uc.status = 'AVAILABLE' AND uc.expireAt < :now")
    int expireOutdatedCoupons(@Param("now") LocalDateTime now);

    Optional<UserCoupon> findByIdAndUserId(Long id, Long userId);

    /**
     * 原子锁定优惠券（乐观锁）
     * @return 影响行数，0表示锁定失败（已被使用、过期或非该用户）
     */
    @Modifying
    @Query("UPDATE UserCoupon uc SET uc.status = 'LOCKED', uc.lockedOrderId = :orderId, uc.lockedAt = :now " +
           "WHERE uc.id = :id AND uc.userId = :userId AND uc.status = 'AVAILABLE' AND uc.expireAt > :now")
    int lockCoupon(@Param("id") Long id,
                   @Param("userId") Long userId,
                   @Param("orderId") Long orderId,
                   @Param("now") LocalDateTime now);
}
