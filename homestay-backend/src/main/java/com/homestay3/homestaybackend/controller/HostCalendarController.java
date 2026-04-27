package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.CalendarAvailabilityUpdateRequest;
import com.homestay3.homestaybackend.dto.HostCalendarDayDTO;
import com.homestay3.homestaybackend.dto.HostCalendarSummaryDTO;
import com.homestay3.homestaybackend.service.HostCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/host/calendar")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
public class HostCalendarController {

    private final HostCalendarService hostCalendarService;

    @GetMapping
    public ResponseEntity<List<HostCalendarDayDTO>> getCalendar(
            @RequestParam(required = false) Long homestayId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication
    ) {
        return ResponseEntity.ok(hostCalendarService.getCalendarDays(
                authentication.getName(),
                homestayId,
                startDate,
                endDate
        ));
    }

    @GetMapping("/summary")
    public ResponseEntity<HostCalendarSummaryDTO> getSummary(
            @RequestParam(required = false) Long homestayId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication
    ) {
        return ResponseEntity.ok(hostCalendarService.getSummary(
                authentication.getName(),
                homestayId,
                startDate,
                endDate
        ));
    }

    @PatchMapping("/availability")
    public ResponseEntity<Map<String, Object>> updateAvailability(
            @RequestBody CalendarAvailabilityUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(hostCalendarService.updateAvailability(authentication.getName(), request));
    }
}
