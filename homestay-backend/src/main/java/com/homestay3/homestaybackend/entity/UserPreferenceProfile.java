package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_preference_profile")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "preferred_city_json", columnDefinition = "JSON")
    private String preferredCityJson;

    @Column(name = "preferred_type_json", columnDefinition = "JSON")
    private String preferredTypeJson;

    @Column(name = "preferred_amenity_json", columnDefinition = "JSON")
    private String preferredAmenityJson;

    @Column(name = "min_price", precision = 12, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "max_price", precision = 12, scale = 2)
    private BigDecimal maxPrice;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
