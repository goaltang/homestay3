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
public class HostCalendarDayDTO {
    private LocalDate date;
    private Long homestayId;
    private String homestayTitle;
    private String status;
    private String source;
    private String reason;
    private String note;
    private Long orderId;
    private String orderNumber;
    private String guestName;
    private Boolean checkIn;
    private Boolean checkOut;
    private BigDecimal basePrice;
    private BigDecimal finalPrice;
}
