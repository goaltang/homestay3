package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.dto.HomestaySearchRequest;
import com.homestay3.homestaybackend.service.HomestayService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/homestays", "/api/v1/homestays"})
@RequiredArgsConstructor
@CrossOrigin(
    origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, 
    allowedHeaders = "*", 
    methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, 
        RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.PATCH
    },
    exposedHeaders = {
        "Content-Type", "Content-Length", "Authorization",
        "Access-Control-Allow-Origin", "Access-Control-Allow-Headers", 
        "Access-Control-Allow-Methods"
    },
    allowCredentials = "true",
    maxAge = 3600
)
public class HomestayController {

    private static final Logger logger = LoggerFactory.getLogger(HomestayController.class);

    private final HomestayService homestayService;

    /**
     * 获取所有房源
     */
    @GetMapping
    public ResponseEntity<List<HomestayDTO>> getAllHomestays() {
        logger.info("获取所有民宿");
        List<HomestayDTO> homestays = homestayService.getAllHomestays();
        return ResponseEntity.ok(homestays);
    }

    /**
     * 获取推荐房源
     */
    @GetMapping("/featured")
    public ResponseEntity<List<HomestayDTO>> getFeaturedHomestays() {
        logger.info("获取推荐民宿");
        List<HomestayDTO> featuredHomestays = homestayService.getFeaturedHomestays();
        return ResponseEntity.ok(featuredHomestays);
    }

    /**
     * 根据ID获取房源详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<HomestayDTO> getHomestayById(@PathVariable Long id) {
        logger.info("获取民宿详情，ID: {}", id);
        HomestayDTO homestay = homestayService.getHomestayById(id);
        return ResponseEntity.ok(homestay);
    }

    /**
     * 根据房源类型获取房源列表
     */
    @GetMapping("/type/{propertyType}")
    public ResponseEntity<List<HomestayDTO>> getHomestaysByPropertyType(@PathVariable String propertyType) {
        logger.info("按物业类型获取民宿，类型: {}", propertyType);
        List<HomestayDTO> homestays = homestayService.getHomestaysByPropertyType(propertyType);
        return ResponseEntity.ok(homestays);
    }

    /**
     * 搜索房源
     */
    @PostMapping("/search")
    public ResponseEntity<List<HomestayDTO>> searchHomestays(@RequestBody HomestaySearchRequest request) {
        logger.info("搜索民宿，请求参数: {}", request);
        List<HomestayDTO> searchResults = homestayService.searchHomestays(request);
        return ResponseEntity.ok(searchResults);
    }

    /**
     * 创建新房源
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
    public ResponseEntity<HomestayDTO> createHomestay(@RequestBody HomestayDTO homestayDTO) {
        logger.info("创建新民宿");
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        HomestayDTO createdHomestay = homestayService.createHomestay(homestayDTO, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdHomestay);
    }

    /**
     * 更新房源信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
    public ResponseEntity<HomestayDTO> updateHomestay(@PathVariable Long id, @RequestBody HomestayDTO homestayDTO) {
        logger.info("更新民宿信息，ID: {}", id);
        HomestayDTO updatedHomestay = homestayService.updateHomestay(id, homestayDTO);
        return ResponseEntity.ok(updatedHomestay);
    }

    /**
     * 删除房源
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
    public ResponseEntity<Void> deleteHomestay(@PathVariable Long id) {
        logger.info("删除民宿，ID: {}", id);
        homestayService.deleteHomestay(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 更新房源状态
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD', 'ADMIN')")
    public ResponseEntity<HomestayDTO> updateHomestayStatus(
            @PathVariable Long id, 
            @RequestBody Map<String, String> statusMap,
            Authentication authentication) {
        String status = statusMap.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().body(null);
        }
        
        String username = authentication.getName();
        logger.info("更新民宿状态，ID: {}，新状态: {}，用户: {}", id, status, username);
        HomestayDTO updatedHomestay = homestayService.updateHomestayStatus(id, status, username);
        return ResponseEntity.ok(updatedHomestay);
    }
    
    /**
     * 获取当前房东的房源列表
     */
    @GetMapping("/owner")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
    public ResponseEntity<?> getMyHomestays(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false, defaultValue = "") String type,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        try {
            String username = authentication.getName();
            logger.info("获取房东的房源列表，用户名: {}, 状态: {}, 类型: {}, 页码: {}, 大小: {}", 
                    username, status, type, page, size);
            
            List<HomestayDTO> homestays = homestayService.getHomestaysByOwner(username);
            
            // 根据状态和类型过滤
            if (!status.isEmpty() || !type.isEmpty()) {
                homestays = homestays.stream()
                        .filter(h -> status.isEmpty() || h.getStatus().equalsIgnoreCase(status))
                        .filter(h -> type.isEmpty() || h.getType().equalsIgnoreCase(type))
                        .toList();
            }
            
            // 处理分页
            int total = homestays.size();
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, total);
            
