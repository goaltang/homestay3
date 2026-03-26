package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 价格计算响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceCalculationResponse {
    /**
     * 房源ID
     */
    private Long homestayId;

    /**
     * 房源标题
     */
    private String homestayTitle;

    /**
     * 入住日期
     */
    private LocalDate checkInDate;

    /**
     * 退房日期
     */
    private LocalDate checkOutDate;

    /**
     * 入住人数
     */
    private Integer guestCount;

    /**
     * 住宿晚数
     */
    private Integer nights;

    /**
     * 每日价格列表（用于显示每天的价格差异）
     */
    private java.util.List<DailyPrice> dailyPrices;

    /**
     * 基础房费（未打折前的总价）
     */
    private BigDecimal basePrice;

    /**
     * 折扣金额
     */
    private BigDecimal discountAmount;

    /**
     * 折后基础房费
     */
    private BigDecimal finalBasePrice;

    /**
     * 清洁费
     */
    private BigDecimal cleaningFee;

    /**
     * 服务费
     */
    private BigDecimal serviceFee;

    /**
     * 总计金额
     */
    private BigDecimal totalPrice;

    /**
     * 价格详情
     */
    private PriceDetails priceDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyPrice {
        private LocalDate date;
        private BigDecimal price;
        private boolean isWeekend;
        private boolean isHoliday;
        private String holidayName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceDetails {
        /**
         * 清洁费金额（固定金额，与入住晚数无关）
         */
        private BigDecimal cleaningFeeAmount;

        /**
         * 服务费费率
         */
        private BigDecimal serviceFeeRate;

        /**
         * 周末价格系数
         */
        private BigDecimal weekendRate;

        /**
         * 节假日价格系数
         */
        private BigDecimal holidayRate;

        /**
         * 房源折扣系数（null表示无折扣）
         */
        private BigDecimal discountRate;
    }
}
