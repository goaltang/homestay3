package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.entity.PricingRule;
import com.homestay3.homestaybackend.repository.PricingRuleRepository;
import com.homestay3.homestaybackend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/host/pricing-rules")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('HOST') or hasRole('LANDLORD')")
public class HostPricingRuleController {

    private final PricingRuleRepository pricingRuleRepository;

    @GetMapping
    public ResponseEntity<?> list() {
        CustomUserDetails user = getCurrentUser();
        // 查询房东自己创建的全局/HOST/房源级规则
        List<PricingRule> rules = pricingRuleRepository.findByEnabledTrueOrderByPriorityAsc();
        // 过滤出该房东创建的规则，或作用域包含该房东的规则
        List<PricingRule> hostRules = rules.stream()
                .filter(r -> user.getUserId().equals(r.getCreatedBy())
                        || ("HOST".equals(r.getScopeType()) && r.getScopeValueJson() != null
                                && r.getScopeValueJson().contains("\"hostId\":" + user.getUserId())))
                .toList();
        return ResponseEntity.ok(hostRules);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PricingRule rule) {
        CustomUserDetails user = getCurrentUser();
        rule.setCreatedBy(user.getUserId());
        // 房东只能创建 HOST / HOMESTAY / GROUP 作用域的规则
        if (!List.of("HOST", "HOMESTAY", "GROUP").contains(rule.getScopeType())) {
            return ResponseEntity.badRequest().body("房东只能创建 HOST、HOMESTAY 或 GROUP 作用域的规则");
        }
        PricingRule saved = pricingRuleRepository.save(rule);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody PricingRule rule) {
        CustomUserDetails user = getCurrentUser();
        Optional<PricingRule> opt = pricingRuleRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        PricingRule existing = opt.get();
        // 只能修改自己创建的规则
        if (!user.getUserId().equals(existing.getCreatedBy())) {
            return ResponseEntity.status(403).body("无权修改此规则");
        }
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
        return ResponseEntity.ok(pricingRuleRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        CustomUserDetails user = getCurrentUser();
        Optional<PricingRule> opt = pricingRuleRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!user.getUserId().equals(opt.get().getCreatedBy())) {
            return ResponseEntity.status(403).body("无权删除此规则");
        }
        pricingRuleRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private CustomUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (CustomUserDetails) auth.getPrincipal();
    }
}
