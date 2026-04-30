package com.homestay3.homestaybackend.service;

import java.util.Map;

/**
 * 订单超时处理服务接口
 * 定义订单超时处理的方法
 */
public interface IOrderTimeoutService {

    /**
     * 处理超时订单
     * 定时任务每小时执行一次，自动处理各种状态的超时订单
     */
    void handleTimeoutOrders();

    /**
     * 获取超时配置
     * @return 超时配置项
     */
    Map<String, Integer> getTimeoutConfig();
}
