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

        ReferralRecord record = referralRecordRepository
                .findByReferralCodeAndStatusAndExpireAtAfter(referralCode, "ACTIVE", LocalDateTime.now())
                .orElse(null);
        if (record == null) {
            log.info("邀请码无效或已过期，code={}", referralCode);
            return false;
        }

        if (record.getUsedCount() >= record.getMaxUses()) {
            log.info("邀请码已达使用上限，code={}", referralCode);
            record.setStatus("USED");
            referralRecordRepository.save(record);
            return false;
        }

        // 给被邀请人发券
        if (record.getTemplateIdForInvitee() != null) {
            try {
                couponService.claimCoupon(inviteeId, record.getTemplateIdForInvitee());
                log.info("被邀请人得券成功，inviteeId={}, templateId={}", inviteeId, record.getTemplateIdForInvitee());
            } catch (Exception e) {
                log.warn("被邀请人得券失败，inviteeId={}, 原因={}", inviteeId, e.getMessage());
            }
        }

        // 给邀请人发券
        if (record.getTemplateIdForInviter() != null) {
            try {
                couponService.claimCoupon(record.getInviterId(), record.getTemplateIdForInviter());
                log.info("邀请人得券成功，inviterId={}, templateId={}", record.getInviterId(), record.getTemplateIdForInviter());
            } catch (Exception e) {
                log.warn("邀请人得券失败，inviterId={}, 原因={}", record.getInviterId(), e.getMessage());
            }
        }

        record.setUsedCount(record.getUsedCount() + 1);
        if (record.getUsedCount() >= record.getMaxUses()) {
            record.setStatus("USED");
        }
        referralRecordRepository.save(record);
        return true;
    }

    @Override
    public Map<String, Object> getReferralStats(Long inviterId) {
        List<ReferralRecord> records = referralRecordRepository.findAll().stream()
                .filter(r -> r.getInviterId().equals(inviterId))
                .toList();

        int totalCodes = records.size();
        int totalUsed = records.stream().mapToInt(ReferralRecord::getUsedCount).sum();
        int activeCodes = (int) records.stream().filter(r -> "ACTIVE".equals(r.getStatus())).count();

        Map<String, Object> result = new HashMap<>();
        result.put("totalCodes", totalCodes);
        result.put("totalUsed", totalUsed);
        result.put("activeCodes", activeCodes);
        return result;
    }

    private String generateUniqueCode() {
        return "REF" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
