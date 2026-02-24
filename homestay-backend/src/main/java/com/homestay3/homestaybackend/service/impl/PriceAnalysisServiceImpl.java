package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.PriceCompetitivenessDTO;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.model.HomestayStatus;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.service.PriceAnalysisService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 价格分析服务实现类
 */
@Service
@RequiredArgsConstructor
public class PriceAnalysisServiceImpl implements PriceAnalysisService {
    
    private static final Logger log = LoggerFactory.getLogger(PriceAnalysisServiceImpl.class);
    private static final HomestayStatus COMPARABLE_HOMESTAY_STATUS = HomestayStatus.ACTIVE;
    private static final int MIN_COMPARABLE_HOMESTAYS = 2; // 降低从3降到2，增加触发概率
    
    private final HomestayRepository homestayRepository;
    
    // 季节性调整因子 - 可以后续从配置文件或数据库读取
    private static final Map<String, Map<Month, Double>> SEASONAL_FACTORS = new HashMap<>();
    
    static {
        // 示例：广州的季节性因子
        Map<Month, Double> guangzhouFactors = new HashMap<>();
        guangzhouFactors.put(Month.JANUARY, 1.1);    // 春节旺季
        guangzhouFactors.put(Month.FEBRUARY, 1.15);  // 春节旺季
        guangzhouFactors.put(Month.MARCH, 1.0);      // 平季
        guangzhouFactors.put(Month.APRIL, 1.05);     // 小旺季
        guangzhouFactors.put(Month.MAY, 1.1);        // 五一假期
        guangzhouFactors.put(Month.JUNE, 0.95);      // 雨季淡季
        guangzhouFactors.put(Month.JULY, 1.2);       // 暑假旺季
        guangzhouFactors.put(Month.AUGUST, 1.2);     // 暑假旺季
        guangzhouFactors.put(Month.SEPTEMBER, 1.0);  // 平季
        guangzhouFactors.put(Month.OCTOBER, 1.15);   // 国庆旺季
        guangzhouFactors.put(Month.NOVEMBER, 1.0);   // 平季
        guangzhouFactors.put(Month.DECEMBER, 1.05);  // 年底小旺季
        
        SEASONAL_FACTORS.put("440100", guangzhouFactors); // 广州市代码
        // 可以添加更多城市的季节性因子
    }
    
    @Override
    public PriceCompetitivenessDTO analyzePriceCompetitiveness(Homestay homestay) {
        log.info("开始分析房源价格竞争力，房源ID: {}", homestay.getId());
        
        if (!isValidForPriceAnalysis(homestay)) {
            log.warn("房源不满足价格分析条件，房源ID: {}", homestay.getId());
            return null;
        }
        
        try {
            return performDetailedPriceAnalysis(homestay);
        } catch (Exception e) {
            log.error("价格竞争力分析失败，房源ID: {}, 错误: {}", homestay.getId(), e.getMessage(), e);
            return null;
        }
    }
    
    private boolean isValidForPriceAnalysis(Homestay homestay) {
        return homestay.getId() != null &&
               homestay.getPrice() != null &&
               homestay.getPrice().compareTo(BigDecimal.ZERO) > 0 &&
               homestay.getCityCode() != null &&
               homestay.getType() != null &&
               homestay.getMaxGuests() != null;
    }
    
    private PriceCompetitivenessDTO performDetailedPriceAnalysis(Homestay homestay) {
        // 1. 获取多维度的可比房源价格数据
        List<BigDecimal> comparablePrices = getComparablePrices(homestay);
        
        if (comparablePrices.size() < MIN_COMPARABLE_HOMESTAYS) {
            log.warn("可比房源数量不足({})，无法进行有效分析", comparablePrices.size());
            return createLimitedAnalysisResult(homestay);
        }
        
        // 2. 计算统计指标
        PriceStatistics stats = calculatePriceStatistics(comparablePrices);
        
        // 3. 计算竞争力指标
        BigDecimal currentPrice = homestay.getPrice();
        double percentileBelowCurrent = calculatePercentileBelow(currentPrice, comparablePrices);
        double avgDifference = calculateDifferenceFromAverage(currentPrice, stats.average);
        double medianDifference = calculateDifferenceFromMedian(currentPrice, stats.median);
        
        // 4. 确定竞争力等级
        PriceCompetitivenessDTO.PriceCompetitivenessLevel level = 
            determineCompetitivenessLevel(avgDifference);
        
        // 5. 季节性调整
        boolean isSeasonalPeak = isSeasonalPeak(homestay.getCityCode());
        BigDecimal seasonalAdjustedAverage = applySeasonalAdjustment(stats.average, homestay.getCityCode());
        
        // 6. 确定比较范围描述
        String comparisonScope = determineComparisonScope(homestay);
        
        return PriceCompetitivenessDTO.builder()
                .currentPrice(currentPrice)
                .comparisonScope(comparisonScope)
                .comparableHomestaysCount(comparablePrices.size())
                .averagePrice(stats.average)
                .medianPrice(stats.median)
                .minPrice(stats.min)
                .maxPrice(stats.max)
                .percentileBelowCurrentPrice(percentileBelowCurrent)
                .priceDifferenceFromAverage(avgDifference)
                .priceDifferenceFromMedian(medianDifference)
                .competitivenessLevel(level)
                .isSeasonalPeak(isSeasonalPeak)
                .seasonalAdjustedAverage(seasonalAdjustedAverage)
                .build();
    }
    
