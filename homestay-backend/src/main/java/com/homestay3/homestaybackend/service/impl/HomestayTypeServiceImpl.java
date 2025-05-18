package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.HomestayTypeDTO;
import com.homestay3.homestaybackend.dto.TypeCategoryDTO;
import com.homestay3.homestaybackend.model.HomestayType;
import com.homestay3.homestaybackend.model.TypeCategory;
import com.homestay3.homestaybackend.repository.HomestayTypeRepository;
import com.homestay3.homestaybackend.repository.TypeCategoryRepository;
import com.homestay3.homestaybackend.service.HomestayTypeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomestayTypeServiceImpl implements HomestayTypeService {

    private final HomestayTypeRepository homestayTypeRepository;
    private final TypeCategoryRepository typeCategoryRepository;

    // DTO转换方法
    private HomestayTypeDTO convertToDTO(HomestayType homestayType) {
        if (homestayType == null) return null;
        
        return HomestayTypeDTO.builder()
                .id(homestayType.getId())
                .code(homestayType.getCode())
                .name(homestayType.getName())
                .description(homestayType.getDescription())
                .icon(homestayType.getIcon())
                .active(homestayType.isActive())
                .sortOrder(homestayType.getSortOrder())
                .categoryId(homestayType.getCategory() != null ? homestayType.getCategory().getId() : null)
                .categoryName(homestayType.getCategory() != null ? homestayType.getCategory().getName() : null)
                .createdAt(homestayType.getCreatedAt())
                .updatedAt(homestayType.getUpdatedAt())
                .build();
    }
    
    private TypeCategoryDTO convertToCategoryDTO(TypeCategory category) {
        if (category == null) return null;
        
        return TypeCategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .sortOrder(category.getSortOrder())
                .types(category.getTypes().stream().map(this::convertToDTO).collect(Collectors.toList()))
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
    
    private TypeCategoryDTO convertToCategoryDTOWithoutTypes(TypeCategory category) {
        if (category == null) return null;
        
        return TypeCategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .sortOrder(category.getSortOrder())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
    
    // 房源类型实现方法
    @Override
    @Transactional(readOnly = true)
    public List<HomestayTypeDTO> getAllHomestayTypes() {
        return homestayTypeRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomestayTypeDTO> getActiveHomestayTypes() {
        return homestayTypeRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public HomestayTypeDTO getHomestayTypeById(Long id) {
        return homestayTypeRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new EntityNotFoundException("未找到ID为 " + id + " 的房源类型"));
    }

    @Override
    @Transactional(readOnly = true)
    public HomestayTypeDTO getHomestayTypeByCode(String code) {
        return homestayTypeRepository.findByCode(code)
                .map(this::convertToDTO)
                .orElseThrow(() -> new EntityNotFoundException("未找到代码为 " + code + " 的房源类型"));
    }

    @Override
    @Transactional
    public HomestayTypeDTO createHomestayType(HomestayTypeDTO homestayTypeDTO) {
        TypeCategory category = homestayTypeDTO.getCategoryId() != null
                ? typeCategoryRepository.findById(homestayTypeDTO.getCategoryId())
                        .orElseThrow(() -> new EntityNotFoundException("未找到ID为 " + homestayTypeDTO.getCategoryId() + " 的类别"))
                : null;
        
        HomestayType homestayType = HomestayType.builder()
                .code(homestayTypeDTO.getCode())
                .name(homestayTypeDTO.getName())
                .description(homestayTypeDTO.getDescription())
                .icon(homestayTypeDTO.getIcon())
                .active(homestayTypeDTO.isActive())
                .sortOrder(homestayTypeDTO.getSortOrder())
                .category(category)
                .build();
        
        HomestayType savedType = homestayTypeRepository.save(homestayType);
        return convertToDTO(savedType);
    }

    @Override
    @Transactional
    public HomestayTypeDTO updateHomestayType(Long id, HomestayTypeDTO homestayTypeDTO) {
        HomestayType existingType = homestayTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("未找到ID为 " + id + " 的房源类型"));
        
        TypeCategory category = homestayTypeDTO.getCategoryId() != null
                ? typeCategoryRepository.findById(homestayTypeDTO.getCategoryId())
                        .orElseThrow(() -> new EntityNotFoundException("未找到ID为 " + homestayTypeDTO.getCategoryId() + " 的类别"))
                : null;
        
        existingType.setCode(homestayTypeDTO.getCode());
        existingType.setName(homestayTypeDTO.getName());
        existingType.setDescription(homestayTypeDTO.getDescription());
        existingType.setIcon(homestayTypeDTO.getIcon());
        existingType.setActive(homestayTypeDTO.isActive());
        existingType.setSortOrder(homestayTypeDTO.getSortOrder());
        existingType.setCategory(category);
        
        HomestayType updatedType = homestayTypeRepository.save(existingType);
        return convertToDTO(updatedType);
    }

    @Override
    @Transactional
    public void deleteHomestayType(Long id) {
        if (!homestayTypeRepository.existsById(id)) {
            throw new EntityNotFoundException("未找到ID为 " + id + " 的房源类型");
        }
        homestayTypeRepository.deleteById(id);
    }

    // 分类实现方法
    @Override
    @Transactional(readOnly = true)
    public List<TypeCategoryDTO> getAllCategories() {
        return typeCategoryRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(this::convertToCategoryDTOWithoutTypes) // 不包含类型列表，避免循环依赖
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TypeCategoryDTO getCategoryById(Long id) {
        return typeCategoryRepository.findById(id)
                .map(this::convertToCategoryDTO)
                .orElseThrow(() -> new EntityNotFoundException("未找到ID为 " + id + " 的类别"));
    }

    @Override
    @Transactional
    public TypeCategoryDTO createCategory(TypeCategoryDTO categoryDTO) {
        TypeCategory category = TypeCategory.builder()
                .name(categoryDTO.getName())
                .description(categoryDTO.getDescription())
                .sortOrder(categoryDTO.getSortOrder())
                .build();
        
        TypeCategory savedCategory = typeCategoryRepository.save(category);
        return convertToCategoryDTOWithoutTypes(savedCategory);
    }

    @Override
    @Transactional
    public TypeCategoryDTO updateCategory(Long id, TypeCategoryDTO categoryDTO) {
        TypeCategory existingCategory = typeCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("未找到ID为 " + id + " 的类别"));
        
        existingCategory.setName(categoryDTO.getName());
        existingCategory.setDescription(categoryDTO.getDescription());
        existingCategory.setSortOrder(categoryDTO.getSortOrder());
        
        TypeCategory updatedCategory = typeCategoryRepository.save(existingCategory);
        return convertToCategoryDTOWithoutTypes(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (!typeCategoryRepository.existsById(id)) {
            throw new EntityNotFoundException("未找到ID为 " + id + " 的类别");
        }
        typeCategoryRepository.deleteById(id);
    }

    // 按分类获取房源类型
    @Override
    @Transactional(readOnly = true)
    public Map<String, List<HomestayTypeDTO>> getHomestayTypesByCategory() {
        List<TypeCategory> categories = typeCategoryRepository.findAllByOrderBySortOrderAsc();
        Map<String, List<HomestayTypeDTO>> result = new HashMap<>();
        
        for (TypeCategory category : categories) {
            List<HomestayTypeDTO> types = homestayTypeRepository.findByCategoryIdAndActiveTrueOrderBySortOrderAsc(category.getId())
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            if (!types.isEmpty()) {
                result.put(category.getName(), types);
            }
        }
        
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomestayTypeDTO> getHomestayTypesByCategoryId(Long categoryId) {
        return homestayTypeRepository.findByCategoryIdAndActiveTrueOrderBySortOrderAsc(categoryId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<HomestayType> getAllTypes() {
        // 返回所有类型，可以根据需要添加排序逻辑，比如按 id 或 name 排序
        return homestayTypeRepository.findAll(); 
    }
} 