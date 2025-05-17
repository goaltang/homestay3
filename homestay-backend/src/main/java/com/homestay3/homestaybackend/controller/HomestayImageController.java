package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.entity.HomestayImage;
import com.homestay3.homestaybackend.service.HomestayImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/homestay-images")
@CrossOrigin(
    origins = {"*"}, 
    allowCredentials = "false",
    allowedHeaders = "*", 
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}
)
public class HomestayImageController {
    
    private static final Logger logger = LoggerFactory.getLogger(HomestayImageController.class);
    
    @Autowired
    private HomestayImageService homestayImageService;
    
    /**
     * 上传单张民宿图片
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("homestayId") Long homestayId) {
        
        logger.info("接收到民宿图片上传请求，民宿ID: {}, 文件名: {}", homestayId, file.getOriginalFilename());
        
        try {
            HomestayImage image = homestayImageService.uploadAndSaveHomestayImage(homestayId, file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "图片上传成功");
            response.put("data", convertImageToMap(image));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("上传民宿图片失败: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "图片上传失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 批量上传民宿图片
     */
    @PostMapping("/upload-multiple")
    public ResponseEntity<Map<String, Object>> uploadMultipleImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("homestayId") Long homestayId) {
        
        logger.info("接收到批量上传民宿图片请求，民宿ID: {}, 文件数量: {}", homestayId, files.size());
        
        try {
            List<HomestayImage> images = homestayImageService.uploadAndSaveHomestayImages(homestayId, files);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "图片批量上传成功");
            response.put("data", images.stream().map(this::convertImageToMap).collect(Collectors.toList()));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("批量上传民宿图片失败: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "图片批量上传失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取指定民宿的所有图片
     */
    @GetMapping("/homestay/{homestayId}")
    public ResponseEntity<Map<String, Object>> getImagesByHomestayId(@PathVariable Long homestayId) {
        logger.info("获取民宿图片，民宿ID: {}", homestayId);
        
        try {
            List<HomestayImage> images = homestayImageService.getImagesByHomestayId(homestayId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", images.stream().map(this::convertImageToMap).collect(Collectors.toList()));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取民宿图片失败: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "获取民宿图片失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除指定的图片
     */
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Map<String, Object>> deleteImage(@PathVariable Long imageId) {
        logger.info("删除图片，图片ID: {}", imageId);
        
        try {
            homestayImageService.deleteImage(imageId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "图片删除成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除图片失败: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "删除图片失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除指定民宿的所有图片
     */
    @DeleteMapping("/homestay/{homestayId}")
    public ResponseEntity<Map<String, Object>> deleteImagesByHomestayId(@PathVariable Long homestayId) {
        logger.info("删除民宿所有图片，民宿ID: {}", homestayId);
        
        try {
            homestayImageService.deleteImagesByHomestayId(homestayId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "民宿所有图片删除成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除民宿所有图片失败: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "删除民宿所有图片失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 将HomestayImage对象转换为Map
     */
    private Map<String, Object> convertImageToMap(HomestayImage image) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", image.getId());
        map.put("homestayId", image.getHomestayId());
        map.put("image", image.getImage());
        map.put("imageUrl", image.getImageUrl());
        return map;
    }
} 