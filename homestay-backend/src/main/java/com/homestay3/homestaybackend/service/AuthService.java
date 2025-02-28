package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.*;
import com.homestay3.homestaybackend.model.User;
import com.homestay3.homestaybackend.model.UserRole;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final EmailService emailService;
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

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
            user.setRole(UserRole.valueOf(request.getRole()));
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
        log.info("用户登录: {}", request.getUsername());
        
        // 验证用户名和密码
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 获取用户信息
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        log.info("用户信息: id={}, username={}, realName={}, verificationStatus={}", 
            user.getId(), user.getUsername(), user.getRealName(), user.getVerificationStatus());

        // 生成token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtTokenUtil.generateToken(userDetails);

        // 返回认证响应
        AuthResponse response = new AuthResponse(token, user);
        log.info("登录响应: {}", response);
        return response;
    }

    @Transactional
    public void forgotPassword(String email) {
        log.info("处理忘记密码请求: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("该邮箱未注册"));
        
        // 生成重置令牌
        String resetToken = generateResetToken(user);
        
        // 保存重置令牌到用户记录
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);
        
        // 发送重置邮件
        emailService.sendPasswordResetEmail(email, resetToken);
        log.info("密码重置邮件已发送: {}", email);
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        log.info("处理密码重置请求，验证token");
        
        try {
            // 验证JWT token并获取用户名
            String username = jwtTokenUtil.extractUsername(request.getToken());
            if (username == null) {
                throw new RuntimeException("无效的重置令牌");
            }
            
            // 查找用户
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            log.info("找到用户: {}", username);
            
            // 更新密码
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);
            
            log.info("密码重置成功: {}", username);
        } catch (Exception e) {
            log.error("密码重置失败: {}", e.getMessage());
            throw new RuntimeException("密码重置失败: " + e.getMessage());
        }
    }

    private String generateResetToken(User user) {
        return UUID.randomUUID().toString();
    }

    /**
     * 上传用户头像
     * @param file 头像文件
     * @return 头像URL
     * @throws IOException 如果文件处理出错
     */
    public String uploadAvatar(MultipartFile file) throws IOException {
        // 获取当前认证用户
        User user = getCurrentUser();
        
        // 确保上传目录存在
        String uploadDir = System.getProperty("user.dir") + "/uploads/avatars";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = user.getId() + "_" + System.currentTimeMillis() + extension;
        
        // 保存文件
        File destFile = new File(dir.getAbsolutePath() + File.separator + filename);
        file.transferTo(destFile);
        
        // 更新用户头像URL（使用相对路径）
        String avatarUrl = "/uploads/avatars/" + filename;
        user.setAvatar(avatarUrl);
        userRepository.save(user);
        
        log.info("头像上传成功 - 用户ID: {}, 文件名: {}, URL: {}", user.getId(), filename, avatarUrl);
        log.info("文件保存路径: {}", destFile.getAbsolutePath());
        
        return avatarUrl;
    }
    
    /**
     * 获取当前认证用户
     * @return 当前用户
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRealName(user.getRealName());
        dto.setIdCard(user.getIdCard());
        dto.setRole(user.getRole().name());
        dto.setAvatar(user.getAvatar());
        dto.setVerificationStatus(user.getVerificationStatus() != null ? 
            user.getVerificationStatus().name() : "UNVERIFIED");
        return dto;
    }

    public AuthResponse getUserInfo(String username) {
        log.info("获取用户信息: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        
        return new AuthResponse(user);
    }

    public void sendPasswordResetEmail(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtTokenUtil.generateToken(userDetails);
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        try {
            emailService.sendPasswordResetEmail(email, resetLink);
        } catch (Exception e) {
            throw new RuntimeException("发送重置密码邮件失败: " + e.getMessage());
        }
    }
} 