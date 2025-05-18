package com.homestay3.homestaybackend.service;

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
} 