package com.homestay3.homestaybackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置类
 * 读取application.properties中的文件上传配置
 */
@Component
@ConfigurationProperties(prefix = "file")
public class FileStorageConfig {
    
    /**
     * 文件上传目录
     */
    private String uploadDir;
    
    /**
     * 最大文件大小限制（字节）
     */
    private long maxFileSize;
    
    /**
     * 允许的文件类型（MIME类型）
     */
    private String[] allowedFileTypes;
    
    /**
     * 获取文件上传目录
     */
    public String getUploadDir() {
        return uploadDir;
    }
    
    /**
     * 设置文件上传目录
     */
    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }
    
    /**
     * 获取最大文件大小限制
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }
    
    /**
     * 设置最大文件大小限制
     */
    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
    
    /**
     * 获取允许的文件类型
     */
    public String[] getAllowedFileTypes() {
        return allowedFileTypes;
    }
    
    /**
     * 设置允许的文件类型
     */
    public void setAllowedFileTypes(String[] allowedFileTypes) {
        this.allowedFileTypes = allowedFileTypes;
    }
} 