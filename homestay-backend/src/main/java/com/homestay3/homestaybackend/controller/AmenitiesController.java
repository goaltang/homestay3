package com.homestay3.homestaybackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMethod;

import com.homestay3.homestaybackend.dto.AmenityCategoryDTO;
import com.homestay3.homestaybackend.dto.AmenityDTO;
import com.homestay3.homestaybackend.exception.ResourceAlreadyExistsException;
import com.homestay3.homestaybackend.exception.ResourceInUseException;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.Amenity;
import com.homestay3.homestaybackend.model.AmenityCategory;
import com.homestay3.homestaybackend.service.AmenityService;
import com.homestay3.homestaybackend.repository.AmenityRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/amenities", "/api/v1/amenities"})
@CrossOrigin(
    origins = {"http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:3000", "http://127.0.0.1:3000"}, 
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
public class AmenitiesController {
    
    @Autowired
    private AmenityService amenityService;
    
    @Autowired
    private AmenityRepository amenityRepository;
    
    /**
     * 获取所有可用的房源设施和服务
     * @param keyword 搜索关键词
     * @param onlyActive 是否只显示激活的设施
     * @param categoryCode 分类编码
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 设施和服务列表及分页信息
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllAmenities(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "true") Boolean onlyActive,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        
        List<Amenity> allAmenities;
        
        // 根据参数组合决定使用哪个查询方法
        if (categoryCode != null && !categoryCode.isEmpty()) {
            allAmenities = onlyActive 
                    ? amenityService.getActiveAmenitiesByCategory(categoryCode)
                    : amenityService.getAmenitiesByCategory(categoryCode);
        } else if (keyword != null && !keyword.isEmpty()) {
            allAmenities = onlyActive 
                    ? amenityService.searchActiveAmenities(keyword)
                    : amenityService.searchAmenities(keyword);
        } else {
            allAmenities = onlyActive 
                    ? amenityService.getActiveAmenities()
                    : amenityService.getAllAmenities();
        }
        
        // 计算总记录数
        int totalItems = allAmenities.size();
        
        // 计算总页数
        int totalPages = (int) Math.ceil((double) totalItems / size);
        
        // 校正页码，确保不超出范围
        page = Math.max(0, Math.min(page, totalPages - 1 < 0 ? 0 : totalPages - 1));
        
        // 手动分页
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalItems);
        
        // 获取当前页数据
        List<Amenity> pagedAmenities;
        if (startIndex < totalItems) {
            pagedAmenities = allAmenities.subList(startIndex, endIndex);
        } else {
            pagedAmenities = new ArrayList<>();
        }
        
        // 转换为DTO
        List<AmenityDTO> dtos = pagedAmenities.stream()
                .map(amenity -> {
                    AmenityDTO dto = new AmenityDTO();
                    dto.setValue(amenity.getValue());
                    dto.setLabel(amenity.getLabel());
                    dto.setDescription(amenity.getDescription());
                    dto.setIcon(amenity.getIcon());
                    dto.setActive(amenity.isActive());
                    dto.setUsageCount(amenity.getUsageCount());
                    
                    if (amenity.getCategory() != null) {
                        dto.setCategoryCode(amenity.getCategory().getCode());
                        dto.setCategoryName(amenity.getCategory().getName());
                        dto.setCategoryIcon(amenity.getCategory().getIcon());
                    }
                    
                    return dto;
                })
                .toList();
        
        // 构建响应
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        
        // 包含完整分页信息
        Map<String, Object> pageInfo = new HashMap<>();
        pageInfo.put("content", dtos);
        pageInfo.put("totalElements", totalItems);
        pageInfo.put("totalPages", totalPages);
        pageInfo.put("currentPage", page);
        pageInfo.put("size", size);
        pageInfo.put("numberOfElements", dtos.size());
        pageInfo.put("first", page == 0);
        pageInfo.put("last", page >= totalPages - 1 || totalPages == 0);
        pageInfo.put("empty", dtos.isEmpty());
        
        response.put("data", pageInfo);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 按分类分组获取设施
     * @param onlyActive 是否只显示激活的设施
     * @return 按分类分组的设施列表
     */
    @GetMapping("/by-categories")
    public ResponseEntity<Map<String, Object>> getAmenitiesByCategories(
            @RequestParam(required = false, defaultValue = "true") Boolean onlyActive) {
        
        List<AmenityCategoryDTO> categories = onlyActive 
                ? amenityService.getActiveAmenitiesByCategories()
                : amenityService.getAmenitiesByCategories();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", categories);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取设施使用统计数据
     * @param limit 限制返回的数量
     * @return 设施使用统计
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getAmenityStatistics(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        
        List<AmenityDTO> topAmenities = amenityService.getMostUsedAmenities(limit);
        Map<String, Long> statistics = amenityService.getAmenityUsageStatistics();
        
        Map<String, Object> data = new HashMap<>();
        data.put("topAmenities", topAmenities);
        data.put("statistics", statistics);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取单个设施
     * @param value 设施编码
     * @return 设施详情
     */
    @GetMapping("/{value}")
    public ResponseEntity<Map<String, Object>> getAmenity(@PathVariable String value) {
        try {
            AmenityDTO amenity = amenityService.getAmenityDTOByValue(value);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", amenity);
            
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 创建一个新的设施
     * @param amenityMap 设施数据
     * @return 创建结果
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAmenity(@RequestBody AmenityDTO amenityDTO,
            @RequestHeader(value = "X-Username", required = false, defaultValue = "admin") String username) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (amenityDTO.getValue() == null || amenityDTO.getValue().isEmpty() || 
                amenityDTO.getLabel() == null || amenityDTO.getLabel().isEmpty()) {
                response.put("success", false);
                response.put("message", "设施编码和名称不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            AmenityDTO savedAmenity = amenityService.createAmenity(amenityDTO, username);
            
            response.put("success", true);
            response.put("message", "设施创建成功");
            response.put("data", savedAmenity);
            
            return ResponseEntity.ok(response);
        } catch (ResourceAlreadyExistsException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "创建设施时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 更新设施
     * @param value 设施编码
     * @param amenityDTO 设施数据
     * @return 更新结果
     */
    @PutMapping("/{value}")
    public ResponseEntity<Map<String, Object>> updateAmenity(@PathVariable String value,
            @RequestBody AmenityDTO amenityDTO,
            @RequestHeader(value = "X-Username", required = false, defaultValue = "admin") String username) {
        try {
            AmenityDTO updatedAmenity = amenityService.updateAmenity(value, amenityDTO, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "设施更新成功");
            response.put("data", updatedAmenity);
            
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新设施时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 删除设施
     * @param value 设施编码
     * @return 删除结果
     */
    @DeleteMapping("/{value}")
    public ResponseEntity<Map<String, Object>> deleteAmenity(@PathVariable String value) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 先检查设施是否被使用
            long usageCount = amenityService.countHomestaysUsingAmenity(value);
            if (usageCount > 0) {
                throw new ResourceInUseException("设施正在被" + usageCount + "个房源使用，无法删除");
            }
            
            amenityService.deleteAmenity(value);
            
            response.put("success", true);
            response.put("message", "设施删除成功");
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        } catch (ResourceInUseException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(409).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除设施时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 一键激活所有设施
     * @return 激活结果
     */
    @PutMapping("/activate-all")
    public ResponseEntity<Map<String, Object>> activateAllAmenities() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int count = amenityService.activateAllAmenities();
            
            response.put("success", true);
            response.put("message", "成功激活 " + count + " 个设施");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "激活设施时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 初始化默认设施和分类
     * @return 初始化结果
     */
    @PostMapping("/init-defaults")
    public ResponseEntity<Map<String, Object>> initDefaultAmenities() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 初始化默认分类
            amenityService.initDefaultCategories();
            
            // 初始化默认设施
            amenityService.initDefaultAmenities();
            
            response.put("success", true);
            response.put("message", "成功初始化默认设施和分类");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "初始化设施时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取所有设施分类
     * @return 设施分类列表
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getAllCategories() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<AmenityCategory> categories = amenityService.getAllCategories();
            
            response.put("success", true);
            response.put("data", categories);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取设施分类时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取单个设施分类
     * @param code 分类编码
     * @return 分类详情
     */
    @GetMapping("/categories/{code}")
    public ResponseEntity<Map<String, Object>> getCategory(@PathVariable String code) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            AmenityCategory category = amenityService.getCategoryByCode(code);
            
            response.put("success", true);
            response.put("data", category);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取设施分类时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 创建设施分类
     * @param category 分类数据
     * @return 创建结果
     */
    @PostMapping("/categories")
    public ResponseEntity<Map<String, Object>> createCategory(@RequestBody AmenityCategory category) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            AmenityCategory createdCategory = amenityService.createCategory(category);
            
            response.put("success", true);
            response.put("message", "分类创建成功");
            response.put("data", createdCategory);
            return ResponseEntity.ok(response);
        } catch (ResourceAlreadyExistsException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(409).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "创建设施分类时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 更新设施分类
     * @param code 分类编码
     * @param category 分类数据
     * @return 更新结果
     */
    @PutMapping("/categories/{code}")
    public ResponseEntity<Map<String, Object>> updateCategory(
            @PathVariable String code, 
            @RequestBody AmenityCategory category) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            AmenityCategory updatedCategory = amenityService.updateCategory(code, category);
            
            response.put("success", true);
            response.put("message", "分类更新成功");
            response.put("data", updatedCategory);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "更新设施分类时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 删除设施分类
     * @param code 分类编码
     * @return 删除结果
     */
    @DeleteMapping("/categories/{code}")
    public ResponseEntity<Map<String, Object>> deleteCategory(@PathVariable String code) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            amenityService.deleteCategory(code);
            
            response.put("success", true);
            response.put("message", "设施分类删除成功");
            
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        } catch (ResourceInUseException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除设施分类时发生错误: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 批量删除设施分类
     * @param request 批量删除请求，包含分类编码列表
     * @return 删除结果
     */
    @DeleteMapping("/categories/batch")
    public ResponseEntity<Map<String, Object>> batchDeleteCategories(@RequestBody Map<String, List<String>> request) {
        Map<String, Object> response = new HashMap<>();
        
        List<String> codes = request.get("codes");
        if (codes == null || codes.isEmpty()) {
            response.put("success", false);
            response.put("message", "请提供要删除的分类编码列表");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            // 存储删除失败的分类及原因
            Map<String, String> failures = new HashMap<>();
            int successCount = 0;
            
            for (String code : codes) {
                try {
                    amenityService.deleteCategory(code);
                    successCount++;
                } catch (ResourceNotFoundException e) {
                    failures.put(code, "分类不存在");
                } catch (ResourceInUseException e) {
                    failures.put(code, "分类正在被使用中");
                } catch (Exception e) {
                    failures.put(code, e.getMessage());
                }
            }
            
            if (failures.isEmpty()) {
                response.put("success", true);
                response.put("message", "所有分类删除成功");
            } else if (successCount > 0) {
                response.put("success", true);
                response.put("message", String.format("成功删除%d个分类，%d个分类删除失败", 
                        successCount, failures.size()));
                response.put("failures", failures);
            } else {
                response.put("success", false);
                response.put("message", "所有分类删除失败");
                response.put("failures", failures);
                return ResponseEntity.badRequest().body(response);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "批量删除设施分类时发生错误: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 一键添加所有设施到房源
     * @param homestayId 房源ID
     * @param categoryCode 可选的分类编码
     * @return 添加结果
     */
    @PostMapping("/add-all-to-homestay/{homestayId}")
    public ResponseEntity<Map<String, Object>> addAllAmenitiesToHomestay(
            @PathVariable Long homestayId,
            @RequestParam(required = false) String categoryCode) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<String> addedAmenities = amenityService.addAllAmenitiesToHomestay(homestayId, categoryCode);
            
            response.put("success", true);
            response.put("message", String.format("成功添加%d个设施到房源", addedAmenities.size()));
            response.put("data", addedAmenities);
            
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "添加设施到房源时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 从房源移除所有设施
     * @param homestayId 房源ID
     * @param categoryCode 可选的分类编码
     * @return 移除结果
     */
    @DeleteMapping("/remove-all-from-homestay/{homestayId}")
    public ResponseEntity<Map<String, Object>> removeAllAmenitiesFromHomestay(
            @PathVariable Long homestayId,
            @RequestParam(required = false) String categoryCode) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<String> removedAmenities = amenityService.removeAllAmenitiesFromHomestay(homestayId, categoryCode);
            
            response.put("success", true);
            response.put("message", String.format("成功从房源移除%d个设施", removedAmenities.size()));
            response.put("data", removedAmenities);
            
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "从房源移除设施时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 添加单个设施到房源
     * @param homestayId 房源ID
     * @param amenityValue 设施编码
     * @return 添加结果
     */
    @PostMapping("/add-to-homestay/{homestayId}/{amenityValue}")
    public ResponseEntity<Map<String, Object>> addAmenityToHomestay(
            @PathVariable Long homestayId,
            @PathVariable String amenityValue) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean added = amenityService.addAmenityToHomestay(homestayId, amenityValue);
            
            if (added) {
                response.put("success", true);
                response.put("message", "成功添加设施到房源");
            } else {
                response.put("success", false);
                response.put("message", "设施已经存在于房源中");
            }
            
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "添加设施到房源时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 从房源移除单个设施
     * @param homestayId 房源ID
     * @param amenityValue 设施编码
     * @return 移除结果
     */
    @DeleteMapping("/remove-from-homestay/{homestayId}/{amenityValue}")
    public ResponseEntity<Map<String, Object>> removeAmenityFromHomestay(
            @PathVariable Long homestayId,
            @PathVariable String amenityValue) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean removed = amenityService.removeAmenityFromHomestay(homestayId, amenityValue);
            
            if (removed) {
                response.put("success", true);
                response.put("message", "成功从房源移除设施");
            } else {
                response.put("success", false);
                response.put("message", "设施不存在于房源中");
            }
            
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "从房源移除设施时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取指定房源的设施列表
     * @param homestayId 房源ID
     * @return 设施列表
     */
    @GetMapping("/homestay/{homestayId}")
    public ResponseEntity<Map<String, Object>> getHomestayAmenities(@PathVariable Long homestayId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Amenity> amenities = amenityService.getHomestayAmenities(homestayId);
            
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
            
            response.put("success", true);
            response.put("data", amenityDTOs);
            
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取房源设施时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}