package com.homestay3.homestaybackend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay3.homestaybackend.dto.AppliedPromotionDTO;
import com.homestay3.homestaybackend.dto.PricingResult;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.PromotionCampaign;
import com.homestay3.homestaybackend.entity.PromotionRule;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.PromotionCampaignRepository;
import com.homestay3.homestaybackend.service.PromotionMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionMatchServiceImpl implements PromotionMatchService {

    private final PromotionCampaignRepository campaignRepository;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<PromotionMatch> matchPromotions(Homestay homestay, LocalDate checkIn, LocalDate checkOut,
                                                 Long userId, BigDecimal originalAmount) {
        List<PromotionCampaign> campaigns = campaignRepository.findActiveCampaigns(LocalDateTime.now());
        if (campaigns.isEmpty()) {
            return Collections.emptyList();
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        boolean isFirstOrder = userId != null && orderRepository.countByGuestId(userId) == 0;

        List<PromotionMatch> matches = new ArrayList<>();
        PromotionMatch bestNonStackable = null;

        for (PromotionCampaign campaign : campaigns) {
            // 预算检查
            if (campaign.getBudgetTotal() != null &&
                    campaign.getBudgetUsed().compareTo(campaign.getBudgetTotal()) >= 0) {
                continue;
            }

            // 遍历活动内所有规则，取优惠金额最大的一条
            BigDecimal bestDiscount = BigDecimal.ZERO;
            for (PromotionRule rule : campaign.getRules()) {
                if (!isRuleApplicable(rule, homestay, nights, originalAmount, isFirstOrder)) {
                    continue;
                }

                BigDecimal discount = calculateDiscount(rule, originalAmount, nights);
                if (discount.compareTo(bestDiscount) > 0) {
                    bestDiscount = discount;
                }
            }

            if (bestDiscount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 预算限制
            if (campaign.getBudgetTotal() != null) {
                BigDecimal remaining = campaign.getBudgetTotal().subtract(campaign.getBudgetUsed());
                if (bestDiscount.compareTo(remaining) > 0) {
                    bestDiscount = remaining;
                }
            }

            PromotionMatch match = new PromotionMatch(
                    campaign.getId(), campaign.getName(), campaign.getCampaignType(),
                    bestDiscount, campaign.getSubsidyBearer()
            );

            if (!Boolean.TRUE.equals(campaign.getStackable())) {
                if (bestNonStackable == null || match.getDiscountAmount().compareTo(bestNonStackable.getDiscountAmount()) > 0) {
                    bestNonStackable = match;
                }
            } else {
                matches.add(match);
            }
        }

        if (bestNonStackable != null) {
            // 非叠加活动优先，如果选了非叠加活动，只返回它
            return Collections.singletonList(bestNonStackable);
        }
        return matches;
    }

    @Override
    public void applyPromotions(PricingResult.PricingResultBuilder builder, List<PromotionMatch> matches) {
        BigDecimal activityDiscount = BigDecimal.ZERO;
        BigDecimal fullReduction = BigDecimal.ZERO;
        BigDecimal platformDiscount = BigDecimal.ZERO;
        BigDecimal hostDiscount = BigDecimal.ZERO;
        List<AppliedPromotionDTO> applied = new ArrayList<>();

        for (PromotionMatch match : matches) {
            AppliedPromotionDTO dto = AppliedPromotionDTO.builder()
                    .type(match.getCampaignType())
                    .name(match.getCampaignName())
                    .discountAmount(match.getDiscountAmount())
                    .bearer(match.getBearer())
                    .campaignId(match.getCampaignId())
                    .build();
            applied.add(dto);

            if ("FULL_REDUCTION".equals(match.getCampaignType())) {
                fullReduction = fullReduction.add(match.getDiscountAmount());
            } else {
                activityDiscount = activityDiscount.add(match.getDiscountAmount());
            }

            if ("PLATFORM".equals(match.getBearer()) || "MIXED".equals(match.getBearer())) {
                platformDiscount = platformDiscount.add(match.getDiscountAmount());
            }
            if ("HOST".equals(match.getBearer()) || "MIXED".equals(match.getBearer())) {
                hostDiscount = hostDiscount.add(match.getDiscountAmount());
            }
        }

        builder.activityDiscountAmount(activityDiscount)
                .fullReductionAmount(fullReduction)
                .platformDiscountAmount(platformDiscount)
                .hostDiscountAmount(hostDiscount)
                .appliedPromotions(applied);
    }

    private boolean isRuleApplicable(PromotionRule rule, Homestay homestay, int nights,
                                     BigDecimal originalAmount, boolean isFirstOrder) {
        // 首单限制
        if (Boolean.TRUE.equals(rule.getFirstOrderOnly()) && !isFirstOrder) {
            return false;
        }

        // 入住晚数限制
        if (rule.getMinNights() != null && nights < rule.getMinNights()) {
            return false;
        }
        if (rule.getMaxNights() != null && nights > rule.getMaxNights()) {
            return false;
        }

        // 门槛金额
        if (rule.getThresholdAmount() != null && originalAmount.compareTo(rule.getThresholdAmount()) < 0) {
            return false;
        }

        // 范围匹配
        return isScopeMatch(rule.getScopeType(), rule.getScopeValueJson(), homestay);
    }

    private boolean isScopeMatch(String scopeType, String scopeValueJson, Homestay homestay) {
        if ("ALL".equals(scopeType)) {
            return true;
        }
        try {
            JsonNode scopeValues = objectMapper.readTree(scopeValueJson);
            switch (scopeType) {
                case "HOMESTAY":
                    for (JsonNode node : scopeValues) {
                        if (node.asLong() == homestay.getId()) return true;
                    }
                    break;
                case "HOST":
                    Long hostId = homestay.getOwner() != null ? homestay.getOwner().getId() : null;
                    for (JsonNode node : scopeValues) {
                        if (node.asLong() == hostId) return true;
                    }
                    break;
                case "CITY":
                    for (JsonNode node : scopeValues) {
                        if (node.asText().equals(homestay.getCityCode())) return true;
                    }
                    break;
                case "TYPE":
                    for (JsonNode node : scopeValues) {
                        if (node.asText().equals(homestay.getType())) return true;
                    }
                    break;
                case "GROUP":
                    Long groupId = homestay.getGroup() != null ? homestay.getGroup().getId() : null;
                    for (JsonNode node : scopeValues) {
                        if (node.asLong() == groupId) return true;
                    }
                    break;
            }
        } catch (Exception e) {
            log.warn("解析scopeValueJson失败: {}", e.getMessage());
        }
        return false;
    }

    private BigDecimal calculateDiscount(PromotionRule rule, BigDecimal originalAmount, int nights) {
        BigDecimal discount = BigDecimal.ZERO;
        switch (rule.getRuleType()) {
            case "AMOUNT_OFF":
                if (rule.getDiscountAmount() != null) {
                    discount = rule.getDiscountAmount();
                }
                break;
            case "PERCENT_OFF":
                if (rule.getDiscountRate() != null) {
                    discount = originalAmount.multiply(BigDecimal.ONE.subtract(rule.getDiscountRate()));
                }
                break;
            case "FULL_REDUCTION":
                discount = calculateFullReductionDiscount(rule, originalAmount);
                break;
            case "PER_NIGHT_OFF":
                if (rule.getDiscountAmount() != null) {
                    discount = rule.getDiscountAmount().multiply(BigDecimal.valueOf(nights));
                }
                break;
        }

        if (rule.getMaxDiscount() != null && discount.compareTo(rule.getMaxDiscount()) > 0) {
            discount = rule.getMaxDiscount();
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算满减优惠，支持阶梯满减配置
     */
    private BigDecimal calculateFullReductionDiscount(PromotionRule rule, BigDecimal originalAmount) {
        // 优先使用阶梯配置
        if (rule.getTierConfigJson() != null && !rule.getTierConfigJson().isBlank()) {
            try {
                JsonNode tiers = objectMapper.readTree(rule.getTierConfigJson());
                BigDecimal bestDiscount = BigDecimal.ZERO;
                for (JsonNode tier : tiers) {
                    BigDecimal threshold = new BigDecimal(tier.get("threshold").asText());
                    BigDecimal discount = new BigDecimal(tier.get("discount").asText());
                    if (originalAmount.compareTo(threshold) >= 0 && discount.compareTo(bestDiscount) > 0) {
                        bestDiscount = discount;
                    }
                }
                return bestDiscount;
            } catch (Exception e) {
                log.warn("解析阶梯满减配置失败，回退到单档规则: {}", e.getMessage());
            }
        }
        // 回退到单档规则
        if (rule.getThresholdAmount() != null && originalAmount.compareTo(rule.getThresholdAmount()) >= 0
                && rule.getDiscountAmount() != null) {
            return rule.getDiscountAmount();
        }
        return BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public boolean deductCampaignBudget(Long campaignId, BigDecimal amount) {
        if (campaignId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return true; // 无需扣减
        }
        int updated = campaignRepository.deductBudget(campaignId, amount);
        if (updated == 0) {
            log.warn("活动预算扣减失败，活动ID={}, 金额={}", campaignId, amount);
            return false;
        }

        // 使用数据库原子操作检查预算预警和耗尽，避免竞态条件
        BigDecimal threshold = new BigDecimal("0.80");
        int alertUpdated = campaignRepository.triggerBudgetAlert(campaignId, threshold);
        if (alertUpdated > 0) {
            log.info("活动预算预警触发: 活动ID={}", campaignId);
        }

        int endedUpdated = campaignRepository.endCampaignByBudgetExhaustion(campaignId);
        if (endedUpdated > 0) {
            log.info("活动预算耗尽自动结束: 活动ID={}", campaignId);
        }

        return true;
    }

    @Override
    @Transactional
    public boolean refundCampaignBudget(Long campaignId, BigDecimal amount) {
        if (campaignId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        int updated = campaignRepository.refundBudget(campaignId, amount);
        if (updated == 0) {
            log.warn("活动预算回退失败，活动ID={}, 金额={}", campaignId, amount);
            return false;
        }
        log.info("活动预算回退成功: 活动ID={}, 金额={}", campaignId, amount);
        return true;
    }
}
