package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.*;
import com.homestay3.homestaybackend.model.User;
import com.homestay3.homestaybackend.model.VerificationStatus;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.security.JwtTokenUtil;
import com.homestay3.homestaybackend.service.AuthService;
import com.homestay3.homestaybackend.service.CustomUserDetailsService;
import com.homestay3.homestaybackend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final EmailService emailService;
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
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
            user.setRole(request.getRole() != null ? request.getRole() : "ROLE_USER");
            user.setEnabled(true);
            user.setVerificationStatus(VerificationStatus.UNVERIFIED);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
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

    @Override
    public AuthResponse login(AuthRequest request) {
        log.info("用户登录: {}", request.getUsername());
        
        // 验证用户名和密码
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 获取用户信息
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        log.info("用户信息: id={}, username={}, realName={}", 
            user.getId(), user.getUsername(), user.getRealName());

        // 更新最后登录时间
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // 生成token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtTokenUtil.generateToken(userDetails);

        // 返回认证响应
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUser(convertToDTO(user));
        log.info("登录响应: {}", response);
        return response;
    }

    @Override
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
        sendPasswordResetEmail(email);
        log.info("密码重置邮件已发送: {}", email);
    }

    @Override
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

    @Override
    public String uploadAvatar(MultipartFile file, String username) throws IOException {
        log.info("开始处理头像上传: 用户={}, 文件名={}, 大小={}, 类型={}", 
                username, file.getOriginalFilename(), file.getSize(), file.getContentType());

        // 确保目录存在
        String projectRoot = System.getProperty("user.dir");
        String uploadDir = projectRoot + File.separator + "uploads" + File.separator + "avatars";
        File dir = new File(uploadDir);
        
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            log.info("创建上传目录: {} - {}", uploadDir, created ? "成功" : "失败");
            if (!created) {
                throw new IOException("无法创建上传目录: " + uploadDir);
            }
        }
        
        // 检查目录权限
        if (!dir.canWrite() || !dir.canRead()) {
            log.error("目录权限不足: {} - 读:{} 写:{}", 
                    uploadDir, dir.canRead(), dir.canWrite());
            throw new IOException("目录权限不足: " + uploadDir);
        }
        
        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".jpg";
                
        String filename = System.currentTimeMillis() + "_" + (int)(Math.random() * 1000) + extension;
        File destFile = new File(dir, filename);
        
        log.info("准备保存文件: {}", destFile.getAbsolutePath());
        
        try {
            // 保存文件
            file.transferTo(destFile);
            
            // 验证文件是否成功保存
            if (!destFile.exists()) {
                throw new IOException("文件保存失败: " + destFile.getAbsolutePath());
            }
            
            // 验证文件是否可读
            if (!destFile.canRead()) {
                throw new IOException("无法读取保存的文件: " + destFile.getAbsolutePath());
            }
            
            String avatarUrl = "/uploads/avatars/" + filename;
            log.info("文件保存成功: {} -> {}", destFile.getAbsolutePath(), avatarUrl);
            
            // 更新用户头像路径
            if (username != null && !username.startsWith("anonymous-")) {
                updateUserAvatar(username, avatarUrl);
                log.info("用户头像路径已更新: {}", username);
            } else {
                log.info("匿名用户上传，不更新用户信息");
            }
            
            return avatarUrl;
            
        } catch (IOException e) {
            log.error("保存文件时出错: {}", e.getMessage(), e);
            throw new IOException("保存头像文件失败: " + e.getMessage(), e);
        }
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
        dto.setRole(user.getRole());
        dto.setAvatar(user.getAvatar());
        dto.setVerificationStatus(user.getVerificationStatus());
        dto.setFullName(user.getFullName());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setLastLogin(user.getLastLogin());
        dto.setEnabled(user.isEnabled());
        return dto;
    }

    @Override
    public AuthResponse getUserInfo(String username) {
        log.info("获取用户信息: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        
        AuthResponse response = new AuthResponse();
        response.setUser(convertToDTO(user));
        return response;
    }

    @Override
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

    @Override
    public void updateUserAvatar(String username, String avatarUrl) {
        log.info("更新用户头像: username={}, avatarUrl={}", username, avatarUrl);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        
        user.setAvatar(avatarUrl);
        userRepository.save(user);
        
        log.info("用户头像更新成功: userId={}", user.getId());
    }

    /**
     * 检查用户名是否已存在
     * @param username 用户名
     * @return true如果存在，false如果不存在
     */
    @Override
    public boolean isUsernameExists(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return userRepository.existsByUsername(username.trim());
    }

    /**
     * 检查邮箱是否已存在
     * @param email 邮箱
     * @return true如果存在，false如果不存在
     */
    @Override
    public boolean isEmailExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return userRepository.existsByEmail(email.trim().toLowerCase());
    }
} 