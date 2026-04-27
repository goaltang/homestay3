package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.entity.HomestayAvailability;
import com.homestay3.homestaybackend.entity.HomestayAvailability.AvailabilityStatus;
import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.repository.HomestayAvailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HomestayAvailabilityService 单元测试")
class HomestayAvailabilityServiceTest {

    @Mock private HomestayAvailabilityRepository availabilityRepository;

    @InjectMocks
    private HomestayAvailabilityService availabilityService;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.of(2026, 4, 27);
    }

    @Nested
    @DisplayName("initializeAvailability")
    class InitializeAvailability {

        @Test
        @DisplayName("未初始化的日期创建 AVAILABLE 记录")
        void createsMissingDates() {
            when(availabilityRepository.findByHomestayIdAndDate(eq(100L), any()))
                    .thenReturn(Optional.empty());

            availabilityService.initializeAvailability(100L, today, today.plusDays(3));

            ArgumentCaptor<HomestayAvailability> captor = ArgumentCaptor.forClass(HomestayAvailability.class);
            verify(availabilityRepository, org.mockito.Mockito.times(3)).save(captor.capture());

            List<HomestayAvailability> saved = captor.getAllValues();
            assertThat(saved).allMatch(a -> a.getStatus() == AvailabilityStatus.AVAILABLE);
            assertThat(saved).allMatch(a -> a.getHomestayId().equals(100L));
            assertThat(saved).allMatch(a -> !a.getLocked());
        }

        @Test
        @DisplayName("已存在的日期跳过不创建")
        void skipsExistingDates() {
            when(availabilityRepository.findByHomestayIdAndDate(eq(100L), any()))
                    .thenReturn(Optional.of(HomestayAvailability.builder()
                            .homestayId(100L)
                            .date(today)
                            .status(AvailabilityStatus.AVAILABLE)
                            .build()));

            availabilityService.initializeAvailability(100L, today, today.plusDays(2));

            // Only 1 day, already exists → 0 saves
            verify(availabilityRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("markAsBooked / markAsAvailable")
    class MarkBookedAvailable {

        @Test
        @DisplayName("markAsBooked 更新状态为 BOOKED 并关联 order")
        void markAsBooked() {
            when(availabilityRepository.updateAvailabilityStatus(anyLong(), any(), any(), any(), any())).thenReturn(3);

            Homestay homestay = new Homestay();
            homestay.setId(100L);

            Order order = Order.builder()
                    .checkInDate(today)
                    .checkOutDate(today.plusDays(3))
                    .homestay(homestay)
                    .orderNumber("ORD001")
                    .build();

            availabilityService.markAsBooked(order);

            verify(availabilityRepository).updateAvailabilityStatus(
                    eq(100L), eq(today), eq(today.plusDays(3)),
                    eq(AvailabilityStatus.BOOKED), eq(order.getId()));
        }

        @Test
        @DisplayName("markAsAvailable 释放日期")
        void markAsAvailable() {
            when(availabilityRepository.updateAvailabilityStatus(anyLong(), any(), any(), any(), any())).thenReturn(5);

            availabilityService.markAsAvailable(100L, today, today.plusDays(5));

            verify(availabilityRepository).updateAvailabilityStatus(
                    eq(100L), eq(today), eq(today.plusDays(5)),
                    eq(AvailabilityStatus.AVAILABLE), eq(null));
        }
    }

    @Nested
    @DisplayName("acquireBookingLock / releaseBookingLock")
    class BookingLock {

        @Test
        @DisplayName("全部日期加锁成功时返回 true")
        void acquireAllLocksSuccess() {
            when(availabilityRepository.acquireLocks(anyLong(), any(), any(), any(), any())).thenReturn(3);

            boolean result = availabilityService.acquireBookingLock(100L, today, today.plusDays(3));

            assertThat(result).isTrue();
            verify(availabilityRepository, never()).releaseLocks(anyLong(), any(), any());
        }

        @Test
        @DisplayName("部分加锁失败时调用 releaseLocks 并返回 false")
        void acquireLocksPartialFailure() {
            when(availabilityRepository.acquireLocks(anyLong(), any(), any(), any(), any())).thenReturn(2);

            boolean result = availabilityService.acquireBookingLock(100L, today, today.plusDays(3));

            assertThat(result).isFalse();
            verify(availabilityRepository).releaseLocks(eq(100L), eq(today), eq(today.plusDays(3)));
        }
    }

    @Nested
    @DisplayName("hasOverlappingBooking")
    class HasOverlappingBooking {

        @Test
        @DisplayName("存在预订日期时返回 true")
        void trueWhenBooked() {
            when(availabilityRepository.hasBookedDates(100L, today, today.plusDays(3))).thenReturn(true);
            assertThat(availabilityService.hasOverlappingBooking(100L, today, today.plusDays(3))).isTrue();
        }

        @Test
        @DisplayName("无预订日期时返回 false")
        void falseWhenAvailable() {
            when(availabilityRepository.hasBookedDates(100L, today, today.plusDays(3))).thenReturn(false);
            assertThat(availabilityService.hasOverlappingBooking(100L, today, today.plusDays(3))).isFalse();
        }
    }

    @Nested
    @DisplayName("hasOccupiedDates / setManualAvailability")
    class NewMethods {

        @Test
        @DisplayName("hasOccupiedDates 委托到 repository")
        void hasOccupiedDatesDelegation() {
            when(availabilityRepository.hasOccupiedDates(eq(100L), any(), any(), any())).thenReturn(true);

            assertThat(availabilityService.hasOccupiedDates(100L, today, today.plusDays(3))).isTrue();
        }

        @Test
        @DisplayName("setManualAvailability 委托到 repository 并返回更新数")
        void setManualAvailabilityDelegation() {
            when(availabilityRepository.setManualAvailability(eq(100L), any(), any(), any(), any(), any(), anyLong()))
                    .thenReturn(7);

            int updated = availabilityService.setManualAvailability(
                    100L, today, today.plusDays(7),
                    AvailabilityStatus.UNAVAILABLE, "维修", "备注", 10L);

            assertThat(updated).isEqualTo(7);
        }
    }
}
