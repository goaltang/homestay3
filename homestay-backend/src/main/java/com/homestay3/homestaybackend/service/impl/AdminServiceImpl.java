package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.Admin;
import com.homestay3.homestaybackend.exception.UnauthorizedException;
import com.homestay3.homestaybackend.repository.AdminRepository;
import com.homestay3.homestaybackend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    @Override
    public Admin getAdminByUsername(String username) {
        return adminRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("管理员不存在"));
    }

    @Override
    public void createDefaultAdminIfNotExists() {
        if (!adminRepository.existsByUsername("admin")) {
            Admin admin = new Admin();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin888"));
            adminRepository.save(admin);
            log.info("已创建默认管理员账号 admin，默认密码为 admin888");
        }
    }
    
    @Override
    public void resetAdminPassword(String username, String newPassword) {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("管理员不存在"));
        
        admin.setPassword(passwordEncoder.encode(newPassword));
        adminRepository.save(admin);
        log.info("管理员 {} 的密码已重置", username);
    }
    
    @Bean
    public CommandLineRunner resetAdminPasswordOnStartup() {
        return args -> {
            // 如果admin账号存在，则重置密码
            if (adminRepository.existsByUsername("admin")) {
                resetAdminPassword("admin", "admin888");
                log.info("系统启动时已重置admin账号密码为: admin888");
            } else {
                createDefaultAdminIfNotExists();
            }
        };
    }
} 