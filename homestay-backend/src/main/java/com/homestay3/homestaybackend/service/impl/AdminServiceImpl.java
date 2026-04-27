package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.Admin;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.exception.UnauthorizedException;
import com.homestay3.homestaybackend.repository.AdminRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    @Override
    public Admin getAdminByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("管理员不存在"));
        Admin admin = new Admin();
        admin.setId(user.getId());
        admin.setUsername(user.getUsername());
        admin.setPassword(user.getPassword());
        admin.setRole(user.getRole());
        return admin;
    }

    @Override
    public void createDefaultAdminIfNotExists() {
        if (!userRepository.existsByUsername("admin")) {
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setPassword(passwordEncoder.encode("admin888"));
            adminUser.setRole("ROLE_ADMIN");
            adminUser.setEmail("admin@homestay.local");
            adminUser.setEnabled(true);
            userRepository.save(adminUser);
            log.info("已在 users 表中创建默认管理员账号 admin，密码为 admin888");
        }
    }

    @Override
    public void resetAdminPassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("管理员不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("管理员 {} 的密码已重置", username);
    }

    @Bean
    public CommandLineRunner initializeAdminAndUser() {
        return args -> {
            // 创建默认管理员（仅在不存在时创建，不重置密码）
            if (!userRepository.existsByUsername("admin")) {
                createDefaultAdminIfNotExists();
            }

            // 迁移 admin 表中的所有管理员到 users 表
            var allAdmins = adminRepository.findAll();
            for (Admin admin : allAdmins) {
                if (!userRepository.existsByUsername(admin.getUsername())) {
                    User adminUser = new User();
                    adminUser.setUsername(admin.getUsername());
                    adminUser.setPassword(admin.getPassword());
                    adminUser.setRole("ROLE_ADMIN");
                    adminUser.setEmail(admin.getUsername() + "@homestay.local");
                    adminUser.setEnabled(true);
                    userRepository.save(adminUser);
                    log.info("已将 admin 表中的 {} 迁移到 users 表", admin.getUsername());
                }
            }
        };
    }
}
