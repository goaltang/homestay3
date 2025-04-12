package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostStatisticsDTO {
    private Integer homestayCount; // 房源数量
    private Integer orderCount; // 订单总数
    private Integer reviewCount; // 评价总数
    private Double rating; // 平均评分
    private Double totalEarnings; // 总收入
    private Integer pendingOrders; // 待处理订单
    private Integer completedOrders; // 已完成订单
    private Integer cancelledOrders; // 已取消订单
} 