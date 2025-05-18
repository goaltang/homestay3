package com.homestay3.homestaybackend.dto;

import com.homestay3.homestaybackend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String realName;
    private String idCard;
    private String role;
    private String avatar;
    private String verificationStatus;
    private UserDTO user;
    
    private List<String> authorities;
    
    public AuthResponse(User user) {
        this.user = new UserDTO(user);
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.realName = user.getRealName();
        this.idCard = user.getIdCard();
        this.role = user.getRole();
        this.avatar = user.getAvatar();
        this.verificationStatus = user.getVerificationStatus() != null ? 
                                  user.getVerificationStatus().toString() : null;
    }
    
    public AuthResponse(User user, String token) {
        this(user);
        this.token = token;
    }
} 