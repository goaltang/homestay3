package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.HolidayCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayCalendarRepository extends JpaRepository<HolidayCalendar, Long> {

    Optional<HolidayCalendar> findByDateAndRegionCode(LocalDate date, String regionCode);

    List<HolidayCalendar> findByDateBetweenAndRegionCodeAndIsHolidayTrue(LocalDate start, LocalDate end, String regionCode);

    List<HolidayCalendar> findByDateBetweenAndRegionCode(LocalDate start, LocalDate end, String regionCode);

    boolean existsByDateAndRegionCode(LocalDate date, String regionCode);
}
