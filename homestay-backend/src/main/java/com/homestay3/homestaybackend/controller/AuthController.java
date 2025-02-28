package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.AuthResponse;
import com.homestay3.homestaybackend.dto.LoginRequest;
import com.homestay3.homestaybackend.dto.PasswordResetRequest;
import com.homestay3.homestaybackend.dto.RegisterRequest;
import com.homestay3.homestaybackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    @PostMapping("/upload-avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file) throws IOException {
        String avatarUrl = authService.uploadAvatar(file);
        Map<String, String> response = new HashMap<>();
        response.put("url", avatarUrl);
        return ResponseEntity.ok(response);
    }
} 