            List<HomestayDTO> pagedResults = fromIndex < total 
                ? homestays.subList(fromIndex, toIndex) 
                : List.of();
                
            Map<String, Object> response = new HashMap<>();
            response.put("data", pagedResults);
            response.put("total", total);
            response.put("page", page);
            response.put("size", size);
            response.put("pages", (int) Math.ceil((double) total / size));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取房东房源列表失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "success", false,
                "data", List.of(),
                "total", 0
            ));
        }
    }

    /**
     * 获取房源类型列表
     */
    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> getHomestayTypes() {
        logger.info("获取房源类型列表");
        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> types = Arrays.asList(
            createTypeOption("ENTIRE", "整套"),
            createTypeOption("PRIVATE", "独立房间"),
            createTypeOption("SHARED", "合住房间")
        );
        response.put("success", true);
        response.put("data", types);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取房源设施列表
     */
    @GetMapping("/amenities")
    public ResponseEntity<Map<String, Object>> getHomestayAmenities() {
        logger.info("获取房源设施列表");
        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> amenities = Arrays.asList(
            createTypeOption("WIFI", "无线网络"),
            createTypeOption("AC", "空调"),
            createTypeOption("TV", "电视"),
            createTypeOption("KITCHEN", "厨房"),
            createTypeOption("WASHER", "洗衣机"),
            createTypeOption("DRYER", "烘干机"),
            createTypeOption("PARKING", "停车位"),
            createTypeOption("ELEVATOR", "电梯"),
            createTypeOption("HOT_TUB", "热水浴缸"),
            createTypeOption("POOL", "游泳池"),
            createTypeOption("GYM", "健身房"),
            createTypeOption("BREAKFAST", "含早餐"),
            createTypeOption("WORKSPACE", "工作区域"),
            createTypeOption("HEATING", "暖气"),
            createTypeOption("SECURITY_CAMERA", "安全摄像头")
        );
        response.put("success", true);
        response.put("data", amenities);
        return ResponseEntity.ok(response);
    }

    /**
     * 激活房源
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
    public ResponseEntity<HomestayDTO> activateHomestay(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        logger.info("激活民宿，ID: {}, 用户: {}", id, username);
        HomestayDTO updatedHomestay = homestayService.updateHomestayStatus(id, "ACTIVE", username);
        return ResponseEntity.ok(updatedHomestay);
    }

    /**
     * 停用房源
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
    public ResponseEntity<HomestayDTO> deactivateHomestay(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        logger.info("停用民宿，ID: {}, 用户: {}", id, username);
        HomestayDTO updatedHomestay = homestayService.updateHomestayStatus(id, "INACTIVE", username);
        return ResponseEntity.ok(updatedHomestay);
    }

    /**
     * 获取ACTIVE状态的房源列表
     */
    @GetMapping("/status/ACTIVE")
    public ResponseEntity<?> getActiveHomestays(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false) Boolean featured) {
        
        logger.info("获取ACTIVE状态的房源，页码: {}, 每页数量: {}, 是否推荐: {}", page, size, featured);
        
        try {
            // 获取所有状态为ACTIVE的房源
            List<HomestayDTO> homestays = homestayService.getAllHomestays();
            
            // 根据featured过滤
            if (featured != null) {
                homestays = homestays.stream()
                        .filter(h -> h.isFeatured() == featured)
                        .collect(Collectors.toList());
            }
            
            // 分页处理
            int total = homestays.size();
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, total);
            
            List<HomestayDTO> pagedResults = fromIndex < total 
                ? homestays.subList(fromIndex, toIndex) 
                : List.of();
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", pagedResults);
            response.put("total", total);
            response.put("page", page);
            response.put("size", size);
            response.put("pages", (int) Math.ceil((double) total / size));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取ACTIVE状态房源失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 创建类型选项
    private Map<String, String> createTypeOption(String value, String label) {
        Map<String, String> option = new HashMap<>();
        option.put("value", value);
        option.put("label", label);
        return option;
    }
    
    /**
     * 批量激活房源
     */
    @PostMapping("/batch/activate")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
    public ResponseEntity<?> batchActivateHomestays(
            @RequestBody Map<String, List<Long>> request,
            Authentication authentication) {
        
        String username = authentication.getName();
        List<Long> ids = request.get("ids");
        
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "房源ID列表不能为空"));
        }
        
        logger.info("批量激活房源，用户: {}, 房源数量: {}, IDs: {}", username, ids.size(), ids);
        
        try {
            List<HomestayDTO> updatedHomestays = ids.stream()
                .map(id -> homestayService.updateHomestayStatus(id, "ACTIVE", username))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "批量激活成功",
                "updatedCount", updatedHomestays.size()
            ));
        } catch (Exception e) {
            logger.error("批量激活房源失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 批量下架房源
     */
    @PostMapping("/batch/deactivate")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
    public ResponseEntity<?> batchDeactivateHomestays(
            @RequestBody Map<String, List<Long>> request,
            Authentication authentication) {
        
        String username = authentication.getName();
        List<Long> ids = request.get("ids");
        
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "房源ID列表不能为空"));
        }
        
        logger.info("批量下架房源，用户: {}, 房源数量: {}, IDs: {}", username, ids.size(), ids);
        
        try {
            List<HomestayDTO> updatedHomestays = ids.stream()
                .map(id -> homestayService.updateHomestayStatus(id, "INACTIVE", username))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "批量下架成功",
                "updatedCount", updatedHomestays.size()
            ));
        } catch (Exception e) {
            logger.error("批量下架房源失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 批量删除房源
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
    public ResponseEntity<?> batchDeleteHomestays(
            @RequestBody Map<String, List<Long>> request,
            Authentication authentication) {
            
        String username = authentication.getName();
        List<Long> ids = request.get("ids");
        
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "房源ID列表不能为空"));
        }
        
        logger.info("批量删除房源，用户: {}, 房源数量: {}, IDs: {}", username, ids.size(), ids);
        
        try {
            // 先验证这些房源是否都属于当前用户
            List<HomestayDTO> userHomestays = homestayService.getHomestaysByOwner(username);
            List<Long> userHomestayIds = userHomestays.stream()
                .map(HomestayDTO::getId)
                .collect(Collectors.toList());
            
            // 过滤出属于用户的房源ID
            List<Long> validIds = ids.stream()
                .filter(userHomestayIds::contains)
                .collect(Collectors.toList());
            
            if (validIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "没有找到您有权限删除的房源"));
            }
            
            // 执行批量删除
            validIds.forEach(homestayService::deleteHomestay);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "批量删除成功",
                "deletedCount", validIds.size(),
                "totalRequested", ids.size()
            ));
        } catch (Exception e) {
            logger.error("批量删除房源失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
} 