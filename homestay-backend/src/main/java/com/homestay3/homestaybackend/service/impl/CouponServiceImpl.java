package com.homestay3.homestaybackend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay3.homestaybackend.dto.AvailableCouponDTO;
import com.homestay3.homestaybackend.dto.CouponDiscountResult;
import com.homestay3.homestaybackend.entity.CouponTemplate;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.UserCoupon;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.repository.CouponTemplateRepository;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.UserCouponRepository;
import com.homestay3.homestaybackend.service.CouponService;
import com.homestay3.homestaybackend.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponServiceImpl implements CouponService {

    private final CouponTemplateRepository templateRepository;
    private final UserCouponRepository userCouponRepository;
    private final HomestayRepository homestayRepository;
    private final ObjectMapper objectMapper;
    private final SystemConfigService systemConfigService;

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

        // 1. 计算每张券的折扣明细
        List<CouponDiscountDetail> details = new ArrayList<>();
        for (Long couponId : userCouponIds) {
            CouponDiscountResult result = calculateCouponDiscountInternal(couponId, homestayId, originalAmount, userId);
            if (result.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                UserCoupon uc = findUserCoupon(couponId, userId);
                if (uc != null && uc.getTemplate() != null) {
                    details.add(new CouponDiscountDetail(couponId, uc.getTemplate().getStackGroup(),
                            result.getDiscountAmount(), result.getSubsidyBearer()));
                }
            }
        }

        // 2. 互斥组校验：同一 stack_group（非 DEFAULT）只保留优惠最大的一张
        Map<String, CouponDiscountDetail> bestByGroup = new LinkedHashMap<>();
        for (CouponDiscountDetail detail : details) {
            String group = detail.stackGroup;
            if (!"DEFAULT".equals(group)) {
                CouponDiscountDetail existing = bestByGroup.get(group);
                if (existing == null || detail.discount.compareTo(existing.discount) > 0) {
                    bestByGroup.put(group, detail);
                }
            } else {
                // DEFAULT 组的券直接保留（用唯一 key 避免覆盖）
                bestByGroup.put("DEFAULT_" + detail.couponId, detail);
            }
        }

        // 3. 汇总折扣
        BigDecimal totalDiscount = BigDecimal.ZERO;
        boolean hasPlatform = false;
        boolean hasHost = false;
        for (CouponDiscountDetail detail : bestByGroup.values()) {
            totalDiscount = totalDiscount.add(detail.discount);
            if ("PLATFORM".equals(detail.bearer) || "MIXED".equals(detail.bearer)) hasPlatform = true;
            if ("HOST".equals(detail.bearer) || "MIXED".equals(detail.bearer)) hasHost = true;
        }

        // 4. 全局折扣上限保护
        BigDecimal maxDiscountRate = getMaxDiscountRate();
        BigDecimal maxAllowedDiscount = originalAmount.multiply(maxDiscountRate);
        if (totalDiscount.compareTo(maxAllowedDiscount) > 0) {
            log.warn("优惠券总折扣超过上限，原始折扣={}, 上限={}, 已截断", totalDiscount, maxAllowedDiscount);
            totalDiscount = maxAllowedDiscount;
        }
        totalDiscount = totalDiscount.min(originalAmount).setScale(2, RoundingMode.HALF_UP);

        String finalBearer;
        if (hasPlatform && hasHost) {
            finalBearer = "MIXED";
        } else if (hasHost) {
            finalBearer = "HOST";
        } else {
            finalBearer = "PLATFORM";
        }

        return CouponDiscountResult.builder()
                .discountAmount(totalDiscount)
                .subsidyBearer(finalBearer)
                .build();
    }

    private UserCoupon findUserCoupon(Long couponId, Long userId) {
        if (userId != null) {
            return userCouponRepository.findByIdAndUserId(couponId, userId).orElse(null);
        }
        return userCouponRepository.findById(couponId).orElse(null);
    }

    private BigDecimal getMaxDiscountRate() {
        try {
            String value = systemConfigService.getConfigValue("coupon.max_discount.rate", "1.0");
            return new BigDecimal(value);
        } catch (Exception e) {
            log.warn("读取优惠券折扣上限配置失败，使用默认值1.0: {}", e.getMessage());
            return BigDecimal.ONE;
        }
    }

    private record CouponDiscountDetail(Long couponId, String stackGroup, BigDecimal discount, String bearer) {
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
        int released = userCouponRepository.releaseCouponsByOrderId(orderId);
        if (released > 0) {
            log.info("释放优惠券成功，订单ID={}, 数量={}", orderId, released);
        }
    }

    @Override
    @Transactional
    public void useCoupons(Long orderId) {
        int used = userCouponRepository.useCouponsByOrderId(orderId, LocalDateTime.now());
        if (used > 0) {
            log.info("核销优惠券成功，订单ID={}, 数量={}", orderId, used);
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
        if (template.getScopeValueJson() == null || template.getScopeValueJson().isBlank()) {
            return true; // 未设置范围值时默认适用
        }
        Homestay homestay = homestayRepository.findById(homestayId).orElse(null);
        if (homestay == null) {
            return false;
        }
        try {
            JsonNode scopeValues = objectMapper.readTree(template.getScopeValueJson());
            return switch (template.getScopeType()) {
                case "HOMESTAY" -> {
                    for (JsonNode node : scopeValues) {
                        if (node.asLong() == homestayId) yield true;
                    }
                    yield false;
                }
                case "HOST" -> {
                    Long hostId = homestay.getOwner() != null ? homestay.getOwner().getId() : null;
                    for (JsonNode node : scopeValues) {
                        if (hostId != null && node.asLong() == hostId) yield true;
                    }
                    yield false;
                }
                case "CITY" -> {
                    for (JsonNode node : scopeValues) {
                        if (node.asText().equals(homestay.getCityCode())) yield true;
                    }
                    yield false;
                }
                case "TYPE" -> {
                    for (JsonNode node : scopeValues) {
                        if (node.asText().equals(homestay.getType())) yield true;
                    }
                    yield false;
                }
                case "GROUP" -> {
                    Long groupId = homestay.getGroup() != null ? homestay.getGroup().getId() : null;
                    for (JsonNode node : scopeValues) {
                        if (groupId != null && node.asLong() == groupId) yield true;
                    }
                    yield false;
                }
                default -> true;
            };
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
