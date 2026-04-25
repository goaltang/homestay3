package com.homestay3.homestaybackend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay3.homestaybackend.dto.*;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.OrderPriceSnapshot;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.OrderPriceSnapshotRepository;
import com.homestay3.homestaybackend.service.CouponService;
import com.homestay3.homestaybackend.service.PricingService;
import com.homestay3.homestaybackend.service.PromotionMatchService;
import com.homestay3.homestaybackend.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingServiceImpl implements PricingService {

    private final HomestayRepository homestayRepository;
    private final OrderPriceSnapshotRepository snapshotRepository;
    private final PromotionMatchService promotionMatchService;
    private final CouponService couponService;
    private final ObjectProvider<SystemConfigService> systemConfigServiceProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PricingQuoteResponse quote(PricingQuoteRequest request, Long userId) {
        PricingResult result = calculate(
                request.getHomestayId(), request.getCheckInDate(), request.getCheckOutDate(),
                request.getGuestCount(), request.getCouponIds(), userId
        );

        String quoteToken = generateQuoteToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        // 查询用户可用优惠券（用于前端展示切换）
        List<AvailableCouponDTO> availableCoupons = userId != null
                ? couponService.getAvailableCouponDTOs(userId)
                : Collections.emptyList();

        return PricingQuoteResponse.builder()
                .quoteToken(quoteToken)
                .expiresAt(expiresAt)
                .homestayId(result.getHomestayId())
                .nights(result.getNights())
                .roomOriginalAmount(result.getRoomOriginalAmount())
                .activityDiscountAmount(result.getActivityDiscountAmount())
                .fullReductionAmount(result.getFullReductionAmount())
                .couponDiscountAmount(result.getCouponDiscountAmount())
                .platformDiscountAmount(result.getPlatformDiscountAmount())
                .hostDiscountAmount(result.getHostDiscountAmount())
                .cleaningFee(result.getCleaningFee())
                .serviceFee(result.getServiceFee())
                .payableAmount(result.getPayableAmount())
                .hostReceivableAmount(result.getHostReceivableAmount())
                .dailyPrices(result.getDailyPrices())
                .appliedPromotions(result.getAppliedPromotions())
                .availableCoupons(availableCoupons)
                .priceDetails(result.getPriceDetails())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PricingResult calculate(Long homestayId, LocalDate checkInDate, LocalDate checkOutDate,
                                    Integer guestCount, List<Long> couponIds, Long userId) {
        // 1. 参数校验
        if (homestayId == null) {
            throw new IllegalArgumentException("房源ID不能为空");
        }
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException("入住日期和退房日期不能为空");
        }
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("退房日期必须晚于入住日期");
        }

        // 2. 获取房源
        Homestay homestay = homestayRepository.findById(homestayId)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在"));

        if (guestCount != null && guestCount > homestay.getMaxGuests()) {
            throw new IllegalArgumentException("入住人数不能超过房源最大容纳人数：" + homestay.getMaxGuests());
        }

        int nights = (int) ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (nights < homestay.getMinNights()) {
            throw new IllegalArgumentException("住宿天数不能少于" + homestay.getMinNights() + "晚");
        }

        // 3. 获取定价配置
        BigDecimal cleaningFeeRate = getPricingConfig("pricing.cleaning_fee", "0.1");
        BigDecimal serviceFeeRate = getPricingConfig("pricing.service_fee", "0.15");
        BigDecimal weekendRate = getPricingConfig("pricing.weekend_rate", "1.2");
        BigDecimal holidayRate = getPricingConfig("pricing.holiday_rate", "1.5");

        // 4. 计算每日价格
        BigDecimal basePricePerNight = homestay.getPrice();
        List<PriceCalculationResponse.DailyPrice> dailyPrices = new ArrayList<>();
        BigDecimal totalBasePrice = BigDecimal.ZERO;

        LocalDate currentDate = checkInDate;
        for (int i = 0; i < nights; i++) {
            LocalDate date = currentDate.plusDays(i);
            BigDecimal dailyPrice = calculateDailyPrice(date, basePricePerNight, weekendRate, holidayRate);
            String holidayName = getHolidayName(date);

            dailyPrices.add(PriceCalculationResponse.DailyPrice.builder()
                    .date(date)
                    .price(dailyPrice)
                    .isWeekend(date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY)
                    .isHoliday(holidayName != null)
                    .holidayName(holidayName)
                    .build());

            totalBasePrice = totalBasePrice.add(dailyPrice);
        }

        // 5. 房源折扣（系统配置级别的折扣）
        BigDecimal homestayDiscountRate = getHomestayDiscount(homestayId);
        BigDecimal homestayDiscountAmount = BigDecimal.ZERO;
        if (homestayDiscountRate != null && homestayDiscountRate.compareTo(BigDecimal.ONE) < 0) {
            homestayDiscountAmount = totalBasePrice.multiply(BigDecimal.ONE.subtract(homestayDiscountRate));
        }

        BigDecimal roomOriginalAmount = totalBasePrice;
        BigDecimal afterHomestayDiscount = totalBasePrice.subtract(homestayDiscountAmount);

        // 6. 匹配活动优惠
        PricingResult.PricingResultBuilder resultBuilder = PricingResult.builder()
                .homestayId(homestayId)
                .nights(nights)
                .dailyPrices(dailyPrices)
                .roomOriginalAmount(roomOriginalAmount)
                .hostDiscountAmount(homestayDiscountAmount);

        List<PromotionMatchService.PromotionMatch> promotionMatches =
                promotionMatchService.matchPromotions(homestay, checkInDate, checkOutDate, userId, roomOriginalAmount);
        promotionMatchService.applyPromotions(resultBuilder, promotionMatches);

        PricingResult tempResult = resultBuilder.build();
        BigDecimal afterActivityDiscount = afterHomestayDiscount
                .subtract(tempResult.getActivityDiscountAmount() != null ? tempResult.getActivityDiscountAmount() : BigDecimal.ZERO)
                .subtract(tempResult.getFullReductionAmount() != null ? tempResult.getFullReductionAmount() : BigDecimal.ZERO);

        // 7. 优惠券优惠（按承担方拆分）
        CouponDiscountResult couponResult = CouponDiscountResult.builder()
                .discountAmount(BigDecimal.ZERO)
                .subsidyBearer("PLATFORM")
                .build();
        if (couponIds != null && !couponIds.isEmpty()) {
            couponResult = couponService.calculateCouponDiscount(couponIds, homestayId, roomOriginalAmount, userId);
        }
        BigDecimal couponDiscount = couponResult.getDiscountAmount();
        String couponBearer = couponResult.getSubsidyBearer();

        BigDecimal couponPlatformDiscount = BigDecimal.ZERO;
        BigDecimal couponHostDiscount = BigDecimal.ZERO;
        if ("PLATFORM".equals(couponBearer) || "MIXED".equals(couponBearer)) {
            couponPlatformDiscount = couponDiscount;
        }
        if ("HOST".equals(couponBearer) || "MIXED".equals(couponBearer)) {
            couponHostDiscount = couponDiscount;
        }

        // 8. 计算清洁费和服务费
        BigDecimal cleaningFee = basePricePerNight.multiply(cleaningFeeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountedRoomAmount = afterActivityDiscount.subtract(couponDiscount).max(BigDecimal.ZERO);
        BigDecimal serviceFee = discountedRoomAmount.multiply(serviceFeeRate).setScale(2, RoundingMode.HALF_UP);

        // 9. 计算最终金额
        BigDecimal payableAmount = discountedRoomAmount.add(cleaningFee).add(serviceFee);

        // 房东应收 = 原始房费 - 活动房东承担 - 优惠券房东承担
        BigDecimal totalHostDiscount = tempResult.getHostDiscountAmount() != null ? tempResult.getHostDiscountAmount() : BigDecimal.ZERO;
        totalHostDiscount = totalHostDiscount.add(couponHostDiscount);
        BigDecimal hostReceivable = roomOriginalAmount.subtract(totalHostDiscount);

        // 平台补贴 = 活动优惠中平台承担的部分 + 优惠券平台承担部分
        BigDecimal platformSubsidy = tempResult.getPlatformDiscountAmount() != null ? tempResult.getPlatformDiscountAmount() : BigDecimal.ZERO;
        platformSubsidy = platformSubsidy.add(couponPlatformDiscount);

        return resultBuilder
                .couponDiscountAmount(couponDiscount)
                .platformDiscountAmount(platformSubsidy)
                .hostDiscountAmount(totalHostDiscount)
                .discountedRoomAmount(discountedRoomAmount)
                .cleaningFee(cleaningFee)
                .serviceFee(serviceFee)
                .payableAmount(payableAmount)
                .hostReceivableAmount(hostReceivable)
                .priceDetails(PriceCalculationResponse.PriceDetails.builder()
                        .cleaningFeeAmount(cleaningFeeRate)
                        .serviceFeeRate(serviceFeeRate)
                        .weekendRate(weekendRate)
                        .holidayRate(holidayRate)
                        .discountRate(homestayDiscountRate)
                        .build())
                .build();
    }

    @Override
    @Transactional
    public void savePriceSnapshot(Long orderId, PricingResult result, String quoteToken) {
        try {
            String dailyPriceJson = objectMapper.writeValueAsString(result.getDailyPrices());
            String pricingDetailJson = objectMapper.writeValueAsString(result);

            OrderPriceSnapshot snapshot = OrderPriceSnapshot.builder()
                    .orderId(orderId)
                    .quoteToken(quoteToken)
                    .roomOriginalAmount(result.getRoomOriginalAmount())
                    .dailyPriceJson(dailyPriceJson)
                    .activityDiscountAmount(result.getActivityDiscountAmount())
                    .fullReductionAmount(result.getFullReductionAmount())
                    .couponDiscountAmount(result.getCouponDiscountAmount())
                    .platformDiscountAmount(result.getPlatformDiscountAmount())
                    .hostDiscountAmount(result.getHostDiscountAmount())
                    .cleaningFee(result.getCleaningFee())
                    .serviceFee(result.getServiceFee())
                    .payableAmount(result.getPayableAmount())
                    .hostReceivableAmount(result.getHostReceivableAmount())
                    .pricingDetailJson(pricingDetailJson)
                    .build();

            snapshotRepository.save(snapshot);
        } catch (JsonProcessingException e) {
            log.error("保存价格快照序列化失败: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PricingResult getPriceSnapshot(Long orderId) {
        OrderPriceSnapshot snapshot = snapshotRepository.findByOrderId(orderId).orElse(null);
        if (snapshot == null) {
            return null;
        }
        try {
            List<PriceCalculationResponse.DailyPrice> dailyPrices =
                    objectMapper.readValue(snapshot.getDailyPriceJson(),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, PriceCalculationResponse.DailyPrice.class));

            return PricingResult.builder()
                    .roomOriginalAmount(snapshot.getRoomOriginalAmount())
                    .dailyPrices(dailyPrices)
                    .activityDiscountAmount(snapshot.getActivityDiscountAmount())
                    .fullReductionAmount(snapshot.getFullReductionAmount())
                    .couponDiscountAmount(snapshot.getCouponDiscountAmount())
                    .platformDiscountAmount(snapshot.getPlatformDiscountAmount())
                    .hostDiscountAmount(snapshot.getHostDiscountAmount())
                    .cleaningFee(snapshot.getCleaningFee())
                    .serviceFee(snapshot.getServiceFee())
                    .payableAmount(snapshot.getPayableAmount())
                    .hostReceivableAmount(snapshot.getHostReceivableAmount())
                    .build();
        } catch (JsonProcessingException e) {
            log.error("读取价格快照反序列化失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String generateQuoteToken() {
        return "QUOTE_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BigDecimal calculateDailyPrice(LocalDate date, BigDecimal basePrice,
                                            BigDecimal weekendRate, BigDecimal holidayRate) {
        BigDecimal rate = BigDecimal.ONE;
        String holidayName = getHolidayName(date);
        if (holidayName != null) {
            rate = holidayRate;
        } else if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            rate = weekendRate;
        }
        return basePrice.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private String getHolidayName(LocalDate date) {
        Map<LocalDate, String> holidays = new HashMap<>();
        holidays.put(LocalDate.of(2026, 1, 1), "元旦");
        for (int i = 17; i <= 23; i++) {
            holidays.put(LocalDate.of(2026, 2, i), "春节");
        }
        for (int i = 4; i <= 6; i++) {
            holidays.put(LocalDate.of(2026, 4, i), "清明节");
        }
        for (int i = 1; i <= 3; i++) {
            holidays.put(LocalDate.of(2026, 5, i), "劳动节");
        }
        holidays.put(LocalDate.of(2026, 5, 31), "端午节");
        holidays.put(LocalDate.of(2026, 6, 1), "端午节");
        holidays.put(LocalDate.of(2026, 6, 2), "端午节");
        for (int i = 1; i <= 7; i++) {
            holidays.put(LocalDate.of(2026, 10, i), "国庆节");
        }
        return holidays.get(date);
    }

    private BigDecimal getPricingConfig(String key, String defaultValue) {
        String value = systemConfigServiceProvider.getObject().getConfigValue(key, defaultValue);
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return new BigDecimal(defaultValue);
        }
    }

    private BigDecimal getHomestayDiscount(Long homestayId) {
        String discountKey = "homestay.discount." + homestayId;
        String discountValue = systemConfigServiceProvider.getObject().getConfigValue(discountKey);
        if (discountValue != null) {
            try {
                return new BigDecimal(discountValue);
            } catch (NumberFormatException e) {
                log.warn("房源折扣配置 {} 格式错误", discountKey);
            }
        }
        return null;
    }
}
