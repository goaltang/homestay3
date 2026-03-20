package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 登录日志实体
 */
@Entity
@Table(name = "login_log", indexes = {
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_login_time", columnList = "login_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 登录用户名
     */
    @Column(name = "username", length = 100, nullable = false)
    private String username;

    /**
     * 登录时间
     */
    @CreationTimestamp
    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    /**
     * IP地址
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * 浏览器/客户端信息
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * 登录状态: SUCCESS / FAIL
     */
    @Column(name = "login_status", length = 20, nullable = false)
    private String loginStatus;

    /**
     * 登录类型: ADMIN / USER
     */
    @Column(name = "login_type", length = 20, nullable = false)
    private String loginType;

    /**
     * 登出时间
     */
    @Column(name = "logout_time")
    private LocalDateTime logoutTime;
}
