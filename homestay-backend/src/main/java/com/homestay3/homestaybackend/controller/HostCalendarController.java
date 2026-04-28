package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.CalendarAvailabilityUpdateRequest;
import com.homestay3.homestaybackend.dto.CalendarPriceUpdateRequest;
import com.homestay3.homestaybackend.dto.HostCalendarResponse;
import com.homestay3.homestaybackend.dto.HostCalendarSummaryDTO;
import com.homestay3.homestaybackend.service.HostCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import java.util.Map;

@RestController
@RequestMapping("/api/host/calendar")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
public class HostCalendarController {

    private final HostCalendarService hostCalendarService;

    @GetMapping
    public ResponseEntity<HostCalendarResponse> getCalendar(
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

    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportCalendar(
            @RequestParam(required = false) Long homestayId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication
    ) {
        byte[] data = hostCalendarService.exportCalendarCsv(authentication.getName(), homestayId, startDate, endDate);
        String filename = "calendar_" + startDate + "_to_" + endDate + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }

    @PatchMapping("/availability")
    public ResponseEntity<Map<String, Object>> updateAvailability(
            @RequestBody CalendarAvailabilityUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(hostCalendarService.updateAvailability(authentication.getName(), request));
    }

    @PatchMapping("/price")
    public ResponseEntity<Map<String, Object>> updatePrice(
            @RequestBody CalendarPriceUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(hostCalendarService.updatePrice(authentication.getName(), request));
    }
}
