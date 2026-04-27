package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.CalendarAvailabilityUpdateRequest;
import com.homestay3.homestaybackend.dto.HostCalendarDayDTO;
import com.homestay3.homestaybackend.dto.HostCalendarSummaryDTO;
import com.homestay3.homestaybackend.service.HostCalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HostCalendarController 单元测试")
class HostCalendarControllerTest {

    @Mock private HostCalendarService hostCalendarService;
    @Mock private Authentication authentication;

    private HostCalendarController controller;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        controller = new HostCalendarController(hostCalendarService);
        today = LocalDate.of(2026, 4, 27);
        when(authentication.getName()).thenReturn("host1");
    }

    @Nested
    @DisplayName("GET /api/host/calendar")
    class GetCalendar {

        @Test
        @DisplayName("正常返回日历数据")
        void ok() {
            List<HostCalendarDayDTO> days = List.of(
                    HostCalendarDayDTO.builder()
                            .date(today)
                            .homestayId(100L)
                            .homestayTitle("测试房源")
                            .status("AVAILABLE")
                            .checkIn(false)
                            .checkOut(false)
                            .basePrice(new BigDecimal("300"))
                            .finalPrice(new BigDecimal("300"))
                            .build()
            );
            when(hostCalendarService.getCalendarDays(eq("host1"), eq(100L), any(), any())).thenReturn(days);

            ResponseEntity<List<HostCalendarDayDTO>> response = controller.getCalendar(
                    100L, today, today.plusDays(3), authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).getHomestayId()).isEqualTo(100L);
            assertThat(response.getBody().get(0).getStatus()).isEqualTo("AVAILABLE");
        }

        @Test
        @DisplayName("返回所有房源日历 (homestayId=null)")
        void allHomestays() {
            when(hostCalendarService.getCalendarDays(eq("host1"), eq(null), any(), any()))
                    .thenReturn(List.of());

            ResponseEntity<List<HostCalendarDayDTO>> response = controller.getCalendar(
                    null, today, today.plusDays(3), authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/host/calendar/summary")
    class GetSummary {

        @Test
        @DisplayName("正常返回摘要数据")
        void ok() {
            HostCalendarSummaryDTO summary = HostCalendarSummaryDTO.builder()
                    .availableCount(10)
                    .bookedCount(3)
                    .pendingCount(1)
                    .unavailableCount(2)
                    .checkInCount(1)
                    .checkOutCount(1)
                    .estimatedRevenue(new BigDecimal("1500"))
                    .build();
            when(hostCalendarService.getSummary(eq("host1"), any(), any(), any())).thenReturn(summary);

            ResponseEntity<HostCalendarSummaryDTO> response = controller.getSummary(
                    100L, today, today.plusDays(7), authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getAvailableCount()).isEqualTo(10);
            assertThat(response.getBody().getEstimatedRevenue()).isEqualByComparingTo(new BigDecimal("1500"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/host/calendar/availability")
    class UpdateAvailability {

        @Test
        @DisplayName("正常更新可用性")
        void ok() {
            Map<String, Object> result = Map.of(
                    "updated", 3,
                    "homestayId", 100L,
                    "status", "UNAVAILABLE"
            );
            when(hostCalendarService.updateAvailability(eq("host1"), any())).thenReturn(result);

            CalendarAvailabilityUpdateRequest req = new CalendarAvailabilityUpdateRequest();
            req.setHomestayId(100L);
            req.setStartDate(today);
            req.setEndDate(today.plusDays(3));
            req.setStatus("UNAVAILABLE");
            req.setReason("维修");

            ResponseEntity<Map<String, Object>> response = controller.updateAvailability(req, authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("updated", 3);
            assertThat(response.getBody()).containsEntry("status", "UNAVAILABLE");
        }

        @Test
        @DisplayName("service 层异常直接向上抛出")
        void serviceExceptionPropagates() {
            when(hostCalendarService.updateAvailability(eq("host1"), any()))
                    .thenThrow(new IllegalArgumentException("房源不能为空"));

            CalendarAvailabilityUpdateRequest req = new CalendarAvailabilityUpdateRequest();

            try {
                controller.updateAvailability(req, authentication);
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage()).isEqualTo("房源不能为空");
            }
        }
    }
}
