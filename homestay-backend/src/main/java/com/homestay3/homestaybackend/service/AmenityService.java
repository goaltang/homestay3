package com.homestay3.homestaybackend.service;

import java.util.List;
import java.util.Map;

import com.homestay3.homestaybackend.dto.AmenityCategoryDTO;
import com.homestay3.homestaybackend.dto.AmenityDTO;
import com.homestay3.homestaybackend.model.Amenity;
import com.homestay3.homestaybackend.model.AmenityCategory;

public interface AmenityService {
    
    // 基本CRUD操作
    List<Amenity> getAllAmenities();
    
    List<Amenity> getActiveAmenities();
    
    List<Amenity> searchAmenities(String keyword);
    
    List<Amenity> searchActiveAmenities(String keyword);
    
    Amenity getAmenityByValue(String value);
    
    AmenityDTO getAmenityDTOByValue(String value);
    
    Amenity createAmenity(Amenity amenity, String username);
    
    AmenityDTO createAmenity(AmenityDTO amenityDTO, String username);
    
    Amenity updateAmenity(String value, Amenity amenity, String username);
    
    AmenityDTO updateAmenity(String value, AmenityDTO amenityDTO, String username);
    
    void deleteAmenity(String value);
    
    void softDeleteAmenity(String value);
    
    void restoreAmenity(String value);
    
    // 分类相关操作
    List<AmenityCategory> getAllCategories();
    
    AmenityCategory getCategoryByCode(String code);
    
    AmenityCategory createCategory(AmenityCategory category);
    
    AmenityCategory updateCategory(String code, AmenityCategory category);
    
    void deleteCategory(String code);
    
    List<Amenity> getAmenitiesByCategory(String categoryCode);
    
    List<Amenity> getActiveAmenitiesByCategory(String categoryCode);
    
    // 数据分析相关操作
    Map<String, Long> getAmenityUsageStatistics();
    
    List<AmenityDTO> getMostUsedAmenities(int limit);
    
    // 以分类分组返回所有设施
    List<AmenityCategoryDTO> getAmenitiesByCategories();
    
    List<AmenityCategoryDTO> getActiveAmenitiesByCategories();
    
    // 检查设施使用情况
    long countHomestaysUsingAmenity(String value);
    
    // 设施使用计数管理
    void updateAmenityUsageCount(String value, boolean increment);
    
    // 房源设施关联管理
    boolean addAmenityToHomestay(Long homestayId, String amenityValue);
    
    boolean removeAmenityFromHomestay(Long homestayId, String amenityValue);
    
    List<String> addAllAmenitiesToHomestay(Long homestayId, String categoryCode);
    
    List<String> removeAllAmenitiesFromHomestay(Long homestayId, String categoryCode);
    
    List<Amenity> getHomestayAmenities(Long homestayId);
    
    // 初始化默认数据
    void initDefaultCategories();
    
    /**
     * 初始化默认设施
     */
    void initDefaultAmenities();
    
    /**
     * 初始化默认设施
     * @param force 是否强制初始化
     */
    void initDefaultAmenities(boolean force);

    /**
     * 激活所有设施
     * @return 激活的设施数量
     */
    int activateAllAmenities();
} 