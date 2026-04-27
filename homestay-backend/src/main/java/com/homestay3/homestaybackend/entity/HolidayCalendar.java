package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "holiday_calendar",
        indexes = {
                @Index(name = "idx_date", columnList = "date"),
                @Index(name = "idx_region_date", columnList = "region_code, date")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String type;

    @Column(name = "region_code", nullable = false, length = 20)
    @Builder.Default
    private String regionCode = "CN";

    @Column(name = "is_holiday", nullable = false)
    @Builder.Default
    private Boolean isHoliday = true;

    @Column(name = "is_makeup_workday", nullable = false)
    @Builder.Default
    private Boolean isMakeupWorkday = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
