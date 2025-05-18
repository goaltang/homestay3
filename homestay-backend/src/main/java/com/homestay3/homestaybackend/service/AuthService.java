package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.*;
import com.homestay3.homestaybackend.entity.User;
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

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(AuthRequest request);
    void forgotPassword(String email);
    void resetPassword(PasswordResetRequest request);
    String uploadAvatar(MultipartFile file, String username) throws IOException;
    AuthResponse getUserInfo(String username);
    void sendPasswordResetEmail(String email);
    void updateUserAvatar(String username, String avatarUrl);
    
    /**
     * 检查用户名是否已存在
     * @param username 用户名
     * @return true如果存在，false如果不存在
     */
    boolean isUsernameExists(String username);
    
    /**
     * 检查邮箱是否已存在
     * @param email 邮箱
     * @return true如果存在，false如果不存在
     */
    boolean isEmailExists(String email);
} 