package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.ReferralRecord;
import com.homestay3.homestaybackend.repository.ReferralRecordRepository;
import com.homestay3.homestaybackend.service.CouponService;
import com.homestay3.homestaybackend.service.ReferralService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferralServiceImpl implements ReferralService {

    private final ReferralRecordRepository referralRecordRepository;
    private final CouponService couponService;

    @Override
    @Transactional
    public Map<String, Object> generateReferralCode(Long inviterId, Long templateIdForInvitee,
                                                     Long templateIdForInviter, Integer maxUses, Integer validDays) {
        String code = generateUniqueCode();
        LocalDateTime expireAt = validDays != null && validDays > 0
                ? LocalDateTime.now().plusDays(validDays)
                : LocalDateTime.now().plusDays(30);

        ReferralRecord record = ReferralRecord.builder()
                .referralCode(code)
                .inviterId(inviterId)
                .templateIdForInvitee(templateIdForInvitee)
                .templateIdForInviter(templateIdForInviter)
                .maxUses(maxUses != null ? maxUses : 1)
                .usedCount(0)
                .status("ACTIVE")
                .expireAt(expireAt)
                .build();
        referralRecordRepository.save(record);

        Map<String, Object> result = new HashMap<>();
        result.put("referralCode", code);
        result.put("expireAt", expireAt);
        result.put("maxUses", record.getMaxUses());
        return result;
    }

    @Override
    @Transactional
    public boolean useReferralCode(String referralCode, Long inviteeId) {
        if (referralCode == null || referralCode.isBlank() || inviteeId == null) {
            return false;
        }

        // 自用拦截
        ReferralRecord checkRecord = referralRecordRepository
                .findByReferralCodeAndStatusAndExpireAtAfter(referralCode, "ACTIVE", LocalDateTime.now())
                .orElse(null);
        if (checkRecord == null) {
            log.info("邀请码无效或已过期，code={}", referralCode);
            return false;
        }
        if (inviteeId.equals(checkRecord.getInviterId())) {
            log.info("邀请码不能自用，inviteeId={}, inviterId={}", inviteeId, checkRecord.getInviterId());
            return false;
        }

        // 行级锁查询，防止并发超发
        ReferralRecord record = referralRecordRepository
                .findByReferralCodeAndStatusAndExpireAtAfterForUpdate(referralCode, "ACTIVE", LocalDateTime.now())
                .orElse(null);
        if (record == null) {
            log.info("邀请码无效或已过期（加锁后），code={}", referralCode);
            return false;
        }

        if (record.getUsedCount() >= record.getMaxUses()) {
            log.info("邀请码已达使用上限，code={}", referralCode);
            record.setStatus("USED");
            referralRecordRepository.save(record);
            return false;
        }

        boolean anyClaimed = false;

        // 给被邀请人发券（核心奖励，失败应阻断）
        if (record.getTemplateIdForInvitee() != null) {
            try {
                couponService.claimCoupon(inviteeId, record.getTemplateIdForInvitee());
                log.info("被邀请人得券成功，inviteeId={}, templateId={}", inviteeId, record.getTemplateIdForInvitee());
                anyClaimed = true;
            } catch (Exception e) {
                log.error("被邀请人得券失败，邀请码使用中断，code={}, inviteeId={}", referralCode, inviteeId, e);
                throw new RuntimeException("邀请奖励发放失败: " + e.getMessage());
            }
        }

        // 给邀请人发券（附属奖励，失败不影响主流程）
        if (record.getTemplateIdForInviter() != null) {
            try {
                couponService.claimCoupon(record.getInviterId(), record.getTemplateIdForInviter());
                log.info("邀请人得券成功，inviterId={}, templateId={}", record.getInviterId(), record.getTemplateIdForInviter());
                anyClaimed = true;
            } catch (Exception e) {
                log.warn("邀请人得券失败，inviterId={}, 原因={}", record.getInviterId(), e.getMessage());
            }
        }

        // 只有实际成功发放了券，才增加使用次数
        if (anyClaimed) {
            record.setUsedCount(record.getUsedCount() + 1);
            if (record.getUsedCount() >= record.getMaxUses()) {
                record.setStatus("USED");
            }
            referralRecordRepository.save(record);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, Object> getReferralStats(Long inviterId) {
        List<ReferralRecord> records = referralRecordRepository.findByInviterIdOrderByCreatedAtDesc(inviterId);

        int totalCodes = records.size();
        int totalUsed = records.stream().mapToInt(ReferralRecord::getUsedCount).sum();
        int activeCodes = (int) records.stream().filter(r -> "ACTIVE".equals(r.getStatus())).count();

        Map<String, Object> result = new HashMap<>();
        result.put("totalCodes", totalCodes);
        result.put("totalUsed", totalUsed);
        result.put("activeCodes", activeCodes);
        return result;
    }

    @Override
    public List<Map<String, Object>> getMyReferralCodes(Long inviterId) {
        List<ReferralRecord> records = referralRecordRepository.findByInviterIdOrderByCreatedAtDesc(inviterId);
        return records.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("referralCode", r.getReferralCode());
            map.put("status", r.getStatus());
            map.put("usedCount", r.getUsedCount());
            map.put("maxUses", r.getMaxUses());
            map.put("expireAt", r.getExpireAt());
            map.put("createdAt", r.getCreatedAt());
            return map;
        }).toList();
    }

    private String generateUniqueCode() {
        return "REF" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
