package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.OrderDTO;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.Order;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderDTO orderDTO) {
        try {
            OrderDTO createdOrder = orderService.createOrder(orderDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        try {
            Map<String, String> params = new HashMap<>();
            if (status != null) {
                params.put("status", status);
            }
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<OrderDTO> orders;
            
            // 根据用户角色返回不同的订单列表
            String role = authentication.getAuthorities().iterator().next().getAuthority();
            if (role.equals("ROLE_HOST")) {
                String username = authentication.getName();
                orders = orderService.getOwnerOrders(username, params, pageable);
            } else {
                orders = orderService.getMyOrders(params, pageable);
            }
            
            return ResponseEntity.ok(Map.of("data", orders));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        try {
            OrderDTO order = orderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<?> getOrderByNumber(@PathVariable String orderNumber) {
        try {
            OrderDTO order = orderService.getOrderByOrderNumber(orderNumber);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusMap) {
        try {
            String status = statusMap.get("status");
            if (status == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "状态参数不能为空"));
            }
            
            OrderDTO updatedOrder = orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        try {
            OrderDTO cancelledOrder = orderService.cancelOrder(id);
            return ResponseEntity.ok(cancelledOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/pay")
    public ResponseEntity<?> payOrder(@PathVariable Long id) {
        try {
            OrderDTO paidOrder = orderService.payOrder(id);
            return ResponseEntity.ok(paidOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/pending/count")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<?> getPendingOrderCount(Authentication authentication) {
        try {
            String username = authentication.getName();
            Long count = orderService.getPendingOrderCount(username);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 创建订单预览，用于在用户确认前生成订单信息
     */
    @PostMapping("/preview")
    public ResponseEntity<?> createOrderPreview(@RequestBody OrderDTO orderDTO) {
        try {
            OrderDTO previewOrder = orderService.createOrderPreview(orderDTO);
            return ResponseEntity.ok(previewOrder);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "创建订单预览失败: " + e.getMessage()));
        }
    }
    
    /**
     * 模拟支付成功（仅用于测试）
     */
    @PostMapping("/{orderId}/payment/mock")
    public ResponseEntity<?> mockPayment(@PathVariable Long orderId) {
        try {
            // 直接查询订单并更新状态，跳过权限检查
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
            
            order.setStatus(OrderStatus.PAID.name());
            Order updatedOrder = orderRepository.save(order);
            
            OrderDTO orderDTO = convertToDTO(updatedOrder);
            
            return ResponseEntity.ok(Map.of(
                "message", "模拟支付成功", 
                "paid", true,
                "order", orderDTO
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 生成支付二维码
     */
    @PostMapping("/{orderId}/payment/qrcode")
    public ResponseEntity<?> generatePaymentQRCode(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> paymentInfo) {
        try {
            String method = paymentInfo.get("method");
            if (method == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "支付方式不能为空"));
            }
            
            // 生成支付ID
            String paymentId = UUID.randomUUID().toString();
            
            // 生成模拟二维码URL
            String qrCodeUrl;
            if ("wechat".equals(method)) {
                qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?data=wechat:pay:" + orderId + ":" + paymentId + "&size=200x200";
            } else if ("alipay".equals(method)) {
                qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?data=alipay:pay:" + orderId + ":" + paymentId + "&size=200x200";
            } else {
                qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?data=unknown:pay:" + orderId + ":" + paymentId + "&size=200x200";
            }
            
            return ResponseEntity.ok(Map.of("qrCode", qrCodeUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 检查支付状态
     */
    @GetMapping("/{orderId}/payment/status")
    public ResponseEntity<?> checkPaymentStatus(@PathVariable Long orderId) {
        try {
            OrderDTO order = orderService.getOrderById(orderId);
            boolean isPaid = OrderStatus.PAID.name().equals(order.getStatus());
            return ResponseEntity.ok(Map.of("paid", isPaid));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/host")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<?> getHostOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long homestayId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Map<String, String> params = new HashMap<>();
            if (status != null) {
                params.put("status", status);
            }
            if (homestayId != null) {
                params.put("homestayId", homestayId.toString());
            }
            if (startDate != null) {
                params.put("startDate", startDate);
            }
            if (endDate != null) {
                params.put("endDate", endDate);
            }
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<OrderDTO> orders = orderService.getOwnerOrders(username, params, pageable);
            
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 确认订单
     */
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<?> confirmOrder(@PathVariable Long id) {
        try {
            OrderDTO confirmedOrder = orderService.updateOrderStatus(id, OrderStatus.CONFIRMED.name());
            return ResponseEntity.ok(confirmedOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 拒绝订单
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<?> rejectOrder(
            @PathVariable Long id, 
            @RequestBody Map<String, String> reasonMap) {
        try {
            String reason = reasonMap.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "拒绝原因不能为空"));
            }
            
            OrderDTO rejectedOrder = orderService.rejectOrder(id, reason);
            return ResponseEntity.ok(rejectedOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 获取房东订单统计信息
     */
    @GetMapping("/host/stats")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<?> getHostOrderStats(Authentication authentication) {
        try {
            String username = authentication.getName();
            
            // 获取不同状态的订单数量
            Map<String, Long> stats = new HashMap<>();
            Long pendingCount = orderService.getPendingOrderCount(username);
            
            // 使用自定义查询获取各种状态的订单数量
            Long totalCount = orderRepository.countByHomestayOwnerUsername(username);
            Long completedCount = orderRepository.countByHomestayOwnerUsernameAndStatus(username, OrderStatus.COMPLETED.name());
            Long cancelledCount = orderRepository.countByHomestayOwnerUsernameAndStatus(username, OrderStatus.CANCELLED.name());
            Long rejectedCount = orderRepository.countByHomestayOwnerUsernameAndStatus(username, OrderStatus.REJECTED.name());
            Long confirmedCount = orderRepository.countByHomestayOwnerUsernameAndStatus(username, OrderStatus.CONFIRMED.name());
            Long checkedInCount = orderRepository.countByHomestayOwnerUsernameAndStatus(username, OrderStatus.CHECKED_IN.name());
            Long paidCount = orderRepository.countByHomestayOwnerUsernameAndStatus(username, OrderStatus.PAID.name());
            
            stats.put("total", totalCount);
            stats.put("pending", pendingCount);
            stats.put("completed", completedCount);
            stats.put("cancelled", cancelledCount);
            stats.put("rejected", rejectedCount);
            stats.put("confirmed", confirmedCount);
            stats.put("checkedIn", checkedInCount);
            stats.put("paid", paidCount);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Helper method to convert Order to OrderDTO
    private OrderDTO convertToDTO(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .homestayId(order.getHomestay().getId())
                .homestayTitle(order.getHomestay().getTitle())
                .guestId(order.getGuest().getId())
                .guestName(order.getGuest().getUsername())
                .guestPhone(order.getGuestPhone())
                .checkInDate(order.getCheckInDate())
                .checkOutDate(order.getCheckOutDate())
                .nights(order.getNights())
                .guestCount(order.getGuestCount())
                .price(order.getPrice())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .remark(order.getRemark())
                .createTime(order.getCreatedAt())
                .updateTime(order.getUpdatedAt())
                .build();
    }
} 