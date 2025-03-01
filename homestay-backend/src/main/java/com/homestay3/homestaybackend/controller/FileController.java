package com.homestay3.homestaybackend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private static final Logger log = LoggerFactory.getLogger(FileController.class);
    private final String projectRoot = System.getProperty("user.dir");
    private final String uploadDir = projectRoot + File.separator + "uploads";
    private final String avatarDir = uploadDir + File.separator + "avatars";

    /**
     * 通过API访问头像文件
     */
    @GetMapping("/avatar/{filename:.+}")
    public ResponseEntity<Resource> getAvatarFile(@PathVariable String filename) {
        log.info("请求头像文件: {}", filename);
        try {
            Path filePath = Paths.get(avatarDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                log.info("找到头像文件: {}", filePath);
                
                // 确定内容类型
                String contentType = determineContentType(filename);
                
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                log.warn("头像文件不存在: {}", filePath);
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            log.error("无法加载头像文件: {}", filename, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 测试头像文件是否可访问
     */
    @GetMapping("/test-avatar")
    public ResponseEntity<Map<String, Object>> testAvatarAccess(@RequestParam String path) {
        log.info("测试头像访问: {}", path);
        Map<String, Object> result = new HashMap<>();
        
        // 从路径中提取文件名
        String filename = path;
        if (path.contains("/")) {
            filename = path.substring(path.lastIndexOf("/") + 1);
        }
        
        Path filePath = Paths.get(avatarDir).resolve(filename).normalize();
        File file = filePath.toFile();
        
        result.put("requestPath", path);
        result.put("extractedFilename", filename);
        result.put("resolvedPath", filePath.toString());
        result.put("exists", file.exists());
        result.put("canRead", file.canRead());
        result.put("size", file.exists() ? file.length() : 0);
        
        if (file.exists()) {
            try {
                result.put("contentType", Files.probeContentType(filePath));
            } catch (IOException e) {
                result.put("contentTypeError", e.getMessage());
            }
            
            // 构建可访问的URL
            String apiUrl = "/api/files/avatar/" + filename;
            result.put("apiUrl", apiUrl);
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 根据文件名确定内容类型
     */
    private String determineContentType(String filename) {
        try {
            Path filePath = Paths.get(avatarDir).resolve(filename);
            String contentType = Files.probeContentType(filePath);
            if (contentType != null) {
                return contentType;
            }
        } catch (IOException e) {
            log.warn("无法确定文件内容类型: {}", filename, e);
        }
        
        // 根据文件扩展名判断
        if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filename.toLowerCase().endsWith(".png")) {
            return "image/png";
        } else if (filename.toLowerCase().endsWith(".gif")) {
            return "image/gif";
        } else {
            return "application/octet-stream";
        }
    }

    /**
     * 测试权限配置
     */
    @GetMapping("/test-auth")
    public ResponseEntity<Map<String, Object>> testAuth() {
        log.info("测试权限配置端点被访问");
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "如果您看到此消息，表示您已通过身份验证");
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }
    
    /**
     * 测试无需认证的端点
     */
    @GetMapping("/test-public")
    public ResponseEntity<Map<String, Object>> testPublic() {
        log.info("测试公共端点被访问");
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "这是一个公共端点，无需认证");
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }
} 