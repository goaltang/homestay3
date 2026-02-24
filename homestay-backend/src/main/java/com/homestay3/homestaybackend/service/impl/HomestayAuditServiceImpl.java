package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.AuditLogDTO;
import com.homestay3.homestaybackend.dto.ReviewRequest;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.HomestayAuditLog;
import com.homestay3.homestaybackend.model.HomestayStatus;
import com.homestay3.homestaybackend.repository.HomestayAuditLogRepository;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.HomestayAuditService;
import com.homestay3.homestaybackend.service.NotificationService;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 房源审核服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomestayAuditServiceImpl implements HomestayAuditService {

    private final HomestayRepository homestayRepository;
    private final HomestayAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public AuditLogDTO reviewHomestay(Long homestayId, ReviewRequest reviewRequest, 
                                     String reviewerUsername, String ipAddress, String userAgent) {
        log.info("开始审核房源，ID: {}, 操作类型: {}, 审核员: {}", 
                homestayId, reviewRequest.getActionType(), reviewerUsername);

        // 查找房源
        Homestay homestay = homestayRepository.findById(homestayId)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + homestayId));

        // 查找审核员
        User reviewer = userRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("审核员不存在: " + reviewerUsername));

        // 获取当前状态
        HomestayStatus oldStatus = homestay.getStatus();
        HomestayStatus newStatus;

        // 根据操作类型确定新状态
        switch (reviewRequest.getActionType()) {
            case APPROVE:
                newStatus = HomestayStatus.ACTIVE;
                break;
            case REJECT:
                newStatus = HomestayStatus.REJECTED;
                break;
            default:
                newStatus = HomestayStatus.valueOf(reviewRequest.getTargetStatus());
        }

        // 验证状态转换是否合法
        if (!canTransitionStatus(oldStatus, newStatus)) {
            throw new IllegalArgumentException(
                String.format("无法从状态 %s 转换到 %s", oldStatus, newStatus));
        }

        // 更新房源状态
        homestay.setStatus(newStatus);
        homestay.setUpdatedAt(LocalDateTime.now());
        homestayRepository.save(homestay);

        // 记录审核日志
        HomestayAuditLog auditLog = logStatusChange(
                homestayId, oldStatus, newStatus, reviewRequest.getActionType(),
                reviewerUsername, reviewRequest.getReviewReason(), reviewRequest.getReviewNotes(),
                ipAddress, userAgent);

        // 发送审核结果通知给房东
        sendAuditResultNotification(homestay, auditLog, reviewer.getId());

        log.info("房源审核完成，ID: {}, 状态从 {} 变更为 {}", homestayId, oldStatus, newStatus);

        return convertToDTO(auditLog);
    }

    @Override
    @Transactional
    public AuditLogDTO submitHomestayForReview(Long homestayId, String ownerUsername) {
        log.info("提交房源审核，ID: {}, 房东: {}", homestayId, ownerUsername);

        // 查找房源
        Homestay homestay = homestayRepository.findById(homestayId)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + homestayId));

        // 验证房东权限
        if (!homestay.getOwner().getUsername().equals(ownerUsername)) {
            throw new IllegalArgumentException("您不是此房源的拥有者，无权提交审核");
        }

        // 查找房东用户
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + ownerUsername));

        HomestayStatus oldStatus = homestay.getStatus();
        HomestayStatus newStatus;

        // 根据当前状态确定新状态 - 简化为统一的PENDING状态
        if (oldStatus == HomestayStatus.DRAFT || oldStatus == HomestayStatus.REJECTED) {
            newStatus = HomestayStatus.PENDING;
        } else {
            throw new IllegalArgumentException("当前状态不允许提交审核: " + oldStatus);
        }

        // 更新房源状态
        homestay.setStatus(newStatus);
        homestay.setUpdatedAt(LocalDateTime.now());
        homestayRepository.save(homestay);

        // 记录审核日志
        HomestayAuditLog auditLog = logStatusChange(
                homestayId, oldStatus, newStatus, HomestayAuditLog.AuditActionType.SUBMIT,
                ownerUsername, "房东提交审核", null, null, null);

        // 发送提交审核通知给管理员
        sendSubmitNotification(homestay);

        log.info("房源提交审核成功，ID: {}, 状态从 {} 变更为 {}", homestayId, oldStatus, newStatus);

        return convertToDTO(auditLog);
    }

    @Override
    @Transactional
    public AuditLogDTO withdrawHomestayReview(Long homestayId, String ownerUsername) {
        log.info("撤回房源审核申请，ID: {}, 房东: {}", homestayId, ownerUsername);

        // 查找房源
        Homestay homestay = homestayRepository.findById(homestayId)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + homestayId));

        // 验证房东权限
        if (!homestay.getOwner().getUsername().equals(ownerUsername)) {
            throw new IllegalArgumentException("您不是此房源的拥有者，无权撤回审核");
        }

        // 验证当前状态是否允许撤回
        HomestayStatus oldStatus = homestay.getStatus();
        if (oldStatus != HomestayStatus.PENDING) {
            throw new IllegalArgumentException("只有待审核状态的房源才能撤回申请，当前状态: " + oldStatus);
        }

        // 查找房东用户
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + ownerUsername));

        // 撤回到草稿状态
        HomestayStatus newStatus = HomestayStatus.DRAFT;

        // 更新房源状态
        homestay.setStatus(newStatus);
        homestay.setUpdatedAt(LocalDateTime.now());
        homestayRepository.save(homestay);

        // 记录审核日志
        HomestayAuditLog auditLog = logStatusChange(
                homestayId, oldStatus, newStatus, HomestayAuditLog.AuditActionType.WITHDRAW,
                ownerUsername, "房东撤回审核申请", "房东主动撤回审核申请，状态回到草稿", null, null);

        // 发送撤回通知给管理员
        sendWithdrawNotification(homestay);

        log.info("房源审核申请撤回成功，ID: {}, 状态从 {} 变更为 {}", homestayId, oldStatus, newStatus);

        return convertToDTO(auditLog);
    }

    @Override
    public Page<AuditLogDTO> getHomestayAuditHistory(Long homestayId, Pageable pageable) {
        log.info("查询房源审核历史，ID: {}", homestayId);

        Page<HomestayAuditLog> auditLogs = auditLogRepository.findByHomestayIdOrderByCreatedAtDesc(homestayId, pageable);
        
        return auditLogs.map(this::convertToDTO);
    }



    @Override
    public List<Object[]> getAuditStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        log.info("查询审核统计，时间范围: {} - {}", startTime, endTime);

        return auditLogRepository.countByActionTypeAndDateRange(startTime, endTime);
    }

    @Override
    public boolean canTransitionStatus(HomestayStatus currentStatus, HomestayStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        
        return currentStatus.canTransitionTo(targetStatus);
    }

    @Override
    @Transactional
    public HomestayAuditLog logStatusChange(Long homestayId, HomestayStatus oldStatus, HomestayStatus newStatus,
                                          HomestayAuditLog.AuditActionType actionType, String reviewerUsername,
                                          String reason, String notes, String ipAddress, String userAgent) {
        
        // 查找房源
        Homestay homestay = homestayRepository.findById(homestayId)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + homestayId));

        // 查找审核员
        User reviewer = userRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + reviewerUsername));

        // 创建审核记录
        HomestayAuditLog auditLog = HomestayAuditLog.builder()
                .homestay(homestay)
                .reviewer(reviewer)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .actionType(actionType)
                .reviewReason(reason)
                .reviewNotes(notes)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        return auditLogRepository.save(auditLog);
    }

    @Override
    public long getPendingReviewCount() {
        LocalDateTime since = LocalDateTime.now().minusDays(30); // 最近30天的待审核房源
        List<HomestayAuditLog> pendingReviews = auditLogRepository.findPendingReviewsSince(since);
        
        return pendingReviews.stream()
                .map(log -> log.getHomestay().getId())
                .distinct()
                .count();
    }

    @Override
    public Page<AuditLogDTO> getAllAuditHistory(Pageable pageable, String actionType, String reviewerName, 
                                               String homestayId, LocalDateTime startTime, LocalDateTime endTime) {
        log.info("查询全局审核历史，参数: actionType={}, reviewerName={}, homestayId={}, startTime={}, endTime={}", 
                actionType, reviewerName, homestayId, startTime, endTime);
        
        // 使用仓库方法查询，根据条件进行过滤
        Page<HomestayAuditLog> auditLogs;
        
        if (startTime != null && endTime != null) {
            // 有时间范围条件，只查询审核操作（排除SUBMIT）
            auditLogs = auditLogRepository.findByCreatedAtBetweenAndActionTypeNotOrderByCreatedAtDesc(
                startTime, endTime, HomestayAuditLog.AuditActionType.SUBMIT, pageable);
        } else {
            // 无时间范围条件，查询所有审核操作（排除SUBMIT）
            auditLogs = auditLogRepository.findByActionTypeNotOrderByCreatedAtDesc(
                HomestayAuditLog.AuditActionType.SUBMIT, pageable);
        }
        
        return auditLogs.map(this::convertToDTO);
    }



    /**
     * 将审核记录实体转换为DTO
     */
    private AuditLogDTO convertToDTO(HomestayAuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }

        return AuditLogDTO.builder()
                .id(auditLog.getId())
                .homestayId(auditLog.getHomestay().getId())
                .homestayTitle(auditLog.getHomestay().getTitle())
                .reviewerId(auditLog.getReviewer().getId())
                .reviewerName(auditLog.getReviewer().getFullName() != null ? 
                            auditLog.getReviewer().getFullName() : auditLog.getReviewer().getUsername())
                .oldStatus(auditLog.getOldStatus())
                .newStatus(auditLog.getNewStatus())
                .actionType(auditLog.getActionType())
                .reviewReason(auditLog.getReviewReason())
                .reviewNotes(auditLog.getReviewNotes())
                .createdAt(auditLog.getCreatedAt())
                .ipAddress(auditLog.getIpAddress())
                .build();
    }

    /**
     * 发送审核结果通知给房东
     */
    private void sendAuditResultNotification(Homestay homestay, HomestayAuditLog auditLog, Long reviewerId) {
        try {
            NotificationType notificationType = auditLog.getActionType() == HomestayAuditLog.AuditActionType.APPROVE 
                    ? NotificationType.HOMESTAY_APPROVED 
                    : NotificationType.HOMESTAY_REJECTED;
            
            String content = String.format("您的房源《%s》%s。%s", 
                    homestay.getTitle(),
                    auditLog.getActionType() == HomestayAuditLog.AuditActionType.APPROVE ? "审核通过" : "审核未通过",
                    auditLog.getReviewReason() != null ? "原因：" + auditLog.getReviewReason() : "");
            
            notificationService.createNotification(
                    homestay.getOwner().getId(),
                    reviewerId,
                    notificationType,
                    EntityType.HOMESTAY,
                    homestay.getId().toString(),
                    content
            );
            
            log.info("审核结果通知已发送，房源ID: {}, 房东ID: {}", homestay.getId(), homestay.getOwner().getId());
        } catch (Exception e) {
            log.error("发送审核结果通知失败，房源ID: {}", homestay.getId(), e);
        }
    }

    /**
     * 发送提交审核通知给管理员
     */
    private void sendSubmitNotification(Homestay homestay) {
        try {
            // 简化通知类型，统一使用提交审核通知
            NotificationType notificationType = NotificationType.HOMESTAY_SUBMITTED;
            
            String content = String.format("房东提交了房源《%s》审核，请及时处理。", 
                    homestay.getTitle());
            
            // 查找管理员用户（假设用户名为admin或角色为ADMIN）
            User admin = findAdminUser();
            if (admin != null) {
                notificationService.createNotification(
                        admin.getId(),
                        homestay.getOwner().getId(),
                        notificationType,
                        EntityType.HOMESTAY,
                        homestay.getId().toString(),
                        content
                );
                log.info("提交审核通知已发送给管理员，房源ID: {}", homestay.getId());
            } else {
                log.warn("未找到管理员用户，无法发送审核通知，房源ID: {}", homestay.getId());
            }
        } catch (Exception e) {
            log.error("发送提交审核通知失败，房源ID: {}", homestay.getId(), e);
        }
    }

    /**
     * 发送撤回审核通知给管理员
     */
    private void sendWithdrawNotification(Homestay homestay) {
        try {
            String content = String.format("房东撤回了房源《%s》的审核申请。", homestay.getTitle());
            
            // 查找管理员用户
            User admin = findAdminUser();
            if (admin != null) {
                notificationService.createNotification(
                        admin.getId(),
                        homestay.getOwner().getId(),
                        NotificationType.HOMESTAY_SUBMITTED, // 使用相同的通知类型
                        EntityType.HOMESTAY,
                        homestay.getId().toString(),
                        content
                );
                log.info("撤回审核通知已发送给管理员，房源ID: {}", homestay.getId());
            } else {
                log.warn("未找到管理员用户，无法发送撤回通知，房源ID: {}", homestay.getId());
            }
        } catch (Exception e) {
            log.error("发送撤回审核通知失败，房源ID: {}", homestay.getId(), e);
        }
    }

    /**
     * 发送开始审核通知（简化版，不涉及审核员分配）
     */
    private void sendReviewerAssignmentNotification(Homestay homestay, User reviewer, Long assignerId) {
        try {
            // 只通知房东房源进入审核状态
            String ownerContent = String.format("您的房源《%s》已开始审核，管理员正在处理中。", homestay.getTitle());
            notificationService.createNotification(
                    homestay.getOwner().getId(),
                    reviewer.getId(), // 这里的reviewer实际上是管理员
                    NotificationType.HOMESTAY_SUBMITTED, // 使用统一的提交通知类型
                    EntityType.HOMESTAY,
                    homestay.getId().toString(),
                    ownerContent
            );
            
            log.info("开始审核通知已发送给房东，房源ID: {}", homestay.getId());
        } catch (Exception e) {
            log.error("发送开始审核通知失败，房源ID: {}", homestay.getId(), e);
        }
    }

    /**
     * 查找管理员用户
     * 根据实际情况调整查找逻辑
     */
    private User findAdminUser() {
        try {
            // 方案1: 根据用户名查找（假设管理员用户名为admin）
            return userRepository.findByUsername("admin").orElse(null);
            
            // 方案2: 根据角色查找（如果有角色字段）
            // return userRepository.findByRole("ADMIN").stream().findFirst().orElse(null);
            
            // 方案3: 根据ID查找（如果知道管理员的固定ID）
            // return userRepository.findById(1L).orElse(null);
        } catch (Exception e) {
            log.error("查找管理员用户失败", e);
            return null;
        }
    }
} 