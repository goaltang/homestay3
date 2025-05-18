package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.HomestayImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomestayImageRepository extends JpaRepository<HomestayImage, Long> {
    
    /**
     * 根据民宿ID查找所有图片
     * @param homestayId 民宿ID
     * @return 图片列表
     */
    List<HomestayImage> findByHomestayId(Long homestayId);
    
    /**
     * 根据民宿ID删除所有图片
     * @param homestayId 民宿ID
     */
    void deleteByHomestayId(Long homestayId);
} 