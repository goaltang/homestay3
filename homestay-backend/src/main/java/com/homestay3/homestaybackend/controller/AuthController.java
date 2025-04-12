package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.AuthRequest;
import com.homestay3.homestaybackend.dto.AuthResponse;
import com.homestay3.homestaybackend.dto.PasswordResetRequest;
import com.homestay3.homestaybackend.dto.RegisterRequest;
import com.homestay3.homestaybackend.service.AuthService;
import com.homestay3.homestaybackend.dto.AdminLoginRequest;
import com.homestay3.homestaybackend.dto.AdminLoginResponse;
import com.homestay3.homestaybackend.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationService authenticationService;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        log.info("注册请求: {}", request.getUsername());
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("注册失败: {}", e.getMessage());
            throw e;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        log.info("登录请求: {}", request.getUsername());
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            throw e;
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestParam String email) {
        log.info("忘记密码请求: {}", email);
        try {
            authService.forgotPassword(email);
            Map<String, String> response = new HashMap<>();
            response.put("message", "密码重置邮件已发送");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("忘记密码处理失败: {}", e.getMessage());
            throw e;
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody PasswordResetRequest request) {
        log.info("重置密码请求");
        try {
            authService.resetPassword(request);
            Map<String, String> response = new HashMap<>();
            response.put("message", "密码重置成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("重置密码失败: {}", e.getMessage());
            throw e;
        }
    }

    @PostMapping("/upload-avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "username", required = false) String username) {
        
        log.info("上传头像请求: username={}, filename={}, size={}KB", 
                username, file.getOriginalFilename(), file.getSize() / 1024);
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "文件为空"));
            }
            
            String avatarUrl = authService.uploadAvatar(file, username);
            return ResponseEntity.ok(Map.of("url", avatarUrl));
        } catch (IOException e) {
            log.error("上传头像失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "上传头像失败: " + e.getMessage()));
        }
    }

    @GetMapping("/user-info")
    public ResponseEntity<AuthResponse> getUserInfo(@RequestParam String username) {
        log.info("获取用户信息请求: {}", username);
        try {
            AuthResponse response = authService.getUserInfo(username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 检查用户名是否已存在
     * @param username 用户名
     * @return 包含exists字段的JSON，表示用户名是否存在
     */
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        log.info("检查用户名是否存在: {}", username);
        boolean exists = authService.isUsernameExists(username);
        log.info("用户名 {} 存在状态: {}", username, exists);
        return ResponseEntity.ok(Map.of("exists", exists));
    }
    
    /**
     * 检查邮箱是否已存在
     * @param email 邮箱
     * @return 包含exists字段的JSON，表示邮箱是否存在
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        log.info("检查邮箱是否存在: {}", email);
        boolean exists = authService.isEmailExists(email);
        log.info("邮箱 {} 存在状态: {}", email, exists);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * 获取当前认证用户信息
     * @param authentication Spring Security认证对象
     * @return 当前用户信息
     */
    @GetMapping("/current")
    public ResponseEntity<AuthResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            log.warn("获取当前用户信息失败: 未认证");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        log.info("获取当前用户信息: {}", authentication.getName());
        try {
            // 获取基本用户信息
            AuthResponse response = authService.getUserInfo(authentication.getName());
            
            // 确保角色信息正确返回
            if (response != null) {
                log.info("用户 {} 的角色: {}", authentication.getName(), response.getRole());
                
                // 添加authorities信息到响应
                if (authentication.getAuthorities() != null && !authentication.getAuthorities().isEmpty()) {
                    List<String> authorities = authentication.getAuthorities().stream()
                        .map(auth -> auth.getAuthority())
                        .collect(Collectors.toList());
                    response.setAuthorities(authorities);
                    log.info("用户 {} 的authorities: {}", authentication.getName(), authorities);
                }
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取当前用户信息失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.builder()
                            .username(null)
                            .id(null) 
                            .email(null)
                            .phone(null)
                            .realName(null)
                            .avatar(null)
                            .build());
        }
    }

} 