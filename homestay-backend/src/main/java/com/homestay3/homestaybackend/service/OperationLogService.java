package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface OperationLogService {

    /**
     * 记录操作日志
     */
    OperationLog log(String operator, String operationType, String resource,
                     String resourceId, String ipAddress, String detail, String status);

    /**
     * 分页查询日志
     */
    Page<OperationLog> getLogs(Pageable pageable);

    /**
     * 根据操作人查询
     */
    Page<OperationLog> getLogsByOperator(String operator, Pageable pageable);

    /**
     * 根据时间范围查询
     */
    Page<OperationLog> getLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 搜索日志
     */
    Page<OperationLog> searchLogs(String operator, String operationType, String resource,
                                 LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 获取最近的日志
     */
    Page<OperationLog> getRecentLogs(Pageable pageable);
}
