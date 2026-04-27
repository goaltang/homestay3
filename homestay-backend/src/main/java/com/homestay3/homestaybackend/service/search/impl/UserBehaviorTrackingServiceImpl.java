package com.homestay3.homestaybackend.service.search.impl;

import com.homestay3.homestaybackend.entity.UserBehaviorEvent;
import com.homestay3.homestaybackend.repository.UserBehaviorEventRepository;
import com.homestay3.homestaybackend.service.search.UserBehaviorTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserBehaviorTrackingServiceImpl implements UserBehaviorTrackingService {

    private final UserBehaviorEventRepository userBehaviorEventRepository;

    @Override
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackView(Long userId, String sessionId, Long homestayId,
                          String cityCode, String type, BigDecimal price) {
        try {
            UserBehaviorEvent event = UserBehaviorEvent.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .eventType("VIEW")
                    .homestayId(homestayId)
                    .cityCode(cityCode)
                    .type(type)
                    .price(price)
                    .build();
            userBehaviorEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Failed to track VIEW event: {}", e.getMessage());
        }
    }

    @Override
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackSearch(Long userId, String sessionId, String keyword,
                            String cityCode, String type) {
        try {
            UserBehaviorEvent event = UserBehaviorEvent.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .eventType("SEARCH")
                    .keyword(keyword)
                    .cityCode(cityCode)
                    .type(type)
                    .build();
            userBehaviorEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Failed to track SEARCH event: {}", e.getMessage());
        }
    }

    @Override
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackClick(Long userId, String sessionId, Long homestayId) {
        try {
            UserBehaviorEvent event = UserBehaviorEvent.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .eventType("CLICK")
                    .homestayId(homestayId)
                    .build();
            userBehaviorEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Failed to track CLICK event: {}", e.getMessage());
        }
    }

    @Override
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackFavorite(Long userId, String sessionId, Long homestayId,
                              String cityCode, String type, BigDecimal price) {
        try {
            UserBehaviorEvent event = UserBehaviorEvent.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .eventType("FAVORITE")
                    .homestayId(homestayId)
                    .cityCode(cityCode)
                    .type(type)
                    .price(price)
                    .build();
            userBehaviorEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Failed to track FAVORITE event: {}", e.getMessage());
        }
    }

    @Override
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackBooking(Long userId, String sessionId, Long homestayId,
                             String cityCode, String type, BigDecimal price) {
        try {
            UserBehaviorEvent event = UserBehaviorEvent.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .eventType("BOOKING")
                    .homestayId(homestayId)
                    .cityCode(cityCode)
                    .type(type)
                    .price(price)
                    .build();
            userBehaviorEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Failed to track BOOKING event: {}", e.getMessage());
        }
    }

    @Override
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackShare(Long userId, String sessionId, Long homestayId) {
        try {
            UserBehaviorEvent event = UserBehaviorEvent.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .eventType("SHARE")
                    .homestayId(homestayId)
                    .build();
            userBehaviorEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Failed to track SHARE event: {}", e.getMessage());
        }
    }

    @Override
    public List<UserBehaviorEvent> getRecentEvents(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        return userBehaviorEventRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, since);
    }
}
