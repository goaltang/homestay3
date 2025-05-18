package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.Admin;
import com.homestay3.homestaybackend.exception.UnauthorizedException;
import com.homestay3.homestaybackend.repository.AdminRepository;
import com.homestay3.homestaybackend.service.AdminService;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.repository.UserRepository;
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
    private final UserRepository userRepository;
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
            log.info("已创建默认管理员账号 admin (admin表)，默认密码为 admin888");
        }
    }
    
    @Override
    public void resetAdminPassword(String username, String newPassword) {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("管理员不存在"));
        
        admin.setPassword(passwordEncoder.encode(newPassword));
        adminRepository.save(admin);
        log.info("管理员 {} 的密码已重置 (admin表)", username);
    }
    
    @Bean
    public CommandLineRunner initializeAdminAndUser() {
        return args -> {
            // 1. 确保 admin 表中有 admin 账号
            boolean adminExistsInAdminTable = adminRepository.existsByUsername("admin");
            if (adminExistsInAdminTable) {
                // 如果存在，重置密码 (保留原逻辑)
                resetAdminPassword("admin", "admin888");
                log.info("系统启动时已重置 admin 账号密码 (admin表) 为: admin888");
            } else {
                // 如果不存在，创建默认管理员
                createDefaultAdminIfNotExists(); // 这个方法内部已有日志
            }

            // 2. 确保 user 表中也有对应的 admin 账号
            if (!userRepository.existsByUsername("admin")) {
                User adminUser = new User();
                adminUser.setUsername("admin");
                adminUser.setPassword(passwordEncoder.encode("admin888"));
                // 重要：设置管理员角色，请根据你的 User 实体和权限配置修改 "ROLE_ADMIN"
                adminUser.setRole("ROLE_ADMIN");
                // 设置其他必要字段，例如：
                adminUser.setEmail("admin@example.com"); // 设置一个默认邮箱或其他必填项
                adminUser.setEnabled(true); // 确保账号是启用的
                // 如果你的 User 实体还有其他非空字段，也需要在这里设置默认值

                userRepository.save(adminUser);
                log.info("已在 user 表中创建对应的 admin 用户账号，密码为 admin888，角色为 ROLE_ADMIN");
            } else {
                 // 可选：如果 user 表中已存在 admin，也可以考虑同步密码或角色
                 log.info("user 表中已存在 admin 用户账号。");
                 // 如果需要同步密码：
                 User existingAdminUser = userRepository.findByUsername("admin").orElse(null);
                 if (existingAdminUser != null && !passwordEncoder.matches("admin888", existingAdminUser.getPassword())) {
                     existingAdminUser.setPassword(passwordEncoder.encode("admin888"));
                     userRepository.save(existingAdminUser);
                     log.info("已同步 user 表中 admin 用户的密码为 admin888");
                 }
            }
        };
    }
} 