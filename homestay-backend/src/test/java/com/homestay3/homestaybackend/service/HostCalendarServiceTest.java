package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.CalendarAvailabilityUpdateRequest;
import com.homestay3.homestaybackend.dto.HostCalendarDayDTO;
import com.homestay3.homestaybackend.dto.HostCalendarResponse;
import com.homestay3.homestaybackend.dto.HostCalendarSummaryDTO;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.HomestayAvailability;
import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.exception.AccessDeniedException;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.repository.HomestayAvailabilityRepository;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("HostCalendarService 单元测试")
class HostCalendarServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private HomestayRepository homestayRepository;
    @Mock private HomestayAvailabilityRepository availabilityRepository;
    @Mock private HomestayAvailabilityService availabilityService;
    @Mock private OrderRepository orderRepository;

    @InjectMocks
    private HostCalendarService hostCalendarService;

    private User host;
    private Homestay homestay;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        host = new User();
        host.setId(10L);
        host.setUsername("host1");

        homestay = new Homestay();
        homestay.setId(100L);
        homestay.setTitle("测试房源");
        homestay.setPrice(new BigDecimal("300"));
        homestay.setOwner(host);

        today = LocalDate.of(2026, 4, 27);

        when(userRepository.findByUsername("host1")).thenReturn(Optional.of(host));
    }

    // ---- getCalendarDays ----

    @Nested
    @DisplayName("getCalendarDays")
    class GetCalendarDays {

        @Test
        @DisplayName("返回单房源在日期范围内每日数据")
        void singleHomestayRange() {
            LocalDate start = today;
            LocalDate end = today.plusDays(3);
            when(homestayRepository.findById(100L)).thenReturn(Optional.of(homestay));
            when(availabilityRepository.findByHomestayIdsAndDateRange(anyList(), any(), any()))
                    .thenReturn(List.of());
            when(orderRepository.findHostCalendarOrders(anyLong(), any(), any(), any(), anyList()))
                    .thenReturn(List.of());

            List<HostCalendarDayDTO> days = hostCalendarService.getCalendarDays("host1", 100L, start, end).getDays();

            assertThat(days).hasSize(3);
            assertThat(days.get(0).getDate()).isEqualTo(start);
            assertThat(days.get(0).getHomestayId()).isEqualTo(100L);
            assertThat(days.get(0).getHomestayTitle()).isEqualTo("测试房源");
            assertThat(days.get(0).getStatus()).isEqualTo("AVAILABLE");
            assertThat(days.get(0).getBasePrice()).isEqualTo(new BigDecimal("300"));
            assertThat(days.get(0).getFinalPrice()).isEqualTo(new BigDecimal("300"));
            assertThat(days.get(0).getCheckIn()).isFalse();
            assertThat(days.get(0).getCheckOut()).isFalse();
        }

        @Test
        @DisplayName("homestayId=null 时返回房东所有房源数据")
        void allHomestays() {
            Homestay homestay2 = new Homestay();
            homestay2.setId(101L);
            homestay2.setTitle("房源2");
            homestay2.setPrice(new BigDecimal("400"));
            homestay2.setOwner(host);

            when(homestayRepository.findByOwnerId(host.getId())).thenReturn(List.of(homestay, homestay2));
            when(availabilityRepository.findByHomestayIdsAndDateRange(anyList(), any(), any()))
                    .thenReturn(List.of());
            when(orderRepository.findHostCalendarOrders(anyLong(), any(), any(), any(), anyList()))
                    .thenReturn(List.of());

            List<HostCalendarDayDTO> days = hostCalendarService.getCalendarDays("host1", null, today, today.plusDays(2)).getDays();

            // 2 homestays * 2 days = 4 entries
            assertThat(days).hasSize(4);
            assertThat(days.stream().map(HostCalendarDayDTO::getHomestayId).distinct()).containsExactlyInAnyOrder(100L, 101L);
        }

        @Test
        @DisplayName("房东无房源时返回空列表和零值摘要")
        void noHomestays() {
            when(homestayRepository.findByOwnerId(host.getId())).thenReturn(List.of());

            HostCalendarResponse response = hostCalendarService.getCalendarDays("host1", null, today, today.plusDays(2));

            assertThat(response.getDays()).isEmpty();
            assertThat(response.getSummary().getAvailableCount()).isZero();
            assertThat(response.getSummary().getEstimatedRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("homestayId 对应的房源不属于当前房东时抛 AccessDeniedException")
        void accessDenied() {
            User otherHost = new User();
            otherHost.setId(99L);
            homestay.setOwner(otherHost);
            when(homestayRepository.findById(100L)).thenReturn(Optional.of(homestay));

            assertThatThrownBy(() ->
                    hostCalendarService.getCalendarDays("host1", 100L, today, today.plusDays(2)))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("只能查看自己的房源日历");
        }

        @Test
        @DisplayName("开始日期和结束日期为 null 时抛异常")
        void nullDates() {
            assertThatThrownBy(() ->
                    hostCalendarService.getCalendarDays("host1", 100L, null, today))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("开始日期和结束日期不能为空");
        }

        @Test
        @DisplayName("结束日期不晚于开始日期时抛异常")
        void endNotAfterStart() {
            assertThatThrownBy(() ->
                    hostCalendarService.getCalendarDays("host1", 100L, today, today))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("结束日期必须晚于开始日期");
        }

        @Test
        @DisplayName("日期范围超过 370 天时抛异常")
        void rangeTooLarge() {
            assertThatThrownBy(() ->
                    hostCalendarService.getCalendarDays("host1", 100L, today, today.plusDays(371)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("不能超过一年");
        }

        @Test
        @DisplayName("房源不存在时抛 ResourceNotFoundException")
        void homestayNotFound() {
            when(homestayRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    hostCalendarService.getCalendarDays("host1", 999L, today, today.plusDays(2)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("房源不存在");
        }
    }

    // ---- applyAvailability 逻辑 ----

    @Nested
    @DisplayName("applyAvailability — 状态覆盖")
    class ApplyAvailability {

        @Test
        @DisplayName("房东设为不可订的日期显示 UNAVAILABLE 及相关信息")
        void unavailableDates() {
            LocalDate start = today;
            LocalDate end = today.plusDays(3);
            when(homestayRepository.findById(100L)).thenReturn(Optional.of(homestay));
            when(orderRepository.findHostCalendarOrders(anyLong(), any(), any(), any(), anyList()))
                    .thenReturn(List.of());

            HomestayAvailability av = HomestayAvailability.builder()
                    .homestayId(100L)
                    .date(today)
                    .status(HomestayAvailability.AvailabilityStatus.UNAVAILABLE)
                    .source("HOST")
                    .reason("维修")
                    .note("卫生间漏水")
                    .locked(false)
                    .build();
            when(availabilityRepository.findByHomestayIdsAndDateRange(anyList(), any(), any()))
                    .thenReturn(List.of(av));

            List<HostCalendarDayDTO> days = hostCalendarService.getCalendarDays("host1", 100L, start, end).getDays();

            HostCalendarDayDTO day = days.get(0);
            assertThat(day.getStatus()).isEqualTo("UNAVAILABLE");
            assertThat(day.getSource()).isEqualTo("HOST");
            assertThat(day.getReason()).isEqualTo("维修");
            assertThat(day.getNote()).isEqualTo("卫生间漏水");
        }

        @Test
        @DisplayName("被锁定的日期显示 LOCKED 状态")
        void lockedDates() {
            LocalDate start = today;
            LocalDate end = today.plusDays(2);
            when(homestayRepository.findById(100L)).thenReturn(Optional.of(homestay));
            when(orderRepository.findHostCalendarOrders(anyLong(), any(), any(), any(), anyList()))
                    .thenReturn(List.of());

            HomestayAvailability av = HomestayAvailability.builder()
                    .homestayId(100L)
                    .date(today)
                    .status(HomestayAvailability.AvailabilityStatus.AVAILABLE)
                    .locked(true)
                    .lockExpiresAt(LocalDateTime.now().plusMinutes(10))
                    .build();
            when(availabilityRepository.findByHomestayIdsAndDateRange(anyList(), any(), any()))
                    .thenReturn(List.of(av));

            List<HostCalendarDayDTO> days = hostCalendarService.getCalendarDays("host1", 100L, start, end).getDays();

            assertThat(days.get(0).getStatus()).isEqualTo("LOCKED");
            assertThat(days.get(0).getSource()).isEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("过期锁不改变状态")
        void expiredLock() {
            LocalDate start = today;
            LocalDate end = today.plusDays(2);
            when(homestayRepository.findById(100L)).thenReturn(Optional.of(homestay));
            when(orderRepository.findHostCalendarOrders(anyLong(), any(), any(), any(), anyList()))
                    .thenReturn(List.of());

            HomestayAvailability av = HomestayAvailability.builder()
                    .homestayId(100L)
                    .date(today)
                    .status(HomestayAvailability.AvailabilityStatus.AVAILABLE)
                    .locked(true)
                    .lockExpiresAt(LocalDateTime.now().minusMinutes(10))
                    .build();
            when(availabilityRepository.findByHomestayIdsAndDateRange(anyList(), any(), any()))
                    .thenReturn(List.of(av));

            List<HostCalendarDayDTO> days = hostCalendarService.getCalendarDays("host1", 100L, start, end).getDays();

            // 过期锁不应设为 LOCKED，保持默认 AVAILABLE
            assertThat(days.get(0).getStatus()).isEqualTo("AVAILABLE");
        }
    }

    // ---- applyOrders 逻辑 ----

    @Nested
    @DisplayName("applyOrders — 订单覆盖")
    class ApplyOrders {

        @Test
        @DisplayName("订单占用的日期显示 BOOKED 及订单信息")
        void bookedDates() {
            LocalDate start = today;
            LocalDate end = today.plusDays(5);
            when(homestayRepository.findById(100L)).thenReturn(Optional.of(homestay));
            when(availabilityRepository.findByHomestayIdsAndDateRange(anyList(), any(), any()))
                    .thenReturn(List.of());

            User guest = new User();
            guest.setId(200L);
            guest.setUsername("guest1");
            guest.setNickname("张三");

            Order order = Order.builder()
                    .id(1000L)
                    .orderNumber("ORD202604270001")
                    .homestay(homestay)
                    .guest(guest)
                    .checkInDate(today.plusDays(1))
                    .checkOutDate(today.plusDays(4))
                    .status("CONFIRMED")
                    .totalAmount(new BigDecimal("900"))
                    .build();
            when(orderRepository.findHostCalendarOrders(anyLong(), any(), any(), any(), anyList()))
                    .thenReturn(List.of(order));

            List<HostCalendarDayDTO> days = hostCalendarService.getCalendarDays("host1", 100L, start, end).getDays();

            // day 1 = today+1, occupied
            HostCalendarDayDTO occupied = days.get(1);
            assertThat(occupied.getStatus()).isEqualTo("BOOKED");
            assertThat(occupied.getSource()).isEqualTo("ORDER");
            assertThat(occupied.getOrderId()).isEqualTo(1000L);
            assertThat(occupied.getOrderNumber()).isEqualTo("ORD202604270001");
            assertThat(occupied.getGuestName()).isEqualTo("张三");
            assertThat(occupied.getCheckIn()).isTrue(); // first occupied day

            // day 4 = today+4, should be checkout day
            HostCalendarDayDTO checkoutDay = days.get(4);
            assertThat(checkoutDay.getCheckOut()).isTrue();
        }

        @Test
        @DisplayName("PENDING 状态订单显示 PENDING_CONFIRM")
        void pendingOrder() {
            LocalDate start = today;
            LocalDate end = today.plusDays(3);
            when(homestayRepository.findById(100L)).thenReturn(Optional.of(homestay));
            when(availabilityRepository.findByHomestayIdsAndDateRange(anyList(), any(), any()))
                    .thenReturn(List.of());

            User guest = new User();
            guest.setId(200L);
            guest.setNickname("李四");

            Order order = Order.builder()
                    .id(1001L)
                    .orderNumber("ORD002")
                    .homestay(homestay)
                    .guest(guest)
                    .checkInDate(today)
                    .checkOutDate(today.plusDays(2))
                    .status("PENDING")
                    .totalAmount(BigDecimal.ZERO)
                    .build();
            when(orderRepository.findHostCalendarOrders(anyLong(), any(), any(), any(), anyList()))
                    .thenReturn(List.of(order));

            List<HostCalendarDayDTO> days = hostCalendarService.getCalendarDays("host1", 100L, start, end).getDays();

            assertThat(days.get(0).getStatus()).isEqualTo("PENDING_CONFIRM");
        }

        @Test
        @DisplayName("CHECKED_IN / CHECKED_OUT 状态正确映射")
        void checkedInOutStatus() {
            LocalDate start = today;
            LocalDate end = today.plusDays(3);
            when(homestayRepository.findById(100L)).thenReturn(Optional.of(homestay));
            when(availabilityRepository.findByHomestayIdsAndDateRange(anyList(), any(), any()))
                    .thenReturn(List.of());

            User guest = new User();
            guest.setId(200L);
            guest.setUsername("guest2");

            Order order = Order.builder()
                    .id(1002L)
                    .orderNumber("ORD003")
                    .homestay(homestay)
                    .guest(guest)
                    .checkInDate(today)
                    .checkOutDate(today.plusDays(2))
                    .status("CHECKED_IN")
                    .totalAmount(BigDecimal.ZERO)
                    .build();
            when(orderRepository.findHostCalendarOrders(anyLong(), any(), any(), any(), anyList()))
                    .thenReturn(List.of(order));

            List<HostCalendarDayDTO> days = hostCalendarService.getCalendarDays("host1", 100L, start, end).getDays();

            assertThat(days.get(0).getStatus()).isEqualTo("CHECKED_IN");
        }

        @Test
        @DisplayName("guest 的 nickname 为空时回退到 username")
        void fallbackToUsername() {
            LocalDate start = today;
            LocalDate end = today.plusDays(3);
            when(homestayRepository.findById(100L)).thenReturn(Optional.of(homestay));
            when(availabilityRepository.findByHomestayIdsAndDateRange(anyList(), any(), any()))
                    .thenReturn(List.of());

            User guest = new User();
            guest.setId(200L);
            guest.setUsername("guest3");
            guest.setNickname(null);

            Order order = Order.builder()
                    .id(1003L)
                    .orderNumber("ORD004")
                    .homestay(homestay)
                    .guest(guest)
                    .checkInDate(today)
                    .checkOutDate(today.plusDays(2))
                    .status("CONFIRMED")
                    .totalAmount(BigDecimal.ZERO)
                    .build();
            when(orderRepository.findHostCalendarOrders(anyLong(), any(), any(), any(), anyList()))
                    .thenReturn(List.of(order));

            List<HostCalendarDayDTO> days = hostCalendarService.getCalendarDays("host1", 100L, start, end).getDays();

            assertThat(days.get(0).getGuestName()).isEqualTo("guest3");
        }
    }

    // ---- getSummary ----

    @Nested
    @DisplayName("getSummary")
    class GetSummary {

        @Test
        @DisplayName("正确统计各状态数量")
        void summaryCounts() {
            LocalDate start = today;
            LocalDate end = today.plusDays(5);
            when(homestayRepository.findById(100L)).thenReturn(Optional.of(homestay));
            when(availabilityRepository.findByHomestayIdsAndDateRange(anyList(), any(), any()))
                    .thenReturn(List.of());

            User guest = new User();
            guest.setId(200L);
            guest.setUsername("g1");
            guest.setNickname("客人");

            // 1 night order with CHECKED_IN status
            Order order = Order.builder()
                    .id(1000L)
                    .orderNumber("ORD001")
                    .homestay(homestay)
                    .guest(guest)
                    .checkInDate(today.plusDays(1))
                    .checkOutDate(today.plusDays(2))
                    .status("CHECKED_IN")
                    .totalAmount(new BigDecimal("300"))
                    .build();
            when(orderRepository.findHostCalendarOrders(anyLong(), any(), any(), any(), anyList()))
                    .thenReturn(List.of(order));

            HostCalendarSummaryDTO summary = hostCalendarService.getSummary("host1", 100L, start, end);

            // 5 days total: 0 is booked/checked_in, 1-4 available
            assertThat(summary.getAvailableCount()).isEqualTo(4);
            assertThat(summary.getBookedCount()).isEqualTo(1); // CHECKED_IN counts as booked
            assertThat(summary.getPendingCount()).isEqualTo(0);
            assertThat(summary.getUnavailableCount()).isEqualTo(0);
            assertThat(summary.getCheckInCount()).isEqualTo(1);
            assertThat(summary.getCheckOutCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("无订单数据时 summary 全为零/空")
        void emptySummary() {
            LocalDate start = today;
            LocalDate end = today.plusDays(3);
            when(homestayRepository.findById(100L)).thenReturn(Optional.of(homestay));
            when(availabilityRepository.findByHomestayIdsAndDateRange(anyList(), any(), any()))
                    .thenReturn(List.of());
            when(orderRepository.findHostCalendarOrders(anyLong(), any(), any(), any(), anyList()))
                    .thenReturn(List.of());

            HostCalendarSummaryDTO summary = hostCalendarService.getSummary("host1", 100L, start, end);

            assertThat(summary.getAvailableCount()).isEqualTo(3);
            assertThat(summary.getBookedCount()).isZero();
            assertThat(summary.getEstimatedRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ---- updateAvailability ----

    @Nested
    @DisplayName("updateAvailability")
    class UpdateAvailability {

        @Test
        @DisplayName("成功设置为 UNAVAILABLE")
        void setUnavailable() {
            when(homestayRepository.findById(100L)).thenReturn(Optional.of(homestay));
            when(availabilityService.hasOccupiedDates(anyLong(), any(), any())).thenReturn(false);
            when(orderRepository.findHostCalendarOrders(anyLong(), any(), any(), any(), anyList()))
                    .thenReturn(List.of());
            when(availabilityService.setManualAvailability(eq(100L), any(), any(), any(), any(), any(), anyLong()))
                    .thenReturn(5);

            CalendarAvailabilityUpdateRequest req = new CalendarAvailabilityUpdateRequest();
            req.setHomestayId(100L);
            req.setStartDate(today);
            req.setEndDate(today.plusDays(5));
            req.setStatus("UNAVAILABLE");
            req.setReason("维修");
            req.setNote("临时维修");

            Map<String, Object> result = hostCalendarService.updateAvailability("host1", req);

            assertThat(result.get("updated")).isEqualTo(5);
            assertThat(result.get("status")).isEqualTo("UNAVAILABLE");
            verify(availabilityService).setManualAvailability(eq(100L), any(), any(),
                    eq(HomestayAvailability.AvailabilityStatus.UNAVAILABLE), eq("维修"), eq("临时维修"), eq(10L));
        }

        @Test
        @DisplayName("成功设置为 AVAILABLE")
        void setAvailable() {
            when(homestayRepository.findById(100L)).thenReturn(Optional.of(homestay));
            when(availabilityService.setManualAvailability(eq(100L), any(), any(), any(), any(), any(), anyLong()))
                    .thenReturn(3);

            CalendarAvailabilityUpdateRequest req = new CalendarAvailabilityUpdateRequest();
            req.setHomestayId(100L);
            req.setStartDate(today);
            req.setEndDate(today.plusDays(3));
            req.setStatus("AVAILABLE");

            Map<String, Object> result = hostCalendarService.updateAvailability("host1", req);

            assertThat(result.get("updated")).isEqualTo(3);
            assertThat(result.get("status")).isEqualTo("AVAILABLE");
        }

        @Test
        @DisplayName("request 为 null 或 homestayId 为 null 时抛异常")
        void nullRequest() {
            CalendarAvailabilityUpdateRequest req = new CalendarAvailabilityUpdateRequest();
            assertThatThrownBy(() -> hostCalendarService.updateAvailability("host1", req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("房源不能为空");

            assertThatThrownBy(() -> hostCalendarService.updateAvailability("host1", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("房源不能为空");
        }

        @Test
        @DisplayName("非法状态值时抛异常")
        void invalidStatus() {
            when(homestayRepository.findById(100L)).thenReturn(Optional.of(homestay));

            CalendarAvailabilityUpdateRequest req = new CalendarAvailabilityUpdateRequest();
            req.setHomestayId(100L);
            req.setStartDate(today);
            req.setEndDate(today.plusDays(3));
            req.setStatus("BOOKED");

            assertThatThrownBy(() -> hostCalendarService.updateAvailability("host1", req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("只能设置为 AVAILABLE 或 UNAVAILABLE");
        }

        @Test
        @DisplayName("设为不可订但存在订单冲突时抛异常")
        void orderConflict() {
            when(homestayRepository.findById(100L)).thenReturn(Optional.of(homestay));

            Order conflict = Order.builder()
                    .id(500L)
                    .orderNumber("ORD-CONFLICT")
                    .homestay(homestay)
                    .checkInDate(today)
                    .checkOutDate(today.plusDays(2))
                    .status("CONFIRMED")
                    .build();
            when(orderRepository.findHostCalendarOrders(anyLong(), any(), any(), any(), anyList()))
                    .thenReturn(List.of(conflict));

            CalendarAvailabilityUpdateRequest req = new CalendarAvailabilityUpdateRequest();
            req.setHomestayId(100L);
            req.setStartDate(today);
            req.setEndDate(today.plusDays(3));
            req.setStatus("UNAVAILABLE");

            assertThatThrownBy(() -> hostCalendarService.updateAvailability("host1", req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("所选日期存在订单占用");

            // 不应调用 setManualAvailability
            verify(availabilityService, never()).setManualAvailability(anyLong(), any(), any(), any(), any(), any(), anyLong());
        }

        @Test
        @DisplayName("设为不可订但 hasOccupiedDates 返回 true 时抛异常")
        void hasOccupiedDates() {
            when(homestayRepository.findById(100L)).thenReturn(Optional.of(homestay));
            when(orderRepository.findHostCalendarOrders(anyLong(), any(), any(), any(), anyList()))
                    .thenReturn(List.of());
            when(availabilityService.hasOccupiedDates(anyLong(), any(), any())).thenReturn(true);

            CalendarAvailabilityUpdateRequest req = new CalendarAvailabilityUpdateRequest();
            req.setHomestayId(100L);
            req.setStartDate(today);
            req.setEndDate(today.plusDays(3));
            req.setStatus("UNAVAILABLE");

            assertThatThrownBy(() -> hostCalendarService.updateAvailability("host1", req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("存在订单占用");
        }

        @Test
        @DisplayName("操作他人房源时抛 AccessDeniedException")
        void notOwner() {
            User otherHost = new User();
            otherHost.setId(99L);
            homestay.setOwner(otherHost);
            when(homestayRepository.findById(100L)).thenReturn(Optional.of(homestay));

            CalendarAvailabilityUpdateRequest req = new CalendarAvailabilityUpdateRequest();
            req.setHomestayId(100L);
            req.setStartDate(today);
            req.setEndDate(today.plusDays(3));
            req.setStatus("AVAILABLE");

            assertThatThrownBy(() -> hostCalendarService.updateAvailability("host1", req))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("只能操作自己的房源日历");
        }
    }

}
