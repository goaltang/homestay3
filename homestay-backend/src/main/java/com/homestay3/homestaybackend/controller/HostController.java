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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.List;
import com.homestay3.homestaybackend.dto.HomestayOptionDTO;

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
     * 获取房东的房源选项列表 (用于下拉菜单)
     * @param authentication 当前认证用户信息
     * @return 只包含 id 和 title 的房源列表
     */
    @GetMapping("/homestay-options")
    @PreAuthorize("hasAnyRole('LANDLORD', 'HOST')")
    public ResponseEntity<List<HomestayOptionDTO>> getHostHomestayOptions(Authentication authentication) {
        String username = authentication.getName();
        logger.info("获取房东 {} 的房源选项列表", username);
        List<HomestayOptionDTO> options = hostService.getHostHomestayOptions(username);
        return ResponseEntity.ok(options);
    }

    /**
     * 获取房源的房东信息
     */
    @GetMapping("/info/{homestayId}")
    public ResponseEntity<HostDTO> getHomestayHostInfo(@PathVariable Long homestayId) {
        logger.info("获取房源的房东信息，房源ID: {}", homestayId);
        return ResponseEntity.ok(hostService.getHomestayHostInfo(homestayId));
    }

    /**
     * 更新房东个人资料
     * @param profileData 个人资料数据
     * @return 更新后的个人资料
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateHostProfile(@RequestBody Map<String, Object> profileData) {
        logger.info("更新房东个人资料请求: {}", profileData);
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("请先登录");
            }
            
            String username = auth.getName();
            Map<String, Object> updatedProfile = hostService.updateHostProfile(username, profileData);
            return ResponseEntity.ok(updatedProfile);
        } catch (Exception e) {
            logger.error("更新房东个人资料失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "更新个人资料失败: " + e.getMessage()));
        }
    }

    /**
     * 获取房东个人资料
     * @return 房东个人资料
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getHostProfile() {
        logger.info("获取房东个人资料请求");
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("请先登录");
            }
            
            String username = auth.getName();
            Map<String, Object> hostProfile = hostService.getHostProfile(username);
            return ResponseEntity.ok(hostProfile);
        } catch (Exception e) {
            logger.error("获取房东个人资料失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "获取个人资料失败: " + e.getMessage()));
        }
    }

    /**
     * 上传房东证件照片
     * @param file 文件
     * @param type 文件类型
     * @return 上传后的文件URL
     */
    @PostMapping("/upload-document")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type) {
        logger.info("上传房东证件照片请求: type={}, filename={}, size={}KB", 
                type, file.getOriginalFilename(), file.getSize() / 1024);
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "文件为空"));
            }
            
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("请先登录");
            }
            
            String username = auth.getName();
            String fileUrl = hostService.uploadHostDocument(username, file, type);
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (Exception e) {
            logger.error("上传房东证件照片失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "上传证件照片失败: " + e.getMessage()));
        }
    }
} 