package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.entity.Admin;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.model.UserRole;
import com.homestay3.homestaybackend.repository.AdminRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("尝试加载用户: {}", username);
        
        // 先尝试从普通用户库中加载
        try {
            return userRepository.findByUsername(username)
                    .map(user -> {
                        log.info("从用户库中找到了用户: {}, 角色: {}", username, user.getRole());
                        // 检查用户角色是否有前缀ROLE_
                        String role = user.getRole();
                        if (!role.startsWith("ROLE_")) {
                            role = "ROLE_" + role;
                            log.info("添加ROLE_前缀到角色: {}", role);
                        }
                        
                        log.info("创建用户权限: {}", role);
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
                        log.info("创建的权限详情: {}", authority);
                        
                        return new org.springframework.security.core.userdetails.User(
                                user.getUsername(),
                                user.getPassword(),
                                user.isEnabled(),
                                true,
                                true,
                                true,
                                Collections.singletonList(authority)
                        );
                    })
                    .orElseThrow(() -> {
                        log.warn("在用户库中未找到用户: {}, 继续尝试管理员库", username);
                        return new RuntimeException("继续尝试管理员库");
                    });
        } catch (RuntimeException e) {
            if (!"继续尝试管理员库".equals(e.getMessage())) {
                throw e;
            }
            // 不处理，继续尝试管理员库
        }
        
        // 如果普通用户库中不存在，则尝试从管理员库中加载
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("用户在普通用户库和管理员库中均不存在: {}", username);
                    return new UsernameNotFoundException("用户不存在: " + username);
                });

        log.info("从管理员库中找到了用户: {}", username);
        String adminRoleName = UserRole.ROLE_ADMIN.name(); // e.g., "ADMIN"
        
        // Ensure the authority string starts with "ROLE_"
        String authorityString = adminRoleName;
        if (!authorityString.startsWith("ROLE_")) {
            authorityString = "ROLE_" + authorityString;
            log.info("为管理员添加 ROLE_ 前缀，最终权限字符串: {}", authorityString);
        } else {
            log.info("管理员权限字符串已包含 ROLE_ 前缀: {}", authorityString);
        }
        
        // Since admin table has no enabled field, assume admin is always enabled if found.
        boolean isAdminEnabled = true; 
        log.info("管理员 {} 的启用状态 (假定为 true): {}", username, isAdminEnabled);

        // Use the full constructor for UserDetails
        return new org.springframework.security.core.userdetails.User(
                admin.getUsername(),
                admin.getPassword(),
                isAdminEnabled,      // Pass true for enabled status
                true,               // accountNonExpired (usually true)
                true,               // credentialsNonExpired (usually true)
                true,               // accountNonLocked (usually true)
                Collections.singletonList(new SimpleGrantedAuthority(authorityString)) // Use the prefixed string
        );
    }
} 