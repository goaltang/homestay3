package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.AvailableCouponDTO;
import com.homestay3.homestaybackend.dto.CouponDiscountResult;
import com.homestay3.homestaybackend.entity.CouponTemplate;
import com.homestay3.homestaybackend.entity.UserCoupon;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券服务
 */
public interface CouponService {

    /**
     * 领取优惠券
     */
    UserCoupon claimCoupon(Long userId, Long templateId);

    /**
     * 获取用户当前可用的优惠券列表
     */
    List<UserCoupon> getAvailableCoupons(Long userId);

    /**
     * 获取用户可用优惠券DTO列表（用于前端展示）
     */
    List<AvailableCouponDTO> getAvailableCouponDTOs(Long userId);

    /**
     * 按状态获取用户的优惠券列表（用于"我的优惠券"页面）
     */
    List<AvailableCouponDTO> getMyCoupons(Long userId, String status);

    /**
     * 计算指定优惠券的优惠金额及承担方
     *
     * @param userCouponId 用户优惠券ID
     * @param homestayId   房源ID
     * @param originalAmount 原始金额（房费合计）
     * @return 优惠结果（金额+承担方），如果不适用则返回0金额
     */
    CouponDiscountResult calculateCouponDiscount(Long userCouponId, Long homestayId, BigDecimal originalAmount);

    /**
     * 批量计算优惠券优惠（用于报价时）
     *
     * @param userCouponIds 用户优惠券ID列表
     * @param homestayId   房源ID
     * @param originalAmount 原始金额
     * @param userId       当前用户ID（用于越权校验）
     */
    CouponDiscountResult calculateCouponDiscount(List<Long> userCouponIds, Long homestayId, BigDecimal originalAmount, Long userId);

    /**
     * 锁定优惠券（下单时）
     *
     * @param userCouponIds 用户优惠券ID列表
     * @param orderId       订单ID
     * @param userId        当前用户ID（用于越权校验与原子锁定）
     */
    void lockCoupons(List<Long> userCouponIds, Long orderId, Long userId);

    /**
     * 释放锁定的优惠券（取消订单时）
     */
    void releaseCoupons(Long orderId);

    /**
     * 使用优惠券（支付成功时）
     */
    void useCoupons(Long orderId);

    /**
     * 创建优惠券模板（管理后台）
     */
    CouponTemplate createTemplate(CouponTemplate template);

    /**
     * 检查优惠券是否适用于指定房源
     */
    boolean isApplicable(UserCoupon userCoupon, Long homestayId);
}
