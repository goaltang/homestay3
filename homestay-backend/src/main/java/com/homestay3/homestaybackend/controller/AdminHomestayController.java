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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type
    ) {
        logger.info("管理员获取房源列表，页码: {}, 每页数量: {}, 标题: {}, 状态: {}, 类型: {}", 
                page, size, title, status, type);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<HomestayDTO> homestays = homestayService.getAdminHomestays(pageable, title, status, type);
        
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
        HomestayDTO homestay = homestayService.getHomestayById(id);
        return ResponseEntity.ok(homestay);
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
} 