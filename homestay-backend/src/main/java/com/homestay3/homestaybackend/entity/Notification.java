package com.homestay3.homestaybackend.entity;

import com.homestay3.homestaybackend.model.converter.EntityTypeConverter;
import com.homestay3.homestaybackend.model.converter.NotificationTypeConverter;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user_id", columnList = "user_id"),
        @Index(name = "idx_notification_type", columnList = "type"),
        @Index(name = "idx_notification_user_read", columnList = "user_id, is_read"),
        @Index(name = "idx_notification_user_created", columnList = "user_id, created_at DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "actor_id")
    private Long actorId;

    @Convert(converter = NotificationTypeConverter.class)
    @Column(name = "type", length = 50, nullable = false)
    private NotificationType type;

    @Column(name = "type", length = 50, insertable = false, updatable = false)
    private String rawType;

    @Convert(converter = EntityTypeConverter.class)
    @Column(name = "entity_type", length = 50)
    private EntityType entityType;

    @Column(name = "entity_type", length = 50, insertable = false, updatable = false)
    private String rawEntityType;

    @Column(name = "entity_id", length = 255)
    private String entityId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
