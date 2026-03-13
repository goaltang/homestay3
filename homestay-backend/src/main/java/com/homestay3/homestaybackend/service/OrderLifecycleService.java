package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.OrderDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * 订单核心生命周期服务
 * 封装订单的创建、查询和核心状态转换业务逻辑
 */
public interface OrderLifecycleService {

    /**
     * 创建订单
     * @param orderDTO 订单数据传输对象
     * @return 创建后的订单DTO
     */
    OrderDTO createOrder(OrderDTO orderDTO);

    /**
     * 根据ID获取订单
     * @param id 订单ID
     * @return 订单DTO
     */
    OrderDTO getOrderById(Long id);

    /**
     * 根据订单号获取订单
     * @param orderNumber 订单号
     * @return 订单DTO
     */
    OrderDTO getOrderByOrderNumber(String orderNumber);

    /**
     * 获取当前用户的订单列表（分页）
     * @param params 查询参数
     * @param pageable 分页信息
     * @return 订单DTO的分页结果
     */
    Page<OrderDTO> getMyOrders(Map<String, String> params, Pageable pageable);

    /**
     * 获取指定房东的订单列表（分页）
     * @param ownerUsername 房东用户名
     * @param params 查询参数
     * @param pageable 分页信息
     * @return 订单DTO的分页结果
     */
    Page<OrderDTO> getOwnerOrders(String ownerUsername, Map<String, String> params, Pageable pageable);

    /**
     * 获取指定房东的待处理订单数量
     * @param ownerUsername 房东用户名
     * @return 待处理订单数量
     */
    Long getPendingOrderCount(String ownerUsername);

    /**
     * 确认订单（房东操作）
     * @param id 订单ID
     * @return 确认后的订单DTO
     */
    OrderDTO confirmOrder(Long id);

    /**
     * 拒绝订单（房东操作）
     * @param id 订单ID
     * @param reason 拒绝原因
     * @return 拒绝后的订单DTO
     */
    OrderDTO rejectOrder(Long id, String reason);

    /**
     * 办理入住（房东操作）
     * @param id 订单ID
     * @return 入住后的订单DTO
     */
    OrderDTO checkIn(Long id);

    /**
     * 办理退房（房东操作）
     * @param id 订单ID
     * @return 退房后的订单DTO
     */
    OrderDTO checkOut(Long id);

    /**
     * 取消订单
     * @param id 订单ID
     * @return 取消后的订单DTO
     */
    OrderDTO cancelOrder(Long id);

    /**
     * 根据原因取消订单
     * @param id 订单ID
     * @param cancelType 取消类型
     * @param reason 取消原因
     * @return 取消后的订单DTO
     */
    OrderDTO cancelOrderWithReason(Long id, String cancelType, String reason);

    /**
     * 更新订单状态（通用方法）
     * @param id 订单ID
     * @param status 目标状态
     * @return 更新后的订单DTO
     */
    OrderDTO updateOrderStatus(Long id, String status);

    /**
     * 获取房东订单统计信息
     * @return 订单统计信息
     */
    Object getHostOrderStats();
}