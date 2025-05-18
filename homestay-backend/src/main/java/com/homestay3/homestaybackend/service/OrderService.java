package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.OrderDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface OrderService {
    
    // 创建订单
    OrderDTO createOrder(OrderDTO orderDTO);
    
    // 获取订单详情
    OrderDTO getOrderById(Long id);
    
    // 获取订单详情（根据订单号）
    OrderDTO getOrderByOrderNumber(String orderNumber);
    
    // 获取当前用户的订单列表
    Page<OrderDTO> getMyOrders(Map<String, String> params, Pageable pageable);
    
    // 获取指定房东的订单列表
    Page<OrderDTO> getOwnerOrders(String ownerUsername, Map<String, String> params, Pageable pageable);
    
    // 获取房东待处理订单数量
    Long getPendingOrderCount(String ownerUsername);
    
    // 更新订单状态
    OrderDTO updateOrderStatus(Long id, String status);
    
    // 取消订单
    OrderDTO cancelOrder(Long id);
    
    // 带原因和类型的取消订单方法
    OrderDTO cancelOrderWithReason(Long id, String cancelType, String reason);
    
    // 支付订单
    OrderDTO payOrder(Long id);
    
    // 使用指定支付方式支付订单
    OrderDTO payOrder(Long id, String paymentMethod);
    
    // 创建订单预览
    OrderDTO createOrderPreview(OrderDTO orderDTO);
    
    // 拒绝订单
    OrderDTO rejectOrder(Long id, String reason);
    
    // 管理员获取订单列表（分页和筛选） - 使用更具体的参数
    Page<OrderDTO> getAdminOrders(
            Pageable pageable,
            String orderNumber, 
            String guestName,
            String homestayTitle,
            String status, 
            String paymentStatus,
            String paymentMethod,
            String hostName, // 按房东名称筛选
            LocalDate checkInDateStart, // 按入住日期范围筛选
            LocalDate checkInDateEnd,
            LocalDate createTimeStart, // 按创建日期范围筛选
            LocalDate createTimeEnd
            // 可以根据需要添加更多筛选参数, 如 totalAmountMin/Max 等
    );
    
    // 管理员手动确认支付
    OrderDTO confirmPayment(Long id);
    
    // 管理员发起退款 (可能需要更复杂的参数)
    OrderDTO initiateRefund(Long id /*, RefundRequest refundRequest */);
    
    // 管理员删除订单
    void deleteOrder(Long id);
    
    // 管理员导出订单
    byte[] exportOrders(Map<String, String> params);
} 