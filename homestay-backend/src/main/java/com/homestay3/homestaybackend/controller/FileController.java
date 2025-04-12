package com.homestay3.homestaybackend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(
    origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, 
    allowedHeaders = "*", 
    methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, 
        RequestMethod.DELETE, RequestMethod.OPTIONS
    },
    exposedHeaders = {
        "Content-Type", "Content-Length", "Authorization",
        "Access-Control-Allow-Origin", "Access-Control-Allow-Headers"
    },
    allowCredentials = "true",
    maxAge = 3600
)
public class FileController {
    private static final Logger log = LoggerFactory.getLogger(FileController.class);
    private final String projectRoot = System.getProperty("user.dir");
    private final String uploadDir = projectRoot + File.separator + "uploads";
    private final String avatarDir = uploadDir + File.separator + "avatars";
    private final String homestayDir = uploadDir + File.separator + "homestays";

    /**
     * 获取头像文件
     */
    @GetMapping("/avatar/{filename:.+}")
    public ResponseEntity<Resource> getAvatarFile(@PathVariable String filename) {
        try {
            // 构建文件路径
            Path filePath = Paths.get(avatarDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            // 检查文件是否存在
            if (resource.exists()) {
                // 确定内容类型
                String contentType = determineContentType(filePath);
                
                log.info("Serving avatar file: {}, content type: {}", filename, contentType);
                
                // 返回文件
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                log.warn("Avatar file not found: {}", filename);
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            log.error("Error getting avatar file: {}", filename, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取房源图片文件
     */
    @GetMapping("/homestay/{filename:.+}")
    public ResponseEntity<Resource> getHomestayFile(@PathVariable String filename) {
        try {
            // 构建文件路径
            Path filePath = Paths.get(homestayDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            // 检查文件是否存在
            if (resource.exists()) {
                // 确定内容类型
                String contentType = determineContentType(filePath);
                
                log.info("Serving homestay file: {}, content type: {}", filename, contentType);
                
                // 返回文件
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                log.warn("Homestay file not found: {}", filename);
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            log.error("Error getting homestay file: {}", filename, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 上传文件通用接口
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "homestay") String type) {
        log.info("Receiving file upload request, type: {}, original filename: {}, size: {}", 
                type, file.getOriginalFilename(), file.getSize());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 检查文件是否为空
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "请选择要上传的文件");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取文件名和扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // 生成唯一文件名
            String newFilename = UUID.randomUUID().toString() + extension;
            
            // 确定上传目录
            Path uploadPath;
            String fileUrl;
            if ("avatar".equals(type)) {
                uploadPath = Paths.get(avatarDir);
                fileUrl = "/api/files/avatar/" + newFilename;
            } else {
                uploadPath = Paths.get(homestayDir);
                fileUrl = "/api/files/homestay/" + newFilename;
            }
            
            // 确保目录存在
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created directory: {}", uploadPath);
            }
            
            // 保存文件
            Path targetLocation = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved to: {}", targetLocation);
            
            // 构建响应
            response.put("success", true);
            response.put("data", Map.of(
                "url", fileUrl,
                "filename", newFilename,
                "originalFilename", originalFilename
            ));
            response.put("message", "文件上传成功");
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to upload file", e);
            response.put("success", false);
            response.put("message", "文件上传失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 确定文件的内容类型
     */
    private String determineContentType(Path filePath) {
        try {
            return Files.probeContentType(filePath);
        } catch (IOException e) {
            log.warn("Could not determine content type for file: {}", filePath, e);
            return "application/octet-stream";
        }
    }

    /**
     * 测试文件访问
     */
    @GetMapping("/test")
    public ResponseEntity<String> testFileAccess() {
        // 测试上传目录是否存在
        Path avatarPath = Paths.get(avatarDir);
        Path homestayPath = Paths.get(homestayDir);
        
        boolean avatarDirExists = Files.exists(avatarPath);
        boolean homestayDirExists = Files.exists(homestayPath);
        
        return ResponseEntity.ok("File access test: Avatar directory exists: " + avatarDirExists + 
                                ", Homestay directory exists: " + homestayDirExists);
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