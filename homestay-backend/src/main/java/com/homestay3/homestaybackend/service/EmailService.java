package com.homestay3.homestaybackend.service;

public interface EmailService {
    
    /**
     * 发送密码重置邮件
     * @param email 收件人邮箱
     * @param resetLink 重置链接
     */
    void sendPasswordResetEmail(String email, String resetLink);
    
    /**
     * 发送验证邮件
     * @param email 收件人邮箱
     * @param verificationToken 验证令牌
     */
    void sendVerificationEmail(String email, String verificationToken);
} 