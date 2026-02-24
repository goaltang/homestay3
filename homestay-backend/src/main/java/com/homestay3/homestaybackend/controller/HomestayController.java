package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.AmenityDTO;
import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.dto.HomestaySearchRequest;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.entity.Amenity;
// 注意：Amenity 已在 entity 包中
import com.homestay3.homestaybackend.service.AmenityService;
import com.homestay3.homestaybackend.service.HomestayRecommendationService;
import com.homestay3.homestaybackend.service.HomestayService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Date;
import java.time.LocalDate;

@RestController
@RequestMapping({"/api/homestays", "/api/v1/homestays"})
@RequiredArgsConstructor
public class HomestayController {

    private static final Logger logger = LoggerFactory.getLogger(HomestayController.class);

    private final HomestayService homestayService;
    private final HomestayRecommendationService homestayRecommendationService;
    
    @Autowired
    private AmenityService amenityService;

    /**
     * 获取所有房源
     */
    @GetMapping
    public ResponseEntity<?> getAllHomestays() {
        logger.info("获取所有民宿");
        
        try {
            List<HomestayDTO> homestays = homestayService.getAllHomestays();
            logger.info("成功获取{}个房源", homestays.size());
            return ResponseEntity.ok(homestays);
        } catch (Exception e) {
            logger.error("获取所有房源时发生错误: {}", e.getMessage(), e);
            
            // 返回错误响应而不是抛出异常
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取房源数据失败，请稍后重试");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", new java.util.Date());
            errorResponse.put("path", "/api/homestays");
            
            // 即使出现错误，也返回200状态码和空列表，避免前端500错误
            errorResponse.put("data", new ArrayList<>());
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 获取推荐房源
     */
    @GetMapping("/featured")
    public ResponseEntity<List<HomestayDTO>> getFeaturedHomestays() {
        logger.info("获取推荐民宿");
        // 使用新的推荐服务替代已废弃的方法
        List<HomestayDTO> featuredHomestays = homestayRecommendationService.getRecommendedHomestays(6);
        return ResponseEntity.ok(featuredHomestays);
    }

    /**
     * 根据ID获取房源详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<HomestayDTO> getHomestayById(
            @PathVariable Long id, 
            @RequestParam(name = "referring_criteria", required = false) String referringCriteriaString) {
        logger.info("获取民宿详情，ID: {}, 来源搜索条件: {}", id, referringCriteriaString);
        
        List<String> referringCriteria = null;
        if (referringCriteriaString != null && !referringCriteriaString.isEmpty()) {
            referringCriteria = Arrays.asList(referringCriteriaString.split(","));
            // Trim whitespace from each criterion
            if (referringCriteria != null) {
                 referringCriteria = referringCriteria.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                 if (referringCriteria.isEmpty()) referringCriteria = null;
            }
        }
        
        HomestayDTO homestay = homestayService.getHomestayById(id, referringCriteria);
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
    @PreAuthorize("hasAnyAuthority('ROLE_HOST')")
    public ResponseEntity<HomestayDTO> createHomestay(@RequestBody HomestayDTO homestayDTO) {
        logger.info("创建新民宿: {}", homestayDTO.getTitle());
        
        // 提前检查amenities是否为空，避免空指针异常
        if (homestayDTO.getAmenities() == null) {
            logger.info("创建房源时amenities为null，设置为空列表");
            homestayDTO.setAmenities(new ArrayList<>());
        } else if (!homestayDTO.getAmenities().isEmpty()) {
            logger.info("创建房源时包含{}个设施", homestayDTO.getAmenities().size());
            // 检查amenities是否为字符串数组，如果是则转为AmenityDTO
            boolean needConversion = false;
            for (Object item : homestayDTO.getAmenities()) {
                if (item instanceof String) {
                    needConversion = true;
                    break;
                }
            }
            
            if (needConversion) {
                logger.info("检测到amenities需要转换格式");
                List<AmenityDTO> convertedAmenities = new ArrayList<>();
                for (Object item : homestayDTO.getAmenities()) {
                    if (item instanceof String) {
                        String value = (String) item;
                        AmenityDTO dto = new AmenityDTO();
                        dto.setValue(value);
                        dto.setLabel(value);
                        convertedAmenities.add(dto);
                    } else if (item instanceof Map) {
                        // 处理前端发送的对象格式
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) item;
                        AmenityDTO dto = new AmenityDTO();
                        dto.setValue((String) map.get("value"));
                        dto.setLabel((String) map.get("label"));
                        if (map.containsKey("icon")) {
                            dto.setIcon((String) map.get("icon"));
                        }
                        convertedAmenities.add(dto);
                    } else if (item instanceof AmenityDTO) {
                        convertedAmenities.add((AmenityDTO) item);
                    }
                }
                homestayDTO.setAmenities(convertedAmenities);
                logger.info("转换后的amenities数量: {}", convertedAmenities.size());
            }
        }
        
        // 记录设施数据
        if (homestayDTO.getAmenities() != null && !homestayDTO.getAmenities().isEmpty()) {
            logger.info("包含设施数量: {}", homestayDTO.getAmenities().size());
            for (AmenityDTO amenity : homestayDTO.getAmenities()) {
                logger.info("设施: value={}, label={}", amenity.getValue(), amenity.getLabel());
            }
        } else {
            logger.warn("创建房源时未提供设施数据");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        HomestayDTO createdHomestay = homestayService.createHomestay(homestayDTO, username);
        logger.info("民宿创建成功: id={}", createdHomestay.getId());

        // 检查返回数据中的设施数量
        if (createdHomestay.getAmenities() != null) {
            logger.info("创建后设施数量: {}", createdHomestay.getAmenities().size());
        } else {
            logger.warn("创建后设施数据为null");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(createdHomestay);
    }

    /**
     * 更新房源信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST', 'ROLE_ADMIN')")
    public ResponseEntity<HomestayDTO> updateHomestay(
            @PathVariable Long id, 
            @RequestBody HomestayDTO homestayDTO,
            @RequestHeader(value = "X-Username", required = false) String headerUsername) {
        try {
            // 获取当前用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            // 如果请求头中包含用户名，记录下来用于调试
            if (headerUsername != null && !headerUsername.isEmpty()) {
                logger.info("请求头中的用户名: {}, 认证用户名: {}", headerUsername, username);
                // 这里不替换认证中的用户名，只用于日志
            }
            
            logger.info("当前用户: {}, 尝试更新房源ID: {}", username, id);
            logger.debug("更新数据: {}", homestayDTO);
            
            // 确保DTO中包含正确的所有者信息
            if (homestayDTO.getOwnerUsername() == null || homestayDTO.getOwnerUsername().isEmpty()) {
                logger.info("设置DTO中的所有者用户名为当前用户: {}", username);
                homestayDTO.setOwnerUsername(username);
            } else if (!homestayDTO.getOwnerUsername().equals(username)) {
                logger.warn("DTO中的所有者({})与当前用户({})不匹配", homestayDTO.getOwnerUsername(), username);
            }

            // 执行更新操作
            HomestayDTO updatedHomestay = homestayService.updateHomestay(id, homestayDTO);
            logger.info("房源更新成功，ID: {}", id);
            return ResponseEntity.ok(updatedHomestay);
        } catch (Exception e) {
            logger.error("更新房源时发生错误，ID: {}, 错误类型: {}, 错误消息: {}",
                id, e.getClass().getName(), e.getMessage(), e);

            // 返回详细的错误信息
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", new java.util.Date());
            errorResponse.put("path", "/api/homestays/" + id);
            errorResponse.put("success", false);

            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            
            if (e instanceof IllegalArgumentException) {
                status = HttpStatus.BAD_REQUEST;
            } else if (e instanceof AccessDeniedException || 
                      e.getMessage() != null && (
                          e.getMessage().contains("权限") || 
                          e.getMessage().contains("permission") || 
                          e.getMessage().contains("Access Denied"))) {
                status = HttpStatus.FORBIDDEN;
                errorResponse.put("message", "您没有权限更新此房源");
            } else if (e instanceof ResourceNotFoundException) {
                status = HttpStatus.NOT_FOUND;
                errorResponse.put("message", "找不到指定的房源");
            }

            // 创建错误对象并返回
            HomestayDTO errorDTO = new HomestayDTO();
            errorDTO.setId(id);
            errorDTO.setStatus("ERROR");
            
            return ResponseEntity.status(status).body(errorDTO);
        }
    }

    /**
     * 删除房源
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteHomestay(@PathVariable Long id) {
        try {
            logger.info("收到删除房源请求, ID: {}", id);
            
            // 获取当前用户信息，用于日志记录
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("当前用户: {}, 尝试删除房源ID: {}", username, id);
            
            // 执行删除操作
            homestayService.deleteHomestay(id);
            logger.info("房源删除成功, ID: {}", id);
            
            // 返回成功信息
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "房源已成功删除");
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            logger.error("删除失败 - 找不到房源, ID: {}, 错误: {}", id, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            logger.error("删除房源时发生错误, ID: {}, 错误类型: {}, 错误信息: {}", 
                          id, e.getClass().getName(), e.getMessage(), e);
            
            // 构造错误响应
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            
            // 确定合适的HTTP状态码
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            
            // 处理权限问题导致的异常
            if (e.getMessage() != null && 
                (e.getMessage().contains("Access Denied") || 
                 e.getMessage().contains("权限") || 
                 e.getMessage().contains("所有者") || 
                 e.getMessage().contains("无权"))) {
                status = HttpStatus.FORBIDDEN;
                errorResponse.put("message", "您没有权限删除此房源");
                errorResponse.put("error", "Access Denied");
            } else {
                errorResponse.put("message", "删除房源时发生错误：" + e.getMessage());
                errorResponse.put("error", e.getMessage());
            }
            
            return ResponseEntity.status(status).body(errorResponse);
        }
    }

    /**
     * 更新房源状态
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST', 'ROLE_ADMIN')")
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
     * 检查房源是否可以提交审核
     */
    @GetMapping("/{id}/check-review-ready")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> checkHomestayReadyForReview(@PathVariable Long id) {
        try {
            logger.info("检查房源审核就绪状态，ID: {}", id);
            
            boolean ready = homestayService.checkHomestayReadyForReview(id);
            String details = homestayService.getHomestayReviewReadinessDetails(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("ready", ready);
            response.put("details", details);
            response.put("timestamp", new Date());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("检查房源审核就绪状态失败: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("ready", false);
            errorResponse.put("details", e.getMessage());
            errorResponse.put("timestamp", new Date());
            
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            if (e instanceof ResourceNotFoundException) {
                status = HttpStatus.NOT_FOUND;
            } else if (e instanceof AccessDeniedException) {
                status = HttpStatus.FORBIDDEN;
            }
            
            return ResponseEntity.status(status).body(errorResponse);
        }
    }

    /**
     * 获取当前房东的房源列表
     */
    @GetMapping("/owner")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST')")
    public ResponseEntity<?> getMyHomestays(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false, defaultValue = "") String type,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        try {
            // 检查authentication是否为null
            if (authentication == null) {
                logger.error("获取房东房源列表失败: 用户未认证");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "用户未认证",
                    "success", false,
                    "data", List.of(),
                    "total", 0
                ));
            }

            String username = authentication.getName();
            logger.info("获取房东的房源列表，用户名: {}, 状态: {}, 类型: {}, 页码: {}, 大小: {}",
                    username, status, type, page, size);

            List<HomestayDTO> homestays;
            try {
                homestays = homestayService.getHomestaysByOwner(username);
                logger.info("从数据库获取到{}条房源记录", homestays != null ? homestays.size() : 0);
            } catch (Exception e) {
                logger.error("调用service获取房源列表时发生异常: {}", e.getMessage(), e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "获取房源数据失败: " + e.getMessage());
                errorResponse.put("data", List.of());
                errorResponse.put("total", 0);
                errorResponse.put("page", page);
                errorResponse.put("size", size);
                errorResponse.put("pages", 0);
                return ResponseEntity.ok(errorResponse);
            }

            // 如果获取到的homestays为null，处理为空列表
            if (homestays == null) {
                logger.warn("service返回null，处理为空列表");
                homestays = new ArrayList<>();
            }

            // 根据状态和类型过滤
            if (!status.isEmpty() || !type.isEmpty()) {
                homestays = homestays.stream()
                        .filter(h -> h != null) // 过滤掉null的记录
                        .filter(h -> status.isEmpty() || (h.getStatus() != null && h.getStatus().equalsIgnoreCase(status)))
                        .filter(h -> type.isEmpty() || (h.getType() != null && h.getType().equalsIgnoreCase(type)))
                        .collect(Collectors.toList());
                        
                logger.info("过滤后剩余{}条房源记录", homestays.size());
            }

            // 处理分页
            int total = homestays.size();
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, total);

            List<HomestayDTO> pagedResults = fromIndex < total
                ? homestays.subList(fromIndex, toIndex)
                : List.of();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", pagedResults);
            response.put("total", total);
            response.put("page", page);
            response.put("size", size);
            response.put("pages", (int) Math.ceil((double) total / size));

            logger.info("成功返回房源列表，总计: {}, 当前页: {}, 页面大小: {}", total, page, size);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取房东房源列表时发生未预期的异常: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "系统处理失败，请重试");
            errorResponse.put("data", List.of());
            errorResponse.put("total", 0);
            errorResponse.put("page", page);
            errorResponse.put("size", size);
            errorResponse.put("pages", 0);
            
