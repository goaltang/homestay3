package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.AdminLoginRequest;
import com.homestay3.homestaybackend.dto.AdminLoginResponse;
import com.homestay3.homestaybackend.entity.Admin;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.AuthenticationService;
import com.homestay3.homestaybackend.service.LoginLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final LoginLogService loginLogService;

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@RequestBody AdminLoginRequest request,
                                                    HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        try {
            AdminLoginResponse response = authenticationService.login(request);
            // 记录登录成功日志
            loginLogService.recordLogin(request.getUsername(), ipAddress, userAgent, "SUCCESS", "ADMIN");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 记录登录失败日志
            loginLogService.recordLogin(request.getUsername(), ipAddress, userAgent, "FAIL", "ADMIN");
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        // 从token中获取用户名（简化处理，实际可通过SecurityContext获取）
        authenticationService.logout();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/info")
    public ResponseEntity<Admin> getAdminInfo(@RequestHeader("Authorization") String token) {
        String username = authenticationService.getUsernameFromToken(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
        Admin admin = new Admin();
        admin.setId(user.getId());
        admin.setUsername(user.getUsername());
        admin.setRole(user.getRole());
        return ResponseEntity.ok(admin);
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
} 