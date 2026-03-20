package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.entity.LoginLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LoginLogService {

    /**
     * 记录登录日志
     */
    LoginLog recordLogin(String username, String ipAddress, String userAgent, String loginStatus, String loginType);

    /**
     * 获取所有登录日志（分页）
     */
    Page<LoginLog> getLoginLogs(Pageable pageable);

    /**
     * 搜索登录日志
     */
    Page<LoginLog> searchLogs(String username, String loginType, String startTime, String endTime, String loginStatus, Pageable pageable);
}
