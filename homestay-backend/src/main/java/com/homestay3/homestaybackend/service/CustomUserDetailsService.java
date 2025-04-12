package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.entity.Admin;
import com.homestay3.homestaybackend.model.User;
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
                        return new org.springframework.security.core.userdetails.User(
                                user.getUsername(),
                                user.getPassword(),
                                user.isEnabled(),
                                true,
                                true,
                                true,
                                Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
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
        return new org.springframework.security.core.userdetails.User(
                admin.getUsername(),
                admin.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }
} 