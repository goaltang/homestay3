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
public class HostCalendarSummaryDTO {
    private Integer availableCount;
    private Integer bookedCount;
    private Integer pendingCount;
    private Integer unavailableCount;
    private Integer checkInCount;
    private Integer checkOutCount;
    private BigDecimal estimatedRevenue;
}
