package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.entity.LoginLog;
import com.homestay3.homestaybackend.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/login-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class AdminLoginLogController {

    private final LoginLogService loginLogService;

    /**
     * 分页查询登录日志
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getLoginLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String loginType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String loginStatus) {

        Sort sort = Sort.by(Sort.Direction.DESC, "loginTime");
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<LoginLog> logs = loginLogService.searchLogs(
                username, loginType, startTime, endTime, loginStatus, pageRequest);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", logs.getContent());
        result.put("total", logs.getTotalElements());
        result.put("totalPages", logs.getTotalPages());
        result.put("currentPage", logs.getNumber());

        return ResponseEntity.ok(result);
    }
}
