package com.homestay3.homestaybackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetRequest {
    @NotBlank(message = "重置令牌不能为空")
    private String token;
    
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
} 