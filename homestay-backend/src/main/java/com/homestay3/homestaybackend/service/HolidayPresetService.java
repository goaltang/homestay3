package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.entity.HolidayCalendar;
import com.homestay3.homestaybackend.repository.HolidayCalendarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 节假日预设数据服务
 * 内置中国法定节假日模板（含调休），支持按年份一键生成
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HolidayPresetService {

    private final HolidayCalendarRepository holidayCalendarRepository;

    /**
     * 按年份生成中国法定节假日（含调休补班）
     * 目前内置 2024-2027 年数据，超出范围返回空列表
     */
    @Transactional
    public List<HolidayCalendar> generateForYear(int year) {
        List<HolidayCalendar> presets = getPresetsForYear(year);
        List<HolidayCalendar> saved = new ArrayList<>();

        for (HolidayCalendar preset : presets) {
            boolean exists = holidayCalendarRepository.existsByDateAndRegionCode(preset.getDate(), preset.getRegionCode());
            if (!exists) {
                saved.add(holidayCalendarRepository.save(preset));
            } else {
                log.info("节假日已存在，跳过: {} {}", preset.getDate(), preset.getName());
            }
        }

        log.info("成功导入 {} 年节假日 {} 条", year, saved.size());
        return saved;
    }

    /**
     * 批量创建节假日（用于外部导入）
     */
    @Transactional
    public List<HolidayCalendar> batchCreate(List<HolidayCalendar> holidays) {
        List<HolidayCalendar> saved = new ArrayList<>();
        for (HolidayCalendar holiday : holidays) {
            boolean exists = holidayCalendarRepository.existsByDateAndRegionCode(holiday.getDate(), holiday.getRegionCode());
            if (!exists) {
                saved.add(holidayCalendarRepository.save(holiday));
            }
        }
        return saved;
    }

    /**
     * 获取内置的节假日模板数据
     */
    private List<HolidayCalendar> getPresetsForYear(int year) {
        List<HolidayCalendar> list = new ArrayList<>();

        switch (year) {
            case 2024 -> {
                // 元旦
                list.add(build("2024-01-01", "元旦", true));
                // 春节
                list.add(build("2024-02-09", "春节除夕", true));
                list.add(build("2024-02-10", "春节", true));
                list.add(build("2024-02-11", "春节", true));
                list.add(build("2024-02-12", "春节", true));
                list.add(build("2024-02-13", "春节", true));
                list.add(build("2024-02-14", "春节", true));
                list.add(build("2024-02-15", "春节", true));
                list.add(build("2024-02-16", "春节", true));
                list.add(build("2024-02-17", "春节", true));
                list.add(build("2024-02-04", "春节调休补班", false, true));
                list.add(build("2024-02-18", "春节调休补班", false, true));
                // 清明
                list.add(build("2024-04-04", "清明节", true));
                list.add(build("2024-04-05", "清明节", true));
                list.add(build("2024-04-06", "清明节", true));
                // 劳动
                list.add(build("2024-05-01", "劳动节", true));
                list.add(build("2024-05-02", "劳动节", true));
                list.add(build("2024-05-03", "劳动节", true));
                list.add(build("2024-05-04", "劳动节", true));
                list.add(build("2024-05-05", "劳动节", true));
                list.add(build("2024-04-28", "劳动节调休补班", false, true));
                list.add(build("2024-05-11", "劳动节调休补班", false, true));
                // 端午
                list.add(build("2024-06-10", "端午节", true));
                // 中秋
                list.add(build("2024-09-15", "中秋节", true));
                list.add(build("2024-09-16", "中秋节", true));
                list.add(build("2024-09-17", "中秋节", true));
                list.add(build("2024-09-14", "中秋调休补班", false, true));
                // 国庆
                list.add(build("2024-10-01", "国庆节", true));
                list.add(build("2024-10-02", "国庆节", true));
                list.add(build("2024-10-03", "国庆节", true));
                list.add(build("2024-10-04", "国庆节", true));
                list.add(build("2024-10-05", "国庆节", true));
                list.add(build("2024-10-06", "国庆节", true));
                list.add(build("2024-10-07", "国庆节", true));
                list.add(build("2024-09-29", "国庆调休补班", false, true));
                list.add(build("2024-10-12", "国庆调休补班", false, true));
            }
            case 2025 -> {
                // 元旦
                list.add(build("2025-01-01", "元旦", true));
                // 春节
                list.add(build("2025-01-28", "春节", true));
                list.add(build("2025-01-29", "春节", true));
                list.add(build("2025-01-30", "春节", true));
                list.add(build("2025-01-31", "春节", true));
                list.add(build("2025-02-01", "春节", true));
                list.add(build("2025-02-02", "春节", true));
                list.add(build("2025-02-03", "春节", true));
                list.add(build("2025-02-04", "春节", true));
                list.add(build("2025-01-26", "春节调休补班", false, true));
                list.add(build("2025-02-08", "春节调休补班", false, true));
                // 清明
                list.add(build("2025-04-04", "清明节", true));
                list.add(build("2025-04-05", "清明节", true));
                list.add(build("2025-04-06", "清明节", true));
                // 劳动
                list.add(build("2025-05-01", "劳动节", true));
                list.add(build("2025-05-02", "劳动节", true));
                list.add(build("2025-05-03", "劳动节", true));
                list.add(build("2025-05-04", "劳动节", true));
                list.add(build("2025-05-05", "劳动节", true));
                list.add(build("2025-04-27", "劳动节调休补班", false, true));
                // 端午
                list.add(build("2025-05-31", "端午节", true));
                list.add(build("2025-06-01", "端午节", true));
                list.add(build("2025-06-02", "端午节", true));
                // 国庆+中秋
                list.add(build("2025-10-01", "国庆节", true));
                list.add(build("2025-10-02", "国庆节", true));
                list.add(build("2025-10-03", "国庆节", true));
                list.add(build("2025-10-04", "国庆节", true));
                list.add(build("2025-10-05", "国庆节", true));
                list.add(build("2025-10-06", "国庆节/中秋节", true));
                list.add(build("2025-10-07", "国庆节", true));
                list.add(build("2025-10-08", "国庆节", true));
                list.add(build("2025-09-28", "国庆调休补班", false, true));
                list.add(build("2025-10-11", "国庆调休补班", false, true));
            }
            case 2026 -> {
                // 元旦
                list.add(build("2026-01-01", "元旦", true));
                list.add(build("2026-01-02", "元旦", true));
                list.add(build("2026-01-03", "元旦", true));
                list.add(build("2025-12-29", "元旦调休补班", false, true)); // 注意跨年
                // 春节
                list.add(build("2026-02-17", "春节", true));
                list.add(build("2026-02-18", "春节", true));
                list.add(build("2026-02-19", "春节", true));
                list.add(build("2026-02-20", "春节", true));
                list.add(build("2026-02-21", "春节", true));
                list.add(build("2026-02-22", "春节", true));
                list.add(build("2026-02-23", "春节", true));
                list.add(build("2026-02-24", "春节", true));
                list.add(build("2026-02-15", "春节调休补班", false, true));
                // 清明
                list.add(build("2026-04-04", "清明节", true));
                list.add(build("2026-04-05", "清明节", true));
                list.add(build("2026-04-06", "清明节", true));
                // 劳动
                list.add(build("2026-05-01", "劳动节", true));
                list.add(build("2026-05-02", "劳动节", true));
                list.add(build("2026-05-03", "劳动节", true));
                list.add(build("2026-05-04", "劳动节", true));
                list.add(build("2026-05-05", "劳动节", true));
                // 端午
                list.add(build("2026-06-19", "端午节", true));
                list.add(build("2026-06-20", "端午节", true));
                list.add(build("2026-06-21", "端午节", true));
                // 中秋
                list.add(build("2026-09-25", "中秋节", true));
                list.add(build("2026-09-26", "中秋节", true));
                list.add(build("2026-09-27", "中秋节", true));
                // 国庆
                list.add(build("2026-10-01", "国庆节", true));
                list.add(build("2026-10-02", "国庆节", true));
                list.add(build("2026-10-03", "国庆节", true));
                list.add(build("2026-10-04", "国庆节", true));
                list.add(build("2026-10-05", "国庆节", true));
                list.add(build("2026-10-06", "国庆节", true));
                list.add(build("2026-10-07", "国庆节", true));
                list.add(build("2026-10-08", "国庆节", true));
            }
            case 2027 -> {
                // 2027 年数据为预估，实际以国务院发布为准
                // 元旦
                list.add(build("2027-01-01", "元旦", true));
                list.add(build("2027-01-02", "元旦", true));
                list.add(build("2027-01-03", "元旦", true));
                // 春节
                list.add(build("2027-02-06", "春节", true));
                list.add(build("2027-02-07", "春节", true));
                list.add(build("2027-02-08", "春节", true));
                list.add(build("2027-02-09", "春节", true));
                list.add(build("2027-02-10", "春节", true));
                list.add(build("2027-02-11", "春节", true));
                list.add(build("2027-02-12", "春节", true));
                list.add(build("2027-02-13", "春节", true));
                // 清明
                list.add(build("2027-04-03", "清明节", true));
                list.add(build("2027-04-04", "清明节", true));
                list.add(build("2027-04-05", "清明节", true));
                // 劳动
                list.add(build("2027-05-01", "劳动节", true));
                list.add(build("2027-05-02", "劳动节", true));
                list.add(build("2027-05-03", "劳动节", true));
                list.add(build("2027-05-04", "劳动节", true));
                list.add(build("2027-05-05", "劳动节", true));
                // 端午
                list.add(build("2027-06-09", "端午节", true));
                list.add(build("2027-06-10", "端午节", true));
                list.add(build("2027-06-11", "端午节", true));
                // 中秋
                list.add(build("2027-09-21", "中秋节", true));
                list.add(build("2027-09-22", "中秋节", true));
                list.add(build("2027-09-23", "中秋节", true));
                // 国庆
                list.add(build("2027-10-01", "国庆节", true));
                list.add(build("2027-10-02", "国庆节", true));
                list.add(build("2027-10-03", "国庆节", true));
                list.add(build("2027-10-04", "国庆节", true));
                list.add(build("2027-10-05", "国庆节", true));
                list.add(build("2027-10-06", "国庆节", true));
                list.add(build("2027-10-07", "国庆节", true));
            }
            default -> {
                log.warn("暂不支持 {} 年的节假日预设数据", year);
            }
        }

        return list;
    }

    private HolidayCalendar build(String dateStr, String name, boolean isHoliday) {
        return build(dateStr, name, isHoliday, false);
    }

    private HolidayCalendar build(String dateStr, String name, boolean isHoliday, boolean isMakeupWorkday) {
        return HolidayCalendar.builder()
                .date(LocalDate.parse(dateStr))
                .name(name)
                .type(isMakeupWorkday ? "MAKEUP_WORKDAY" : "PUBLIC_HOLIDAY")
                .regionCode("CN")
                .isHoliday(isHoliday)
                .isMakeupWorkday(isMakeupWorkday)
                .build();
    }
}
