package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 价格计算请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceCalculationRequest {
    /**
     * 房源ID
     */
    private Long homestayId;

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
}
