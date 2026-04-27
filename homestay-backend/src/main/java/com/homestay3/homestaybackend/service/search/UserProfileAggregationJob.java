package com.homestay3.homestaybackend.service.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 用户画像定时聚合任务
 * 自动按行为事件更新用户偏好画像
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileAggregationJob {

    private final UserProfileService userProfileService;

    /**
     * 每小时执行一次：聚合最近活跃用户画像
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void aggregateActiveUserProfiles() {
        log.info("[定时任务] 开始聚合活跃用户画像...");
        try {
            userProfileService.aggregateAllActiveProfiles();
            log.info("[定时任务] 活跃用户画像聚合完成");
        } catch (Exception e) {
            log.error("[定时任务] 用户画像聚合失败", e);
        }
    }
}
