package com.homestay3.homestaybackend.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private Boolean remember;
} 