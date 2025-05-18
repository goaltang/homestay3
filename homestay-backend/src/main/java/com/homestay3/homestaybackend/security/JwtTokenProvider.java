package com.homestay3.homestaybackend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        
        // 获取用户的所有角色，以逗号分隔
        String authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        
        log.info("为用户 {} 生成token, 权限: {}", userDetails.getUsername(), authorities);
        
        // 直接调用另一个generateToken方法，传入所有权限
        return generateToken(userDetails.getUsername(), authorities);
    }

    public String generateToken(String username, String authorities) {
        log.info("生成JWT Token: 用户={}, 权限={}", username, authorities);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
        log.info("Token过期时间: {}", expiryDate);

        // 创建 Claims Map 用于日志记录
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("sub", username);
        claimsMap.put("authorities", authorities);
        claimsMap.put("iat", now);
        claimsMap.put("exp", expiryDate);
        log.info("构建 JWT Claims: {}", claimsMap);

        String token = Jwts.builder()
                .setSubject(username)
                .claim("authorities", authorities)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
        
        log.info("JWT Token生成成功, 长度: {}", token.length());
        return token;
    }

    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            String username = claims.getSubject();
            log.debug("从Token解析出用户名: {}", username);
            
            // 尝试获取并记录 "authorities" claim
            Object authoritiesClaim = claims.get("authorities");
            if (authoritiesClaim != null) {
                log.debug("Token中的权限信息 (authorities): {}", authoritiesClaim);
            } else {
                log.warn("Token中未包含 'authorities' claim");
            }
            
            return username;
        } catch (Exception e) {
            log.error("从Token解析用户名失败: {}", e.getMessage());
            throw e;
        }
    }

    public String getAuthoritiesFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            String authorities = claims.get("authorities", String.class);
            log.debug("从Token解析出权限: {}", authorities);
            return authorities;
        } catch (Exception e) {
            log.error("从Token解析权限失败: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            log.debug("JWT Token验证成功");
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("无效的JWT Token: {}", e.getMessage());
            return false;
        }
    }
} 