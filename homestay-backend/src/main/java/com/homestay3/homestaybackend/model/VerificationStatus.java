package com.homestay3.homestaybackend.model;

public enum VerificationStatus {
    UNVERIFIED,    // 未认证
    PENDING,       // 审核中
    VERIFIED,      // 已认证
    REJECTED       // 已拒绝
}