package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 入住凭证数据传输对象
 * 用于设置和获取入住凭证信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInCredentialDTO {

    /**
     * 入住方式: SELF_SERVICE(自助入住), MANUAL(前台办理)
     */
    private String checkInMethod;

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
     * 入住凭证有效期开始时间 (格式: HH:mm)
     */
    private String validFrom;

    /**
     * 入住凭证有效期结束时间 (格式: HH:mm)
     */
    private String validUntil;

    /**
     * 入住码 (系统生成，6位数字)
     */
    private String checkInCode;

    /**
     * 入住备注
     */
    private String remark;
}
