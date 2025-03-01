package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.AuthResponse;
import com.homestay3.homestaybackend.dto.LoginRequest;
import com.homestay3.homestaybackend.dto.PasswordResetRequest;
import com.homestay3.homestaybackend.dto.RegisterRequest;
import com.homestay3.homestaybackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/info")
    public ResponseEntity<AuthResponse> getUserInfo(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.getUserInfo(userDetails.getUsername()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required");
        }
        authService.sendPasswordResetEmail(email);
        return ResponseEntity.ok().body("Password reset email sent successfully");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        log.info("收到密码重置请求: token={}", request.getToken());
        try {
            authService.resetPassword(request);
            Map<String, String> response = new HashMap<>();
            response.put("message", "密码重置成功");
            log.info("密码重置成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("密码重置失败: {}", e.getMessage(), e);
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 上传用户头像
     */
    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("avatar") MultipartFile file, Authentication authentication) {
        try {
            log.info("开始处理头像上传请求，文件大小: {}KB", file.getSize() / 1024);
            
            if (file.isEmpty()) {
                log.warn("上传的头像文件为空");
                return ResponseEntity.badRequest().body("上传的文件为空");
            }
            
            // 检查文件大小
            if (file.getSize() > 10 * 1024 * 1024) { // 10MB
                log.warn("上传的头像文件过大: {}KB", file.getSize() / 1024);
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body("文件大小超过限制，最大允许10MB");
            }
            
            // 获取当前用户
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            log.info("用户 {} 正在上传头像", username);
            
            // 上传头像
            String avatarPath = authService.uploadAvatar(file, username);
            log.info("头像上传成功，路径: {}", avatarPath);
            
            return ResponseEntity.ok(avatarPath);
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("头像上传失败: " + e.getMessage());
        }
    }
} 