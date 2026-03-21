package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.CheckInCredentialDTO;
import com.homestay3.homestaybackend.dto.CheckInDTO;

/**
 * 入住服务接口
 * 封装入住流程相关的业务逻辑
 */
public interface CheckInService {

    /**
     * 房东设置准备入住（生成入住凭证）
     * 将订单状态从 PAID 转为 READY_FOR_CHECKIN，并创建入住记录
     *
     * @param orderId 订单ID
     * @param credentialDTO 入住凭证信息
     * @return 入住记录DTO
     */
    CheckInDTO prepareCheckIn(Long orderId, CheckInCredentialDTO credentialDTO);

    /**
     * 获取入住凭证
     * 权限: 房东、订单客人
     *
     * @param orderId 订单ID
     * @return 入住凭证DTO
     */
    CheckInCredentialDTO getCheckInCredential(Long orderId);

    /**
     * 办理入住（房东/管理员手动操作）
     * 将订单状态从 READY_FOR_CHECKIN 转为 CHECKED_IN
     *
     * @param orderId 订单ID
     * @return 入住记录DTO
     */
    CheckInDTO performCheckIn(Long orderId);

    /**
     * 自助入住（输入入住码）
     * 客人通过输入入住码办理入住
     *
     * @param checkInCode 入住码
     * @return 入住记录DTO
     */
    CheckInDTO selfCheckIn(String checkInCode);

    /**
     * 客人确认到达
     * 通知房东已到达，等待办理入住
     *
     * @param orderId 订单ID
     * @return 入住记录DTO
     */
    CheckInDTO confirmArrival(Long orderId);

    /**
     * 取消准备入住
     * 将订单状态从 READY_FOR_CHECKIN 退回 PAID
     *
     * @param orderId 订单ID
     * @return 入住记录DTO
     */
    CheckInDTO cancelPreparation(Long orderId);

    /**
     * 获取入住记录
     *
     * @param orderId 订单ID
     * @return 入住记录DTO
     */
    CheckInDTO getCheckInRecord(Long orderId);

    /**
     * 验证入住码是否有效
     *
     * @param orderId 订单ID
     * @param checkInCode 入住码
     * @return 是否有效
     */
    boolean validateCheckInCode(Long orderId, String checkInCode);

    /**
     * 校验用户是否有权访问订单的入住信息
     *
     * @param orderId 订单ID
     * @param username 用户名
     * @throws AccessDeniedException 如果无权访问
     */
    void validateAccess(Long orderId, String username);
}
