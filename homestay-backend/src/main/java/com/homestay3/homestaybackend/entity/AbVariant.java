package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ab_variant")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "experiment_id", nullable = false)
    private Long experimentId;

    @Column(name = "variant_key", nullable = false, length = 30)
    private String variantKey;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "traffic_ratio", nullable = false)
    @Builder.Default
    private Integer trafficRatio = 50;

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
