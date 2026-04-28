package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_analytics",
        indexes = {
                @Index(name = "idx_template_event", columnList = "template_id, event_type, created_at"),
                @Index(name = "idx_campaign_event", columnList = "campaign_id, event_type, created_at"),
                @Index(name = "idx_channel_created", columnList = "channel, created_at"),
                @Index(name = "idx_user_event", columnList = "user_id, event_type, created_at"),
                @Index(name = "idx_event_created", columnList = "event_type, created_at")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(length = 50)
    private String channel;

    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "homestay_id")
    private Long homestayId;

    @Column(name = "order_id")
    private Long orderId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
