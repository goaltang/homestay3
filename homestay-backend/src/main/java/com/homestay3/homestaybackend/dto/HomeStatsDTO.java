package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeStatsDTO {

    /** 已上架房源总数 */
    private Long homestayCount;

    /** 覆盖城市数（去重） */
    private Long cityCount;

    /** 好评率百分比（0-100） */
    private Double positiveRate;

    /** 平台总注册用户数 */
    private Long totalUsers;

    /** 平台总订单数 */
    private Long totalOrders;
}
