package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.HomestaySummaryDTO;
import com.homestay3.homestaybackend.dto.PagedResponse;
import com.homestay3.homestaybackend.dto.UserRecommendationRequest;
import com.homestay3.homestaybackend.entity.Amenity;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.entity.Review;
import com.homestay3.homestaybackend.entity.UserFavorite;
import com.homestay3.homestaybackend.model.HomestayStatus;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.ReviewRepository;
import com.homestay3.homestaybackend.repository.UserFavoriteRepository;
import com.homestay3.homestaybackend.service.HomestayRecommendationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HomestayRecommendationServiceImpl implements HomestayRecommendationService {

    private final HomestayRepository homestayRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final UserFavoriteRepository userFavoriteRepository;
    private final HomestayDtoAssembler homestayDtoAssembler;

    // 算法权重配置
    private static final double WEIGHT_BOOKING_COUNT = 0.4; // 预订量权重
    private static final double WEIGHT_RATING = 0.3; // 评分权重
    private static final double WEIGHT_REVIEW_COUNT = 0.2; // 评论数权重
    private static final double WEIGHT_RECENCY = 0.1; // 时间衰减权重

    // 时间范围配置
    private static final int POPULAR_DAYS_RANGE = 30; // 热门统计天数
    private static final int RECENT_DAYS_RANGE = 7; // 近期活跃天数

    @Autowired
    public HomestayRecommendationServiceImpl(
            HomestayRepository homestayRepository,
            OrderRepository orderRepository,
            ReviewRepository reviewRepository,
            UserFavoriteRepository userFavoriteRepository,
            HomestayDtoAssembler homestayDtoAssembler) {
        this.homestayRepository = homestayRepository;
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
        this.userFavoriteRepository = userFavoriteRepository;
        this.homestayDtoAssembler = homestayDtoAssembler;
    }

    @Override
    @Cacheable(value = "popularHomestays", key = "#limit")
    public List<HomestaySummaryDTO> getPopularHomestays(int limit) {
        log.info("计算热门民宿推荐，数量限制: {}", limit);

        List<Homestay> allHomestays = homestayRepository.findByStatusOrderByCreatedAtDesc(HomestayStatus.ACTIVE);

        // 热门算法：优先选择有预订历史的房源
        List<PopularityScore> scores = allHomestays.stream()
                .filter(homestay -> {
                    // 热门算法过滤：必须有预订记录或高评分
                    LocalDateTime cutoff = LocalDateTime.now().minus(60, ChronoUnit.DAYS);
                    long bookingCount = orderRepository.countByHomestayIdAndCreatedAtAfter(homestay.getId(), cutoff);
                    Double rating = reviewRepository.getAverageRatingByHomestayId(homestay.getId());
                    return bookingCount > 0 || (rating != null && rating >= 4.0);
                })
                .map(this::calculatePopularityScore)
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .collect(Collectors.toList());

        // 热门算法专用多样化：确保包含不同价格层次
        List<PopularityScore> diversified = diversifyHotResults(scores, limit);

        return diversified.stream()
                .map(score -> convertToSummaryDTO(score.getHomestay()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "popularHomestaysPage", key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public PagedResponse<HomestaySummaryDTO> getPopularHomestaysPage(Pageable pageable) {
        log.info("计算热门民宿（分页），页码: {}, 页大小: {}", pageable.getPageNumber(), pageable.getPageSize());

        List<Homestay> activeHomestays = homestayRepository.findByStatusOrderByCreatedAtDesc(HomestayStatus.ACTIVE);

        // 计算所有热门民宿的分数
        List<PopularityScore> allScores = activeHomestays.stream()
                .map(this::calculatePopularityScore)
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .collect(Collectors.toList());

        // 计算分页
        int totalElements = allScores.size();
        int startIndex = pageable.getPageNumber() * pageable.getPageSize();
        int endIndex = Math.min(startIndex + pageable.getPageSize(), totalElements);

        List<HomestaySummaryDTO> pageContent = allScores.subList(startIndex, endIndex).stream()
                .map(score -> convertToSummaryDTO(score.getHomestay()))
                .collect(Collectors.toList());

        return new PagedResponse<>(pageContent, pageable.getPageNumber(), pageable.getPageSize(), totalElements);
    }

    @Override
    @Cacheable(value = "recommendedHomestays", key = "#limit")
    public List<HomestaySummaryDTO> getRecommendedHomestays(int limit) {
        log.info("计算推荐民宿，数量限制: {}", limit);

        List<Homestay> allHomestays = homestayRepository.findByStatusOrderByCreatedAtDesc(HomestayStatus.ACTIVE);
        log.info("从数据库查询到的ACTIVE状态民宿总数: {}", allHomestays.size());

        // 第一轮：使用更合理的过滤条件
        List<RecommendationScore> strictResults = allHomestays.stream()
                .filter(homestay -> {
                    // 推荐算法过滤：只排除明显有问题的房源
                    Double rating = reviewRepository.getAverageRatingByHomestayId(homestay.getId());
                    long daysSinceCreated = ChronoUnit.DAYS.between(homestay.getCreatedAt(), LocalDateTime.now());

                    // 更宽松的条件：满足任一条件即可
                    // 1. 新房源（1年内）
                    // 2. 没有差评（评分为空或>=3.0）
                    // 3. 价格合理（<1000）
                    boolean isNewHomestay = daysSinceCreated <= 365;
                    boolean hasGoodRating = rating == null || rating >= 3.0;
                    boolean hasReasonablePrice = homestay.getPrice().doubleValue() < 1000;

                    log.debug("房源过滤检查 - ID:{}, 天数:{}, 评分:{}, 价格:{}, 新房源:{}, 好评:{}, 价格合理:{}",
                            homestay.getId(), daysSinceCreated, rating, homestay.getPrice(),
                            isNewHomestay, hasGoodRating, hasReasonablePrice);

                    return isNewHomestay || hasGoodRating || hasReasonablePrice;
                })
                .map(this::calculateRecommendationScore)
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .collect(Collectors.toList());

        log.info("推荐民宿严格过滤结果: {} 条", strictResults.size());

        // 如果严格过滤结果不足，使用降级策略
        List<RecommendationScore> finalResults = strictResults;
        if (strictResults.size() < limit) {
            log.info("推荐民宿严格过滤结果不足，启用降级策略");

            // 降级策略：只排除真正有严重问题的房源
            List<RecommendationScore> relaxedResults = allHomestays.stream()
                    .filter(homestay -> {
                        Double rating = reviewRepository.getAverageRatingByHomestayId(homestay.getId());
                        long daysSinceCreated = ChronoUnit.DAYS.between(homestay.getCreatedAt(), LocalDateTime.now());
                        // 只排除：创建超过2年 && 有差评(<2.5) && 价格极高(>2000)的房源
                        boolean isVeryOld = daysSinceCreated > 730;
                        boolean hasBadRating = rating != null && rating < 2.5;
                        boolean isVeryExpensive = homestay.getPrice().doubleValue() > 2000;

                        boolean shouldExclude = isVeryOld && hasBadRating && isVeryExpensive;
                        if (shouldExclude) {
                            log.debug("排除问题房源 - ID:{}, 天数:{}, 评分:{}, 价格:{}",
                                    homestay.getId(), daysSinceCreated, rating, homestay.getPrice());
                        }
                        return !shouldExclude;
                    })
                    .map(this::calculateRecommendationScore)
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .collect(Collectors.toList());

            log.info("推荐民宿降级策略结果: {} 条", relaxedResults.size());

            // 合并结果：先取严格过滤的，再取降级的，去重
            Set<Long> usedIds = strictResults.stream()
                    .map(score -> score.getHomestay().getId())
                    .collect(Collectors.toSet());

            finalResults = new ArrayList<>(strictResults);
            for (RecommendationScore score : relaxedResults) {
                if (finalResults.size() >= limit * 2)
                    break; // 预留多一些供多样化算法选择
                if (!usedIds.contains(score.getHomestay().getId())) {
                    finalResults.add(score);
                    usedIds.add(score.getHomestay().getId());
                }
            }

            // 如果降级策略后结果仍然不足，直接使用所有ACTIVE房源
            if (finalResults.size() < limit && allHomestays.size() > 0) {
                log.warn("降级策略后结果仍不足({} < {})，使用所有ACTIVE房源作为最后兜底", finalResults.size(), limit);
                Set<Long> currentUsedIds = finalResults.stream()
                        .map(score -> score.getHomestay().getId())
                        .collect(Collectors.toSet());

                for (Homestay homestay : allHomestays) {
                    if (finalResults.size() >= limit * 2)
                        break;
                    if (!currentUsedIds.contains(homestay.getId())) {
                        finalResults.add(calculateRecommendationScore(homestay));
                        currentUsedIds.add(homestay.getId());
                    }
                }
            }
        }

        // 推荐算法专用多样化：平衡新房源和经典房源
        List<RecommendationScore> diversified = diversifyRecommendationResults(finalResults, limit);

        log.info("推荐民宿最终结果: {} 条", diversified.size());

        List<HomestaySummaryDTO> result = diversified.stream()
                .map(score -> convertToSummaryDTO(score.getHomestay()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("最终返回给前端的推荐民宿数量: {}", result.size());
        if (result.size() > 0) {
            log.info("推荐民宿详情: {}", result.stream()
                    .map(h -> h.getId() + ":" + h.getTitle())
                    .collect(Collectors.joining(", ")));
        }

        return result;
    }

    @Override
    @Cacheable(value = "recommendedHomestaysPage", key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public PagedResponse<HomestaySummaryDTO> getRecommendedHomestaysPage(Pageable pageable) {
        log.info("计算推荐民宿（分页），页码: {}, 页大小: {}", pageable.getPageNumber(), pageable.getPageSize());

        List<Homestay> activeHomestays = homestayRepository.findByStatusOrderByCreatedAtDesc(HomestayStatus.ACTIVE);

        // 计算所有推荐民宿的分数
        List<RecommendationScore> allScores = activeHomestays.stream()
                .map(this::calculateRecommendationScore)
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .collect(Collectors.toList());

        // 计算分页
        int totalElements = allScores.size();
        int startIndex = pageable.getPageNumber() * pageable.getPageSize();
        int endIndex = Math.min(startIndex + pageable.getPageSize(), totalElements);

        List<HomestaySummaryDTO> pageContent = allScores.subList(startIndex, endIndex).stream()
                .map(score -> convertToSummaryDTO(score.getHomestay()))
                .collect(Collectors.toList());

        return new PagedResponse<>(pageContent, pageable.getPageNumber(), pageable.getPageSize(), totalElements);
    }

    @Override
    @Cacheable(value = "userRecommendations", key = "#userId + '_' + #limit")
    public List<HomestaySummaryDTO> getPersonalizedRecommendations(Long userId, int limit) {
        log.info("计算个性化推荐，用户ID: {}, 数量限制: {}", userId, limit);

        UserPreference preference = analyzeUserPreference(userId);
        List<Homestay> allHomestays = homestayRepository.findByStatusOrderByCreatedAtDesc(HomestayStatus.ACTIVE);

        log.info("用户偏好分析结果 - 价格范围: {}-{}, 房型偏好: {}, 城市偏好: {}",
                preference.getPreferredMinPrice(), preference.getPreferredMaxPrice(),
                preference.getPreferredPropertyTypes(), preference.getPreferredCities());

        // 第一轮：使用用户偏好进行过滤
        List<PersonalizedScore> strictResults = allHomestays.stream()
                .filter(homestay -> {
                    // 价格过滤：更宽松的价格范围（±50%而不是±30%）
                    if (preference.getPreferredMinPrice() != null && preference.getPreferredMaxPrice() != null) {
                        double price = homestay.getPrice().doubleValue();
                        // 放宽价格过滤范围：允许±50%的浮动
                        if (price < preference.getPreferredMinPrice() * 0.5 ||
                                price > preference.getPreferredMaxPrice() * 1.5) {
                            return false;
                        }
                    }

                    // 房型过滤：如果用户偏好超过3种房型，则不进行房型过滤（说明用户接受多样性）
                    if (preference.getPreferredPropertyTypes() != null &&
                            !preference.getPreferredPropertyTypes().isEmpty() &&
                            preference.getPreferredPropertyTypes().size() <= 3) {
                        return preference.getPreferredPropertyTypes().contains(homestay.getType());
                    }

                    return true;
                })
                .map(homestay -> calculatePersonalizedScore(homestay, preference))
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .collect(Collectors.toList());

        log.info("严格过滤后的个性化推荐结果: {} 条", strictResults.size());

        // 如果严格过滤的结果不够，使用降级策略
        List<PersonalizedScore> finalResults = strictResults;
        if (strictResults.size() < limit) {
            log.info("严格过滤结果不足，启用降级策略");

            // 降级策略：只使用基础偏好，不进行严格过滤
            List<PersonalizedScore> relaxedResults = allHomestays.stream()
                    .map(homestay -> calculatePersonalizedScore(homestay, preference))
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .collect(Collectors.toList());

            log.info("降级策略后的结果: {} 条", relaxedResults.size());

            // 合并结果：先取严格过滤的，再取降级的，去重
            Set<Long> usedIds = strictResults.stream()
                    .map(score -> score.getHomestay().getId())
                    .collect(Collectors.toSet());

            finalResults = new ArrayList<>(strictResults);
            for (PersonalizedScore score : relaxedResults) {
                if (finalResults.size() >= limit)
                    break;
                if (!usedIds.contains(score.getHomestay().getId())) {
                    finalResults.add(score);
                    usedIds.add(score.getHomestay().getId());
                }
            }
        }

        // 个性化算法专用多样化：确保推荐多样性同时保持个人偏好
        List<PersonalizedScore> diversified = diversifyPersonalizedResults(finalResults, preference, limit);

        log.info("最终个性化推荐结果: {} 条", diversified.size());

        return diversified.stream()
                .map(score -> convertToSummaryDTO(score.getHomestay()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<HomestaySummaryDTO> getLocationBasedRecommendations(String provinceCode, String cityCode, int limit) {
        log.info("计算基于位置的推荐，省份: {}, 城市: {}, 限制数量: {}", provinceCode, cityCode, limit);

        List<Homestay> locationHomestays = homestayRepository.findByProvinceCodeAndCityCodeAndStatus(
                provinceCode, cityCode, HomestayStatus.ACTIVE);

        List<RecommendationScore> scores = locationHomestays.stream()
                .map(this::calculateRecommendationScore)
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(limit)
                .collect(Collectors.toList());

        return scores.stream()
                .map(score -> convertToSummaryDTO(score.getHomestay()))
                .collect(Collectors.toList());
    }

    @Override
    public List<HomestaySummaryDTO> getSimilarHomestays(Long homestayId, int limit) {
        log.info("计算相似民宿推荐，基准民宿ID: {}, 限制数量: {}", homestayId, limit);

        Optional<Homestay> baseHomestayOpt = homestayRepository.findById(homestayId);
        if (baseHomestayOpt.isEmpty()) {
            return Collections.emptyList();
        }

        Homestay baseHomestay = baseHomestayOpt.get();
        List<Homestay> activeHomestays = homestayRepository.findByStatusOrderByCreatedAtDesc(HomestayStatus.ACTIVE);

        List<SimilarityScore> scores = activeHomestays.stream()
                .filter(h -> !h.getId().equals(homestayId))
                .map(homestay -> calculateSimilarityScore(baseHomestay, homestay))
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(limit)
                .collect(Collectors.toList());

        return scores.stream()
                .map(score -> convertToSummaryDTO(score.getHomestay()))
                .collect(Collectors.toList());
    }

    @Override
    public List<HomestaySummaryDTO> getRecommendationsByRequest(UserRecommendationRequest request) {
        log.info("根据用户请求计算推荐: {}", request);

        switch (request.getRecommendationType()) {
            case POPULAR:
                return getPopularHomestays(request.getUserId() != null ? 20 : 10);
            case PERSONALIZED:
                return request.getUserId() != null ? getPersonalizedRecommendations(request.getUserId(), 20)
                        : getRecommendedHomestays(10);
            case LOCATION_BASED:
                return getLocationBasedRecommendations(
                        request.getPreferredProvinceCode(),
                        request.getPreferredCityCode(),
                        20);
            default:
                return getRecommendedHomestays(10);
        }
    }

    @Override
    @CacheEvict(value = { "popularHomestays", "popularHomestaysPage", "recommendedHomestays",
            "recommendedHomestaysPage", "userRecommendations" }, allEntries = true)
    public void refreshRecommendationCache() {
        log.info("刷新推荐缓存");
    }

    /**
     * 计算热门度评分 - 强化预订量和活跃度权重
     */
    private PopularityScore calculatePopularityScore(Homestay homestay) {
        LocalDateTime cutoffDate = LocalDateTime.now().minus(POPULAR_DAYS_RANGE, ChronoUnit.DAYS);
        LocalDateTime recentCutoff = LocalDateTime.now().minus(7, ChronoUnit.DAYS); // 最近一周

        // 近期预订数 (30天) - 热门算法主要关注预订量
        long recentBookings = orderRepository.countByHomestayIdAndCreatedAtAfter(homestay.getId(), cutoffDate);

        // 最近一周的预订数 (热度加权) - 权重更高
        long weeklyBookings = orderRepository.countByHomestayIdAndCreatedAtAfter(homestay.getId(), recentCutoff);

        // 评分和评论数
        Double avgRatingObj = reviewRepository.getAverageRatingByHomestayId(homestay.getId());
        double avgRating = avgRatingObj != null ? avgRatingObj : 3.0; // 默认评分3.0避免新房源劣势
        long reviewCount = reviewRepository.countByHomestayId(homestay.getId());

        // 热门度算法：70%关注预订活跃度，30%关注评价质量
        double hotScore = weeklyBookings * 5.0 + // 最近一周预订数 x5 (大幅提升权重)
                recentBookings * 2.0 + // 近30天预订数 x2
                avgRating * 0.3 + // 评分权重降低
                Math.sqrt(Math.min(reviewCount, 100)) * 0.2; // 评论数权重降低

        // 对于没有任何预订的房源，给较低基础分数，热门算法不偏向新房源
        if (recentBookings == 0 && weeklyBookings == 0) {
            hotScore = avgRating * 0.1 + Math.sqrt(reviewCount) * 0.05; // 显著降低无预订房源的分数
        }

        return new PopularityScore(homestay, hotScore);
    }

    /**
     * 计算推荐评分 - 强化新房源和性价比权重
     */
    private RecommendationScore calculateRecommendationScore(Homestay homestay) {
        // 综合质量评分
        double qualityScore = calculateQualityScore(homestay);

        // 活跃度评分
        double activityScore = calculateActivityScore(homestay);

        // 性价比评分
        double valueScore = calculateValueScore(homestay);

        // 新房源大幅加分（推荐算法强烈偏向新房源和潜力房源）
        long daysSinceCreated = ChronoUnit.DAYS.between(homestay.getCreatedAt(), LocalDateTime.now());
        double newHouseBonus = daysSinceCreated <= 7 ? 1.5 : // 一周内新房源大幅加分
                daysSinceCreated <= 30 ? 1.0 : // 一月内新房源中等加分
                        daysSinceCreated <= 90 ? 0.5 : 0.0; // 三月内新房源小幅加分

        // 房源特色加分（如果有特殊设施或高性价比）
        double specialBonus = 0.0;
        if (homestay.getPrice().doubleValue() < 200) { // 经济实惠房源加分
            specialBonus += 0.3;
        }
        if (homestay.getAmenities() != null && homestay.getAmenities().size() >= 8) { // 设施丰富加分
            specialBonus += 0.2;
        }

        // 推荐算法：50%质量 + 20%性价比 + 10%活跃度 + 20%新颖性奖励
        double totalScore = qualityScore * 0.5 + valueScore * 0.2 + activityScore * 0.1 +
                newHouseBonus + specialBonus;

        return new RecommendationScore(homestay, totalScore);
    }

    /**
     * 计算个性化评分 - 强化用户偏好权重
     */
    private PersonalizedScore calculatePersonalizedScore(Homestay homestay, UserPreference preference) {
        double baseScore = calculateRecommendationScore(homestay).getScore();

        // 位置偏好匹配（大幅提升权重）
        double locationMatch = calculateEnhancedLocationMatch(homestay, preference);

        // 价格偏好匹配（提升权重）
        double priceMatch = calculateEnhancedPriceMatch(homestay, preference);

        // 房型偏好匹配
        double typeMatch = calculateTypeMatch(homestay, preference);

        // 设施偏好匹配（提升权重）
        double amenityMatch = calculateAmenityMatch(homestay, preference);

        // 评分偏好匹配
        double ratingMatch = calculateRatingMatch(homestay, preference);

        // 个性化调整权重 - 大幅增强偏好匹配的影响
        double personalizedScore = baseScore * (1.0 +
                locationMatch * 0.6 + // 位置匹配权重从0.25提升到0.6
                priceMatch * 0.4 + // 价格匹配权重从0.2提升到0.4
                typeMatch * 0.3 + // 房型匹配权重从0.15提升到0.3
                amenityMatch * 0.5 + // 设施匹配权重从0.2提升到0.5
                ratingMatch * 0.2); // 评分匹配权重从0.1提升到0.2

        return new PersonalizedScore(homestay, personalizedScore);
    }

    /**
     * 计算相似度评分
     */
    private SimilarityScore calculateSimilarityScore(Homestay base, Homestay target) {
        double similarity = 0.0;

        // 位置相似度
        if (Objects.equals(base.getCityCode(), target.getCityCode())) {
            similarity += 0.3;
        } else if (Objects.equals(base.getProvinceCode(), target.getProvinceCode())) {
            similarity += 0.1;
        }

        // 房型相似度
        if (Objects.equals(base.getType(), target.getType())) {
            similarity += 0.2;
        }

        // 价格相似度
        double priceRatio = Math.min(base.getPrice().doubleValue(), target.getPrice().doubleValue()) /
                Math.max(base.getPrice().doubleValue(), target.getPrice().doubleValue());
        similarity += priceRatio * 0.2;

        // 容量相似度
        double capacityRatio = Math.min(base.getMaxGuests(), target.getMaxGuests()) /
                Math.max(base.getMaxGuests(), target.getMaxGuests());
        similarity += capacityRatio * 0.1;

        // 评分相似度
        Double ratingBaseObj = reviewRepository.getAverageRatingByHomestayId(base.getId());
        Double ratingTargetObj = reviewRepository.getAverageRatingByHomestayId(target.getId());
        double ratingBase = ratingBaseObj != null ? ratingBaseObj : 0.0;
        double ratingTarget = ratingTargetObj != null ? ratingTargetObj : 0.0;
        double ratingDiff = Math.abs(ratingBase - ratingTarget);
        similarity += Math.max(0, (5.0 - ratingDiff) / 5.0) * 0.2;

        return new SimilarityScore(target, similarity);
    }

    // 辅助方法
    private UserPreference analyzeUserPreference(Long userId) {
        // 分析用户历史订单和行为
        List<Order> userOrders = orderRepository.findByGuest_IdOrderByCreatedAtDesc(userId);

        UserPreference preference = new UserPreference();

        if (!userOrders.isEmpty()) {
            // 分析偏好价格范围
            DoubleSummaryStatistics priceStats = userOrders.stream()
                    .mapToDouble(order -> order.getHomestay().getPrice().doubleValue())
                    .summaryStatistics();
            preference.setPreferredMinPrice((int) priceStats.getMin());
            preference.setPreferredMaxPrice((int) priceStats.getMax());

            // 分析偏好位置 - 包含省份和城市权重
            Map<String, Long> cityFreq = userOrders.stream()
                    .collect(Collectors.groupingBy(
                            order -> order.getHomestay().getCityCode(),
                            Collectors.counting()));
            Map<String, Long> provinceFreq = userOrders.stream()
                    .collect(Collectors.groupingBy(
                            order -> order.getHomestay().getProvinceCode(),
                            Collectors.counting()));
            preference.setPreferredCities(new ArrayList<>(cityFreq.keySet()));
            preference.setPreferredProvinces(new ArrayList<>(provinceFreq.keySet()));

            // 分析偏好房型
            Map<String, Long> typeFreq = userOrders.stream()
                    .collect(Collectors.groupingBy(
                            order -> order.getHomestay().getType(),
                            Collectors.counting()));
            preference.setPreferredPropertyTypes(new ArrayList<>(typeFreq.keySet()));

            // 分析设施偏好
            Map<String, Long> amenityFreq = new HashMap<>();
            userOrders.forEach(order -> {
                if (order.getHomestay().getAmenities() != null) {
                    order.getHomestay().getAmenities().forEach(amenity -> {
                        amenityFreq.merge(amenity.getValue(), 1L, Long::sum);
                    });
                }
            });
            // 只保留出现频率较高的设施（至少出现在30%的订单中）
            long threshold = Math.max(1, userOrders.size() * 3 / 10);
            List<String> preferredAmenities = amenityFreq.entrySet().stream()
                    .filter(entry -> entry.getValue() >= threshold)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            preference.setPreferredAmenities(preferredAmenities);

            // 分析价格敏感度
            double priceVariance = calculatePriceVariance(userOrders);
            preference.setPriceSensitivity(priceVariance < 500 ? "HIGH" : priceVariance < 1500 ? "MEDIUM" : "LOW");

            // 分析评分偏好
            Double avgMinRating = userOrders.stream()
                    .map(order -> reviewRepository.getAverageRatingByHomestayId(order.getHomestay().getId()))
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            preference.setMinRatingPreference(avgMinRating);
        }

        // 分析收藏行为（即使没有订单也可能有收藏）
        try {
            List<UserFavorite> userFavorites = userFavoriteRepository.findByUserIdOrderByCreatedAtDesc(userId);
            if (!userFavorites.isEmpty()) {
                // 分析收藏的房源特征
                analyzeFavoritePreferences(userFavorites, preference);
            }
        } catch (Exception e) {
            log.warn("分析用户收藏偏好失败，用户ID: {}", userId, e);
        }

        // 如果没有足够的历史数据，设置默认偏好
        if (userOrders.isEmpty()) {
            setDefaultPreferences(preference);
        }

        return preference;
    }

    private double calculatePriceVariance(List<Order> userOrders) {
        if (userOrders.size() < 2)
            return 0;

        double mean = userOrders.stream()
                .mapToDouble(order -> order.getHomestay().getPrice().doubleValue())
                .average()
                .orElse(0);

        return userOrders.stream()
                .mapToDouble(order -> Math.pow(order.getHomestay().getPrice().doubleValue() - mean, 2))
                .average()
                .orElse(0);
    }

    private void analyzeFavoritePreferences(List<UserFavorite> userFavorites, UserPreference preference) {
        List<Homestay> favoriteHomestays = userFavorites.stream()
                .map(favorite -> homestayRepository.findById(favorite.getHomestayId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        if (!favoriteHomestays.isEmpty()) {
            // 分析收藏房源的价格倾向
            DoubleSummaryStatistics favoritesPriceStats = favoriteHomestays.stream()
                    .mapToDouble(homestay -> homestay.getPrice().doubleValue())
                    .summaryStatistics();

            // 如果没有订单历史，使用收藏数据设置价格偏好
            if (preference.getPreferredMinPrice() == null) {
                preference.setPreferredMinPrice((int) favoritesPriceStats.getMin());
                preference.setPreferredMaxPrice((int) favoritesPriceStats.getMax());
            }

            // 合并收藏的房型偏好
            Map<String, Long> favoriteTypeFreq = favoriteHomestays.stream()
                    .collect(Collectors.groupingBy(Homestay::getType, Collectors.counting()));

            if (preference.getPreferredPropertyTypes() == null || preference.getPreferredPropertyTypes().isEmpty()) {
                preference.setPreferredPropertyTypes(new ArrayList<>(favoriteTypeFreq.keySet()));
            } else {
                // 合并现有偏好和收藏偏好
                Set<String> combinedTypes = new HashSet<>(preference.getPreferredPropertyTypes());
                combinedTypes.addAll(favoriteTypeFreq.keySet());
                preference.setPreferredPropertyTypes(new ArrayList<>(combinedTypes));
            }

            // 分析收藏房源的设施偏好
            Map<String, Long> favoriteAmenityFreq = new HashMap<>();
            favoriteHomestays.forEach(homestay -> {
                if (homestay.getAmenities() != null) {
                    homestay.getAmenities().forEach(amenity -> {
                        favoriteAmenityFreq.merge(amenity.getValue(), 1L, Long::sum);
                    });
                }
            });

            // 至少在20%的收藏房源中出现的设施
            long favoriteThreshold = Math.max(1, favoriteHomestays.size() * 2 / 10);
            List<String> favoriteAmenities = favoriteAmenityFreq.entrySet().stream()
                    .filter(entry -> entry.getValue() >= favoriteThreshold)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (preference.getPreferredAmenities() == null || preference.getPreferredAmenities().isEmpty()) {
                preference.setPreferredAmenities(favoriteAmenities);
            } else {
                // 合并现有设施偏好和收藏设施偏好
                Set<String> combinedAmenities = new HashSet<>(preference.getPreferredAmenities());
                combinedAmenities.addAll(favoriteAmenities);
                preference.setPreferredAmenities(new ArrayList<>(combinedAmenities));
            }
        }
    }

    private void setDefaultPreferences(UserPreference preference) {
        // 为新用户设置合理的默认偏好
        preference.setPreferredMinPrice(100);
        preference.setPreferredMaxPrice(1000);
        preference.setPriceSensitivity("MEDIUM");
        preference.setMinRatingPreference(4.0);

        // 设置常见的偏好设施
        preference.setPreferredAmenities(Arrays.asList("WiFi", "空调", "热水器", "洗衣机"));
    }

    private double calculateQualityScore(Homestay homestay) {
        Double ratingObj = reviewRepository.getAverageRatingByHomestayId(homestay.getId());
        double rating = ratingObj != null ? ratingObj : 0.0;
        long reviewCount = reviewRepository.countByHomestayId(homestay.getId());

        // 质量评分 = 评分 * sqrt(评论数) / 10
        return rating * Math.sqrt(Math.min(reviewCount, 100)) / 10.0;
    }

    private double calculateActivityScore(Homestay homestay) {
        LocalDateTime cutoffDate = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        long recentActivity = orderRepository.countByHomestayIdAndCreatedAtAfter(homestay.getId(), cutoffDate);

        return Math.min(recentActivity, 20) / 20.0;
    }

    private double calculateValueScore(Homestay homestay) {
        Double ratingObj = reviewRepository.getAverageRatingByHomestayId(homestay.getId());
        double rating = ratingObj != null ? ratingObj : 0.0;
        double price = homestay.getPrice().doubleValue();

        // 根据价格区间计算性价比基准
        double priceNormalized = Math.min(price / 500.0, 2.0); // 500为基准价格
        return rating / (1.0 + priceNormalized);
    }

    private double calculateEnhancedLocationMatch(Homestay homestay, UserPreference preference) {
        double score = 0.0;

        // 城市完全匹配（权重最高）
        if (preference.getPreferredCities() != null &&
                preference.getPreferredCities().contains(homestay.getCityCode())) {
            score = 1.0;
        }
        // 省份匹配（次优）
        else if (preference.getPreferredProvinces() != null &&
                preference.getPreferredProvinces().contains(homestay.getProvinceCode())) {
            score = 0.6;
        }

        return score;
    }

    private double calculateEnhancedPriceMatch(Homestay homestay, UserPreference preference) {
        if (preference.getPreferredMinPrice() == null || preference.getPreferredMaxPrice() == null) {
            return 0.5; // 中性分数
        }

        int price = homestay.getPrice().intValue();
        int minPrice = preference.getPreferredMinPrice();
        int maxPrice = preference.getPreferredMaxPrice();

        // 在偏好范围内
        if (price >= minPrice && price <= maxPrice) {
            // 根据价格敏感度调整分数
            double score = 1.0;
            if ("HIGH".equals(preference.getPriceSensitivity())) {
                // 价格敏感用户偏好价格区间中的较低价格
                double priceRatio = (double) (price - minPrice) / (maxPrice - minPrice);
                score = 1.2 - 0.4 * priceRatio; // 价格越低分数越高
            }
            return Math.min(score, 1.0);
        }

        // 超出范围的渐变处理
        double deviation;
        if (price < minPrice) {
            deviation = (double) (minPrice - price) / minPrice;
        } else {
            deviation = (double) (price - maxPrice) / maxPrice;
        }

        // 根据价格敏感度调整容忍度
        double tolerance = "HIGH".equals(preference.getPriceSensitivity()) ? 0.2
                : "MEDIUM".equals(preference.getPriceSensitivity()) ? 0.4 : 0.6;

        return Math.max(0, 1.0 - deviation / tolerance);
    }

    private double calculateTypeMatch(Homestay homestay, UserPreference preference) {
        if (preference.getPreferredPropertyTypes() != null &&
                preference.getPreferredPropertyTypes().contains(homestay.getType())) {
            return 1.0;
        }
        return 0.0;
    }

    private double calculateAmenityMatch(Homestay homestay, UserPreference preference) {
        if (preference.getPreferredAmenities() == null || preference.getPreferredAmenities().isEmpty()) {
            return 0.5; // 中性分数
        }

        if (homestay.getAmenities() == null || homestay.getAmenities().isEmpty()) {
            return 0.0;
        }

        Set<String> homestayAmenities = homestay.getAmenities().stream()
                .map(Amenity::getValue)
                .collect(Collectors.toSet());
        Set<String> preferredAmenities = new HashSet<>(preference.getPreferredAmenities());

        // 计算匹配的设施数量
        long matchCount = preferredAmenities.stream()
                .mapToLong(amenity -> homestayAmenities.contains(amenity) ? 1 : 0)
                .sum();

        return (double) matchCount / preferredAmenities.size();
    }

    private double calculateRatingMatch(Homestay homestay, UserPreference preference) {
        Double rating = reviewRepository.getAverageRatingByHomestayId(homestay.getId());
        if (rating == null) {
            return 0.3; // 无评分房源给较低分数
        }

        double minRating = preference.getMinRatingPreference();
        if (rating >= minRating) {
            // 超过最低要求的评分，给予额外加分
            return 1.0 + Math.min(0.5, (rating - minRating) / 2.0);
        } else {
            // 低于要求的评分，按比例减分
            return Math.max(0, rating / minRating);
        }
    }

    // 保留原有的简单匹配方法作为兼容
    private double calculateLocationMatch(Homestay homestay, UserPreference preference) {
        return calculateEnhancedLocationMatch(homestay, preference);
    }

    private double calculatePriceMatch(Homestay homestay, UserPreference preference) {
        return calculateEnhancedPriceMatch(homestay, preference);
    }

    private HomestaySummaryDTO convertToSummaryDTO(Homestay homestay) {
        try {
            HomestaySummaryDTO dto = homestayDtoAssembler.toSummaryDTO(homestay, Collections.emptyList());
            if (dto != null) {
                return dto;
            }
        } catch (Exception e) {
            log.error("转换民宿DTO失败，民宿ID: {}", homestay.getId(), e);
            // 创建简化的DTO作为降级方案
            HomestaySummaryDTO dto = new HomestaySummaryDTO();
            dto.setId(homestay.getId());
            dto.setTitle(homestay.getTitle());
            dto.setPrice(homestay.getPrice().toString());
            dto.setMaxGuests(homestay.getMaxGuests());
            dto.setStatus(homestay.getStatus().name());
            dto.setType(homestay.getType());
            dto.setFeatured(homestay.getFeatured());
            dto.setCoverImage(homestay.getCoverImage());
            dto.setImages(homestay.getImages() != null ? homestay.getImages() : Collections.emptyList());
            // dto.setAmenities(homestay.getAmenities() != null ? homestay.getAmenities() :
            // Collections.emptyList()); // 这需要转换为AmenityDTO，先跳过
            dto.setAddressDetail(homestay.getAddressDetail());
            return dto;
        }

        HomestaySummaryDTO fallback = new HomestaySummaryDTO();
        fallback.setId(homestay.getId());
        fallback.setTitle(homestay.getTitle());
        fallback.setPrice(homestay.getPrice() != null ? homestay.getPrice().toString() : null);
        fallback.setMaxGuests(homestay.getMaxGuests());
        fallback.setStatus(homestay.getStatus() != null ? homestay.getStatus().name() : null);
        fallback.setType(homestay.getType());
        fallback.setFeatured(homestay.getFeatured());
        fallback.setCoverImage(homestay.getCoverImage());
        fallback.setImages(homestay.getImages() != null ? new ArrayList<>(homestay.getImages()) : new ArrayList<>());
        fallback.setAddressDetail(homestay.getAddressDetail());
        return fallback;
    }

    /**
     * 热门民宿专用多样化 - 确保包含不同价格层次和活跃度
     */
    private List<PopularityScore> diversifyHotResults(List<PopularityScore> scores, int limit) {
        List<PopularityScore> result = new ArrayList<>();
        Set<String> seenPriceRanges = new HashSet<>();

        // 第一轮：确保每个价格区间都有代表
        for (PopularityScore score : scores) {
            if (result.size() >= limit)
                break;

            String priceRange = getPriceRange(score.getHomestay().getPrice().intValue());
            if (!seenPriceRanges.contains(priceRange)) {
                result.add(score);
                seenPriceRanges.add(priceRange);
            }
        }

        // 第二轮：按热门度填充剩余位置，优先高活跃度房源
        for (PopularityScore score : scores) {
            if (result.size() >= limit)
                break;
            if (!result.contains(score)) {
                result.add(score);
            }
        }

        return result;
    }

    /**
     * 推荐民宿专用多样化 - 平衡新房源和经典房源
     */
    private List<RecommendationScore> diversifyRecommendationResults(List<RecommendationScore> scores, int limit) {
        List<RecommendationScore> result = new ArrayList<>();
        List<RecommendationScore> newHomestays = new ArrayList<>();
        List<RecommendationScore> establishedHomestays = new ArrayList<>();

        // 按房源新旧程度分类
        for (RecommendationScore score : scores) {
            long daysSinceCreated = ChronoUnit.DAYS.between(
                    score.getHomestay().getCreatedAt(),
                    LocalDateTime.now());

            if (daysSinceCreated <= 60) {
                newHomestays.add(score);
            } else {
                establishedHomestays.add(score);
            }
        }

        // 推荐算法特色：理想情况40%新房源，60%成熟房源，但优先保证数量
        int idealNewCount = (int) (limit * 0.4);
        int idealEstablishedCount = limit - idealNewCount;

        // 实际分配：如果某一类不足，用另一类补充
        int actualNewCount = Math.min(newHomestays.size(), idealNewCount);
        int actualEstablishedCount = Math.min(establishedHomestays.size(), limit - actualNewCount);

        // 如果成熟房源不足，用新房源补充
        if (actualEstablishedCount < idealEstablishedCount && newHomestays.size() > actualNewCount) {
            int remainingFromNew = Math.min(newHomestays.size() - actualNewCount,
                    idealEstablishedCount - actualEstablishedCount);
            actualNewCount += remainingFromNew;
        }

        // 如果新房源不足，用成熟房源补充
        if (actualNewCount < idealNewCount && establishedHomestays.size() > actualEstablishedCount) {
            int remainingFromEstablished = Math.min(establishedHomestays.size() - actualEstablishedCount,
                    idealNewCount - actualNewCount);
            actualEstablishedCount += remainingFromEstablished;
        }

        // 添加到结果中
        result.addAll(newHomestays.stream().limit(actualNewCount).collect(Collectors.toList()));
        result.addAll(establishedHomestays.stream().limit(actualEstablishedCount).collect(Collectors.toList()));

        log.info("推荐民宿多样化中间结果：新房源{}个，成熟房源{}个，当前总数{}个",
                actualNewCount, actualEstablishedCount, result.size());

        // 如果还是不够，按评分顺序填充剩余
        if (result.size() < limit) {
            Set<Long> usedIds = result.stream()
                    .map(score -> score.getHomestay().getId())
                    .collect(Collectors.toSet());

            int addedExtra = 0;
            for (RecommendationScore score : scores) {
                if (result.size() >= limit)
                    break;
                if (!usedIds.contains(score.getHomestay().getId())) {
                    result.add(score);
                    addedExtra++;
                }
            }
            log.info("推荐民宿多样化补充了{}个房源", addedExtra);
        }

        log.info("推荐民宿多样化最终结果：总共{}个房源", result.size());
        return result;
    }

    /**
     * 个性化推荐专用多样化 - 在保持偏好的同时确保多样性
     */
    private List<PersonalizedScore> diversifyPersonalizedResults(
            List<PersonalizedScore> scores,
            UserPreference preference,
            int limit) {

        List<PersonalizedScore> result = new ArrayList<>();
        Set<String> seenTypes = new HashSet<>();
        Set<String> seenCities = new HashSet<>();

        // 第一轮：确保类型多样性（如果用户没有强烈的类型偏好）
        if (preference.getPreferredPropertyTypes() == null ||
                preference.getPreferredPropertyTypes().size() > 2) {

            for (PersonalizedScore score : scores) {
                if (result.size() >= limit / 2)
                    break;

                String type = score.getHomestay().getType();
                if (!seenTypes.contains(type)) {
                    result.add(score);
                    seenTypes.add(type);
                }
            }
        }

        // 第二轮：确保地域多样性（如果用户没有强烈的地域偏好）
        if (preference.getPreferredCities() == null ||
                preference.getPreferredCities().size() > 2) {

            for (PersonalizedScore score : scores) {
                if (result.size() >= limit * 3 / 4)
                    break;
                if (result.contains(score))
                    continue;

                String city = score.getHomestay().getCityCode();
                if (!seenCities.contains(city)) {
                    result.add(score);
                    seenCities.add(city);
                }
            }
        }

        // 第三轮：按个性化评分填充剩余位置
        for (PersonalizedScore score : scores) {
            if (result.size() >= limit)
                break;
            if (!result.contains(score)) {
                result.add(score);
            }
        }

        return result;
    }

    /**
     * 根据价格确定价格区间
     */
    private String getPriceRange(int price) {
        if (price < 100)
            return "budget";
        else if (price < 300)
            return "mid";
        else if (price < 500)
            return "high";
        else
            return "luxury";
    }

    // 评分数据类
    private static class PopularityScore {
        private final Homestay homestay;
        private final double score;

        public PopularityScore(Homestay homestay, double score) {
            this.homestay = homestay;
            this.score = score;
        }

        public Homestay getHomestay() {
            return homestay;
        }

        public double getScore() {
            return score;
        }
    }

    private static class RecommendationScore {
        private final Homestay homestay;
        private final double score;

        public RecommendationScore(Homestay homestay, double score) {
            this.homestay = homestay;
            this.score = score;
        }

        public Homestay getHomestay() {
            return homestay;
        }

        public double getScore() {
            return score;
        }
    }

    private static class PersonalizedScore {
        private final Homestay homestay;
        private final double score;

        public PersonalizedScore(Homestay homestay, double score) {
            this.homestay = homestay;
            this.score = score;
        }

        public Homestay getHomestay() {
            return homestay;
        }

        public double getScore() {
            return score;
        }
    }

    private static class SimilarityScore {
        private final Homestay homestay;
        private final double score;

        public SimilarityScore(Homestay homestay, double score) {
            this.homestay = homestay;
            this.score = score;
        }

        public Homestay getHomestay() {
            return homestay;
        }

        public double getScore() {
            return score;
        }
    }

    // 用户偏好数据类
    private static class UserPreference {
        private Integer preferredMinPrice;
        private Integer preferredMaxPrice;
        private List<String> preferredCities;
        private List<String> preferredPropertyTypes;
        private List<String> preferredProvinces;
        private List<String> preferredAmenities;
        private String priceSensitivity;
        private double minRatingPreference;

        // getters and setters
        public Integer getPreferredMinPrice() {
            return preferredMinPrice;
        }

        public void setPreferredMinPrice(Integer preferredMinPrice) {
            this.preferredMinPrice = preferredMinPrice;
        }

        public Integer getPreferredMaxPrice() {
            return preferredMaxPrice;
        }

        public void setPreferredMaxPrice(Integer preferredMaxPrice) {
            this.preferredMaxPrice = preferredMaxPrice;
        }

        public List<String> getPreferredCities() {
            return preferredCities;
        }

        public void setPreferredCities(List<String> preferredCities) {
            this.preferredCities = preferredCities;
        }

        public List<String> getPreferredPropertyTypes() {
            return preferredPropertyTypes;
        }

        public void setPreferredPropertyTypes(List<String> preferredPropertyTypes) {
            this.preferredPropertyTypes = preferredPropertyTypes;
        }

        public List<String> getPreferredProvinces() {
            return preferredProvinces;
        }

        public void setPreferredProvinces(List<String> preferredProvinces) {
            this.preferredProvinces = preferredProvinces;
        }

        public List<String> getPreferredAmenities() {
            return preferredAmenities;
        }

        public void setPreferredAmenities(List<String> preferredAmenities) {
            this.preferredAmenities = preferredAmenities;
        }

        public String getPriceSensitivity() {
            return priceSensitivity;
        }

        public void setPriceSensitivity(String priceSensitivity) {
            this.priceSensitivity = priceSensitivity;
        }

        public double getMinRatingPreference() {
            return minRatingPreference;
        }

        public void setMinRatingPreference(double minRatingPreference) {
            this.minRatingPreference = minRatingPreference;
        }
    }
}
