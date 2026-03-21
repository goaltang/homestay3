package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 入住记录数据传输对象
 * 用于返回入住记录的完整信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInDTO {

    private Long id;

    private Long orderId;

    private String orderNumber;

    /**
     * 入住方式: SELF_SERVICE(自助入住), MANUAL(前台办理), CODE(入住码)
     */
    private String checkInMethod;

    /**
     * 实际入住时间
     */
    private LocalDateTime checkedInAt;

    /**
     * 入住办理者类型: HOST, ADMIN, SYSTEM, SELF_SERVICE
     */
    private String operatorType;

    /**
     * 入住码
     */
    private String checkInCode;

    /**
     * 门锁密码
     */
    private String doorPassword;

    /**
     * 密钥箱密码
     */
    private String lockboxCode;

    /**
     * 物理位置描述
     */
    private String locationDescription;

    /**
     * 入住凭证有效期开始
     */
    private LocalDateTime validFrom;

    /**
     * 入住凭证有效期结束
     */
    private LocalDateTime validUntil;

    /**
     * 状态: ACTIVE, EXPIRED, CANCELLED
     */
    private String status;

    /**
     * 入住备注
     */
    private String remark;

    private LocalDateTime createdAt;
}
