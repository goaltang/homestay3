package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.HomestayAuditLog;
import com.homestay3.homestaybackend.model.HomestayStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 房源审核记录数据访问层
 */
@Repository
public interface HomestayAuditLogRepository extends JpaRepository<HomestayAuditLog, Long> {

    /**
     * 根据房源ID查询审核记录，按时间倒序
     */
    List<HomestayAuditLog> findByHomestayIdOrderByCreatedAtDesc(Long homestayId);

    /**
     * 根据房源ID分页查询审核记录
     */
    Page<HomestayAuditLog> findByHomestayIdOrderByCreatedAtDesc(Long homestayId, Pageable pageable);



    /**
     * 根据新状态查询审核记录
     */
    List<HomestayAuditLog> findByNewStatusOrderByCreatedAtDesc(HomestayStatus newStatus);

    /**
     * 根据操作类型查询审核记录
     */
    List<HomestayAuditLog> findByActionTypeOrderByCreatedAtDesc(HomestayAuditLog.AuditActionType actionType);

    /**
     * 查询指定时间范围内的审核记录
     */
    @Query("SELECT al FROM HomestayAuditLog al WHERE al.createdAt BETWEEN :startTime AND :endTime ORDER BY al.createdAt DESC")
    List<HomestayAuditLog> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 分页查询指定时间范围内的审核记录
     */
    Page<HomestayAuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startTime, 
                                                                     LocalDateTime endTime, 
                                                                     Pageable pageable);

    /**
     * 分页查询所有审核记录，按时间倒序
     */
    Page<HomestayAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 分页查询指定时间范围内的审核记录，排除指定操作类型
     */
    Page<HomestayAuditLog> findByCreatedAtBetweenAndActionTypeNotOrderByCreatedAtDesc(
            LocalDateTime startTime, 
            LocalDateTime endTime, 
            HomestayAuditLog.AuditActionType excludeActionType,
            Pageable pageable);

    /**
     * 分页查询所有审核记录，排除指定操作类型，按时间倒序
     */
    Page<HomestayAuditLog> findByActionTypeNotOrderByCreatedAtDesc(
            HomestayAuditLog.AuditActionType excludeActionType,
            Pageable pageable);



    /**
     * 查询房源的最新审核记录
     */
    @Query("SELECT al FROM HomestayAuditLog al WHERE al.homestay.id = :homestayId " +
           "ORDER BY al.createdAt DESC LIMIT 1")
    HomestayAuditLog findLatestByHomestayId(@Param("homestayId") Long homestayId);

    /**
     * 统计各种操作类型的数量
     */
    @Query("SELECT al.actionType, COUNT(al) FROM HomestayAuditLog al " +
           "WHERE al.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY al.actionType")
    List<Object[]> countByActionTypeAndDateRange(@Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);

    /**
     * 查询待审核房源的审核记录（用于工作量分析）
     */
    @Query("SELECT al FROM HomestayAuditLog al WHERE al.newStatus IN ('PENDING') " +
           "AND al.createdAt >= :since ORDER BY al.createdAt ASC")
    List<HomestayAuditLog> findPendingReviewsSince(@Param("since") LocalDateTime since);
} 