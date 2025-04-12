package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarningsSummaryDTO {
    private BigDecimal totalEarnings;
    private Long totalOrders;
    private BigDecimal averagePerOrder;
} 