package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.PricingResult;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.PromotionCampaign;
import com.homestay3.homestaybackend.entity.PromotionRule;
import com.homestay3.homestaybackend.security.CustomUserDetails;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.PromotionCampaignRepository;
import com.homestay3.homestaybackend.repository.PromotionUsageRepository;
import com.homestay3.homestaybackend.service.PromotionMatchService;
import com.homestay3.homestaybackend.service.RoiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/host/promotions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('HOST') or hasRole('LANDLORD')")
public class HostPromotionController {

    private final PromotionCampaignRepository campaignRepository;
    private final HomestayRepository homestayRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final PromotionMatchService promotionMatchService;
    private final RoiAnalysisService roiAnalysisService;

    // ========== 房东活动管理 ==========

    @GetMapping("/campaigns")
    public ResponseEntity<?> getHostCampaigns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        CustomUserDetails currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        // 使用 JpaSpecificationExecutor 或自定义查询
        List<PromotionCampaign> all = campaignRepository.findByHostIdOrderByCreatedAtDesc(currentUser.getUserId());
        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + size, all.size());
        List<PromotionCampaign> pageContent = start < all.size() ? all.subList(start, end) : Collections.emptyList();
        Page<PromotionCampaign> campaigns = new org.springframework.data.domain.PageImpl<>(
                pageContent, pageable, all.size());
        return ResponseEntity.ok(campaigns);
    }

    @PostMapping("/campaigns")
    public ResponseEntity<?> createHostCampaign(@RequestBody PromotionCampaign campaign) {
        CustomUserDetails currentUser = getCurrentUser();

        // 限制房东只能创建 HOST 承担的活动
        campaign.setHostId(currentUser.getUserId());
        campaign.setSubsidyBearer("HOST");
        campaign.setStatus("DRAFT");
        campaign.setCreatedBy(currentUser.getUsername());

        // 校验活动类型
        if (!"HOMESTAY_DISCOUNT".equals(campaign.getCampaignType())
                && !"FLASH_SALE".equals(campaign.getCampaignType())) {
            return ResponseEntity.badRequest().body(Map.of("error", "房东只能创建 HOMESTAY_DISCOUNT 或 FLASH_SALE 类型的活动"));
        }

        // 自动设置规则范围为该房东的所有房源
        if (campaign.getRules() != null && !campaign.getRules().isEmpty()) {
            List<Homestay> hostHomestays = homestayRepository.findByOwnerId(currentUser.getUserId());
            List<Long> homestayIds = hostHomestays.stream().map(Homestay::getId).toList();
            for (PromotionRule rule : campaign.getRules()) {
                rule.setScopeType("HOMESTAY");
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    rule.setScopeValueJson(mapper.writeValueAsString(homestayIds));
                } catch (Exception e) {
                    rule.setScopeValueJson("[]");
                }
                rule.setCampaign(campaign);
            }
        }

        try {
            PromotionCampaign saved = campaignRepository.save(campaign);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/campaigns/{id}")
    public ResponseEntity<?> updateHostCampaign(@PathVariable Long id, @RequestBody PromotionCampaign campaign) {
        CustomUserDetails currentUser = getCurrentUser();
        Optional<PromotionCampaign> existing = campaignRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        PromotionCampaign old = existing.get();
        if (!currentUser.getUserId().equals(old.getHostId())) {
            return ResponseEntity.status(403).body(Map.of("error", "只能修改自己创建的活动"));
        }
        if ("ACTIVE".equals(old.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "已发布的活动不能修改，请先暂停"));
        }

        campaign.setId(id);
        campaign.setHostId(currentUser.getUserId());
        campaign.setSubsidyBearer("HOST");
        // 保留不可修改字段
        campaign.setCreatedAt(old.getCreatedAt());
        campaign.setBudgetUsed(old.getBudgetUsed());

        try {
            PromotionCampaign updated = campaignRepository.save(campaign);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/campaigns/{id}/pause")
    public ResponseEntity<?> pauseHostCampaign(@PathVariable Long id) {
        CustomUserDetails currentUser = getCurrentUser();
        Optional<PromotionCampaign> opt = campaignRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        PromotionCampaign campaign = opt.get();
        if (!currentUser.getUserId().equals(campaign.getHostId())) {
            return ResponseEntity.status(403).body(Map.of("error", "只能暂停自己创建的活动"));
        }
        campaign.setStatus("PAUSED");
        campaignRepository.save(campaign);
        return ResponseEntity.ok(Map.of("message", "活动已暂停"));
    }

    @PostMapping("/campaigns/{id}/end")
    public ResponseEntity<?> endHostCampaign(@PathVariable Long id) {
        CustomUserDetails currentUser = getCurrentUser();
        Optional<PromotionCampaign> opt = campaignRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        PromotionCampaign campaign = opt.get();
        if (!currentUser.getUserId().equals(campaign.getHostId())) {
            return ResponseEntity.status(403).body(Map.of("error", "只能结束自己创建的活动"));
        }
        campaign.setStatus("ENDED");
        campaignRepository.save(campaign);
        return ResponseEntity.ok(Map.of("message", "活动已结束"));
    }

    // ========== 房东营销统计 ==========

    @GetMapping("/statistics")
    public ResponseEntity<?> getHostStatistics() {
        CustomUserDetails currentUser = getCurrentUser();
        return ResponseEntity.ok(roiAnalysisService.getHostRoiOverview(currentUser.getUserId(), null, null));
    }

    @GetMapping("/roi/overview")
    public ResponseEntity<?> getHostRoiOverview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        CustomUserDetails currentUser = getCurrentUser();
        LocalDateTime start = parseDateTime(startDate, true);
        LocalDateTime end = parseDateTime(endDate, false);
        return ResponseEntity.ok(roiAnalysisService.getHostRoiOverview(currentUser.getUserId(), start, end));
    }

    @GetMapping("/roi/campaigns")
    public ResponseEntity<?> getHostRoiCampaigns(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        CustomUserDetails currentUser = getCurrentUser();
        LocalDateTime start = parseDateTime(startDate, true);
        LocalDateTime end = parseDateTime(endDate, false);
        return ResponseEntity.ok(roiAnalysisService.getHostCampaignRoi(currentUser.getUserId(), start, end));
    }

    private LocalDateTime parseDateTime(String dateStr, boolean startOfDay) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        return startOfDay
                ? LocalDateTime.parse(dateStr + "T00:00:00")
                : LocalDateTime.parse(dateStr + "T23:59:59");
    }

    private CustomUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) auth.getPrincipal();
        }
        throw new IllegalStateException("无法获取当前用户");
    }
}
