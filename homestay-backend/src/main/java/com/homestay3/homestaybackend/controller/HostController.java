package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.HostDTO;
import com.homestay3.homestaybackend.dto.HostStatisticsDTO;
import com.homestay3.homestaybackend.service.HostService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/host")
@RequiredArgsConstructor
public class HostController {

    private static final Logger logger = LoggerFactory.getLogger(HostController.class);
    private final HostService hostService;

    /**
     * 获取房东信息
     */
    @GetMapping("/info")
    @PreAuthorize("hasAnyRole('LANDLORD', 'HOST')")
    public ResponseEntity<HostDTO> getHostInfo(Authentication authentication) {
        String username = authentication.getName();
        logger.info("获取房东信息，用户名: {}", username);
        return ResponseEntity.ok(hostService.getHostInfo(username));
    }

    /**
     * 更新房东信息
     */
    @PutMapping("/info")
    @PreAuthorize("hasAnyRole('LANDLORD', 'HOST')")
    public ResponseEntity<HostDTO> updateHostInfo(
            @RequestBody HostDTO hostDTO,
            Authentication authentication) {
        String username = authentication.getName();
        logger.info("更新房东信息，用户名: {}", username);
        return ResponseEntity.ok(hostService.updateHostInfo(hostDTO, username));
    }

    /**
     * 上传房东头像
     */
    @PostMapping("/avatar")
    @PreAuthorize("hasAnyRole('LANDLORD', 'HOST')")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        String username = authentication.getName();
        logger.info("上传房东头像，用户名: {}", username);
        String fileUrl = hostService.uploadAvatar(file, username);
        return ResponseEntity.ok(Map.of("url", fileUrl));
    }

    /**
     * 获取房东统计数据
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('LANDLORD', 'HOST')")
    public ResponseEntity<HostStatisticsDTO> getHostStatistics(Authentication authentication) {
        String username = authentication.getName();
        logger.info("获取房东统计数据，用户名: {}", username);
        return ResponseEntity.ok(hostService.getHostStatistics(username));
    }

    /**
     * 获取房东的房源列表
     */
    @GetMapping("/homestays")
    @PreAuthorize("hasAnyRole('LANDLORD', 'HOST')")
    public ResponseEntity<?> getHostHomestays(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String username = authentication.getName();
        logger.info("获取房东的房源列表，用户名: {}", username);
        return ResponseEntity.ok(hostService.getHostHomestays(username, page, size));
    }

    /**
     * 获取房东的订单列表
     */
    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('LANDLORD', 'HOST')")
    public ResponseEntity<?> getHostOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        String username = authentication.getName();
        logger.info("获取房东的订单列表，用户名: {}", username);
        return ResponseEntity.ok(hostService.getHostOrders(username, page, size, status));
    }

    /**
     * 获取房东的评价列表
     */
    @GetMapping("/reviews")
    @PreAuthorize("hasAnyRole('LANDLORD', 'HOST')")
    public ResponseEntity<?> getHostReviews(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String username = authentication.getName();
        logger.info("获取房东的评价列表，用户名: {}", username);
        return ResponseEntity.ok(hostService.getHostReviews(username, page, size));
    }

    /**
     * 获取房源的房东信息
     */
    @GetMapping("/info/{homestayId}")
    public ResponseEntity<HostDTO> getHomestayHostInfo(@PathVariable Long homestayId) {
        logger.info("获取房源的房东信息，房源ID: {}", homestayId);
        return ResponseEntity.ok(hostService.getHomestayHostInfo(homestayId));
    }
} 