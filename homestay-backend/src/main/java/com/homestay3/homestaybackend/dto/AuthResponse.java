package com.homestay3.homestaybackend.dto;

import com.homestay3.homestaybackend.model.User;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthResponse {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String realName;
    private String idCard;
    private String avatar;
    private String role;
    private String verificationStatus;
    private String token;
    private UserDTO user;

    public AuthResponse(User user) {
        this.user = new UserDTO();
        this.user.setId(user.getId());
        this.user.setUsername(user.getUsername());
        this.user.setEmail(user.getEmail());
        this.user.setPhone(user.getPhone());
        this.user.setRealName(user.getRealName());
        this.user.setIdCard(user.getIdCard());
        this.user.setRole(user.getRole().name());
        this.user.setAvatar(user.getAvatar());
        this.user.setVerificationStatus(user.getVerificationStatus() != null ? 
            user.getVerificationStatus().name() : "UNVERIFIED");
    }

    public AuthResponse(String token, User user) {
        this.token = token;
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.realName = user.getRealName();
        this.idCard = user.getIdCard();
        this.role = user.getRole().name();
        this.verificationStatus = user.getVerificationStatus() != null ? 
                                 user.getVerificationStatus().name() : 
                                 "UNVERIFIED";
        this.avatar = user.getAvatar();
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
} 