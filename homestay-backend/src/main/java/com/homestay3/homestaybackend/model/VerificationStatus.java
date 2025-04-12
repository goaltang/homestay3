package com.homestay3.homestaybackend.model;

/**
 * 用户验证状态枚举
 */
public enum VerificationStatus {
    /**
     * 未验证
     */
    UNVERIFIED,
    
    /**
     * 待验证
     */
    PENDING,
    
    /**
     * 已验证
     */
    VERIFIED,
    
    /**
     * 验证失败
     */
    REJECTED
}