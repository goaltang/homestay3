package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单数据传输对象
 * 用于前后端交互的订单信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private String orderNumber;
    private Long homestayId;
    private String homestayTitle;
    private Long guestId;
    private String guestName;
    private String guestPhone;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer nights;
    private Integer guestCount;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private String status;
    private String remark;
    private Long hostId;
    private String hostName;
    private String paymentStatus;
    private String paymentMethod;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private boolean isReviewed;
    private ReviewDTO review;
} 