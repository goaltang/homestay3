package com.homestay3.homestaybackend.entity;

import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user_id", columnList = "userId"),
        @Index(name = "idx_notification_type", columnList = "type"),
        @Index(name = "idx_notification_user_read", columnList = "userId, isRead"),
        @Index(name = "idx_notification_user_created", columnList = "userId, createdAt DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId; // 接收通知的用户 ID

    @Column
    private Long actorId; // 触发通知的用户 ID (可选)

    @Enumerated(EnumType.STRING) // 将枚举存储为字符串
    @Column(length = 50, nullable = false)
    private NotificationType type; // 通知类型

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private EntityType entityType; // 关联实体类型 (可选)

    @Column(length = 255)
    private String entityId; // 关联实体 ID (可选)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 通知内容

    @Builder.Default
    @Column(nullable = false)
    private boolean isRead = false; // 是否已读，默认为 false

    @Column
    private LocalDateTime readAt; // 标记已读的时间

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
} 