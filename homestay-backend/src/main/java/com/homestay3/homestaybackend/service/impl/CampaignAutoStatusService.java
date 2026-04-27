package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.PromotionCampaign;
import com.homestay3.homestaybackend.repository.PromotionCampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 营销活动自动状态流转服务
 * 处理基于时间的活动自动启用与自动结束
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignAutoStatusService {

    private final PromotionCampaignRepository campaignRepository;

    /**
     * 每分钟检查并处理活动的自动启用与自动结束
     */
    @Scheduled(fixedRate = 60 * 1000)
    @Transactional
    public void handleAutoStatusTransition() {
        try {
            autoActivateCampaigns();
        } catch (Exception e) {
            log.error("自动启用活动失败: {}", e.getMessage(), e);
        }

        try {
            autoEndCampaigns();
        } catch (Exception e) {
            log.error("自动结束活动失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 自动启用：DRAFT 状态且 startAt 已到达的活动 → ACTIVE
     */
    private void autoActivateCampaigns() {
        LocalDateTime now = LocalDateTime.now();
        List<PromotionCampaign> readyList = campaignRepository.findDraftCampaignsReadyToActivate(now);

        for (PromotionCampaign campaign : readyList) {
            campaign.setStatus("ACTIVE");
            campaignRepository.save(campaign);
            log.info("活动自动启用: id={}, name={}", campaign.getId(), campaign.getName());
        }
    }

    /**
     * 自动结束：ACTIVE 状态且 endAt 已过期的活动 → ENDED
     */
    private void autoEndCampaigns() {
        LocalDateTime now = LocalDateTime.now();
        List<PromotionCampaign> expiredList = campaignRepository.findActiveCampaignsPastEndTime(now);

        for (PromotionCampaign campaign : expiredList) {
            campaign.setStatus("ENDED");
            campaignRepository.save(campaign);
            log.info("活动自动结束: id={}, name={}", campaign.getId(), campaign.getName());
        }
    }
}
