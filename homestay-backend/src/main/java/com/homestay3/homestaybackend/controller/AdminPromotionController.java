package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.entity.CouponTemplate;
import com.homestay3.homestaybackend.entity.PromotionCampaign;
import com.homestay3.homestaybackend.entity.PromotionUsage;
import com.homestay3.homestaybackend.repository.CouponTemplateRepository;
import com.homestay3.homestaybackend.repository.PromotionCampaignRepository;
import com.homestay3.homestaybackend.repository.PromotionUsageRepository;
import com.homestay3.homestaybackend.service.CouponService;
import com.homestay3.homestaybackend.service.RoiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/promotions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromotionController {

    private final PromotionCampaignRepository campaignRepository;
    private final CouponTemplateRepository couponTemplateRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final CouponService couponService;
    private final RoiAnalysisService roiAnalysisService;

    // ========== 活动管理 ==========

    @GetMapping("/campaigns")
    public ResponseEntity<?> getCampaigns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String campaignType,
            @RequestParam(required = false) String subsidyBearer,
            @RequestParam(required = false) Long hostId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // 使用 Specification 动态查询
        org.springframework.data.jpa.domain.Specification<PromotionCampaign> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isEmpty()) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (campaignType != null && !campaignType.isEmpty()) {
                predicates.add(cb.equal(root.get("campaignType"), campaignType));
            }
            if (subsidyBearer != null && !subsidyBearer.isEmpty()) {
                predicates.add(cb.equal(root.get("subsidyBearer"), subsidyBearer));
            }
            if (hostId != null) {
                predicates.add(cb.equal(root.get("hostId"), hostId));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<PromotionCampaign> campaigns = campaignRepository.findAll(spec, pageable);
        return ResponseEntity.ok(campaigns);
    }

    @PostMapping("/campaigns")
    public ResponseEntity<?> createCampaign(@RequestBody PromotionCampaign campaign) {
        try {
            // 设置规则的级联回引用
            if (campaign.getRules() != null) {
                campaign.getRules().forEach(rule -> rule.setCampaign(campaign));
            }
            PromotionCampaign saved = campaignRepository.save(campaign);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/campaigns/{id}")
    public ResponseEntity<?> updateCampaign(@PathVariable Long id, @RequestBody PromotionCampaign campaign) {
        Optional<PromotionCampaign> existingOpt = campaignRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        PromotionCampaign existing = existingOpt.get();
        // 清除旧规则，前端传入新规则
        existing.getRules().clear();
        campaignRepository.flush();

        existing.setName(campaign.getName());
        existing.setCampaignType(campaign.getCampaignType());
        existing.setStartAt(campaign.getStartAt());
        existing.setEndAt(campaign.getEndAt());
        existing.setPriority(campaign.getPriority());
        existing.setStackable(campaign.getStackable());
        existing.setBudgetTotal(campaign.getBudgetTotal());
        existing.setSubsidyBearer(campaign.getSubsidyBearer());
        existing.setBudgetAlertThreshold(campaign.getBudgetAlertThreshold());

        if (campaign.getRules() != null) {
            for (var rule : campaign.getRules()) {
                rule.setCampaign(existing);
                existing.getRules().add(rule);
            }
        }

        PromotionCampaign updated = campaignRepository.save(existing);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/campaigns/{id}/publish")
    public ResponseEntity<?> publishCampaign(@PathVariable Long id) {
        Optional<PromotionCampaign> opt = campaignRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        PromotionCampaign campaign = opt.get();
        campaign.setStatus("ACTIVE");
        campaignRepository.save(campaign);
        return ResponseEntity.ok(Map.of("message", "活动已发布"));
    }

    @PostMapping("/campaigns/{id}/pause")
    public ResponseEntity<?> pauseCampaign(@PathVariable Long id) {
        Optional<PromotionCampaign> opt = campaignRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        PromotionCampaign campaign = opt.get();
        campaign.setStatus("PAUSED");
        campaignRepository.save(campaign);
        return ResponseEntity.ok(Map.of("message", "活动已暂停"));
    }

    @DeleteMapping("/campaigns/{id}")
    public ResponseEntity<?> deleteCampaign(@PathVariable Long id) {
        Optional<PromotionCampaign> opt = campaignRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        PromotionCampaign campaign = opt.get();
        if (!"DRAFT".equals(campaign.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "只有草稿状态的活动可以删除"));
        }
        campaignRepository.delete(campaign);
        return ResponseEntity.ok(Map.of("message", "活动已删除"));
    }

    // ========== 优惠券模板管理 ==========

    @GetMapping("/templates")
    public ResponseEntity<?> getCouponTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String subsidyBearer) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        org.springframework.data.jpa.domain.Specification<CouponTemplate> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isEmpty()) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (subsidyBearer != null && !subsidyBearer.isEmpty()) {
                predicates.add(cb.equal(root.get("subsidyBearer"), subsidyBearer));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<CouponTemplate> templates = couponTemplateRepository.findAll(spec, pageable);
        return ResponseEntity.ok(templates);
    }

    @PostMapping("/templates")
    public ResponseEntity<?> createTemplate(@RequestBody CouponTemplate template) {
        try {
            CouponTemplate saved = couponService.createTemplate(template);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<?> updateTemplate(@PathVariable Long id, @RequestBody CouponTemplate template) {
        Optional<CouponTemplate> existing = couponTemplateRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        template.setId(id);
        CouponTemplate updated = couponTemplateRepository.save(template);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id) {
        Optional<CouponTemplate> opt = couponTemplateRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CouponTemplate template = opt.get();
        if (!"DRAFT".equals(template.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "只有草稿状态的模板可以删除"));
        }
        couponTemplateRepository.delete(template);
        return ResponseEntity.ok(Map.of("message", "模板已删除"));
    }

    // ========== 使用流水与统计 ==========

    @GetMapping("/usages")
    public ResponseEntity<?> getUsages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PromotionUsage> usages = promotionUsageRepository.findAll(pageable);
        return ResponseEntity.ok(usages);
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(
            @RequestParam(required = false) String campaignType,
            @RequestParam(required = false) String subsidyBearer,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        List<PromotionUsage> usages = promotionUsageRepository.findAll();

        // 按 campaignId 分组统计
        Map<Long, PromotionCampaign> campaignMap = new HashMap<>();
        for (PromotionUsage usage : usages) {
            if (usage.getCampaignId() != null && !campaignMap.containsKey(usage.getCampaignId())) {
                campaignRepository.findById(usage.getCampaignId()).ifPresent(c -> campaignMap.put(c.getId(), c));
            }
        }

        // 过滤
        List<PromotionUsage> filtered = usages.stream().filter(u -> {
            if (u.getCampaignId() != null) {
                PromotionCampaign c = campaignMap.get(u.getCampaignId());
                if (c != null) {
                    if (campaignType != null && !campaignType.isEmpty() && !campaignType.equals(c.getCampaignType())) {
                        return false;
                    }
                    if (subsidyBearer != null && !subsidyBearer.isEmpty() && !subsidyBearer.equals(c.getSubsidyBearer())) {
                        return false;
                    }
                }
            }
            if (u.getCreatedAt() != null) {
                if (startDate != null && !startDate.isEmpty() && u.getCreatedAt().isBefore(LocalDateTime.parse(startDate + "T00:00:00"))) {
                    return false;
                }
                if (endDate != null && !endDate.isEmpty() && u.getCreatedAt().isAfter(LocalDateTime.parse(endDate + "T23:59:59"))) {
                    return false;
                }
            }
            return true;
        }).toList();

        BigDecimal totalDiscount = filtered.stream()
                .map(PromotionUsage::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long usedCount = filtered.stream().filter(u -> "USED".equals(u.getStatus())).count();

        // 承担方拆分
        BigDecimal platformDiscount = filtered.stream()
                .filter(u -> "PLATFORM".equals(u.getBearer()) || "MIXED".equals(u.getBearer()))
                .map(PromotionUsage::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal hostDiscount = filtered.stream()
                .filter(u -> "HOST".equals(u.getBearer()) || "MIXED".equals(u.getBearer()))
                .map(PromotionUsage::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(Map.of(
                "totalDiscount", totalDiscount,
                "totalUsageCount", filtered.size(),
                "usedCount", usedCount,
                "platformDiscount", platformDiscount,
                "hostDiscount", hostDiscount,
                "campaignCount", campaignMap.values().stream().distinct().count()
        ));
    }

    // ========== ROI 分析 ==========

    @GetMapping("/roi/overview")
    public ResponseEntity<?> getRoiOverview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDateTime start = parseDateTime(startDate, true);
        LocalDateTime end = parseDateTime(endDate, false);
        return ResponseEntity.ok(roiAnalysisService.getPlatformRoiOverview(start, end));
    }

    @GetMapping("/roi/campaigns")
    public ResponseEntity<?> getRoiCampaigns(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "10") int limit) {
        LocalDateTime start = parseDateTime(startDate, true);
        LocalDateTime end = parseDateTime(endDate, false);
        return ResponseEntity.ok(roiAnalysisService.getPlatformCampaignRoi(start, end, limit));
    }

    private LocalDateTime parseDateTime(String dateStr, boolean startOfDay) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        return startOfDay
                ? LocalDateTime.parse(dateStr + "T00:00:00")
                : LocalDateTime.parse(dateStr + "T23:59:59");
    }
}
