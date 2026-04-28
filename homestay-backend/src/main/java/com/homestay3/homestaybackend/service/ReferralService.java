package com.homestay3.homestaybackend.service;

import java.util.Map;

public interface ReferralService {

    /**
     * 生成邀请码
     */
    Map<String, Object> generateReferralCode(Long inviterId, Long templateIdForInvitee,
                                              Long templateIdForInviter, Integer maxUses, Integer validDays);

    /**
     * 使用邀请码（被邀请人注册/下单时调用）
     * @return 是否成功处理
     */
    boolean useReferralCode(String referralCode, Long inviteeId);

    /**
     * 查询用户的邀请记录
     */
    Map<String, Object> getReferralStats(Long inviterId);
}
