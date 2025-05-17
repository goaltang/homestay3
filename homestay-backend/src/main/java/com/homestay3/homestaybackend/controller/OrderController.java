package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.OrderDTO;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.Order;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.model.PaymentStatus;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.homestay3.homestaybackend.exception.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
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
            if (role.equals("ROLE_HOST") || role.equals("ROLE_LANDLORD")) {
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
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
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
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> requestBody,
            Authentication authentication) {
        try {
            String reason = requestBody != null ? requestBody.get("reason") : null;
            
            // 获取用户角色，以确定是用户取消还是房东取消
            String role = authentication.getAuthorities().iterator().next().getAuthority();
            String cancelType;
            
            if (role.equals("ROLE_HOST")) {
                cancelType = OrderStatus.CANCELLED_BY_HOST.name();
            } else {
                cancelType = OrderStatus.CANCELLED_BY_USER.name();
            }
            
            OrderDTO cancelledOrder = orderService.cancelOrderWithReason(id, cancelType, reason);
            return ResponseEntity.ok(cancelledOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "errorCode", e instanceof IllegalArgumentException ? "ORDER_CANCELLATION_NOT_ALLOWED" : "INTERNAL_SERVER_ERROR"
            ));
        }
    }
    
    @PutMapping("/{id}/pay")
    public ResponseEntity<?> payOrder(
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody) {
        log.info("收到订单 ID: {} 的支付请求，请求体: {}", id, requestBody);
        try {
            String paymentMethod = requestBody.getOrDefault("paymentMethod", "default");
            log.debug("提取支付方式: {}", paymentMethod);
            
            // 首先将订单状态设置为支付中 (这一步在业务逻辑中可能不是必需的，取决于具体流程)
            // OrderDTO paymentPendingOrder = orderService.updateOrderStatus(id, OrderStatus.PAYMENT_PENDING.name());
            // log.debug("订单 {} 状态更新为 PENDING_PAYMENT (临时)", id);
            
            // 然后处理支付
            log.info("准备调用 OrderService 处理订单 ID: {} 的支付...", id);
            OrderDTO paidOrder = orderService.payOrder(id, paymentMethod);
            log.info("OrderService 支付处理完成，订单 ID: {}, 返回结果: {}", id, paidOrder);
            return ResponseEntity.ok(paidOrder);
        } catch (IllegalArgumentException | AccessDeniedException e) {
            // 处理业务逻辑或权限错误
            log.warn("处理订单 {} 支付失败 (业务/权限错误): {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "errorCode", "PAYMENT_VALIDATION_ERROR"
            ));
        } catch (Exception e) {
            // 处理其他意外错误
            log.error("处理订单 {} 支付时发生意外错误: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "支付处理失败，请稍后重试",
                "errorCode", "INTERNAL_SERVER_ERROR"
            ));
        }
    }
    
    @GetMapping("/pending/count")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
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
            order.setPaymentStatus(PaymentStatus.PAID);
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
        log.info("收到为订单 ID: {} 生成支付二维码的请求，支付方式: {}", orderId, paymentInfo.get("method"));
        try {
            String method = paymentInfo.get("method");
            if (method == null) {
                log.warn("订单 {} 生成二维码请求缺少支付方式", orderId);
                return ResponseEntity.badRequest().body(Map.of("error", "支付方式不能为空"));
            }
            
            // --- 新增：将订单状态更新为待支付 ---
            try {
                log.info("准备将订单 ID: {} 状态更新为 PAYMENT_PENDING...", orderId);
                orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_PENDING.name());
                log.info("订单 ID: {} 状态已更新为 PAYMENT_PENDING", orderId);
            } catch (Exception e) {
                // 如果更新状态失败，记录错误但仍尝试生成二维码，让用户有机会重试支付
                log.error("更新订单 {} 状态为 PAYMENT_PENDING 时失败: {}，将继续生成二维码", orderId, e.getMessage(), e);
                // 不在此处中断流程，允许用户获取二维码
            }
            // --- 状态更新结束 ---
            
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
            log.info("为订单 ID: {} 生成二维码 URL 成功: {}", orderId, qrCodeUrl);
            
            return ResponseEntity.ok(Map.of("qrCode", qrCodeUrl));
        } catch (Exception e) {
            log.error("为订单 {} 生成支付二维码时发生意外错误: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "生成支付二维码失败，请稍后重试",
                "errorCode", "QRCODE_GENERATION_ERROR"
            ));
        }
    }
    
    /**
     * 检查支付状态 (改造为确认支付)
     */
    @GetMapping("/{orderId}/payment/status")
    public ResponseEntity<?> checkPaymentStatus(@PathVariable Long orderId) {
        log.info("收到检查订单 ID: {} 支付状态的请求", orderId);
        try {
            OrderDTO order = orderService.getOrderById(orderId); // 获取最新订单信息
            log.debug("获取到订单 {} 的当前状态: status={}, paymentStatus={}", orderId, order.getStatus(), order.getPaymentStatus());

            // --- 修改核心逻辑：只检查状态，不触发支付 ---
            boolean isPaid = OrderStatus.PAID.name().equals(order.getStatus()) ||
                             (order.getPaymentStatus() != null && PaymentStatus.PAID.name().equals(order.getPaymentStatus())); // 检查 status 或 paymentStatus (添加 null 检查)

            if (isPaid) {
                log.info("订单 {} 状态已为 PAID，返回支付成功", orderId);
                return ResponseEntity.ok(Map.of("paid", true));
            } else {
                // 对于其他所有状态 (PENDING, CONFIRMED, PAYMENT_PENDING, FAILED, UNPAID, CANCELLED 等)
                log.info("订单 {} 状态为 status={}, paymentStatus={}，返回支付未完成", orderId, order.getStatus(), order.getPaymentStatus());
                return ResponseEntity.ok(Map.of("paid", false));
            }
            // --- 移除原先处理 PAYMENT_PENDING 并调用 orderService.payOrder() 的逻辑 ---

        } catch (ResourceNotFoundException e) {
             log.warn("检查支付状态失败，未找到订单 ID: {}", orderId);
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("paid", false, "error", "订单不存在"));
        } catch (Exception e) {
            log.error("检查订单 {} 支付状态时发生意外错误: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "paid", false,
                "error", "检查支付状态失败，请稍后重试"
            ));
        }
    }
    
    @GetMapping("/host")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
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
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
    public ResponseEntity<?> confirmOrder(@PathVariable Long id) {
        try {
            System.out.println("开始确认订单，订单ID: " + id);
            // 获取当前用户信息用于日志
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            System.out.println("当前用户: " + username + ", 角色: " + authentication.getAuthorities());
            
            OrderDTO confirmedOrder = orderService.updateOrderStatus(id, OrderStatus.CONFIRMED.name());
            System.out.println("订单确认成功，订单ID: " + id);
            return ResponseEntity.ok(confirmedOrder);
        } catch (ResourceNotFoundException e) {
            System.err.println("订单确认失败，订单不存在: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
            System.err.println("订单确认失败，权限不足: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            System.err.println("订单确认失败，参数错误: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("订单确认失败，未知错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "确认订单时发生错误: " + e.getMessage()));
        }
    }
    
    /**
     * 拒绝订单
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
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
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
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
    
    /**
     * 办理入住
     */
    @PutMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
    public ResponseEntity<?> checkIn(@PathVariable Long id) {
        try {
            OrderDTO checkedInOrder = orderService.updateOrderStatus(id, OrderStatus.CHECKED_IN.name());
            return ResponseEntity.ok(checkedInOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "errorCode", e instanceof IllegalArgumentException ? "INVALID_STATUS_TRANSITION" : "INTERNAL_SERVER_ERROR"
            ));
        }
    }
    
    /**
     * 办理退房
     */
    @PutMapping("/{id}/check-out")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
    public ResponseEntity<?> checkOut(@PathVariable Long id) {
        try {
            OrderDTO completedOrder = orderService.updateOrderStatus(id, OrderStatus.COMPLETED.name());
            return ResponseEntity.ok(completedOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "errorCode", e instanceof IllegalArgumentException ? "INVALID_STATUS_TRANSITION" : "INTERNAL_SERVER_ERROR"
            ));
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