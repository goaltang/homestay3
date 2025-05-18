package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarningDTO {
    private Long id;
    private Long hostId;
    private String orderNumber;
    private Long homestayId;
    private String homestayTitle;
    private Long orderId;
    private String guestName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer nights;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
} 