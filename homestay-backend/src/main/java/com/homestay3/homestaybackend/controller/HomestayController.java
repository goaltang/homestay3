package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.dto.HomestaySearchRequest;
import com.homestay3.homestaybackend.service.HomestayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/homestays")
@CrossOrigin(origins = "*")
public class HomestayController {

    private static final Logger logger = LoggerFactory.getLogger(HomestayController.class);

    @Autowired
    private HomestayService homestayService;

    @GetMapping
    public ResponseEntity<List<HomestayDTO>> getAllHomestays() {
        logger.info("获取所有民宿");
        return ResponseEntity.ok(homestayService.getAllHomestays());
    }
    
    @GetMapping("/featured")
    public ResponseEntity<List<HomestayDTO>> getFeaturedHomestays() {
        logger.info("获取推荐民宿");
        return ResponseEntity.ok(homestayService.getFeaturedHomestays());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<HomestayDTO> getHomestayById(@PathVariable Long id) {
        logger.info("获取民宿详情，ID: {}", id);
        return ResponseEntity.ok(homestayService.getHomestayById(id));
    }
    
    @GetMapping("/property-type/{type}")
    public ResponseEntity<List<HomestayDTO>> getHomestaysByPropertyType(@PathVariable String type) {
        logger.info("按物业类型获取民宿，类型: {}", type);
        return ResponseEntity.ok(homestayService.getHomestaysByPropertyType(type));
    }
    
    @PostMapping("/search")
    public ResponseEntity<List<HomestayDTO>> searchHomestays(@RequestBody HomestaySearchRequest request) {
        logger.info("搜索民宿，请求参数: {}", request);
        return ResponseEntity.ok(homestayService.searchHomestays(request));
    }
} 