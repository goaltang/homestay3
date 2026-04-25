package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.PricingQuoteRequest;
import com.homestay3.homestaybackend.dto.PricingQuoteResponse;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PricingController {

    private final PricingService pricingService;
    private final UserRepository userRepository;

    @PostMapping("/quote")
    public ResponseEntity<?> quote(@RequestBody PricingQuoteRequest request, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            PricingQuoteResponse response = pricingService.quote(request, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "报价失败: " + e.getMessage()));
        }
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        try {
            User user = userRepository.findByUsername(authentication.getName()).orElse(null);
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
