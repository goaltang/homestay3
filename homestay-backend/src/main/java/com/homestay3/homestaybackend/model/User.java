package com.homestay3.homestaybackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "users")
@Data
public class User {
    private static final Logger log = LoggerFactory.getLogger(User.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phone;

    private String realName;

    private String idCard;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    private Boolean enabled = true;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        log.info("创建新用户: {}", username);
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
        log.info("更新用户: {}", username);
    }
} 