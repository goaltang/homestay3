package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.HomestayImage;
import com.homestay3.homestaybackend.repository.HomestayImageRepository;
import com.homestay3.homestaybackend.service.FileService;
import com.homestay3.homestaybackend.service.HomestayImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.IOException;

@Service
public class HomestayImageServiceImpl implements HomestayImageService {
    
    @Autowired
    private HomestayImageRepository homestayImageRepository;
    
    @Autowired
    private FileService fileService;
    
    @Override
    @Transactional
    public HomestayImage uploadAndSaveHomestayImage(Long homestayId, MultipartFile file) {
        // 上传文件并获取包含URL的Map
        try {
            Map<String, Object> fileResult = fileService.uploadFile(file, "homestay");
            String imageUrl = (String) fileResult.get("fileUrl"); // Extract fileUrl from the map
            String originalFilename = (String) fileResult.get("originalFileName"); // Extract original filename if needed

            // 创建并保存HomestayImage实体
            HomestayImage homestayImage = new HomestayImage();
            homestayImage.setHomestayId(homestayId);
            homestayImage.setImageUrl(imageUrl); // Use the constructed URL
            homestayImage.setImage(originalFilename); // Use original filename from result
            
            return homestayImageRepository.save(homestayImage);
        } catch (IOException e) {
            // Handle or rethrow the exception appropriately
            throw new RuntimeException("Failed to upload and save homestay image", e);
        }
    }
    
    @Override
    @Transactional
    public List<HomestayImage> uploadAndSaveHomestayImages(Long homestayId, List<MultipartFile> files) {
        List<HomestayImage> results = new ArrayList<>();
        
        for (MultipartFile file : files) {
            HomestayImage image = uploadAndSaveHomestayImage(homestayId, file);
            results.add(image);
        }
        
        return results;
    }
    
    @Override
    public List<HomestayImage> getImagesByHomestayId(Long homestayId) {
        return homestayImageRepository.findByHomestayId(homestayId);
    }
    
    @Override
    @Transactional
    public void deleteImage(Long imageId) {
        homestayImageRepository.findById(imageId).ifPresent(image -> {
            // 从文件系统删除图片
            try {
                fileService.deleteFile(image.getImageUrl());
            } catch (Exception e) {
                // 记录错误但继续执行，确保数据库记录被删除
                System.err.println("删除图片文件失败: " + e.getMessage());
            }
            
            // 从数据库删除记录
            homestayImageRepository.deleteById(imageId);
        });
    }
    
    @Override
    @Transactional
    public void deleteImagesByHomestayId(Long homestayId) {
        List<HomestayImage> images = homestayImageRepository.findByHomestayId(homestayId);
        
        // 从文件系统删除图片
        for (HomestayImage image : images) {
            try {
                fileService.deleteFile(image.getImageUrl());
            } catch (Exception e) {
                // 记录错误但继续执行
                System.err.println("删除图片文件失败: " + e.getMessage());
            }
        }
        
        // 从数据库批量删除
        homestayImageRepository.deleteByHomestayId(homestayId);
    }
} 