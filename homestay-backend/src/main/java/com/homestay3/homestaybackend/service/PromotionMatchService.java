package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.AppliedPromotionDTO;
import com.homestay3.homestaybackend.dto.PricingResult;
import com.homestay3.homestaybackend.entity.Homestay;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 活动匹配服务
 * 根据房源、日期、用户等条件匹配适用的营销活动
 */
public interface PromotionMatchService {

    /**
     * 匹配房源在指定日期范围内适用的活动优惠
     *
     * @param homestay  房源
     * @param checkIn   入住日期
     * @param checkOut  退房日期
     * @param userId    用户ID（用于首单判断等）
     * @param originalAmount 原始房费合计
     * @return 匹配到的优惠活动列表及优惠金额
     */
    List<PromotionMatch> matchPromotions(Homestay homestay, LocalDate checkIn, LocalDate checkOut,
                                         Long userId, BigDecimal originalAmount);

    /**
     * 将匹配结果应用到定价结果中
     */
    void applyPromotions(PricingResult.PricingResultBuilder builder, List<PromotionMatch> matches);

    /**
     * 扣减活动预算并检查预警
     * @param campaignId 活动ID
     * @param amount 扣减金额
     * @return true 扣减成功
     */
    boolean deductCampaignBudget(Long campaignId, BigDecimal amount);

    /**
     * 回退活动预算
     * @param campaignId 活动ID
     * @param amount 回退金额
     * @return true 回退成功
     */
    boolean refundCampaignBudget(Long campaignId, BigDecimal amount);

    /**
     * 活动匹配结果
     */
    class PromotionMatch {
        private final Long campaignId;
        private final String campaignName;
        private final String campaignType;
        private final BigDecimal discountAmount;
        private final String bearer;

        public PromotionMatch(Long campaignId, String campaignName, String campaignType,
                              BigDecimal discountAmount, String bearer) {
            this.campaignId = campaignId;
            this.campaignName = campaignName;
            this.campaignType = campaignType;
            this.discountAmount = discountAmount;
            this.bearer = bearer;
        }

        public Long getCampaignId() { return campaignId; }
        public String getCampaignName() { return campaignName; }
        public String getCampaignType() { return campaignType; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public String getBearer() { return bearer; }
    }
}
