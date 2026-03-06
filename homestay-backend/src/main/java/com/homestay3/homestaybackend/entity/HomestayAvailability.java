package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "homestay_availability",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_homestay_date", 
                            columnNames = {"homestay_id", "date"})
       },
       indexes = {
           @Index(name = "idx_homestay_date", columnList = "homestay_id, date"),
           @Index(name = "idx_date_status", columnList = "date, status")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomestayAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "homestay_id", nullable = false)
    private Long homestayId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AvailabilityStatus status;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "locked", nullable = false)
    @Builder.Default
    private Boolean locked = false;

    @Column(name = "lock_expires_at")
    private LocalDateTime lockExpiresAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum AvailabilityStatus {
        AVAILABLE,    // 可预订
        BOOKED,       // 已预订
        UNAVAILABLE   // 不可用（房东设置）
    }
}
