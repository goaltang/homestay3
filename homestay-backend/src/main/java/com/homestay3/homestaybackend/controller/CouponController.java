package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.AvailableCouponDTO;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.entity.UserCoupon;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.CouponAnalyticsService;
import com.homestay3.homestaybackend.service.CouponService;
import com.homestay3.homestaybackend.service.ReferralService;
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
    private final ReferralService referralService;
    private final CouponAnalyticsService couponAnalyticsService;

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
            // 转化漏斗埋点：领取
            couponAnalyticsService.track(templateId, null, "HOMESTAY_DETAIL", "CLAIM",
                    currentUser.getId(), null, null);
            return ResponseEntity.ok(coupon);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "领取失败: " + e.getMessage()));
        }
    }

    @PostMapping("/referral/{code}/claim")
    public ResponseEntity<?> useReferralCode(@PathVariable String code, Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            boolean success = referralService.useReferralCode(code, currentUser.getId());
            if (success) {
                return ResponseEntity.ok(Map.of("message", "邀请码使用成功，奖励已发放"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "邀请码无效或已过期"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/referral/stats")
    public ResponseEntity<?> getReferralStats(Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            return ResponseEntity.ok(referralService.getReferralStats(currentUser.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
