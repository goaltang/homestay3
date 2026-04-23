package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.service.HomestayService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/homestays")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminHomestayController {

    private static final Logger logger = LoggerFactory.getLogger(AdminHomestayController.class);
    private final HomestayService homestayService;

    /**
     * 获取房源列表，支持分页和筛选
     */
    @GetMapping
    public ResponseEntity<?> getHomestays(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type
    ) {
        logger.info("管理员获取房源列表，请求页码 (1-based): {}, 每页数量: {}, 标题: {}, 状态: {}, 类型: {}", 
                page, size, title, status, type);
        
        int pageZeroBased = Math.max(0, page - 1);
        
        Pageable pageable = PageRequest.of(pageZeroBased, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<HomestayDTO> homestays = homestayService.getAdminHomestays(pageable, title, status, type);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", homestays.getContent());
        response.put("totalElements", homestays.getTotalElements());
        response.put("totalPages", homestays.getTotalPages());
        response.put("page", homestays.getNumber() + 1);
        response.put("size", homestays.getSize());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取待审核房源列表 - 必须在 /{id} 路由之前定义
     */
    @GetMapping("/pending-review")
    public ResponseEntity<?> getPendingReviewHomestays(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        logger.info("管理员获取待审核房源列表，页码: {}, 每页数量: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<HomestayDTO> homestays = homestayService.getAdminHomestays(pageable, null, "PENDING", null);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", homestays.getContent());
        response.put("totalElements", homestays.getTotalElements());
        response.put("totalPages", homestays.getTotalPages());
        response.put("page", homestays.getNumber());
        response.put("size", homestays.getSize());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取房源详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getHomestayDetail(@PathVariable Long id) {
        logger.info("管理员获取房源详情，ID: {}", id);
        HomestayDTO homestay = homestayService.getHomestayById(id, null);
        return ResponseEntity.ok(homestay);
    }

    /**
     * 获取房源详情（包含完整房东信息）
     */
    @GetMapping("/{id}/with-owner")
    public ResponseEntity<?> getHomestayDetailWithOwner(@PathVariable Long id) {
        logger.info("管理员获取带完整房东信息的房源详情，ID: {}", id);
        
        try {
            HomestayDTO homestay = homestayService.getHomestayWithOwnerDetails(id);
            return ResponseEntity.ok(homestay);
        } catch (Exception e) {
            logger.error("获取带房东信息的房源详情失败，ID: {}, 错误: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "获取房源详情失败: " + e.getMessage()));
        }
    }

    /**
     * 创建房源
     */
    @PostMapping
    public ResponseEntity<?> createHomestay(@RequestBody HomestayDTO homestayDTO) {
        logger.info("管理员创建房源: {}", homestayDTO.getTitle());
        HomestayDTO createdHomestay = homestayService.createHomestay(homestayDTO);
        return ResponseEntity.ok(createdHomestay);
    }

    /**
     * 更新房源
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateHomestay(@PathVariable Long id, @RequestBody HomestayDTO homestayDTO) {
        logger.info("管理员更新房源，ID: {}", id);
        HomestayDTO updatedHomestay = homestayService.updateHomestay(id, homestayDTO);
        return ResponseEntity.ok(updatedHomestay);
    }

    /**
     * 删除房源
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHomestay(@PathVariable Long id) {
        logger.info("管理员删除房源，ID: {}", id);
        homestayService.deleteHomestay(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 更新房源状态
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateHomestayStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> statusData
    ) {
        String status = (String) statusData.get("status");
        logger.info("管理员更新房源状态，ID: {}, 状态: {}", id, status);
        homestayService.updateHomestayStatus(id, status);
        return ResponseEntity.ok().build();
    }

    /**
     * 批量删除房源
     */
    @DeleteMapping("/batch")
    public ResponseEntity<?> batchDeleteHomestays(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");
        
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "房源ID列表不能为空"));
        }
        
        logger.info("管理员批量删除房源，数量: {}, IDs: {}", ids.size(), ids);
        
        try {
            ids.forEach(homestayService::deleteHomestay);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "批量删除成功",
                "deletedCount", ids.size()
            ));
        } catch (Exception e) {
            logger.error("批量删除房源失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 批量更新房源状态
     */
    @PutMapping("/batch/status")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> batchUpdateHomestayStatus(@RequestBody Map<String, Object> request) {
        List<Long> ids = (List<Long>) request.get("ids");
        String status = (String) request.get("status");
        
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "房源ID列表不能为空"));
        }
        
        if (status == null || status.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "房源状态不能为空"));
        }
        
        logger.info("管理员批量更新房源状态，状态: {}, 数量: {}, IDs: {}", status, ids.size(), ids);
        
        try {
            ids.forEach(id -> homestayService.updateHomestayStatus(id, status));
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", String.format("批量更新状态为 %s 成功", status),
                "updatedCount", ids.size()
            ));
        } catch (Exception e) {
            logger.error("批量更新房源状态失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 强制下架房源（因违规）
     */
    @PostMapping("/{id}/force-delist")
    public ResponseEntity<?> forceDelistHomestay(
            @PathVariable Long id,
            @RequestBody Map<String, String> request
    ) {
        String reason = request.get("reason");
        String notes = request.get("notes");
        String violationType = request.get("violationType");
        
        logger.info("管理员强制下架房源，ID: {}, 原因: {}, 违规类型: {}", id, reason, violationType);
        
        if (reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "下架原因不能为空"));
        }
        
        try {
            homestayService.forceDelistHomestay(id, reason, notes, violationType);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "强制下架成功"
            ));
        } catch (Exception e) {
            logger.error("强制下架房源失败，ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "强制下架失败: " + e.getMessage()));
        }
    }

    /**
     * 批量补全缺失的地理编码坐标
     */
    @PostMapping("/batch-populate-coordinates")
    public ResponseEntity<?> batchPopulateCoordinates(@RequestBody(required = false) Map<String, Object> request) {
        int batchSize = 100;
        if (request != null && request.get("batchSize") instanceof Number) {
            batchSize = ((Number) request.get("batchSize")).intValue();
        }

        logger.info("管理员批量补全房源坐标，batchSize={}", batchSize);
        try {
            int successCount = homestayService.batchPopulateCoordinates(batchSize);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "批量补全坐标完成",
                "successCount", successCount
            ));
        } catch (Exception e) {
            logger.error("批量补全坐标失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 批量强制下架房源
     */
    @PostMapping("/batch/force-delist")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> batchForceDelistHomestays(@RequestBody Map<String, Object> request) {
        List<Long> ids = (List<Long>) request.get("ids");
        String reason = (String) request.get("reason");
        String notes = (String) request.get("notes");
        String violationType = (String) request.get("violationType");
        
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "房源ID列表不能为空"));
        }
        
        if (reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "下架原因不能为空"));
        }
        
        logger.info("管理员批量强制下架房源，数量: {}, 原因: {}", ids.size(), reason);
        
        try {
            int successCount = 0;
            for (Long id : ids) {
                try {
                    homestayService.forceDelistHomestay(id, reason, notes, violationType);
                    successCount++;
                } catch (Exception e) {
                    logger.error("强制下架房源失败，ID: {}", id, e);
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "批量强制下架完成",
                "successCount", successCount,
                "totalCount", ids.size()
            ));
        } catch (Exception e) {
            logger.error("批量强制下架房源失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

} 