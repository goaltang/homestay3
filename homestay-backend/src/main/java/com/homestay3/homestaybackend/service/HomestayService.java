package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.dto.HomestayRequest;
import com.homestay3.homestaybackend.dto.HomestaySearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface HomestayService {
    
    /**
     * 获取所有房源
     */
    List<HomestayDTO> getAllHomestays();
    
    /**
     * 获取推荐房源
     */
    List<HomestayDTO> getFeaturedHomestays();
    
    /**
     * 根据ID获取房源详情
     */
    HomestayDTO getHomestayById(Long id);
    
    /**
     * 根据房源类型获取房源列表
     */
    List<HomestayDTO> getHomestaysByPropertyType(String propertyType);
    
    /**
     * 搜索房源
     */
    List<HomestayDTO> searchHomestays(HomestaySearchRequest request);
    
    /**
     * 上传民宿图片
     */
    String uploadHomestayImage(MultipartFile file);
    
    /**
     * 分页获取民宿
     */
    Page<HomestayDTO> getHomestaysByPage(Pageable pageable);
    
    /**
     * 获取用户拥有的民宿
     */
    List<HomestayDTO> getHomestaysByOwner(String username);
    
    /**
     * 创建民宿
     */
    HomestayDTO createHomestay(HomestayDTO homestayDTO, String ownerUsername);
    
    /**
     * 更新民宿
     */
    HomestayDTO updateHomestay(Long id, HomestayDTO homestayDTO);
    
    /**
     * 搜索民宿
     */
    List<HomestayDTO> searchHomestays(String keyword, String province, String city, 
                                     Integer minPrice, Integer maxPrice, 
                                     Integer guests, String type);
    
    /**
     * 删除民宿
     */
    void deleteHomestay(Long id);
    
    /**
     * 更新民宿状态
     */
    HomestayDTO updateHomestayStatus(Long id, String status, String ownerUsername);
    
    /**
     * 管理员获取民宿列表（分页和筛选）
     */
    Page<HomestayDTO> getAdminHomestays(Pageable pageable, String title, String status, String type);
    
    /**
     * 管理员创建民宿
     */
    HomestayDTO createHomestay(HomestayDTO homestayDTO);
    
    /**
     * 管理员更新民宿状态
     */
    void updateHomestayStatus(Long id, String status);
} 