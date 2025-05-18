package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.config.FileStorageConfig;
import com.homestay3.homestaybackend.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;

/**
 * 文件服务实现类
 * 处理文件上传、下载和管理
 */
@Service
public class FileServiceImpl implements FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    // 上传目录常量
    private static final String AVATAR_DIR = "avatar";
    private static final String HOMESTAY_DIR = "homestay";
    private static final String REVIEW_DIR = "review";
    private static final String COMMON_DIR = "common";

    // 文件上传根目录
    private final Path uploadPath;
    
    // 各类型文件的子目录
    private final Path avatarDir;
    private final Path homestayDir;
    private final Path reviewDir;
    private final Path commonDir;
    
    // 文件存储配置
    private final FileStorageConfig fileStorageConfig;

    /**
     * 构造函数，初始化上传目录
     */
    @Autowired
    public FileServiceImpl(@Value("${file.upload-dir:uploads}") String uploadDir, FileStorageConfig fileStorageConfig) {
        this.fileStorageConfig = fileStorageConfig;
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        // 初始化各类型文件的目录
        this.avatarDir = this.uploadPath.resolve(AVATAR_DIR);
        this.homestayDir = this.uploadPath.resolve(HOMESTAY_DIR);
        this.reviewDir = this.uploadPath.resolve(REVIEW_DIR);
        this.commonDir = this.uploadPath.resolve(COMMON_DIR);
        
        try {
            // 创建所有必要的目录
            Files.createDirectories(this.uploadPath);
            Files.createDirectories(this.avatarDir);
            Files.createDirectories(this.homestayDir);
            Files.createDirectories(this.reviewDir);
            Files.createDirectories(this.commonDir);
            
            logger.info("已创建上传目录: {}", this.uploadPath);
        } catch (IOException ex) {
            logger.error("无法创建上传目录: {}", this.uploadPath);
            throw new RuntimeException("无法创建上传目录", ex);
        }
    }
    
    /**
     * 存储文件
     * @param file 文件对象
     * @param type 文件类型
     * @return 存储的文件名
     */
    @Override
    public String storeFile(MultipartFile file, String type) {
        // 检查文件类型是否有效
        if (!isValidFileType(file)) {
            throw new IllegalArgumentException("不支持的文件类型");
        }
        
        // 检查文件大小
        if (file.getSize() > fileStorageConfig.getMaxFileSize()) {
            throw new IllegalArgumentException("文件大小超过限制");
        }
        
        // 获取文件名和扩展名
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = getFileExtension(originalFilename);
        
        // 生成唯一文件名
        String newFilename = UUID.randomUUID().toString() + fileExtension;
        
        // 选择目标目录
        Path targetLocation = getTargetLocation(type);
        Path targetPath = targetLocation.resolve(newFilename);
        
        try {
            // 复制文件到目标位置，如果存在则替换
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("已上传文件: {} 到 {}", originalFilename, targetPath);
            return newFilename;
        } catch (IOException ex) {
            logger.error("文件存储失败", ex);
            throw new RuntimeException("文件存储失败", ex);
        }
    }

    /**
     * 上传文件
     * @param file 上传的文件
     * @param type 文件类型
     * @return 包含文件URL和相关信息的Map
     * @throws IOException 文件处理异常
     */
    @Override
    public Map<String, Object> uploadFile(MultipartFile file, String type) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("无法上传空文件");
        }
        
        // 使用storeFile方法存储文件 (storeFile 返回的是带扩展名的UUID文件名)
        String newFilename = storeFile(file, type);
        
        // 构建静态资源访问URL (基于WebConfig中的映射)
        String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/static/uploads/") // Use the static mapping path configured in WebConfig
                .path(type + "/") // Add the type sub-directory
                .path(newFilename) // Add the unique filename
                .toUriString();
        
        logger.info("Generated static file URL: {}", fileUrl);

        // 返回文件信息
        Map<String, Object> result = new HashMap<>();
        result.put("fileName", newFilename); // Keep the filename itself if needed
        result.put("originalFileName", file.getOriginalFilename());
        result.put("fileUrl", fileUrl); // This now holds the static URL
        result.put("fileSize", file.getSize());
        result.put("contentType", file.getContentType());
        
        return result;
    }

    /**
     * 加载文件作为资源
     * @param filename 文件名
     * @param type 文件类型
     * @return 文件资源
     * @throws MalformedURLException 无效的URL异常
     */
    @Override
    public Resource loadFileAsResource(String filename, String type) throws MalformedURLException {
        try {
            Path targetPath = getTargetLocation(type);
            logger.info("加载文件资源: filename={}, type={}, targetPath={}", filename, type, targetPath);
            
            Path filePath = targetPath.resolve(filename).normalize();
            logger.info("解析的文件路径: {}", filePath.toAbsolutePath());
            
            // 如果文件不存在，尝试在目录中查找所有文件，看是否能找到匹配的UUID文件
            if (!Files.exists(filePath)) {
                logger.warn("文件不存在，尝试查找UUID格式文件: {}", filePath.toAbsolutePath());
                
                // 列出目录中的所有文件
                if (Files.exists(targetPath) && Files.isDirectory(targetPath)) {
                    try (var files = Files.list(targetPath)) {
                        // 优先查找UUID文件 (UUID-filename.ext)
                        var uuidFile = files.filter(file -> {
                            String fn = file.getFileName().toString();
                            // 如果文件名包含原始名称，可能是UUID形式
                            return fn.endsWith(getFileExtension(filename)) && Files.isRegularFile(file);
                        }).findFirst();
                        
                        if (uuidFile.isPresent()) {
                            filePath = uuidFile.get();
                            logger.info("找到了匹配的UUID格式文件: {}", filePath);
                        } else {
                            logger.error("在目录中未找到任何匹配的文件: {} 目录: {}", filename, targetPath);
                            throw new RuntimeException("文件不存在: " + filename);
                        }
                    } catch (IOException e) {
                        logger.error("浏览目录时发生错误: {}", targetPath, e);
                        throw new RuntimeException("查找文件时发生错误", e);
                    }
                } else {
                    logger.error("目标目录不存在或不是目录: {}", targetPath);
                    throw new RuntimeException("文件不存在: " + filename);
                }
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            logger.info("文件资源URI: {}", resource.getURI());
            
            if (resource.exists() || resource.isReadable()) {
                logger.info("成功加载文件资源: {}", filePath);
                return resource;
            } else {
                logger.error("文件不存在或不可读: {}", filePath);
                throw new RuntimeException("文件不存在或不可读: " + filename);
            }
        } catch (MalformedURLException ex) {
            logger.error("无效的文件URL: {}", filename, ex);
            throw new MalformedURLException("无效的文件URL: " + filename);
        } catch (IOException ex) {
            logger.error("读取文件时发生IO异常: {}", filename, ex);
            throw new RuntimeException("读取文件时发生IO异常: " + filename, ex);
        }
    }

    /**
     * 删除文件
     * @param fileUrl 文件URL或文件名
     * @return 是否删除成功
     */
    @Override
    public boolean deleteFile(String fileUrl) {
        try {
            // 如果是完整URL，提取文件名
            String filename = fileUrl;
            if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
                // 提取URL中的文件路径部分
                String[] parts = fileUrl.split("/");
                if (parts.length >= 2) {
                    // 获取最后两部分: type/filename
                    String type = parts[parts.length - 2];
                    filename = parts[parts.length - 1];
                    
                    // 根据type找到目标目录
                    Path targetLocation = getTargetLocation(type);
                    Path filePath = targetLocation.resolve(filename).normalize();
                    return Files.deleteIfExists(filePath);
                }
            } else if (fileUrl.startsWith("/uploads/")) {
                // 处理相对路径格式，例如 /uploads/homestay/filename.jpg
                String relativePath = fileUrl.substring("/uploads/".length());
                String[] parts = relativePath.split("/", 2);
                if (parts.length >= 2) {
                    String type = parts[0];
                    filename = parts[1];
                    
                    // 根据type找到目标目录
                    Path targetLocation = getTargetLocation(type);
                    Path filePath = targetLocation.resolve(filename).normalize();
                    return Files.deleteIfExists(filePath);
                }
            } else {
                // 假设是直接的文件名，尝试在各个目录中查找
                for (String type : new String[]{AVATAR_DIR, HOMESTAY_DIR, REVIEW_DIR, COMMON_DIR}) {
                    Path targetLocation = getTargetLocation(type);
                    Path filePath = targetLocation.resolve(filename).normalize();
                    if (Files.exists(filePath)) {
                        return Files.deleteIfExists(filePath);
                    }
                }
            }
            
            logger.warn("找不到文件: {}", fileUrl);
            return false;
        } catch (IOException ex) {
            logger.error("删除文件失败: {}", fileUrl, ex);
            return false;
        }
    }

    /**
     * 确定文件内容类型
     * @param filePath 文件路径
     * @return 内容类型
     */
    @Override
    public String determineContentType(Path filePath) {
        String filename = filePath.getFileName().toString();
        String extension = getFileExtension(filename).toLowerCase();
        
        switch (extension) {
            case ".png":
                return "image/png";
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".gif":
                return "image/gif";
            case ".webp":
                return "image/webp";
            case ".pdf":
                return "application/pdf";
            case ".doc":
                return "application/msword";
            case ".docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xls":
                return "application/vnd.ms-excel";
            case ".xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".zip":
                return "application/zip";
            case ".rar":
                return "application/x-rar-compressed";
            case ".txt":
                return "text/plain";
            case ".mp4":
                return "video/mp4";
            case ".mp3":
                return "audio/mpeg";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * 根据文件类型获取目标目录
     * @param type 文件类型
     * @return 目标目录路径
     */
    private Path getTargetLocation(String type) {
        if (type == null) {
            return this.commonDir;
        }
        
        switch (type.toLowerCase()) {
            case AVATAR_DIR:
                return this.avatarDir;
            case HOMESTAY_DIR:
                return this.homestayDir;
            case REVIEW_DIR:
                return this.reviewDir;
            case COMMON_DIR:
            default:
                return this.commonDir;
        }
    }

    /**
     * 获取文件扩展名
     * @param filename 文件名
     * @return 文件扩展名（包含"."）
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    /**
     * 检查文件类型是否有效
     * @param file 文件对象
     * @return 是否为有效文件类型
     */
    private boolean isValidFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }
        return Arrays.asList(fileStorageConfig.getAllowedFileTypes()).contains(contentType.toLowerCase());
    }
} 