package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.OperationLog;
import com.homestay3.homestaybackend.repository.OperationLogRepository;
import com.homestay3.homestaybackend.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogRepository operationLogRepository;

    @Override
    @Async
    public OperationLog log(String operator, String operationType, String resource,
                            String resourceId, String ipAddress, String detail, String status) {
        OperationLog log = OperationLog.builder()
                .operator(operator)
                .operationType(operationType)
                .resource(resource)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .detail(detail)
                .status(status)
                .build();
        return operationLogRepository.save(log);
    }

    @Override
    public Page<OperationLog> getLogs(Pageable pageable) {
        return operationLogRepository.findAll(pageable);
    }

    @Override
    public Page<OperationLog> getLogsByOperator(String operator, Pageable pageable) {
        return operationLogRepository.findByOperator(operator, pageable);
    }

    @Override
    public Page<OperationLog> getLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return operationLogRepository.findByOperateTimeBetween(startTime, endTime, pageable);
    }

    @Override
    public Page<OperationLog> searchLogs(String operator, String operationType, String resource,
                                         LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return operationLogRepository.searchLogs(operator, operationType, resource, startTime, endTime, pageable);
    }

    @Override
    public Page<OperationLog> getRecentLogs(Pageable pageable) {
        return operationLogRepository.findAll(PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "operateTime")));
    }
}
