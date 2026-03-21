package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.CheckOutDTO;

import java.math.BigDecimal;

/**
 * 退房服务接口
 * 封装退房结算流程相关的业务逻辑
 */
public interface CheckOutService {

    /**
     * 办理退房
     * 将订单状态从 CHECKED_IN 转为 CHECKED_OUT，并创建退房记录
     *
     * @param orderId 订单ID
     * @param checkOutDTO 退房信息DTO
     * @return 退房记录DTO
     */
    CheckOutDTO performCheckOut(Long orderId, CheckOutDTO checkOutDTO);

    /**
     * 自助退房（退房日自动触发或客人主动）
     *
     * @param orderId 订单ID
     * @return 退房记录DTO
     */
    CheckOutDTO selfCheckOut(Long orderId);

    /**
     * 处理押金操作
     *
     * @param orderId 订单ID
     * @param action 操作类型: COLLECT(收取), REFUND(退还), RETAIN(扣押), WAIVE(免除)
     * @param amount 金额
     * @param note 备注
     * @return 退房记录DTO
     */
    CheckOutDTO processDeposit(Long orderId, String action, BigDecimal amount, String note);

    /**
     * 获取退房记录
     *
     * @param orderId 订单ID
     * @return 退房记录DTO
     */
    CheckOutDTO getCheckOutRecord(Long orderId);

    /**
     * 确认结算
     * 将订单状态从 CHECKED_OUT 转为 COMPLETED
     *
     * @param orderId 订单ID
     * @return 退房记录DTO
     */
    CheckOutDTO confirmSettlement(Long orderId);

    /**
     * 更新额外费用
     *
     * @param orderId 订单ID
     * @param extraCharges 额外费用
     * @param description 费用说明
     * @return 退房记录DTO
     */
    CheckOutDTO updateExtraCharges(Long orderId, BigDecimal extraCharges, String description);

    /**
     * 校验用户是否有权访问订单的退房信息
     *
     * @param orderId 订单ID
     * @param username 用户名
     * @throws AccessDeniedException 如果无权访问
     */
    void validateAccess(Long orderId, String username);
}
