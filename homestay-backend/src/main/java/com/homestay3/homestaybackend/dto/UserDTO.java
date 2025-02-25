package com.homestay3.homestaybackend.dto;

import com.homestay3.homestaybackend.model.UserRole;
import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String realName;
    private String idCard;
    private UserRole role;
} 