package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.HomestayTypeDTO;
import com.homestay3.homestaybackend.dto.TypeCategoryDTO;
import com.homestay3.homestaybackend.model.HomestayType;
import com.homestay3.homestaybackend.service.HomestayTypeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/homestay-types", "/api/v1/homestay-types"})
@RequiredArgsConstructor
@CrossOrigin(
    origins = {"http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:5174", "http://127.0.0.1:5174"},
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
public class HomestayTypeController {
    
    private static final Logger logger = LoggerFactory.getLogger(HomestayTypeController.class);
    
    private final HomestayTypeService homestayTypeService;
    
    // 房源类型API
    
    /**
     * 获取所有房源类型 (仅激活状态，供前端首页使用)
     */
    @GetMapping
    public ResponseEntity<List<HomestayTypeDTO>> getAllHomestayTypes() {
        logger.info("收到获取所有激活房源类型的请求 (for homepage)");
        try {
            List<HomestayTypeDTO> types = homestayTypeService.getActiveHomestayTypes();
            logger.info("成功获取 {} 条激活的房源类型数据 (DTO)", types.size());
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            logger.error("获取激活房源类型时发生错误: {}", e.getMessage(), e);
            // 返回空列表或错误状态码
            return ResponseEntity.internalServerError().build(); 
        }
    }
    
    /**
     * 获取所有房源类型（包括非激活）- 管理员访问
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllHomestayTypesAdmin() {
        logger.info("获取所有房源类型（管理员）");
        List<HomestayTypeDTO> types = homestayTypeService.getAllHomestayTypes();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", types);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 根据ID获取房源类型
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getHomestayTypeById(@PathVariable Long id) {
        logger.info("根据ID获取房源类型: {}", id);
        HomestayTypeDTO type = homestayTypeService.getHomestayTypeById(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", type);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 根据代码获取房源类型
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<Map<String, Object>> getHomestayTypeByCode(@PathVariable String code) {
        logger.info("根据代码获取房源类型: {}", code);
        HomestayTypeDTO type = homestayTypeService.getHomestayTypeByCode(code);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", type);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 创建房源类型
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createHomestayType(@RequestBody HomestayTypeDTO homestayTypeDTO) {
        logger.info("创建房源类型: {}", homestayTypeDTO.getName());
        HomestayTypeDTO createdType = homestayTypeService.createHomestayType(homestayTypeDTO);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", createdType);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 更新房源类型
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateHomestayType(
            @PathVariable Long id, 
            @RequestBody HomestayTypeDTO homestayTypeDTO) {
        logger.info("更新房源类型, ID: {}", id);
        HomestayTypeDTO updatedType = homestayTypeService.updateHomestayType(id, homestayTypeDTO);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", updatedType);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 删除房源类型
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteHomestayType(@PathVariable Long id) {
        logger.info("删除房源类型, ID: {}", id);
        homestayTypeService.deleteHomestayType(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "房源类型已删除");
        return ResponseEntity.ok(response);
    }
    
    // 分类API
    
    /**
     * 获取所有分类
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getAllCategories() {
        logger.info("获取所有房源类型分类");
        List<TypeCategoryDTO> categories = homestayTypeService.getAllCategories();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", categories);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 根据ID获取分类
     */
    @GetMapping("/categories/{id}")
    public ResponseEntity<Map<String, Object>> getCategoryById(@PathVariable Long id) {
        logger.info("根据ID获取分类: {}", id);
        TypeCategoryDTO category = homestayTypeService.getCategoryById(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", category);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 创建分类
     */
    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createCategory(@RequestBody TypeCategoryDTO categoryDTO) {
        logger.info("创建房源类型分类: {}", categoryDTO.getName());
        TypeCategoryDTO createdCategory = homestayTypeService.createCategory(categoryDTO);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", createdCategory);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 更新分类
     */
    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateCategory(
            @PathVariable Long id, 
            @RequestBody TypeCategoryDTO categoryDTO) {
        logger.info("更新房源类型分类, ID: {}", id);
        TypeCategoryDTO updatedCategory = homestayTypeService.updateCategory(id, categoryDTO);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", updatedCategory);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 删除分类
     */
    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteCategory(@PathVariable Long id) {
        logger.info("删除房源类型分类, ID: {}", id);
        homestayTypeService.deleteCategory(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "房源类型分类已删除");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取按分类分组的房源类型
     */
    @GetMapping("/by-category")
    public ResponseEntity<Map<String, Object>> getHomestayTypesByCategory() {
        logger.info("获取按分类分组的房源类型");
        Map<String, List<HomestayTypeDTO>> typesByCategory = homestayTypeService.getHomestayTypesByCategory();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", typesByCategory);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取指定分类下的房源类型
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Map<String, Object>> getHomestayTypesByCategoryId(@PathVariable Long categoryId) {
        logger.info("获取分类下的房源类型, 分类ID: {}", categoryId);
        List<HomestayTypeDTO> types = homestayTypeService.getHomestayTypesByCategoryId(categoryId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", types);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 兼容原API - 获取旧版房源类型列表
     */
    @GetMapping("/legacy-types")
    public ResponseEntity<Map<String, Object>> getLegacyTypes() {
        logger.info("获取旧版房源类型列表");
        List<HomestayTypeDTO> activeTypes = homestayTypeService.getActiveHomestayTypes();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", activeTypes.stream()
                .map(type -> {
                    Map<String, String> legacyType = new HashMap<>();
                    legacyType.put("value", type.getCode());
                    legacyType.put("label", type.getName());
                    return legacyType;
                })
                .toList());
        
        return ResponseEntity.ok(response);
    }
} 