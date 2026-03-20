package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Entity
@Table(name = "operation_log", indexes = {
    @Index(name = "idx_operate_time", columnList = "operate_time"),
    @Index(name = "idx_operator", columnList = "operator"),
    @Index(name = "idx_resource", columnList = "resource")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 操作人用户名
     */
    @Column(name = "operator", length = 100)
    private String operator;

    /**
     * 操作类型：CREATE, UPDATE, DELETE, LOGIN, LOGOUT, VIEW, EXPORT, OTHER
     */
    @Column(name = "operation_type", length = 50)
    private String operationType;

    /**
     * 操作资源类型：如 USER, HOMESTAY, ORDER, SYSTEM_CONFIG 等
     */
    @Column(name = "resource", length = 50)
    private String resource;

    /**
     * 资源ID
     */
    @Column(name = "resource_id", length = 100)
    private String resourceId;

    /**
     * 操作人IP地址
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * 操作详情，JSON格式存储修改前后的值
     */
    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    /**
     * 操作状态：SUCCESS, FAIL
     */
    @Column(name = "status", length = 20)
    private String status;

    /**
     * 操作时间
     */
    @CreationTimestamp
    @Column(name = "operate_time")
    private LocalDateTime operateTime;

    /**
     * 浏览器/客户端信息
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
}
