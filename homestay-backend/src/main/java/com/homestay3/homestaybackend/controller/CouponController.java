package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.AvailableCouponDTO;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.entity.UserCoupon;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CouponController {

    private final CouponService couponService;
    private final UserRepository userRepository;

    @GetMapping("/available")
    public ResponseEntity<?> getAvailableCoupons(Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            List<AvailableCouponDTO> coupons = couponService.getAvailableCouponDTOs(currentUser.getId());
            return ResponseEntity.ok(coupons);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyCoupons(Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            List<UserCoupon> coupons = couponService.getAvailableCoupons(currentUser.getId());
            return ResponseEntity.ok(coupons);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{templateId}/claim")
    public ResponseEntity<?> claimCoupon(@PathVariable Long templateId, Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            UserCoupon coupon = couponService.claimCoupon(currentUser.getId(), templateId);
            return ResponseEntity.ok(coupon);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "领取失败: " + e.getMessage()));
        }
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("用户未登录");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }
}
