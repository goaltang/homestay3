package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.entity.PricingRule;
import com.homestay3.homestaybackend.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/admin/pricing-rules")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPricingRuleController {

    private final PricingRuleRepository pricingRuleRepository;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String ruleType) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("priority").ascending());
        if (ruleType != null && !ruleType.isEmpty()) {
            java.util.List<PricingRule> list = pricingRuleRepository.findByRuleTypeAndEnabledTrueOrderByPriorityAsc(ruleType);
            // 手动分页
            int start = (int) pageable.getOffset();
            int end = Math.min(start + size, list.size());
            java.util.List<PricingRule> pageContent = start < list.size() ? list.subList(start, end) : java.util.List.of();
            Page<PricingRule> result = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, list.size());
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.ok(pricingRuleRepository.findAll(pageable));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PricingRule rule) {
        PricingRule saved = pricingRuleRepository.save(rule);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody PricingRule rule) {
        return pricingRuleRepository.findById(id)
                .map(existing -> {
                    existing.setName(rule.getName());
                    existing.setScopeType(rule.getScopeType());
                    existing.setScopeValueJson(rule.getScopeValueJson());
                    existing.setRuleType(rule.getRuleType());
                    existing.setAdjustmentType(rule.getAdjustmentType());
                    existing.setAdjustmentValue(rule.getAdjustmentValue());
                    existing.setPriority(rule.getPriority());
                    existing.setStackable(rule.getStackable());
                    existing.setStartDate(rule.getStartDate());
                    existing.setEndDate(rule.getEndDate());
                    existing.setMinNights(rule.getMinNights());
                    existing.setMaxNights(rule.getMaxNights());
                    existing.setMinAdvanceDays(rule.getMinAdvanceDays());
                    existing.setMaxAdvanceDays(rule.getMaxAdvanceDays());
                    existing.setEnabled(rule.getEnabled());
                    PricingRule saved = pricingRuleRepository.save(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        pricingRuleRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(@PathVariable Long id) {
        Optional<PricingRule> opt = pricingRuleRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        PricingRule rule = opt.get();
        rule.setEnabled(!rule.getEnabled());
        return ResponseEntity.ok(pricingRuleRepository.save(rule));
    }
}
