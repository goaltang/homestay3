package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.OrderDTO;
import com.homestay3.homestaybackend.service.OrderService;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private static final Logger logger = LoggerFactory.getLogger(AdminOrderController.class);
    private final OrderService orderService;

    /**
     * 获取订单列表，支持分页和更丰富的筛选
     */
    @GetMapping
    public ResponseEntity<?> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) String guestName,
            @RequestParam(required = false) String homestayTitle,
            @RequestParam(required = false) String status, // 订单状态
            @RequestParam(required = false) String paymentStatus, // 支付状态
            @RequestParam(required = false) String paymentMethod, // 支付方式
            @RequestParam(required = false) String hostName, // 房东名称
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDateEnd,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createTimeStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createTimeEnd,
            @RequestParam(defaultValue = "createdAt,desc") String sort // 排序字段和方向
    ) {
        logger.info("管理员获取订单列表，页码: {}, 每页数量: {}, 排序: {}", page, size, sort);
        logger.info("筛选条件 - orderNumber: {}, guestName: {}, homestayTitle: {}, status: {}, paymentStatus: {}, paymentMethod: {}, hostName: {}, checkInDateStart: {}, checkInDateEnd: {}, createTimeStart: {}, createTimeEnd: {}",
                orderNumber, guestName, homestayTitle, status, paymentStatus, paymentMethod, hostName, checkInDateStart, checkInDateEnd, createTimeStart, createTimeEnd);
        
        // 解析排序参数
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && "desc".equalsIgnoreCase(sortParams[1]) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String property = sortParams[0];
        // 添加对DTO中字段的映射，例如将hostName映射到实体关联字段
        if ("hostName".equals(property)) {
            property = "homestay.owner.username"; // 假设按房东用户名排序
        } else if ("guestName".equals(property)) {
            property = "guest.username";
        } else if ("createTime".equals(property)) { // 新增映射
            property = "createdAt"; 
        } // 可以添加更多映射
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, property));
        
        // 调用更新后的Service方法
        Page<OrderDTO> orders = orderService.getAdminOrders(
                pageable,
                orderNumber, 
                guestName,
                homestayTitle,
                status, 
                paymentStatus,
                paymentMethod,
                hostName, 
                checkInDateStart, 
                checkInDateEnd,
                createTimeStart, 
                createTimeEnd
        );
        
        // 返回格式与之前保持一致
        Map<String, Object> response = new HashMap<>();
        response.put("content", orders.getContent());
        response.put("totalElements", orders.getTotalElements());
        response.put("totalPages", orders.getTotalPages());
        response.put("page", orders.getNumber());
        response.put("size", orders.getSize());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderDetail(@PathVariable Long id) {
        logger.info("管理员获取订单详情，ID: {}", id);
        OrderDTO order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    /**
     * 更新订单状态
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> statusData
    ) {
        String status = (String) statusData.get("status");
        logger.info("管理员更新订单状态，ID: {}, 状态: {}", id, status);
        OrderDTO updatedOrder = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(updatedOrder);
    }

    /**
     * 管理员手动确认订单支付
     */
    @PutMapping("/{id}/confirm-payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> confirmOrderPayment(@PathVariable Long id) {
        logger.info("管理员手动确认订单支付，ID: {}", id);
        try {
            OrderDTO updatedOrder = orderService.confirmPayment(id);
            return ResponseEntity.ok(updatedOrder);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("手动确认支付失败，订单ID: {}", id, e);
            return ResponseEntity.status(500).body(Map.of("error", "确认支付失败: " + e.getMessage()));
        }
    }

    /**
     * 管理员发起退款
     */
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> initiateOrderRefund(@PathVariable Long id /*, @RequestBody RefundRequestDto refundRequest */) {
        logger.info("管理员发起退款，订单ID: {}", id);
        try {
            // 注意：这里的实现只是标记状态，实际退款需要对接支付网关
            OrderDTO updatedOrder = orderService.initiateRefund(id);
            return ResponseEntity.ok(updatedOrder);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("发起退款失败，订单ID: {}", id, e);
            return ResponseEntity.status(500).body(Map.of("error", "发起退款失败: " + e.getMessage()));
        }
    }

    /**
     * 删除订单
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable Long id) {
        logger.info("管理员删除订单，ID: {}", id);
        orderService.deleteOrder(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 导出订单
     */
    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportOrders(
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        logger.info("管理员导出订单，订单号: {}, 状态: {}, 开始日期: {}, 结束日期: {}", 
                orderNumber, status, startDate, endDate);
        
        Map<String, String> params = new HashMap<>();
        if (orderNumber != null) params.put("orderNumber", orderNumber);
        if (status != null) params.put("status", status);
        if (startDate != null) params.put("startDate", startDate);
        if (endDate != null) params.put("endDate", endDate);
        
        byte[] data = orderService.exportOrders(params);
        String filename = "orders_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }
} 