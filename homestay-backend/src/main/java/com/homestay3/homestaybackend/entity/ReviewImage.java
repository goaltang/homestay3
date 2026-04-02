package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private Integer sortOrder;

    @CreationTimestamp
    private LocalDateTime createTime;
}
