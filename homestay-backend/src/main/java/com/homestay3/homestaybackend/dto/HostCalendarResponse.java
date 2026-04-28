package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostCalendarResponse {
    private List<HostCalendarDayDTO> days;
    private HostCalendarSummaryDTO summary;
}
