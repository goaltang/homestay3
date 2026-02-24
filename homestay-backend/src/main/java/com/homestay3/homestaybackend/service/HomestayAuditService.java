package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.AuditLogDTO;
import com.homestay3.homestaybackend.dto.ReviewRequest;
import com.homestay3.homestaybackend.entity.HomestayAuditLog;
import com.homestay3.homestaybackend.model.HomestayStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 房源审核服务接口
 * 提供房源审核相关的业务逻辑
 */
public interface HomestayAuditService {

    /**
     * 执行房源审核操作
     * @param homestayId 房源ID
     * @param reviewRequest 审核请求
     * @param reviewerUsername 审核员用户名
     * @param ipAddress 客户端IP地址
     * @param userAgent 用户代理信息
     * @return 审核记录DTO
     */
    AuditLogDTO reviewHomestay(Long homestayId, ReviewRequest reviewRequest, 
                              String reviewerUsername, String ipAddress, String userAgent);

    /**
     * 提交房源审核
     * @param homestayId 房源ID
     * @param ownerUsername 房东用户名
     * @return 审核记录DTO
     */
    AuditLogDTO submitHomestayForReview(Long homestayId, String ownerUsername);

    /**
     * 撤回房源审核申请
     * @param homestayId 房源ID
     * @param ownerUsername 房东用户名
     * @return 审核记录DTO
     */
    AuditLogDTO withdrawHomestayReview(Long homestayId, String ownerUsername);



    /**
     * 获取房源审核历史
     * @param homestayId 房源ID
     * @param pageable 分页参数
     * @return 审核记录分页数据
     */
    Page<AuditLogDTO> getHomestayAuditHistory(Long homestayId, Pageable pageable);



    /**
     * 获取指定时间范围内的审核统计
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 审核统计数据
     */
    List<Object[]> getAuditStatistics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 检查状态转换是否合法
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return 是否可以转换
     */
    boolean canTransitionStatus(HomestayStatus currentStatus, HomestayStatus targetStatus);

    /**
     * 记录状态变更
     * @param homestayId 房源ID
     * @param oldStatus 原状态
     * @param newStatus 新状态
     * @param actionType 操作类型
     * @param reviewerUsername 操作人用户名
     * @param reason 变更原因
     * @param notes 备注
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     * @return 审核记录
     */
    HomestayAuditLog logStatusChange(Long homestayId, HomestayStatus oldStatus, HomestayStatus newStatus,
                                   HomestayAuditLog.AuditActionType actionType, String reviewerUsername,
                                   String reason, String notes, String ipAddress, String userAgent);

    /**
     * 获取待审核房源数量
     * @return 待审核房源数量
     */
    long getPendingReviewCount();

    /**
     * 获取全局审核历史记录
     * @param pageable 分页参数
     * @param actionType 操作类型过滤
     * @param reviewerName 审核员名称过滤
     * @param homestayId 房源ID过滤
     * @param startTime 开始时间过滤
     * @param endTime 结束时间过滤
     * @return 审核记录分页数据
     */
    Page<AuditLogDTO> getAllAuditHistory(Pageable pageable, String actionType, String reviewerName, 
                                        String homestayId, LocalDateTime startTime, LocalDateTime endTime);

} 