            // 开发环境下返回详细错误信息
            String activeProfile = System.getProperty("spring.profiles.active", "dev");
            if ("dev".equals(activeProfile) || "development".equals(activeProfile)) {
                errorResponse.put("error", e.getMessage());
                errorResponse.put("errorType", e.getClass().getSimpleName());
            }
            
            return ResponseEntity.ok(errorResponse);
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
     * 获取指定房源的设施列表
     */
    @GetMapping("/{id}/amenities")
    public ResponseEntity<Map<String, Object>> getHomestayAmenities(@PathVariable Long id) {
        logger.info("获取房源设施列表, 房源ID: {}", id);
        
        try {
            // 检查房源是否存在
            homestayService.getHomestayById(id, null);
            
            // 调用设施服务获取该房源的设施
            List<Amenity> amenities = amenityService.getHomestayAmenities(id);
            
            // 将Amenity转为AmenityDTO
            List<AmenityDTO> amenityDTOs = amenities.stream()
                .map(amenity -> {
                    AmenityDTO dto = new AmenityDTO();
                    dto.setValue(amenity.getValue());
                    dto.setLabel(amenity.getLabel());
                    dto.setDescription(amenity.getDescription());
                    dto.setIcon(amenity.getIcon());
                    dto.setActive(amenity.isActive());
                    
                    if (amenity.getCategory() != null) {
                        dto.setCategoryCode(amenity.getCategory().getCode());
                        dto.setCategoryName(amenity.getCategory().getName());
                        dto.setCategoryIcon(amenity.getCategory().getIcon());
                    }
                    
                    return dto;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", amenityDTOs);
            
            logger.info("成功获取房源{}的设施, 共{}个", id, amenityDTOs.size());
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            logger.error("获取房源设施失败: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        } catch (Exception e) {
            logger.error("获取房源设施时发生错误: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取房源设施时发生错误: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 激活房源
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_HOST')")
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
                        .filter(h -> h.getFeatured() == featured)
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
    @PreAuthorize("hasAnyAuthority('ROLE_HOST')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_HOST')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_HOST')")
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

    // 帮助方法：将错误信息转换为HomestayDTO，用于返回
    private HomestayDTO convertErrorToDTO(Map<String, Object> errorData) {
        HomestayDTO errorDTO = new HomestayDTO();
        errorDTO.setId(0L); // 设置一个不存在的ID
        errorDTO.setTitle("ERROR: " + errorData.get("error"));
        errorDTO.setDescription(errorData.get("message").toString());
        // 可以添加更多信息
        return errorDTO;
    }

    /**
     * 获取房源不可用日期
     * @param id 房源ID
     * @return 不可用日期列表
     */
    @GetMapping("/{id}/unavailable-dates")
    public ResponseEntity<Map<String, Object>> getUnavailableDates(@PathVariable Long id) {
        logger.info("获取房源ID: {} 的不可用日期", id);
        try {
            List<LocalDate> unavailableDates = homestayService.getUnavailableDates(id);
            
            // 转换为字符串格式，确保前端能正确解析
            List<String> dateStrings = unavailableDates.stream()
                    .map(LocalDate::toString) // 格式为 YYYY-MM-DD
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", dateStrings);
            response.put("count", dateStrings.size());
            response.put("homestayId", id);
            
            logger.info("房源ID: {} 共有 {} 个不可用日期: {}", id, dateStrings.size(), dateStrings);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取房源不可用日期时发生错误: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("data", new ArrayList<>());
            errorResponse.put("count", 0);
            errorResponse.put("error", "获取不可用日期失败: " + e.getMessage());
            return ResponseEntity.ok(errorResponse); // 返回200状态码但包含错误信息
        }
    }
}
