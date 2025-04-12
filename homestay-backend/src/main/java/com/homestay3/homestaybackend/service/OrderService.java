package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.OrderDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    
    // 支付订单
    OrderDTO payOrder(Long id);
    
    // 创建订单预览
    OrderDTO createOrderPreview(OrderDTO orderDTO);
    
    // 拒绝订单
    OrderDTO rejectOrder(Long id, String reason);
    
    // 管理员获取订单列表（分页和筛选）
    Page<OrderDTO> getAdminOrders(Pageable pageable, Map<String, String> params);
    
    // 管理员删除订单
    void deleteOrder(Long id);
    
    // 管理员导出订单
    byte[] exportOrders(Map<String, String> params);
} 