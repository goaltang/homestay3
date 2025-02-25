package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.*;
import com.homestay3.homestaybackend.model.User;
import com.homestay3.homestaybackend.model.UserRole;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("开始注册用户: {}", request.getUsername());
        
        try {
            // 先查询用户数量
            log.info("当前用户总数: {}", userRepository.count());
            
            // 检查用户名是否存在
            boolean usernameExists = userRepository.existsByUsername(request.getUsername());
            log.info("用户名 {} 是否存在: {}", request.getUsername(), usernameExists);
            if (usernameExists) {
                log.warn("用户名已存在: {}", request.getUsername());
                throw new RuntimeException("用户名已存在");
            }

            // 检查邮箱是否存在
            boolean emailExists = userRepository.existsByEmail(request.getEmail());
            log.info("邮箱 {} 是否存在: {}", request.getEmail(), emailExists);
            if (emailExists) {
                log.warn("邮箱已存在: {}", request.getEmail());
                throw new RuntimeException("邮箱已存在");
            }

            // 创建新用户
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setPhone(request.getPhone());
            user.setRole(UserRole.USER);
            user.setEnabled(true);
            
            // 保存用户并立即刷新
            user = userRepository.saveAndFlush(user);
            log.info("用户创建成功，ID: {}, 用户名: {}", user.getId(), user.getUsername());

            // 生成token
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            String token = jwtTokenUtil.generateToken(userDetails);

            // 返回认证响应
            AuthResponse response = new AuthResponse();
            response.setToken(token);
            response.setUser(convertToDTO(user));
            
            log.info("注册完成，返回响应");
            return response;
        } catch (Exception e) {
            log.error("注册失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    public AuthResponse login(LoginRequest request) {
        // 验证用户名和密码
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 获取用户信息
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 生成token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtTokenUtil.generateToken(userDetails);

        // 返回认证响应
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUser(convertToDTO(user));
        return response;
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("该邮箱未注册"));
        
        // TODO: 实现发送重置密码邮件的逻辑
        // 1. 生成重置token
        // 2. 保存重置token
        // 3. 发送重置邮件
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        return dto;
    }
} 