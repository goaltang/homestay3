package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyEarningDTO {
    private int year;
    private int month;
    private BigDecimal amount;
    
    // 获取日期字符串 (如: "2023-05")
    public String getMonthString() {
        return String.format("%d-%02d", year, month);
    }
} 