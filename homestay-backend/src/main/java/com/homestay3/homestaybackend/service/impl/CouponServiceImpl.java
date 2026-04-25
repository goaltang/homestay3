package com.homestay3.homestaybackend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay3.homestaybackend.dto.AvailableCouponDTO;
import com.homestay3.homestaybackend.dto.CouponDiscountResult;
import com.homestay3.homestaybackend.entity.CouponTemplate;
import com.homestay3.homestaybackend.entity.UserCoupon;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.repository.CouponTemplateRepository;
import com.homestay3.homestaybackend.repository.UserCouponRepository;
import com.homestay3.homestaybackend.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponServiceImpl implements CouponService {

    private final CouponTemplateRepository templateRepository;
    private final UserCouponRepository userCouponRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public UserCoupon claimCoupon(Long userId, Long templateId) {
        CouponTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("优惠券模板不存在"));

        if (!"ACTIVE".equals(template.getStatus())) {
            throw new IllegalArgumentException("优惠券模板未激活");
        }

        // 库存检查
        if (template.getTotalStock() > 0 && template.getIssuedCount() >= template.getTotalStock()) {
            throw new IllegalArgumentException("优惠券已领完");
        }

        // 限领检查
        long userClaimedCount = userCouponRepository.countByUserIdAndTemplateId(userId, templateId);
        if (userClaimedCount >= template.getPerUserLimit()) {
            throw new IllegalArgumentException("已达到该优惠券领取上限");
        }

        // 有效期计算
        LocalDateTime expireAt;
        if ("AFTER_CLAIM_DAYS".equals(template.getValidType()) && template.getValidDays() != null) {
            expireAt = LocalDateTime.now().plusDays(template.getValidDays());
        } else {
            expireAt = template.getValidEndAt();
        }

        if (expireAt != null && expireAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("优惠券已过期");
        }

        // 生成券码
        String couponCode = "CP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        UserCoupon userCoupon = UserCoupon.builder()
                .template(template)
                .userId(userId)
                .couponCode(couponCode)
                .status("AVAILABLE")
                .expireAt(expireAt != null ? expireAt : LocalDateTime.now().plusYears(1))
                .build();

        // 增加已领取计数
        if (template.getTotalStock() > 0) {
            int updated = templateRepository.incrementIssuedCount(templateId);
            if (updated == 0) {
                throw new IllegalArgumentException("优惠券库存不足");
            }
        }

        return userCouponRepository.save(userCoupon);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserCoupon> getAvailableCoupons(Long userId) {
        // 先清理过期券
        userCouponRepository.expireOutdatedCoupons(LocalDateTime.now());
        return userCouponRepository.findByUserIdAndStatusAndExpireAtAfterOrderByExpireAtAsc(
                userId, "AVAILABLE", LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableCouponDTO> getAvailableCouponDTOs(Long userId) {
        return getAvailableCoupons(userId).stream()
                .map(this::toAvailableCouponDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CouponDiscountResult calculateCouponDiscount(Long userCouponId, Long homestayId, BigDecimal originalAmount) {
        // 向后兼容：不校验用户归属
        return calculateCouponDiscountInternal(userCouponId, homestayId, originalAmount, null);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponDiscountResult calculateCouponDiscount(List<Long> userCouponIds, Long homestayId, BigDecimal originalAmount, Long userId) {
        if (userCouponIds == null || userCouponIds.isEmpty()) {
            return CouponDiscountResult.builder()
                    .discountAmount(BigDecimal.ZERO)
                    .subsidyBearer("PLATFORM")
                    .build();
        }
        // MVP 阶段只支持一张券
        return calculateCouponDiscountInternal(userCouponIds.get(0), homestayId, originalAmount, userId);
    }

    private CouponDiscountResult calculateCouponDiscountInternal(Long userCouponId, Long homestayId, BigDecimal originalAmount, Long userId) {
        UserCoupon userCoupon;
        if (userId != null) {
            userCoupon = userCouponRepository.findByIdAndUserId(userCouponId, userId).orElse(null);
        } else {
            userCoupon = userCouponRepository.findById(userCouponId).orElse(null);
        }
        if (userCoupon == null || !"AVAILABLE".equals(userCoupon.getStatus())) {
            return CouponDiscountResult.builder()
                    .discountAmount(BigDecimal.ZERO)
                    .subsidyBearer("PLATFORM")
                    .build();
        }

        // 校验过期时间
        if (userCoupon.getExpireAt() != null && userCoupon.getExpireAt().isBefore(LocalDateTime.now())) {
            return CouponDiscountResult.builder()
                    .discountAmount(BigDecimal.ZERO)
                    .subsidyBearer("PLATFORM")
                    .build();
        }

        if (!isApplicable(userCoupon, homestayId)) {
            return CouponDiscountResult.builder()
                    .discountAmount(BigDecimal.ZERO)
                    .subsidyBearer("PLATFORM")
                    .build();
        }

        CouponTemplate template = userCoupon.getTemplate();
        BigDecimal discount = BigDecimal.ZERO;

        // 门槛检查
        if (template.getThresholdAmount() != null && originalAmount.compareTo(template.getThresholdAmount()) < 0) {
            return CouponDiscountResult.builder()
                    .discountAmount(BigDecimal.ZERO)
                    .subsidyBearer(template.getSubsidyBearer())
                    .build();
        }

        switch (template.getCouponType()) {
            case "CASH":
                if (template.getFaceValue() != null) {
                    discount = template.getFaceValue();
                }
                break;
            case "DISCOUNT":
                if (template.getDiscountRate() != null) {
                    discount = originalAmount.multiply(BigDecimal.ONE.subtract(template.getDiscountRate()));
                }
                break;
            case "FULL_REDUCTION":
                if (template.getThresholdAmount() != null && originalAmount.compareTo(template.getThresholdAmount()) >= 0
                        && template.getFaceValue() != null) {
                    discount = template.getFaceValue();
                }
                break;
        }

        if (template.getMaxDiscount() != null && discount.compareTo(template.getMaxDiscount()) > 0) {
            discount = template.getMaxDiscount();
        }

        discount = discount.min(originalAmount).setScale(2, RoundingMode.HALF_UP);

        return CouponDiscountResult.builder()
                .discountAmount(discount)
                .subsidyBearer(template.getSubsidyBearer())
                .build();
    }

    @Override
    @Transactional
    public void lockCoupons(List<Long> userCouponIds, Long orderId, Long userId) {
        if (userCouponIds == null || userCouponIds.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        for (Long couponId : userCouponIds) {
            int updated = userCouponRepository.lockCoupon(couponId, userId, orderId, now);
            if (updated == 0) {
                throw new IllegalStateException("优惠券锁定失败，可能已被使用、过期或不属于当前用户，couponId=" + couponId);
            }
        }
    }

    @Override
    @Transactional
    public void releaseCoupons(Long orderId) {
        List<UserCoupon> locked = userCouponRepository.findAll().stream()
                .filter(c -> orderId.equals(c.getLockedOrderId()) && "LOCKED".equals(c.getStatus()))
                .collect(Collectors.toList());
        for (UserCoupon coupon : locked) {
            coupon.setStatus("AVAILABLE");
            coupon.setLockedOrderId(null);
            coupon.setLockedAt(null);
            userCouponRepository.save(coupon);
        }
    }

    @Override
    @Transactional
    public void useCoupons(Long orderId) {
        List<UserCoupon> locked = userCouponRepository.findAll().stream()
                .filter(c -> orderId.equals(c.getLockedOrderId()) && "LOCKED".equals(c.getStatus()))
                .collect(Collectors.toList());
        for (UserCoupon coupon : locked) {
            coupon.setStatus("USED");
            coupon.setUsedOrderId(orderId);
            coupon.setUsedAt(LocalDateTime.now());
            userCouponRepository.save(coupon);
        }
    }

    @Override
    @Transactional
    public CouponTemplate createTemplate(CouponTemplate template) {
        return templateRepository.save(template);
    }

    @Override
    public boolean isApplicable(UserCoupon userCoupon, Long homestayId) {
        CouponTemplate template = userCoupon.getTemplate();
        if ("ALL".equals(template.getScopeType())) {
            return true;
        }
        try {
            JsonNode scopeValues = objectMapper.readTree(template.getScopeValueJson());
            // 简化处理：HOMESTAY 范围
            if ("HOMESTAY".equals(template.getScopeType())) {
                for (JsonNode node : scopeValues) {
                    if (node.asLong() == homestayId) return true;
                }
                return false;
            }
            // 其他范围类型MVP暂按全部适用处理
            return true;
        } catch (Exception e) {
            log.warn("解析优惠券scopeValueJson失败: {}", e.getMessage());
            return true;
        }
    }

    private AvailableCouponDTO toAvailableCouponDTO(UserCoupon userCoupon) {
        CouponTemplate t = userCoupon.getTemplate();
        return AvailableCouponDTO.builder()
                .id(userCoupon.getId())
                .name(t.getName())
                .couponType(t.getCouponType())
                .faceValue(t.getFaceValue())
                .discountRate(t.getDiscountRate())
                .thresholdAmount(t.getThresholdAmount())
                .maxDiscount(t.getMaxDiscount())
                .expireAt(userCoupon.getExpireAt())
                .scopeType(t.getScopeType())
                .subsidyBearer(t.getSubsidyBearer())
                .build();
    }
}
