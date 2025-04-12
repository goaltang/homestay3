package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.AdminLoginRequest;
import com.homestay3.homestaybackend.dto.AdminLoginResponse;
import com.homestay3.homestaybackend.entity.Admin;
import com.homestay3.homestaybackend.service.AdminService;
import com.homestay3.homestaybackend.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthenticationService authenticationService;
    private final AdminService adminService;

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authenticationService.logout();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/info")
    public ResponseEntity<Admin> getAdminInfo(@RequestHeader("Authorization") String token) {
        String username = authenticationService.getUsernameFromToken(token);
        Admin admin = adminService.getAdminByUsername(username);
        return ResponseEntity.ok(admin);
    }
} 