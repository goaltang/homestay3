package com.homestay3.homestaybackend.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CalendarAvailabilityUpdateRequest {
    private Long homestayId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String reason;
    private String note;
}
