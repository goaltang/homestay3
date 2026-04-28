package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.LoginLog;
import com.homestay3.homestaybackend.repository.LoginLogRepository;
import com.homestay3.homestaybackend.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {

    private final LoginLogRepository loginLogRepository;

    @Override
    public LoginLog recordLogin(String username, String ipAddress, String userAgent, String loginStatus, String loginType) {
        LoginLog log = LoginLog.builder()
                .username(username)
                .loginTime(LocalDateTime.now())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .loginStatus(loginStatus)
                .loginType(loginType)
                .build();
        return loginLogRepository.save(log);
    }

    @Override
    public Page<LoginLog> getLoginLogs(Pageable pageable) {
        return loginLogRepository.findAll(pageable);
    }

    @Override
    public Page<LoginLog> searchLogs(String username, String loginType, String startTime, String endTime, String loginStatus, Pageable pageable) {
        LocalDateTime start = startTime != null ? LocalDateTime.parse(startTime + "T00:00:00") : null;
        LocalDateTime end = endTime != null ? LocalDateTime.parse(endTime + "T23:59:59") : null;
        return loginLogRepository.searchLogs(username, loginType, start, end, loginStatus, pageable);
    }
}
