package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退房记录数据传输对象
 * 用于返回退房结算记录的完整信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckOutDTO {

    private Long id;

    private Long orderId;

    private String orderNumber;

    /**
     * 退房方式: SELF_SERVICE(自助退房), MANUAL(前台办理), AUTO(自动退房)
     */
    private String checkOutMethod;

    /**
     * 实际退房时间
     */
    private LocalDateTime checkedOutAt;

    /**
     * 退房办理者类型: HOST, GUEST, ADMIN, SYSTEM
     */
    private String operatorType;

    /**
     * 实际住宿天数
     */
    private Integer actualNights;

    /**
     * 押金金额
     */
    private BigDecimal depositAmount;

    /**
     * 押金状态: PENDING, PAID, REFUNDED, RETAINED, WAIVED
     */
    private String depositStatus;

    /**
     * 额外费用 (损坏赔偿等)
     */
    private BigDecimal extraCharges;

    /**
     * 额外费用说明
     */
    private String extraChargesDescription;

    /**
     * 结算金额
     */
    private BigDecimal settlementAmount;

    /**
     * 状态: PENDING, COMPLETED, DISPUTED
     */
    private String status;

    /**
     * 退房备注
     */
    private String remark;

    private LocalDateTime createdAt;
}
