package com.homestay3.homestaybackend.dto;

import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.model.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String realName;
    private String idCard;
    private String role;
    private String avatar;
    private String nickname;
    private String occupation;
    private String introduction;
    private List<String> languages;
    private LocalDateTime hostSince;
    private Double hostRating;
    private Integer hostAccommodations;
    private Integer hostYears;
    private String hostResponseRate;
    private String hostResponseTime;
    private List<Map<String, String>> companions;
    private VerificationStatus verificationStatus;
    private String fullName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;
    private boolean enabled;
    
    public UserDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.realName = user.getRealName();
        this.idCard = user.getIdCard();
        this.role = user.getRole();
        this.avatar = user.getAvatar();
        this.nickname = user.getNickname();
        this.occupation = user.getOccupation();
        this.introduction = user.getIntroduction();
        this.hostSince = user.getHostSince();
        this.hostRating = user.getHostRating();
        this.hostAccommodations = user.getHostAccommodations();
        this.hostYears = user.getHostYears();
        this.hostResponseRate = user.getHostResponseRate();
        this.hostResponseTime = user.getHostResponseTime();
        this.verificationStatus = user.getVerificationStatus();
        this.fullName = user.getFullName();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
        this.lastLogin = user.getLastLogin();
        this.enabled = user.isEnabled();
    }
} 