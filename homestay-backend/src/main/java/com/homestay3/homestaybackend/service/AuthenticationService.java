package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.AdminLoginRequest;
import com.homestay3.homestaybackend.dto.AdminLoginResponse;
import com.homestay3.homestaybackend.entity.Admin;
import com.homestay3.homestaybackend.exception.UnauthorizedException;
import com.homestay3.homestaybackend.repository.AdminRepository;
import com.homestay3.homestaybackend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AdminRepository adminRepository;

    public AdminLoginResponse login(AdminLoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtTokenProvider.generateToken(authentication);

            Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("用户名或密码错误"));

            return AdminLoginResponse.builder()
                .token(jwt)
                .admin(admin)
                .build();
        } catch (Exception e) {
            throw new UnauthorizedException("用户名或密码错误");
        }
    }

    public void logout() {
        SecurityContextHolder.clearContext();
    }

    public String getUsernameFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtTokenProvider.getUsernameFromToken(token);
    }
} 