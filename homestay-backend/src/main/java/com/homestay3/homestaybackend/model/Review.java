package com.homestay3.homestaybackend.model;

import com.homestay3.homestaybackend.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homestay_id", nullable = false)
    private Homestay homestay;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", unique = true)
    private Order order;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String response;

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;

    // 清洁度评分 (1-5)
    private Integer cleanlinessRating;

    // 准确性评分 (1-5)
    private Integer accuracyRating;

    // 沟通评分 (1-5)
    private Integer communicationRating;

    // 位置评分 (1-5)
    private Integer locationRating;

    // 入住评分 (1-5)
    private Integer checkInRating;

    // 性价比评分 (1-5)
    private Integer valueRating;

    // 房东响应时间
    private LocalDateTime responseTime;

    // 是否公开
    @Builder.Default
    private Boolean isPublic = true;
}
