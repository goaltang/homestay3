package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /**
     * 管理员用：分页查询所有公告（含软删除过滤）
     */
    Page<Announcement> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 管理员用：按状态查询
     */
    Page<Announcement> findByIsDeletedFalseAndStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    /**
     * 用户端用：查询已发布且在有效期内的公告
     */
    @Query("SELECT a FROM Announcement a WHERE a.isDeleted = false " +
           "AND a.status = 'PUBLISHED' " +
           "AND (a.startTime IS NULL OR a.startTime <= :now) " +
           "AND (a.endTime IS NULL OR a.endTime >= :now) " +
           "ORDER BY a.priority DESC, a.publishedAt DESC")
    Page<Announcement> findActiveAnnouncements(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * 用户端用：按分类查询已发布且在有效期内的公告
     */
    @Query("SELECT a FROM Announcement a WHERE a.isDeleted = false " +
           "AND a.status = 'PUBLISHED' " +
           "AND a.category = :category " +
           "AND (a.startTime IS NULL OR a.startTime <= :now) " +
           "AND (a.endTime IS NULL OR a.endTime >= :now) " +
           "ORDER BY a.priority DESC, a.publishedAt DESC")
    Page<Announcement> findActiveByCategory(@Param("category") String category,
                                             @Param("now") LocalDateTime now,
                                             Pageable pageable);
}
