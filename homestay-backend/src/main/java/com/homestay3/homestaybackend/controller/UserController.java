package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.AuthResponse;
import com.homestay3.homestaybackend.dto.PasswordChangeRequest;
import com.homestay3.homestaybackend.dto.ProfileUpdateRequest;
import com.homestay3.homestaybackend.service.FileService;
import com.homestay3.homestaybackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final FileService fileService;

    @PutMapping("/profile")
    public ResponseEntity<AuthResponse> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(userService.updateProfile(request, userDetails.getUsername()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        userService.changePassword(request, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
} 