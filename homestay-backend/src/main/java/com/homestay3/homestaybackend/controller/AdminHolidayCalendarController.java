package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.entity.HolidayCalendar;
import com.homestay3.homestaybackend.repository.HolidayCalendarRepository;
import com.homestay3.homestaybackend.service.HolidayPresetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/holidays")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHolidayCalendarController {

    private final HolidayCalendarRepository holidayCalendarRepository;
    private final HolidayPresetService holidayPresetService;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, defaultValue = "CN") String regionCode) {
        List<HolidayCalendar> list;
        if (year != null) {
            LocalDate start = LocalDate.of(year, 1, 1);
            LocalDate end = LocalDate.of(year, 12, 31);
            list = holidayCalendarRepository.findByDateBetweenAndRegionCode(start, end, regionCode);
        } else {
            list = holidayCalendarRepository.findAll();
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody HolidayCalendar holiday) {
        if (holidayCalendarRepository.existsByDateAndRegionCode(holiday.getDate(), holiday.getRegionCode())) {
            return ResponseEntity.badRequest().body("该日期已存在");
        }
        HolidayCalendar saved = holidayCalendarRepository.save(holiday);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody HolidayCalendar holiday) {
        return holidayCalendarRepository.findById(id)
                .map(existing -> {
                    existing.setName(holiday.getName());
                    existing.setType(holiday.getType());
                    existing.setIsHoliday(holiday.getIsHoliday());
                    existing.setIsMakeupWorkday(holiday.getIsMakeupWorkday());
                    HolidayCalendar saved = holidayCalendarRepository.save(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        holidayCalendarRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 一键生成指定年份的节假日（含调休补班）
     */
    @PostMapping("/generate/{year}")
    public ResponseEntity<?> generate(@PathVariable int year) {
        List<HolidayCalendar> saved = holidayPresetService.generateForYear(year);
        return ResponseEntity.ok(Map.of(
                "year", year,
                "imported", saved.size(),
                "message", "成功导入 " + saved.size() + " 条节假日数据"
        ));
    }

    /**
     * 批量导入节假日
     */
    @PostMapping("/batch")
    public ResponseEntity<?> batchCreate(@RequestBody List<HolidayCalendar> holidays) {
        if (holidays == null || holidays.isEmpty()) {
            return ResponseEntity.badRequest().body("数据不能为空");
        }
        List<HolidayCalendar> saved = holidayPresetService.batchCreate(holidays);
        return ResponseEntity.ok(Map.of(
                "total", holidays.size(),
                "imported", saved.size(),
                "skipped", holidays.size() - saved.size(),
                "message", "成功导入 " + saved.size() + " 条，跳过 " + (holidays.size() - saved.size()) + " 条已存在"
        ));
    }
}
