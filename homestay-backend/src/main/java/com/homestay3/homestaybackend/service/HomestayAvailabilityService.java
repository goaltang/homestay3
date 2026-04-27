package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.entity.HomestayAvailability;
import com.homestay3.homestaybackend.entity.HomestayAvailability.AvailabilityStatus;
import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.repository.HomestayAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomestayAvailabilityService {

    private final HomestayAvailabilityRepository availabilityRepository;
    private static final int LOCK_DURATION_MINUTES = 30;

    @Transactional
    public void initializeAvailability(Long homestayId, LocalDate startDate, LocalDate endDate) {
        log.info("初始化房源 {} 的可用性日历: {} 到 {}", homestayId, startDate, endDate);
        
        List<LocalDate> dates = IntStream.range(0, (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate))
                .mapToObj(startDate::plusDays)
                .collect(Collectors.toList());

        for (LocalDate date : dates) {
            if (availabilityRepository.findByHomestayIdAndDate(homestayId, date).isEmpty()) {
                HomestayAvailability availability = HomestayAvailability.builder()
                        .homestayId(homestayId)
                        .date(date)
                        .status(AvailabilityStatus.AVAILABLE)
                        .locked(false)
                        .build();
                availabilityRepository.save(availability);
            }
        }
    }

    @Transactional
    public void markAsBooked(Order order) {
        LocalDate checkIn = order.getCheckInDate();
        LocalDate checkOut = order.getCheckOutDate();
        Long homestayId = order.getHomestay().getId();
        
        log.info("标记订单 {} 的日期 {} 到 {} 为已预订", 
                order.getOrderNumber(), checkIn, checkOut);

        int updated = availabilityRepository.updateAvailabilityStatus(
                homestayId,
                checkIn,
                checkOut,
                AvailabilityStatus.BOOKED,
                order.getId()
        );
        
        log.info("成功更新 {} 条可用性记录", updated);
    }

    @Transactional
    public void markAsAvailable(Long homestayId, LocalDate checkIn, LocalDate checkOut) {
        log.info("释放房源 {} 的日期 {} 到 {}", homestayId, checkIn, checkOut);

        int updated = availabilityRepository.updateAvailabilityStatus(
                homestayId,
                checkIn,
                checkOut,
                AvailabilityStatus.AVAILABLE,
                null
        );
        
        log.info("成功释放 {} 条可用性记录", updated);
    }

    public boolean hasOverlappingBooking(Long homestayId, LocalDate checkIn, LocalDate checkOut) {
        return availabilityRepository.hasBookedDates(homestayId, checkIn, checkOut);
    }

    public List<LocalDate> getBookedDates(Long homestayId, LocalDate startDate, LocalDate endDate) {
        List<HomestayAvailability> bookings = availabilityRepository
                .findByHomestayIdAndDateRangeAndStatus(homestayId, startDate, endDate, AvailabilityStatus.BOOKED);
        
        return bookings.stream()
                .map(HomestayAvailability::getDate)
                .sorted()
                .collect(Collectors.toList());
    }

    public List<LocalDate> getAvailableDates(Long homestayId, LocalDate startDate, LocalDate endDate) {
        List<HomestayAvailability> allDates = availabilityRepository
                .findByHomestayIdAndDateBetween(homestayId, startDate, endDate);
        
        return allDates.stream()
                .filter(a -> a.getStatus() == AvailabilityStatus.AVAILABLE)
                .map(HomestayAvailability::getDate)
                .sorted()
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean acquireBookingLock(Long homestayId, LocalDate checkIn, LocalDate checkOut) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(LOCK_DURATION_MINUTES);

        int acquired = availabilityRepository.acquireLocks(
                homestayId, checkIn, checkOut, expiresAt, now
        );

        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        
        if (acquired == totalDays) {
            log.info("成功锁定房源 {} 的日期 {} 到 {}", homestayId, checkIn, checkOut);
            return true;
        } else {
            log.warn("锁定房源 {} 的日期 {} 到 {} 失败，仅锁定 {} 天", 
                    homestayId, checkIn, checkOut, acquired);
            availabilityRepository.releaseLocks(homestayId, checkIn, checkOut);
            return false;
        }
    }

    @Transactional
    public void releaseBookingLock(Long homestayId, LocalDate checkIn, LocalDate checkOut) {
        int released = availabilityRepository.releaseLocks(homestayId, checkIn, checkOut);
        log.info("释放房源 {} 的锁定，释放 {} 条记录", homestayId, released);
    }

    public boolean isDateRangeLocked(Long homestayId, LocalDate checkIn, LocalDate checkOut) {
        List<HomestayAvailability> lockedDates = availabilityRepository.findLockedDates(
                homestayId, checkIn, checkOut, LocalDateTime.now()
        );
        return !lockedDates.isEmpty();
    }

    @Transactional
    public void createAvailabilityRecords(Long homestayId, LocalDate checkIn, LocalDate checkOut) {
        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            boolean exists = availabilityRepository.findByHomestayIdAndDate(homestayId, date).isPresent();
            if (!exists) {
                HomestayAvailability availability = HomestayAvailability.builder()
                        .homestayId(homestayId)
                        .date(date)
                        .status(AvailabilityStatus.AVAILABLE)
                        .locked(false)
                        .build();
                availabilityRepository.save(availability);
            }
        }
    }

    public boolean hasOccupiedDates(Long homestayId, LocalDate startDate, LocalDate endDate) {
        return availabilityRepository.hasOccupiedDates(homestayId, startDate, endDate, LocalDateTime.now());
    }

    @Transactional
    public int setManualAvailability(Long homestayId, LocalDate startDate, LocalDate endDate,
                                     AvailabilityStatus status, String reason, String note, Long createdBy) {
        return availabilityRepository.setManualAvailability(homestayId, startDate, endDate, status, reason, note, createdBy);
    }
}
