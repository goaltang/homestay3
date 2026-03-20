package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 系统公告实体
 */
@Entity
@Table(name = "announcement", indexes = {
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_category", columnList = "category"),
    @Index(name = "idx_published_at", columnList = "published_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 公告标题
     */
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    /**
     * 公告内容
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 分类: SYSTEM_NOTIFICATION / ACTIVITY_ANNOUNCEMENT
     */
    @Column(name = "category", length = 50, nullable = false)
    private String category;

    /**
     * 状态: DRAFT / PUBLISHED / OFFLINE
     */
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "DRAFT";

    /**
     * 优先级，数值越大越靠前
     */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    /**
     * 发布人ID
     */
    @Column(name = "publisher_id")
    private Long publisherId;

    /**
     * 发布人名称
     */
    @Column(name = "publisher_name", length = 100)
    private String publisherName;

    /**
     * 发布时间
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * 展示开始时间
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /**
     * 展示结束时间
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 软删除标记
     */
    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;
}
