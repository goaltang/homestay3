package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.HomestayTypeDTO;
import com.homestay3.homestaybackend.dto.TypeCategoryDTO;
import com.homestay3.homestaybackend.model.HomestayType;

import java.util.List;
import java.util.Map;

public interface HomestayTypeService {
    
    // 房源类型方法
    List<HomestayTypeDTO> getAllHomestayTypes();
    
    List<HomestayTypeDTO> getActiveHomestayTypes();
    
    HomestayTypeDTO getHomestayTypeById(Long id);
    
    HomestayTypeDTO getHomestayTypeByCode(String code);
    
    HomestayTypeDTO createHomestayType(HomestayTypeDTO homestayTypeDTO);
    
    HomestayTypeDTO updateHomestayType(Long id, HomestayTypeDTO homestayTypeDTO);
    
    void deleteHomestayType(Long id);
    
    // 分类方法
    List<TypeCategoryDTO> getAllCategories();
    
    TypeCategoryDTO getCategoryById(Long id);
    
    TypeCategoryDTO createCategory(TypeCategoryDTO categoryDTO);
    
    TypeCategoryDTO updateCategory(Long id, TypeCategoryDTO categoryDTO);
    
    void deleteCategory(Long id);
    
    // 按分类获取房源类型
    Map<String, List<HomestayTypeDTO>> getHomestayTypesByCategory();
    
    List<HomestayTypeDTO> getHomestayTypesByCategoryId(Long categoryId);

    List<HomestayType> getAllTypes();
} 