    private List<BigDecimal> getComparablePrices(Homestay homestay) {
        // 容纳人数范围：扩大到±2人，增加可比房源数量
        int guestRange = 2;
        int minGuests = Math.max(1, homestay.getMaxGuests() - guestRange);
        int maxGuests = homestay.getMaxGuests() + guestRange;
        
        // 首先尝试同区域对比
        List<BigDecimal> prices = homestayRepository.findPricesForDetailedComparison(
                homestay.getCityCode(),
                homestay.getDistrictCode(),
                homestay.getType(),
                minGuests,
                maxGuests,
                homestay.getId(),
                COMPARABLE_HOMESTAY_STATUS
        );
        
        // 如果同区域房源太少，扩大到同城市
        if (prices.size() < MIN_COMPARABLE_HOMESTAYS) {
            prices = homestayRepository.findPricesForDetailedComparison(
                    homestay.getCityCode(),
                    null, // 不限制区域
                    homestay.getType(),
                    minGuests,
                    maxGuests,
                    homestay.getId(),
                    COMPARABLE_HOMESTAY_STATUS
            );
        }
        
        // 如果还是不够，进一步放松房源类型限制
        if (prices.size() < MIN_COMPARABLE_HOMESTAYS) {
            prices = homestayRepository.findPricesForDetailedComparison(
                    homestay.getCityCode(),
                    null, // 不限制区域
                    null, // 不限制房源类型
                    minGuests,
                    maxGuests,
                    homestay.getId(),
                    COMPARABLE_HOMESTAY_STATUS
            );
        }
        
        return prices;
    }
    
    private PriceStatistics calculatePriceStatistics(List<BigDecimal> prices) {
        Collections.sort(prices);
        
        BigDecimal sum = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP);
        
        BigDecimal median;
        int size = prices.size();
        if (size % 2 == 0) {
            median = prices.get(size / 2 - 1).add(prices.get(size / 2))
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        } else {
            median = prices.get(size / 2);
        }
        
        return new PriceStatistics(
                average,
                median,
                prices.get(0), // min
                prices.get(prices.size() - 1) // max
        );
    }
    
    private double calculatePercentileBelow(BigDecimal currentPrice, List<BigDecimal> prices) {
        long belowCount = prices.stream()
                .mapToLong(price -> price.compareTo(currentPrice) < 0 ? 1 : 0)
                .sum();
        return (double) belowCount / prices.size() * 100;
    }
    
    private double calculateDifferenceFromAverage(BigDecimal currentPrice, BigDecimal average) {
        if (average.compareTo(BigDecimal.ZERO) == 0) return 0;
        
        return currentPrice.subtract(average)
                .divide(average, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }
    
    private double calculateDifferenceFromMedian(BigDecimal currentPrice, BigDecimal median) {
        if (median.compareTo(BigDecimal.ZERO) == 0) return 0;
        
        return currentPrice.subtract(median)
                .divide(median, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }
    
    private PriceCompetitivenessDTO.PriceCompetitivenessLevel determineCompetitivenessLevel(double avgDifference) {
        if (avgDifference <= -25) {
            return PriceCompetitivenessDTO.PriceCompetitivenessLevel.EXTREMELY_COMPETITIVE;
        } else if (avgDifference <= -12) {
            return PriceCompetitivenessDTO.PriceCompetitivenessLevel.HIGHLY_COMPETITIVE;
        } else if (avgDifference <= -3) {
            return PriceCompetitivenessDTO.PriceCompetitivenessLevel.COMPETITIVE;
        } else if (avgDifference <= 8) {
            return PriceCompetitivenessDTO.PriceCompetitivenessLevel.MARKET_RATE;
        } else if (avgDifference <= 20) {
            return PriceCompetitivenessDTO.PriceCompetitivenessLevel.ABOVE_MARKET;
        } else {
            return PriceCompetitivenessDTO.PriceCompetitivenessLevel.PREMIUM;
        }
    }
    
    private String determineComparisonScope(Homestay homestay) {
        if (homestay.getDistrictCode() != null) {
            return "同区域同类型";
        } else {
            return "同城市同类型";
        }
    }
    
    private PriceCompetitivenessDTO createLimitedAnalysisResult(Homestay homestay) {
        return PriceCompetitivenessDTO.builder()
                .currentPrice(homestay.getPrice())
                .comparisonScope("数据不足")
                .comparableHomestaysCount(0)
                .competitivenessLevel(PriceCompetitivenessDTO.PriceCompetitivenessLevel.MARKET_RATE)
                .isSeasonalPeak(isSeasonalPeak(homestay.getCityCode()))
                .build();
    }
    
    @Override
    public boolean isSeasonalPeak(String cityCode) {
        double currentFactor = getSeasonalAdjustmentFactor(cityCode, LocalDate.now().getMonthValue());
        return currentFactor > 1.1; // 超过10%的调整因子视为旺季
    }
    
    @Override
    public double getSeasonalAdjustmentFactor(String cityCode, int month) {
        Month monthEnum = Month.of(month);
        return SEASONAL_FACTORS.getOrDefault(cityCode, new HashMap<>())
                .getOrDefault(monthEnum, 1.0);
    }
    
    private BigDecimal applySeasonalAdjustment(BigDecimal basePrice, String cityCode) {
        if (basePrice == null) return null;
        
        double factor = getSeasonalAdjustmentFactor(cityCode, LocalDate.now().getMonthValue());
        return basePrice.multiply(BigDecimal.valueOf(factor))
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    // 内部类用于统计数据
    private static class PriceStatistics {
        final BigDecimal average;
        final BigDecimal median;
        final BigDecimal min;
        final BigDecimal max;
        
        PriceStatistics(BigDecimal average, BigDecimal median, BigDecimal min, BigDecimal max) {
            this.average = average;
            this.median = median;
            this.min = min;
            this.max = max;
        }
    }
} 