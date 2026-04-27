package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_behavior_event")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBehaviorEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "event_type", length = 20, nullable = false)
    private String eventType;

    @Column(name = "homestay_id")
    private Long homestayId;

    @Column(name = "keyword", length = 200)
    private String keyword;

    @Column(name = "city_code", length = 20)
    private String cityCode;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "extra_json", columnDefinition = "JSON")
    private String extraJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
