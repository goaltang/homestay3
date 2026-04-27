package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.UserCoupon;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.repository.UserCouponRepository;
import com.homestay3.homestaybackend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 优惠券过期提醒服务
 * 每日检查即将过期的优惠券并发送通知
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponExpiryNotificationService {

    private static final int EXPIRY_WARNING_HOURS = 24;

    private final UserCouponRepository userCouponRepository;
    private final NotificationService notificationService;

    /**
     * 每小时检查一次即将过期的优惠券
     */
    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Transactional(readOnly = true)
    public void checkExpiringCoupons() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime deadline = now.plusHours(EXPIRY_WARNING_HOURS);

            List<UserCoupon> expiringCoupons = userCouponRepository.findCouponsExpiringBetween(now, deadline);

            if (expiringCoupons.isEmpty()) {
                return;
            }

            // 按用户分组
            Map<Long, List<UserCoupon>> groupedByUser = expiringCoupons.stream()
                    .collect(Collectors.groupingBy(UserCoupon::getUserId));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM月dd日 HH:mm");

            for (Map.Entry<Long, List<UserCoupon>> entry : groupedByUser.entrySet()) {
                Long userId = entry.getKey();
                List<UserCoupon> coupons = entry.getValue();

                StringBuilder content = new StringBuilder();
                content.append("您有").append(coupons.size()).append("张优惠券即将过期，请尽快使用：");
                for (UserCoupon uc : coupons) {
                    String name = uc.getTemplate() != null ? uc.getTemplate().getName() : "优惠券";
                    String expireTime = uc.getExpireAt() != null ? uc.getExpireAt().format(formatter) : "即将";
                    content.append("\n- ").append(name).append("（").append(expireTime).append("到期）");
                }

                try {
                    notificationService.createNotification(
                            userId, null,
                            NotificationType.COUPON_EXPIRING,
                            EntityType.COUPON,
                            null,
                            content.toString()
                    );
                } catch (Exception e) {
                    log.warn("向用户 {} 发送优惠券过期提醒失败: {}", userId, e.getMessage());
                }
            }
            log.info("优惠券过期提醒检查完成: 共{}张券，{}位用户",
                    expiringCoupons.size(), groupedByUser.size());
        } catch (Exception e) {
            log.error("优惠券过期提醒检查异常: {}", e.getMessage(), e);
        }
    }
}
