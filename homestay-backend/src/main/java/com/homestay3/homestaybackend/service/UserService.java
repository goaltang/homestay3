package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.AuthResponse;
import com.homestay3.homestaybackend.dto.PasswordChangeRequest;
import com.homestay3.homestaybackend.dto.ProfileUpdateRequest;
import com.homestay3.homestaybackend.dto.UserDTO;
import com.homestay3.homestaybackend.model.User;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    @Transactional
    public AuthResponse updateProfile(ProfileUpdateRequest request, String currentUsername) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查用户名是否被其他用户使用
        if (!user.getUsername().equals(request.getUsername()) &&
                userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已被使用");
        }

        // 检查邮箱是否被其他用户使用
        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被使用");
        }

        // 更新用户信息
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRealName(request.getRealName());
        user.setIdCard(request.getIdCard());

        user = userRepository.save(user);

        // 生成新的 token（因为用户名可能已更改）
        String token = jwtTokenUtil.generateToken(
                new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        user.getEnabled(),
                        true,
                        true,
                        true,
                        java.util.Collections.emptyList()
                )
        );

        // 返回更新后的用户信息和新 token
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUser(convertToDTO(user));
        return response;
    }

    @Transactional
    public void changePassword(PasswordChangeRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 验证原密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("原密码不正确");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
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