package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.ViolationReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ViolationReportRepository extends JpaRepository<ViolationReport, Long> {
    
    /**
     * 根据房源ID查找所有举报
     */
    List<ViolationReport> findByHomestayId(Long homestayId);
    
    /**
     * 根据房源ID统计举报次数
     */
    @Query("SELECT COUNT(v) FROM ViolationReport v WHERE v.homestay.id = :homestayId")
    Long countByHomestayId(@Param("homestayId") Long homestayId);
    
    /**
     * 根据状态查找举报
     */
    Page<ViolationReport> findByStatus(ViolationReport.ReportStatus status, Pageable pageable);
    
    /**
     * 根据违规类型查找举报
     */
    Page<ViolationReport> findByViolationType(ViolationReport.ViolationType violationType, Pageable pageable);
    
    /**
     * 根据举报人查找举报
     */
    Page<ViolationReport> findByReporterId(Long reporterId, Pageable pageable);
    
    /**
     * 查找未处理的举报
     */
    @Query("SELECT v FROM ViolationReport v WHERE v.status IN ('PENDING', 'PROCESSING') ORDER BY v.createdAt DESC")
    Page<ViolationReport> findPendingReports(Pageable pageable);
    
    /**
     * 根据房源标题模糊查询违规举报
     */
    @Query("SELECT v FROM ViolationReport v WHERE v.homestay.title LIKE %:title% ORDER BY v.createdAt DESC")
    Page<ViolationReport> findByHomestayTitleContaining(@Param("title") String title, Pageable pageable);
    
    /**
     * 根据房东用户名查询违规举报
     */
    @Query("SELECT v FROM ViolationReport v WHERE v.homestay.owner.username LIKE %:ownerUsername% ORDER BY v.createdAt DESC")
    Page<ViolationReport> findByOwnerUsernameContaining(@Param("ownerUsername") String ownerUsername, Pageable pageable);
    
    /**
     * 查找指定时间段内的举报
     */
    @Query("SELECT v FROM ViolationReport v WHERE v.createdAt BETWEEN :startTime AND :endTime ORDER BY v.createdAt DESC")
    Page<ViolationReport> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                                @Param("endTime") LocalDateTime endTime, 
                                                Pageable pageable);
    
    /**
     * 统计各种状态的举报数量
     */
    @Query("SELECT v.status, COUNT(v) FROM ViolationReport v GROUP BY v.status")
    List<Object[]> countByStatus();
    
    /**
     * 统计各种违规类型的举报数量
     */
    @Query("SELECT v.violationType, COUNT(v) FROM ViolationReport v GROUP BY v.violationType")
    List<Object[]> countByViolationType();
    
    /**
     * 查找多次被举报的房源
     */
    @Query("SELECT v.homestay.id, COUNT(v) as reportCount FROM ViolationReport v " +
           "GROUP BY v.homestay.id HAVING COUNT(v) >= :minReportCount " +
           "ORDER BY reportCount DESC")
    List<Object[]> findHomestaysWithMultipleReports(@Param("minReportCount") int minReportCount);
} 