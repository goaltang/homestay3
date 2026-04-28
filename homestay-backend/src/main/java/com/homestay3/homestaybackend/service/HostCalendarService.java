package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.CalendarAvailabilityUpdateRequest;
import com.homestay3.homestaybackend.dto.CalendarPriceUpdateRequest;
import com.homestay3.homestaybackend.dto.HostCalendarDayDTO;
import com.homestay3.homestaybackend.dto.HostCalendarResponse;
import com.homestay3.homestaybackend.dto.HostCalendarSummaryDTO;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.HomestayAvailability;
import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.exception.AccessDeniedException;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.repository.HomestayAvailabilityRepository;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HostCalendarService {

    private static final int MAX_RANGE_DAYS = 370;
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PENDING_CONFIRM = "PENDING_CONFIRM";
    private static final String STATUS_BOOKED = "BOOKED";
    private static final String STATUS_CHECKED_IN = "CHECKED_IN";
    private static final String STATUS_CHECKED_OUT = "CHECKED_OUT";
    private static final String STATUS_UNAVAILABLE = "UNAVAILABLE";
    private static final String STATUS_LOCKED = "LOCKED";

    private static final List<String> OCCUPYING_ORDER_STATUSES = List.of(
            OrderStatus.PENDING.name(),
            OrderStatus.CONFIRMED.name(),
            OrderStatus.PAYMENT_PENDING.name(),
            OrderStatus.PAID.name(),
            OrderStatus.READY_FOR_CHECKIN.name(),
            OrderStatus.CHECKED_IN.name(),
            OrderStatus.CHECKED_OUT.name(),
            OrderStatus.COMPLETED.name(),
            OrderStatus.REFUND_PENDING.name(),
            OrderStatus.DISPUTE_PENDING.name()
    );

    private final UserRepository userRepository;
    private final HomestayRepository homestayRepository;
    private final HomestayAvailabilityRepository availabilityRepository;
    private final HomestayAvailabilityService availabilityService;
    private final OrderRepository orderRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public HostCalendarResponse getCalendarDays(
            String username,
            Long homestayId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);
        User host = getHost(username);
        List<Homestay> homestays = getHostHomestays(host, homestayId);
        if (homestays.isEmpty()) {
            return HostCalendarResponse.builder()
                    .days(List.of())
                    .summary(HostCalendarSummaryDTO.builder()
                            .availableCount(0)
                            .bookedCount(0)
                            .pendingCount(0)
                            .unavailableCount(0)
                            .checkInCount(0)
                            .checkOutCount(0)
                            .estimatedRevenue(BigDecimal.ZERO)
                            .build())
                    .build();
        }

        Map<String, HostCalendarDayDTO> dayMap = buildBaseDays(homestays, startDate, endDate);
        applyAvailability(dayMap, homestays, startDate, endDate);
        List<Order> orders = applyOrders(dayMap, host.getId(), homestayId, startDate, endDate);

        int available = 0;
        int booked = 0;
        int pending = 0;
        int unavailable = 0;
        int checkIns = 0;
        int checkOuts = 0;

        for (HostCalendarDayDTO day : dayMap.values()) {
            if (Boolean.TRUE.equals(day.getCheckIn())) {
                checkIns++;
            }
            if (Boolean.TRUE.equals(day.getCheckOut())) {
                checkOuts++;
            }

            switch (day.getStatus()) {
                case STATUS_AVAILABLE -> available++;
                case STATUS_PENDING_CONFIRM -> pending++;
                case STATUS_UNAVAILABLE -> unavailable++;
                case STATUS_BOOKED, STATUS_CHECKED_IN, STATUS_CHECKED_OUT, STATUS_LOCKED -> booked++;
                default -> {
                }
            }
        }

        BigDecimal estimatedRevenue = computeEstimatedRevenue(orders, startDate, endDate);

        return HostCalendarResponse.builder()
                .days(new ArrayList<>(dayMap.values()))
                .summary(HostCalendarSummaryDTO.builder()
                        .availableCount(available)
                        .bookedCount(booked)
                        .pendingCount(pending)
                        .unavailableCount(unavailable)
                        .checkInCount(checkIns)
                        .checkOutCount(checkOuts)
                        .estimatedRevenue(estimatedRevenue)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public HostCalendarSummaryDTO getSummary(
            String username,
            Long homestayId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return getCalendarDays(username, homestayId, startDate, endDate).getSummary();
    }

    @Transactional
    public Map<String, Object> updateAvailability(String username, CalendarAvailabilityUpdateRequest request) {
        if (request == null || request.getHomestayId() == null) {
            throw new IllegalArgumentException("房源不能为空");
        }
        validateDateRange(request.getStartDate(), request.getEndDate());

        User host = getHost(username);
        Homestay homestay = homestayRepository.findById(request.getHomestayId())
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在"));
        if (homestay.getOwner() == null || !host.getId().equals(homestay.getOwner().getId())) {
            throw new AccessDeniedException("只能操作自己的房源日历");
        }

        HomestayAvailability.AvailabilityStatus targetStatus = parseTargetStatus(request.getStatus());
        if (targetStatus == HomestayAvailability.AvailabilityStatus.UNAVAILABLE) {
            assertNoOrderConflict(host.getId(), request.getHomestayId(), request.getStartDate(), request.getEndDate());
            if (availabilityService.hasOccupiedDates(request.getHomestayId(), request.getStartDate(), request.getEndDate())) {
                throw new IllegalArgumentException("所选日期中存在订单占用，不能设为不可订");
            }
        }

        int updated = availabilityService.setManualAvailability(
                request.getHomestayId(),
                request.getStartDate(),
                request.getEndDate(),
                targetStatus,
                request.getReason(),
                request.getNote(),
                host.getId()
        );

        Map<String, Object> result = new HashMap<>();
        result.put("updated", updated);
        result.put("homestayId", request.getHomestayId());
        result.put("startDate", request.getStartDate());
        result.put("endDate", request.getEndDate());
        result.put("status", targetStatus.name());

        sendCalendarRefresh(host.getId(), "AVAILABILITY_UPDATED");
        return result;
    }

    @Transactional
    public Map<String, Object> updatePrice(String username, CalendarPriceUpdateRequest request) {
        if (request == null || request.getHomestayId() == null) {
            throw new IllegalArgumentException("房源不能为空");
        }
        validateDateRange(request.getStartDate(), request.getEndDate());

        User host = getHost(username);
        Homestay homestay = homestayRepository.findById(request.getHomestayId())
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在"));
        if (homestay.getOwner() == null || !host.getId().equals(homestay.getOwner().getId())) {
            throw new AccessDeniedException("只能操作自己的房源日历");
        }

        int updated;
        if (request.getCustomPrice() == null) {
            updated = availabilityService.clearCustomPrice(
                    request.getHomestayId(),
                    request.getStartDate(),
                    request.getEndDate()
            );
        } else {
            if (request.getCustomPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("价格必须大于等于0");
            }
            updated = availabilityService.setCustomPrice(
                    request.getHomestayId(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getCustomPrice()
            );
        }

        Map<String, Object> result = new HashMap<>();
        result.put("updated", updated);
        result.put("homestayId", request.getHomestayId());
        result.put("startDate", request.getStartDate());
        result.put("endDate", request.getEndDate());
        result.put("customPrice", request.getCustomPrice());

        sendCalendarRefresh(host.getId(), "PRICE_UPDATED");
        return result;
    }

    private void sendCalendarRefresh(Long hostId, String type) {
        if (messagingTemplate != null) {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", type);
                payload.put("timestamp", java.time.Instant.now().toString());
                messagingTemplate.convertAndSend("/topic/calendar/" + hostId, payload);
            } catch (Exception e) {
                // WebSocket 推送失败不影响主流程
            }
        }
    }

    private Map<String, HostCalendarDayDTO> buildBaseDays(List<Homestay> homestays, LocalDate startDate, LocalDate endDate) {
        Map<String, HostCalendarDayDTO> dayMap = new LinkedHashMap<>();
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            for (Homestay homestay : homestays) {
                HostCalendarDayDTO dto = HostCalendarDayDTO.builder()
                        .date(date)
                        .homestayId(homestay.getId())
                        .homestayTitle(homestay.getTitle())
                        .status(STATUS_AVAILABLE)
                        .checkIn(false)
                        .checkOut(false)
                        .basePrice(homestay.getPrice())
                        .finalPrice(homestay.getPrice())
                        .build();
                dayMap.put(dayKey(homestay.getId(), date), dto);
            }
        }
        return dayMap;
    }

    private void applyAvailability(
            Map<String, HostCalendarDayDTO> dayMap,
            List<Homestay> homestays,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<Long> homestayIds = homestays.stream().map(Homestay::getId).toList();
        List<HomestayAvailability> availabilityList = availabilityRepository
                .findByHomestayIdsAndDateRange(homestayIds, startDate, endDate);
        LocalDateTime now = LocalDateTime.now();

        for (HomestayAvailability availability : availabilityList) {
            HostCalendarDayDTO day = dayMap.get(dayKey(availability.getHomestayId(), availability.getDate()));
            if (day == null) {
                continue;
            }

            if (Boolean.TRUE.equals(availability.getLocked())
                    && availability.getLockExpiresAt() != null
                    && availability.getLockExpiresAt().isAfter(now)) {
                day.setStatus(STATUS_LOCKED);
                day.setSource("SYSTEM");
            }

            if (availability.getStatus() == HomestayAvailability.AvailabilityStatus.UNAVAILABLE) {
                day.setStatus(STATUS_UNAVAILABLE);
                day.setSource(availability.getSource());
                day.setReason(availability.getReason());
                day.setNote(availability.getNote());
            } else if (availability.getStatus() == HomestayAvailability.AvailabilityStatus.BOOKED) {
                day.setStatus(STATUS_BOOKED);
                day.setSource(availability.getSource() != null ? availability.getSource() : "ORDER");
            }

            if (availability.getCustomPrice() != null) {
                day.setFinalPrice(availability.getCustomPrice());
            }
        }
    }

    private List<Order> applyOrders(
            Map<String, HostCalendarDayDTO> dayMap,
            Long hostId,
            Long homestayId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<Order> orders = orderRepository.findHostCalendarOrders(
                hostId,
                homestayId,
                startDate,
                endDate,
                OCCUPYING_ORDER_STATUSES
        );

        for (Order order : orders) {
            LocalDate occupyStart = max(order.getCheckInDate(), startDate);
            LocalDate occupyEnd = min(order.getCheckOutDate(), endDate);
            for (LocalDate date = occupyStart; date.isBefore(occupyEnd); date = date.plusDays(1)) {
                HostCalendarDayDTO day = dayMap.get(dayKey(order.getHomestay().getId(), date));
                if (day == null) {
                    continue;
                }
                applyOrderToDay(day, order, date);
            }

            if (!order.getCheckOutDate().isBefore(startDate) && order.getCheckOutDate().isBefore(endDate)) {
                HostCalendarDayDTO checkoutDay = dayMap.get(dayKey(order.getHomestay().getId(), order.getCheckOutDate()));
                if (checkoutDay != null) {
                    checkoutDay.setCheckOut(true);
                }
            }
        }

        return orders;
    }

    private void applyOrderToDay(HostCalendarDayDTO day, Order order, LocalDate date) {
        day.setStatus(mapOrderStatus(order.getStatus()));
        day.setSource("ORDER");
        day.setOrderId(order.getId());
        day.setOrderNumber(order.getOrderNumber());
        day.setGuestName(resolveGuestName(order));
        day.setCheckIn(date.equals(order.getCheckInDate()));
        day.setCheckOut(Boolean.TRUE.equals(day.getCheckOut()));
    }

    private String mapOrderStatus(String orderStatus) {
        if (OrderStatus.PENDING.name().equals(orderStatus)) {
            return STATUS_PENDING_CONFIRM;
        }
        if (OrderStatus.CHECKED_IN.name().equals(orderStatus)) {
            return STATUS_CHECKED_IN;
        }
        if (OrderStatus.CHECKED_OUT.name().equals(orderStatus)) {
            return STATUS_CHECKED_OUT;
        }
        return STATUS_BOOKED;
    }

    private BigDecimal computeEstimatedRevenue(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        return orders.stream()
                .filter(order -> !OrderStatus.PENDING.name().equals(order.getStatus()))
                .filter(order -> !order.getCheckInDate().isBefore(startDate) && order.getCheckInDate().isBefore(endDate))
                .map(order -> order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void assertNoOrderConflict(Long hostId, Long homestayId, LocalDate startDate, LocalDate endDate) {
        List<Order> conflicts = orderRepository.findHostCalendarOrders(
                hostId,
                homestayId,
                startDate,
                endDate,
                OCCUPYING_ORDER_STATUSES
        );
        if (!conflicts.isEmpty()) {
            String orderNumbers = conflicts.stream()
                    .map(Order::getOrderNumber)
                    .collect(Collectors.joining("、"));
            throw new IllegalArgumentException("所选日期存在订单占用：" + orderNumbers);
        }
    }

    private HomestayAvailability.AvailabilityStatus parseTargetStatus(String status) {
        if (HomestayAvailability.AvailabilityStatus.AVAILABLE.name().equals(status)) {
            return HomestayAvailability.AvailabilityStatus.AVAILABLE;
        }
        if (HomestayAvailability.AvailabilityStatus.UNAVAILABLE.name().equals(status)) {
            return HomestayAvailability.AvailabilityStatus.UNAVAILABLE;
        }
        throw new IllegalArgumentException("日历状态只能设置为 AVAILABLE 或 UNAVAILABLE");
    }

    private User getHost(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("房东用户不存在: " + username));
    }

    private List<Homestay> getHostHomestays(User host, Long homestayId) {
        if (homestayId == null) {
            return homestayRepository.findByOwnerId(host.getId());
        }

        Homestay homestay = homestayRepository.findById(homestayId)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在"));
        if (homestay.getOwner() == null || !host.getId().equals(homestay.getOwner().getId())) {
            throw new AccessDeniedException("只能查看自己的房源日历");
        }
        return List.of(homestay);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("开始日期和结束日期不能为空");
        }
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("结束日期必须晚于开始日期");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("查询日期范围不能超过一年");
        }
    }

    public byte[] exportCalendarCsv(String username, Long homestayId, LocalDate startDate, LocalDate endDate) {
        HostCalendarResponse response = getCalendarDays(username, homestayId, startDate, endDate);
        StringBuilder csv = new StringBuilder();
        csv.append("\uFEFF");
        csv.append("日期,房源ID,房源名称,状态,来源,原因,订单号,客人名,入住,退房,基础价格,最终价格\n");

        for (HostCalendarDayDTO day : response.getDays()) {
            csv.append(day.getDate()).append(",");
            csv.append(day.getHomestayId()).append(",");
            csv.append(escapeCsv(day.getHomestayTitle())).append(",");
            csv.append(day.getStatus()).append(",");
            csv.append(day.getSource() != null ? day.getSource() : "").append(",");
            csv.append(escapeCsv(day.getReason())).append(",");
            csv.append(day.getOrderNumber() != null ? day.getOrderNumber() : "").append(",");
            csv.append(escapeCsv(day.getGuestName())).append(",");
            csv.append(Boolean.TRUE.equals(day.getCheckIn()) ? "是" : "否").append(",");
            csv.append(Boolean.TRUE.equals(day.getCheckOut()) ? "是" : "否").append(",");
            csv.append(day.getBasePrice() != null ? day.getBasePrice() : "").append(",");
            csv.append(day.getFinalPrice() != null ? day.getFinalPrice() : "").append("\n");
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String resolveGuestName(Order order) {
        User guest = order.getGuest();
        if (guest == null) {
            return "";
        }
        if (guest.getNickname() != null && !guest.getNickname().isBlank()) {
            return guest.getNickname();
        }
        return guest.getUsername();
    }

    private String dayKey(Long homestayId, LocalDate date) {
        return homestayId + "#" + date;
    }

    private LocalDate max(LocalDate left, LocalDate right) {
        return left.isAfter(right) ? left : right;
    }

    private LocalDate min(LocalDate left, LocalDate right) {
        return left.isBefore(right) ? left : right;
    }
}
