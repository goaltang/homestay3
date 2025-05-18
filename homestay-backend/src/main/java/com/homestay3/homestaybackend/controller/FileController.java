package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件控制器
 * 处理文件上传、下载和删除请求
 */
@RestController
@RequestMapping("/api/files")
@CrossOrigin(
    origins = {"*"}, 
    allowCredentials = "false",
    allowedHeaders = "*", 
    exposedHeaders = {"Content-Disposition", "Access-Control-Allow-Origin"},
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}
)
public class FileController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    
    @Autowired
    private FileService fileService;
    
    /**
     * 上传单个文件
     * @param file 文件
     * @param type 文件类型（avatar, homestay, review, common）
     * @return 包含文件URL和详细信息的响应
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "common") String type,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        String origin = request.getHeader("Origin");
        logger.info("接收到文件上传请求: 来源={}, fileName={}, contentType={}, size={}KB, type={}", 
               origin, file.getOriginalFilename(), file.getContentType(), file.getSize()/1024, type);
        
        try {
            if (file.isEmpty()) {
                logger.warn("接收到空文件上传请求");
                Map<String, Object> response1 = new HashMap<>();
                response1.put("status", "error");
                response1.put("message", "请选择要上传的文件");
                return ResponseEntity.badRequest().body(response1);
            }
            
            // 记录请求头信息，帮助诊断CORS问题
            logger.info("上传请求头信息: Content-Type={}, Origin={}, Referer={}", 
                file.getContentType(), 
                request.getHeader("Origin"),
                request.getHeader("Referer"));
            
            String fileName = fileService.storeFile(file, type);
            logger.info("文件已成功存储: 新文件名={}", fileName);
            
            // 构建文件访问URL
            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/files/")
                    .path(type + "/")
                    .path(fileName)
                    .toUriString();
            
            logger.info("文件上传成功: fileName={}, url={}", fileName, fileDownloadUri);
            
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("status", "success");
            responseMap.put("fileName", fileName);
            responseMap.put("fileType", file.getContentType());
            responseMap.put("fileSize", file.getSize());
            responseMap.put("downloadUrl", fileDownloadUri);
            responseMap.put("data", new HashMap<String, Object>() {{
                put("fileName", fileName);
                put("url", fileDownloadUri);
            }});
            
            return ResponseEntity.ok()
                   .contentType(MediaType.APPLICATION_JSON)
                   .body(responseMap);
            
        } catch (IllegalArgumentException e) {
            logger.error("参数错误导致文件上传失败: {}", e.getMessage());
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("status", "error");
            responseMap.put("message", e.getMessage());
            return ResponseEntity.badRequest()
                   .body(responseMap);
        } catch (Exception e) {
            logger.error("文件上传处理过程中发生异常: fileName={}, error={}", 
                file.getOriginalFilename(), e.getMessage(), e);
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("status", "error");
            responseMap.put("message", "文件上传失败: " + e.getMessage());
            return ResponseEntity.internalServerError()
                   .body(responseMap);
        }
    }
    
    /**
     * 上传多个文件
     * @param files 文件数组
     * @param type 文件类型
     * @return 包含多个文件URL和信息的响应
     */
    @PostMapping("/uploadMultiple")
    public ResponseEntity<Map<String, Object>> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "type", defaultValue = "common") String type) {
        
        try {
            List<Map<String, Object>> fileResponses = Arrays.stream(files)
                    .map(file -> {
                        try {
                            String fileName = fileService.storeFile(file, type);
                            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                                    .path("/api/files/")
                                    .path(type + "/")
                                    .path(fileName)
                                    .toUriString();
                            
                            Map<String, Object> fileInfo = new HashMap<>();
                            fileInfo.put("fileName", fileName);
                            fileInfo.put("fileType", file.getContentType());
                            fileInfo.put("fileSize", file.getSize());
                            fileInfo.put("downloadUrl", fileDownloadUri);
                            return fileInfo;
                        } catch (Exception e) {
                            Map<String, Object> errorInfo = new HashMap<>();
                            errorInfo.put("error", true);
                            errorInfo.put("fileName", file.getOriginalFilename());
                            errorInfo.put("message", e.getMessage());
                            return errorInfo;
                        }
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("files", fileResponses);
            
            return ResponseEntity.ok().body(response);
            
        } catch (Exception e) {
            logger.error("多文件上传失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "多文件上传失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 下载文件
     * @param fileName 文件名
     * @param type 文件类型
     * @param request HTTP请求
     * @return 文件资源
     */
    @GetMapping("/{type}/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileName,
            @PathVariable String type,
            HttpServletRequest request) {
        
        logger.info("接收到文件下载请求: fileName={}, type={}", fileName, type);
        
        try {
            // 先尝试直接根据路径加载文件
            Path filePath = Paths.get("uploads", type, fileName);
            
            // 如果文件不存在，再尝试使用UUID查找
            if (!Files.exists(filePath)) {
                // 加载文件作为资源
                Resource resource = fileService.loadFileAsResource(fileName, type);
                
                // 尝试确定文件的内容类型
                String contentType = null;
                try {
                    contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
                } catch (IOException ex) {
                    logger.info("无法确定文件类型");
                }
                
                // 如果无法确定内容类型，则默认为二进制文件类型
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                
                logger.info("文件下载成功: fileName={}, type={}, contentType={}", fileName, type, contentType);
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                // 直接从路径加载文件
                Resource resource = new UrlResource(filePath.toUri());
                
                // 尝试确定文件的内容类型
                String contentType = request.getServletContext().getMimeType(filePath.toString());
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                
                logger.info("直接从路径加载文件成功: filePath={}, contentType={}", filePath, contentType);
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        .body(resource);
            }
            
        } catch (Exception e) {
            logger.error("文件下载失败: fileName={}, type={}, error={}", fileName, type, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 删除文件
     * @param fileName 文件名
     * @param type 文件类型
     * @return 删除操作结果
     */
    @DeleteMapping("/{type}/{fileName:.+}")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @PathVariable String fileName,
            @PathVariable String type) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 构建文件路径
        String filePath = "/uploads/" + type + "/" + fileName;
        boolean result = fileService.deleteFile(filePath);
        
        if (result) {
            response.put("status", "success");
            response.put("message", "文件删除成功");
            return ResponseEntity.ok().body(response);
        } else {
            response.put("status", "error");
            response.put("message", "文件删除失败");
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 