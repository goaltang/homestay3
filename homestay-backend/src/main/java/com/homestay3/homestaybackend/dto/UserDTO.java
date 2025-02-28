package com.homestay3.homestaybackend.dto;

import com.homestay3.homestaybackend.model.User;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String realName;
    private String idCard;
    private String avatar;
    private String role;
    private String verificationStatus;

    public UserDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.realName = user.getRealName();
        this.idCard = user.getIdCard();
        this.avatar = user.getAvatar();
        this.role = user.getRole().name();
        this.verificationStatus = user.getVerificationStatus().name();
    }
} 