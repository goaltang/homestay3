package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarPriceUpdateRequest {
    private Long homestayId;
    private LocalDate startDate;
    private LocalDate endDate;
    // null 表示清除自定义价格
    private BigDecimal customPrice;
}
