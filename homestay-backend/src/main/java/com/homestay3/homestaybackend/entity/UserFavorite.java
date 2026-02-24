package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_favorites", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "homestay_id"}))
public class UserFavorite {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "homestay_id", nullable = false)
    private Long homestayId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homestay_id", insertable = false, updatable = false)
    private Homestay homestay;
    
    // 默认构造函数
    public UserFavorite() {}
    
    // 构造函数
    public UserFavorite(Long userId, Long homestayId) {
        this.userId = userId;
        this.homestayId = homestayId;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getHomestayId() {
        return homestayId;
    }
    
    public void setHomestayId(Long homestayId) {
        this.homestayId = homestayId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Homestay getHomestay() {
        return homestay;
    }
    
    public void setHomestay(Homestay homestay) {
        this.homestay = homestay;
    }
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
