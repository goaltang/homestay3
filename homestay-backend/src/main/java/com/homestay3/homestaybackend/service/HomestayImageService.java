package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.entity.HomestayImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface HomestayImageService {
    
    /**
     * 上传民宿图片并保存图片信息
     * @param homestayId 民宿ID
     * @param file 图片文件
     * @return 图片实体
     */
    HomestayImage uploadAndSaveHomestayImage(Long homestayId, MultipartFile file);
    
    /**
     * 批量上传民宿图片
     * @param homestayId 民宿ID
     * @param files 图片文件列表
     * @return 图片实体列表
     */
    List<HomestayImage> uploadAndSaveHomestayImages(Long homestayId, List<MultipartFile> files);
    
    /**
     * 根据民宿ID获取所有图片
     * @param homestayId 民宿ID
     * @return 图片列表
     */
    List<HomestayImage> getImagesByHomestayId(Long homestayId);
    
    /**
     * 删除图片
     * @param imageId 图片ID
     */
    void deleteImage(Long imageId);
    
    /**
     * 根据民宿ID删除所有图片
     * @param homestayId 民宿ID
     */
    void deleteImagesByHomestayId(Long homestayId);
} 