package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.config.FileStorageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.Map;

/**
 * 文件服务接口
 * 处理文件的上传、下载和删除
 */
public interface FileService {
    
    /**
     * 常量定义
     */
    String TYPE_AVATAR = "avatar";
    String TYPE_HOMESTAY = "homestay";
    String TYPE_REVIEW = "review";
    String TYPE_COMMON = "common";
    
    /**
     * 存储文件
     * @param file 文件对象
     * @param type 文件类型（avatar, homestay, review, common）
     * @return 存储的文件名（包括UUID）
     */
    String storeFile(MultipartFile file, String type);
    
    /**
     * 上传文件（兼容旧方法）
     * @param file 上传的文件
     * @param type 文件类型
     * @return 包含文件URL和相关信息的Map
     * @throws IOException 文件处理异常
     */
    Map<String, Object> uploadFile(MultipartFile file, String type) throws IOException;
    
    /**
     * 加载文件资源
     * @param fileName 文件名
     * @param type 文件类型
     * @return 文件资源
     * @throws MalformedURLException 无效的URL异常
     */
    Resource loadFileAsResource(String fileName, String type) throws MalformedURLException;
    
    /**
     * 删除文件
     * @param fileUrl 文件URL或文件名
     * @return 是否删除成功
     */
    boolean deleteFile(String fileUrl);
    
    /**
     * 确定文件内容类型
     * @param filePath 文件路径
     * @return 内容类型
     */
    String determineContentType(Path filePath);
} 