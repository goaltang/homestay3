package com.homestay3.homestaybackend.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.homestay3.homestaybackend.dto.AmenityCategoryDTO;
import com.homestay3.homestaybackend.dto.AmenityDTO;
import com.homestay3.homestaybackend.exception.ResourceAlreadyExistsException;
import com.homestay3.homestaybackend.exception.ResourceInUseException;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.Amenity;
import com.homestay3.homestaybackend.model.AmenityCategory;
import com.homestay3.homestaybackend.model.Homestay;
import com.homestay3.homestaybackend.repository.AmenityCategoryRepository;
import com.homestay3.homestaybackend.repository.AmenityRepository;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.service.AmenityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AmenityServiceImpl implements AmenityService {

    private static final Logger logger = LoggerFactory.getLogger(AmenityServiceImpl.class);

    @Autowired
    private AmenityRepository amenityRepository;
    
    @Autowired
    private AmenityCategoryRepository categoryRepository;
    
    @Autowired
    private HomestayRepository homestayRepository;

    @Override
    public List<Amenity> getAllAmenities() {
        return amenityRepository.findAll();
    }
    
    @Override
    public List<Amenity> getActiveAmenities() {
        return amenityRepository.findByActive(true);
    }

    @Override
    public List<Amenity> searchAmenities(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllAmenities();
        }
        return amenityRepository.findByValueContainingIgnoreCaseOrLabelContainingIgnoreCase(keyword, keyword);
    }
    
    @Override
    public List<Amenity> searchActiveAmenities(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getActiveAmenities();
        }
        return amenityRepository.findByValueContainingIgnoreCaseOrLabelContainingIgnoreCaseAndActive(
                keyword, keyword, true);
    }

    @Override
    public Amenity getAmenityByValue(String value) {
        return amenityRepository.findById(value)
                .orElseThrow(() -> new ResourceNotFoundException("设施不存在，编码: " + value));
    }
    
    @Override
    public AmenityDTO getAmenityDTOByValue(String value) {
        Amenity amenity = getAmenityByValue(value);
        return convertToDTO(amenity);
    }

    @Override
    @Transactional
    public Amenity createAmenity(Amenity amenity, String username) {
        if (amenityRepository.existsById(amenity.getValue())) {
            throw new ResourceAlreadyExistsException("设施编码已存在: " + amenity.getValue());
        }
        
        amenity.setCreatedBy(username);
        amenity.setUpdatedBy(username);
        return amenityRepository.save(amenity);
    }
    
    @Override
    @Transactional
    public AmenityDTO createAmenity(AmenityDTO amenityDTO, String username) {
        Amenity amenity = convertToEntity(amenityDTO);
        amenity = createAmenity(amenity, username);
        return convertToDTO(amenity);
    }

    @Override
    @Transactional
    public Amenity updateAmenity(String value, Amenity amenity, String username) {
        if (!amenityRepository.existsById(value)) {
            throw new ResourceNotFoundException("设施不存在，编码: " + value);
        }
        
        // 获取现有实体并更新审计字段
        Amenity existingAmenity = amenityRepository.findById(value).get();
        amenity.setValue(value); // 确保ID一致
        amenity.setCreatedAt(existingAmenity.getCreatedAt());
        amenity.setCreatedBy(existingAmenity.getCreatedBy());
        amenity.setUpdatedBy(username);
        
        return amenityRepository.save(amenity);
    }
    
    @Override
    @Transactional
    public AmenityDTO updateAmenity(String value, AmenityDTO amenityDTO, String username) {
        Amenity amenity = convertToEntity(amenityDTO);
        amenity = updateAmenity(value, amenity, username);
        return convertToDTO(amenity);
    }

    @Override
    @Transactional
    public void deleteAmenity(String value) {
        if (!amenityRepository.existsById(value)) {
            throw new ResourceNotFoundException("设施不存在，编码: " + value);
        }
        
        // 检查是否被使用
        long useCount = countHomestaysUsingAmenity(value);
        if (useCount > 0) {
            throw new ResourceInUseException("该设施正被" + useCount + "个房源使用，无法删除");
        }
        
        // 执行删除
        amenityRepository.deleteById(value);
    }
    
    @Override
    @Transactional
    public void softDeleteAmenity(String value) {
        Amenity amenity = getAmenityByValue(value);
        amenity.setActive(false);
        amenityRepository.save(amenity);
    }
    
    @Override
    @Transactional
    public void restoreAmenity(String value) {
        Amenity amenity = getAmenityByValue(value);
        amenity.setActive(true);
        amenityRepository.save(amenity);
    }
    
    @Override
    public List<AmenityCategory> getAllCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc();
    }
    
    @Override
    public AmenityCategory getCategoryByCode(String code) {
        return categoryRepository.findById(code)
                .orElseThrow(() -> new ResourceNotFoundException("设施分类不存在，编码: " + code));
    }
    
    @Override
    @Transactional
    public AmenityCategory createCategory(AmenityCategory category) {
        if (categoryRepository.existsById(category.getCode())) {
            throw new ResourceAlreadyExistsException("设施分类编码已存在: " + category.getCode());
        }
        return categoryRepository.save(category);
    }
    
    @Override
    @Transactional
    public AmenityCategory updateCategory(String code, AmenityCategory category) {
        if (!categoryRepository.existsById(code)) {
            throw new ResourceNotFoundException("设施分类不存在，编码: " + code);
        }
        
        category.setCode(code); // 确保ID一致
        return categoryRepository.save(category);
    }
    
    @Override
    @Transactional
    public void deleteCategory(String code) {
        AmenityCategory category = getCategoryByCode(code);
        
        // 检查是否有设施使用此分类
        List<Amenity> amenities = amenityRepository.findByCategory(category);
        if (!amenities.isEmpty()) {
            throw new ResourceInUseException("该分类下还有" + amenities.size() + "个设施，无法删除");
        }
        
        categoryRepository.deleteById(code);
    }
    
    @Override
    public List<Amenity> getAmenitiesByCategory(String categoryCode) {
        AmenityCategory category = getCategoryByCode(categoryCode);
        return amenityRepository.findByCategory(category);
    }
    
    @Override
    public List<Amenity> getActiveAmenitiesByCategory(String categoryCode) {
        AmenityCategory category = getCategoryByCode(categoryCode);
        return amenityRepository.findByCategoryAndActive(category, true);
    }
    
    @Override
    public Map<String, Long> getAmenityUsageStatistics() {
        List<Amenity> amenities = getAllAmenities();
        Map<String, Long> statistics = new HashMap<>();
        
        for (Amenity amenity : amenities) {
            statistics.put(amenity.getLabel(), amenity.getUsageCount());
        }
        
        return statistics;
    }
    
    @Override
    public List<AmenityDTO> getMostUsedAmenities(int limit) {
        List<Amenity> allAmenities = getAllAmenities();
        return allAmenities.stream()
                .sorted(Comparator.comparing(Amenity::getUsageCount).reversed())
                .limit(limit)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AmenityCategoryDTO> getAmenitiesByCategories() {
        // 优化N+1查询问题的实现
        List<AmenityCategory> categories = getAllCategories();
        List<Amenity> allAmenities = getAllAmenities();
        
        // 将设施按分类分组
        Map<String, List<Amenity>> amenitiesByCategory = allAmenities.stream()
                .filter(a -> a.getCategory() != null)
                .collect(Collectors.groupingBy(a -> a.getCategory().getCode()));
        
        return categories.stream().map(category -> {
            AmenityCategoryDTO dto = convertToCategoryDTO(category, amenitiesByCategory.getOrDefault(category.getCode(), new ArrayList<>()));
            return dto;
        }).collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AmenityCategoryDTO> getActiveAmenitiesByCategories() {
        // 优化N+1查询问题的实现
        List<AmenityCategory> categories = getAllCategories();
        List<Amenity> allActiveAmenities = getActiveAmenities();
        
        // 将设施按分类分组
        Map<String, List<Amenity>> amenitiesByCategory = allActiveAmenities.stream()
                .filter(a -> a.getCategory() != null)
                .collect(Collectors.groupingBy(a -> a.getCategory().getCode()));
        
        return categories.stream().map(category -> {
            AmenityCategoryDTO dto = convertToCategoryDTO(category, amenitiesByCategory.getOrDefault(category.getCode(), new ArrayList<>()));
            return dto;
        }).collect(Collectors.toList());
    }
    
    @Override
    public long countHomestaysUsingAmenity(String value) {
        return amenityRepository.countHomestaysUsingAmenity(value);
    }
    
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void updateAmenityUsageCount(String value, boolean increment) {
        if (increment) {
            amenityRepository.incrementUsageCount(value);
        } else {
            amenityRepository.decrementUsageCount(value);
        }
    }
    
    @Override
    @Transactional
    public void initDefaultCategories() {
        // 检查数据库中是否已有数据
        if (categoryRepository.count() > 0) {
            return;
        }
        
        // 创建默认分类
        createCategoryIfNotExists("BASIC", "基础设施", "House", 1);
        createCategoryIfNotExists("KITCHEN", "厨房设施", "KitchenKnife", 2);
        createCategoryIfNotExists("OUTDOOR", "户外设施", "Sunny", 3);
        createCategoryIfNotExists("CONVENIENCE", "便利设施", "Suitcase", 4);
        createCategoryIfNotExists("SAFETY", "安全设施", "AlarmClock", 5);
        createCategoryIfNotExists("SPECIAL", "特色服务", "Star", 6);
    }
    
    @Override
    @Transactional
    public void initDefaultAmenities() {
        initDefaultAmenities(false);
    }
    
    @Override
    @Transactional
    public void initDefaultAmenities(boolean force) {
        // 检查数据库中是否已有数据，如果有数据且不是强制初始化则返回
        if (!force && amenityRepository.count() > 0) {
            return;
        }
        
        AmenityCategory basicCategory = categoryRepository.findById("BASIC").orElse(null);
        AmenityCategory kitchenCategory = categoryRepository.findById("KITCHEN").orElse(null);
        AmenityCategory outdoorCategory = categoryRepository.findById("OUTDOOR").orElse(null);
        AmenityCategory convenienceCategory = categoryRepository.findById("CONVENIENCE").orElse(null);
        AmenityCategory safetyCategory = categoryRepository.findById("SAFETY").orElse(null);
        AmenityCategory specialCategory = categoryRepository.findById("SPECIAL").orElse(null);
        
        // 如果分类不存在，先初始化分类
        if (basicCategory == null) {
            initDefaultCategories();
            basicCategory = categoryRepository.findById("BASIC").get();
            kitchenCategory = categoryRepository.findById("KITCHEN").get();
            outdoorCategory = categoryRepository.findById("OUTDOOR").get();
            convenienceCategory = categoryRepository.findById("CONVENIENCE").get();
            safetyCategory = categoryRepository.findById("SAFETY").get();
            specialCategory = categoryRepository.findById("SPECIAL").get();
        }
        
        // 基础设施
        createIfNotExists("WIFI", "无线网络", "提供高速WiFi网络连接", "Connection", basicCategory, "system");
        createIfNotExists("AIR_CONDITIONING", "空调", "房间配备空调设备", "WindPower", basicCategory, "system");
        createIfNotExists("HEATING", "暖气", "房间配备暖气设备", "Sunny", basicCategory, "system");
        createIfNotExists("TV", "电视", "提供电视娱乐", "Monitor", basicCategory, "system");
        createIfNotExists("WASHER", "洗衣机", "房间内或公共区域提供洗衣机", "DishDot", basicCategory, "system");
        createIfNotExists("DRYER", "烘干机", "房间内或公共区域提供烘干机", "Expand", basicCategory, "system");
        
        // 厨房设施
        createIfNotExists("KITCHEN", "厨房", "提供完整厨房设施", "KitchenKnife", kitchenCategory, "system");
        createIfNotExists("REFRIGERATOR", "冰箱", "房间配备冰箱", "RefreshRight", kitchenCategory, "system");
        createIfNotExists("MICROWAVE", "微波炉", "房间配备微波炉", "Box", kitchenCategory, "system");
        createIfNotExists("DISHES", "餐具", "提供餐具和炊具", "ForkSpoon", kitchenCategory, "system");
        
        //
        // 户外设施
        createIfNotExists("POOL", "游泳池", "提供游泳池设施", "Umbrella", outdoorCategory, "system");
        createIfNotExists("HOT_TUB", "热水浴缸", "提供热水浴缸", "WaterCup", outdoorCategory, "system");
        createIfNotExists("BBQ", "烧烤设施", "提供烧烤设备和场地", "Food", outdoorCategory, "system");
        createIfNotExists("PARKING", "停车位", "提供免费停车位", "Van", outdoorCategory, "system");
        
        // 便利设施
        createIfNotExists("GYM", "健身房", "提供健身设施", "Medal", convenienceCategory, "system");
        createIfNotExists("ELEVATOR", "电梯", "房屋所在建筑配有电梯", "TopRight", convenienceCategory, "system");
        createIfNotExists("WORKSPACE", "工作区", "提供舒适的工作场所", "Briefcase", convenienceCategory, "system");
        createIfNotExists("BREAKFAST", "早餐", "提供早餐服务", "Mug", convenienceCategory, "system");
        
        // 安全设施
        createIfNotExists("SMOKE_ALARM", "烟雾报警器", "房间配备烟雾报警器", "Bell", safetyCategory, "system");
        createIfNotExists("FIRE_EXTINGUISHER", "灭火器", "配备灭火器", "MoreFilled", safetyCategory, "system");
        createIfNotExists("FIRST_AID", "急救箱", "配备急救用品", "First-aid-kit", safetyCategory, "system");
        createIfNotExists("SECURITY_CAMERA", "安全摄像头", "公共区域有安全监控", "Camera", safetyCategory, "system");
        
        // 特色服务
        createIfNotExists("PET_FRIENDLY", "宠物友好", "允许携带宠物入住", "Opportunity", specialCategory, "system");
        createIfNotExists("CLEANING", "清洁服务", "提供额外清洁服务", "SwitchButton", specialCategory, "system");
        createIfNotExists("AIRPORT_SHUTTLE", "机场接送", "可提供机场接送服务", "Ship", specialCategory, "system");
    }
    
    // 辅助方法
    private void createCategoryIfNotExists(String code, String name, String icon, Integer sortOrder) {
        if (!categoryRepository.existsById(code)) {
            AmenityCategory category = new AmenityCategory();
            category.setCode(code);
            category.setName(name);
            category.setIcon(icon);
            category.setSortOrder(sortOrder);
            categoryRepository.save(category);
        }
    }
    
    private void createIfNotExists(String value, String label, String description, String icon, 
                                  AmenityCategory category, String username) {
        if (!amenityRepository.existsById(value)) {
            Amenity amenity = new Amenity();
            amenity.setValue(value);
            amenity.setLabel(label);
            amenity.setDescription(description);
            amenity.setIcon(icon);
            amenity.setCategory(category);
            amenity.setActive(true);
            amenity.setUsageCount(0L);
            amenity.setCreatedBy(username);
            amenity.setUpdatedBy(username);
            amenityRepository.save(amenity);
        }
    }
    
    private AmenityDTO convertToDTO(Amenity amenity) {
        if (amenity == null) return null;
        
        AmenityDTO dto = new AmenityDTO();
        dto.setValue(amenity.getValue());
        dto.setLabel(amenity.getLabel());
        dto.setDescription(amenity.getDescription());
        dto.setIcon(amenity.getIcon());
        dto.setActive(amenity.isActive());
        dto.setUsageCount(amenity.getUsageCount());
        dto.setCreatedAt(amenity.getCreatedAt());
        dto.setUpdatedAt(amenity.getUpdatedAt());
        dto.setCreatedBy(amenity.getCreatedBy());
        dto.setUpdatedBy(amenity.getUpdatedBy());
        
        if (amenity.getCategory() != null) {
            dto.setCategoryCode(amenity.getCategory().getCode());
            dto.setCategoryName(amenity.getCategory().getName());
            dto.setCategoryIcon(amenity.getCategory().getIcon());
        }
        
        return dto;
    }
    
    private Amenity convertToEntity(AmenityDTO dto) {
        if (dto == null) return null;
        
        Amenity entity = new Amenity();
        entity.setValue(dto.getValue());
        entity.setLabel(dto.getLabel());
        entity.setDescription(dto.getDescription());
        entity.setIcon(dto.getIcon());
        entity.setActive(dto.isActive());
        entity.setUsageCount(dto.getUsageCount());
        
        if (dto.getCategoryCode() != null) {
            Optional<AmenityCategory> category = categoryRepository.findById(dto.getCategoryCode());
            category.ifPresent(entity::setCategory);
        }
        
        return entity;
    }
    
    private AmenityCategoryDTO convertToCategoryDTO(AmenityCategory category, List<Amenity> amenities) {
        if (category == null) return null;
        
        AmenityCategoryDTO dto = new AmenityCategoryDTO();
        dto.setCode(category.getCode());
        dto.setName(category.getName());
        dto.setIcon(category.getIcon());
        dto.setSortOrder(category.getSortOrder());
        
        if (amenities != null) {
            List<AmenityDTO> amenityDTOs = amenities.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            dto.setAmenities(amenityDTOs);
        }
        
        return dto;
    }
    
    @Override
    @Transactional
    public boolean addAmenityToHomestay(Long homestayId, String amenityValue) {
        logger.info("开始向房源 {} 添加设施 {}", homestayId, amenityValue);

        Homestay homestay = homestayRepository.findById(homestayId)
                .orElseThrow(() -> {
                    logger.error("添加设施失败：找不到房源 {}", homestayId);
                    return new ResourceNotFoundException("房源不存在: " + homestayId);
                });

        Amenity amenity = amenityRepository.findById(amenityValue)
                .orElseThrow(() -> {
                    logger.error("添加设施失败：找不到设施 {}", amenityValue);
                    return new ResourceNotFoundException("设施不存在: " + amenityValue);
                });

        if (homestay.getAmenities().contains(amenity)) {
            logger.warn("设施 {} 已经存在于房源 {}，无需重复添加", amenityValue, homestayId);
            return true;
        }

        logger.info("找到房源: {}, 找到设施: {}", homestay.getTitle(), amenity.getLabel());

        try {
            homestay.getAmenities().add(amenity);
            amenity.getHomestays().add(homestay);
            homestayRepository.save(homestay);
            updateAmenityUsageCount(amenityValue, true);
            logger.info("成功将设施 {} 添加到房源 {}", amenityValue, homestayId);
            return true;
        } catch (Exception e) {
             logger.error("添加设施到房源 {} 时发生异常: {}", homestayId, e.getMessage(), e);
             throw e;
        }
    }
    
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean removeAmenityFromHomestay(Long homestayId, String amenityValue) {
        logger.info("从房源移除设施: homestayId={}, amenityValue={}", homestayId, amenityValue);
        
        // 检查房源是否存在
        Homestay homestay = homestayRepository.findById(homestayId)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + homestayId));
        
        // 检查设施是否存在
        Amenity amenity = amenityRepository.findById(amenityValue)
                .orElseThrow(() -> new ResourceNotFoundException("设施不存在，编码: " + amenityValue));
        
        // 如果设施不包含在房源中，则返回false
        if (!homestay.getAmenities().contains(amenity)) {
            logger.info("设施 {} 不存在于房源 {} 中", amenityValue, homestayId);
            return false;
        }
        
        try {
            // 从房源中移除设施
            homestay.getAmenities().remove(amenity);
            homestayRepository.save(homestay);
            
            // 更新设施使用计数
            updateAmenityUsageCount(amenityValue, false);
            
            logger.info("成功从房源 {} 移除设施 {}", homestayId, amenityValue);
            return true;
        } catch (Exception e) {
            logger.error("从房源移除设施时发生错误: homestayId={}, amenityValue={}, error={}", 
                        homestayId, amenityValue, e.getMessage(), e);
            
            // 尝试直接通过SQL删除
            try {
                logger.info("尝试通过原生SQL从房源移除设施");
                jakarta.persistence.EntityManager entityManager = 
                    ((org.springframework.orm.jpa.JpaTransactionManager) 
                    org.springframework.transaction.support.TransactionSynchronizationManager
                    .getResourceMap().keySet().stream()
                    .filter(tm -> tm instanceof org.springframework.orm.jpa.JpaTransactionManager)
                    .findFirst().orElse(null)).getEntityManagerFactory()
                    .createEntityManager();

                entityManager.getTransaction().begin();
                int affected = entityManager.createNativeQuery(
                    "DELETE FROM homestay_amenity WHERE homestay_id = ? AND amenity_id = ?")
                    .setParameter(1, homestayId)
                    .setParameter(2, amenityValue)
                    .executeUpdate();
                entityManager.getTransaction().commit();
                
                if (affected > 0) {
                    logger.info("通过原生SQL成功从房源移除设施");
                    // 更新设施使用计数
                    updateAmenityUsageCount(amenityValue, false);
                    return true;
                } else {
                    logger.warn("通过原生SQL从房源移除设施失败或已不存在");
                    return false;
                }
            } catch (Exception ex) {
                logger.error("通过原生SQL从房源移除设施时发生错误: {}", ex.getMessage(), ex);
                return false;
            }
        }
    }
    
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<String> addAllAmenitiesToHomestay(Long homestayId, String categoryCode) {
        // 检查房源是否存在
        Homestay homestay = homestayRepository.findById(homestayId)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + homestayId));
        
        // 获取要添加的设施列表
        List<Amenity> amenities;
        if (categoryCode != null && !categoryCode.isEmpty()) {
            amenities = getActiveAmenitiesByCategory(categoryCode);
        } else {
            amenities = getActiveAmenities();
        }
        
        List<String> addedAmenities = new ArrayList<>();
        Set<Amenity> currentAmenities = homestay.getAmenities();
        
        // 添加设施到房源
        for (Amenity amenity : amenities) {
            if (!currentAmenities.contains(amenity)) {
                homestay.getAmenities().add(amenity);
                updateAmenityUsageCount(amenity.getValue(), true);
                addedAmenities.add(amenity.getValue());
            }
        }
        
        if (!addedAmenities.isEmpty()) {
            homestayRepository.save(homestay);
        }
        
        return addedAmenities;
    }
    
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<String> removeAllAmenitiesFromHomestay(Long homestayId, String categoryCode) {
        logger.info("从房源移除所有设施: homestayId={}, categoryCode={}", homestayId, categoryCode);
        
        // 检查房源是否存在
        Homestay homestay = homestayRepository.findById(homestayId)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + homestayId));
        
        List<String> removedAmenities = new ArrayList<>();
        
        try {
            if (categoryCode != null && !categoryCode.isEmpty()) {
                // 只移除特定分类的设施
                List<Amenity> categoryAmenities = getAmenitiesByCategory(categoryCode);
                Set<Amenity> amenitySet = new HashSet<>(categoryAmenities);
                
                // 创建一个新的ArrayList以避免ConcurrentModificationException
                List<Amenity> toRemove = new ArrayList<>();
                
                for (Amenity amenity : homestay.getAmenities()) {
                    if (amenitySet.contains(amenity) || 
                        categoryAmenities.stream().anyMatch(a -> a.getValue().equals(amenity.getValue()))) {
                        toRemove.add(amenity);
                        updateAmenityUsageCount(amenity.getValue(), false);
                        removedAmenities.add(amenity.getValue());
                    }
                }
                
                homestay.getAmenities().removeAll(toRemove);
            } else {
                // 移除所有设施
                for (Amenity amenity : new ArrayList<>(homestay.getAmenities())) {
                    updateAmenityUsageCount(amenity.getValue(), false);
                    removedAmenities.add(amenity.getValue());
                }
                homestay.getAmenities().clear();
            }
            
            if (!removedAmenities.isEmpty()) {
                homestayRepository.save(homestay);
            }
            
            logger.info("成功从房源 {} 移除 {} 个设施", homestayId, removedAmenities.size());
            return removedAmenities;
        } catch (Exception e) {
            logger.error("从房源移除所有设施时发生错误: homestayId={}, categoryCode={}, error={}", 
                        homestayId, categoryCode, e.getMessage(), e);
            
            // 尝试直接通过SQL删除
            try {
                logger.info("尝试通过原生SQL从房源移除设施");
                jakarta.persistence.EntityManager entityManager = 
                    ((org.springframework.orm.jpa.JpaTransactionManager) 
                    org.springframework.transaction.support.TransactionSynchronizationManager
                    .getResourceMap().keySet().stream()
                    .filter(tm -> tm instanceof org.springframework.orm.jpa.JpaTransactionManager)
                    .findFirst().orElse(null)).getEntityManagerFactory()
                    .createEntityManager();

                entityManager.getTransaction().begin();
                
                int affected = 0;
                if (categoryCode != null && !categoryCode.isEmpty()) {
                    // 获取特定分类的设施值列表
                    List<String> amenityValues = getAmenitiesByCategory(categoryCode).stream()
                            .map(Amenity::getValue)
                            .collect(Collectors.toList());
                    
                    if (!amenityValues.isEmpty()) {
                        // 构建IN子句
                        String placeholders = amenityValues.stream()
                                .map(s -> "?")
                                .collect(Collectors.joining(","));
                        
                        String sql = "DELETE FROM homestay_amenity WHERE homestay_id = ? AND amenity_id IN (" + placeholders + ")";
                        jakarta.persistence.Query query = entityManager.createNativeQuery(sql);
                        query.setParameter(1, homestayId);
                        
                        // 设置参数
                        for (int i = 0; i < amenityValues.size(); i++) {
                            query.setParameter(i + 2, amenityValues.get(i));
                        }
                        
                        affected = query.executeUpdate();
                        
                        // 更新计数器
                        for (String value : amenityValues) {
                            try {
                                updateAmenityUsageCount(value, false);
                                removedAmenities.add(value);
                            } catch (Exception ex) {
                                logger.warn("更新设施使用计数失败: {}", value);
                            }
                        }
                    }
                } else {
                    // 获取所有关联设施的ID
                    List<String> currentAmenityIds = amenityRepository.findByHomestayId(homestayId).stream()
                            .map(Amenity::getValue)
                            .collect(Collectors.toList());
                    
                    // 删除所有关联
                    affected = entityManager.createNativeQuery(
                        "DELETE FROM homestay_amenity WHERE homestay_id = ?")
                        .setParameter(1, homestayId)
                        .executeUpdate();
                    
                    // 更新计数器
                    for (String value : currentAmenityIds) {
                        try {
                            updateAmenityUsageCount(value, false);
                            removedAmenities.add(value);
                        } catch (Exception ex) {
                            logger.warn("更新设施使用计数失败: {}", value);
                        }
                    }
                }
                
                entityManager.getTransaction().commit();
                
                if (affected > 0) {
                    logger.info("通过原生SQL成功从房源移除设施，影响行数: {}", affected);
                    return removedAmenities;
                } else {
                    logger.warn("通过原生SQL从房源移除设施失败或已不存在");
                    return new ArrayList<>();
                }
            } catch (Exception ex) {
                logger.error("通过原生SQL从房源移除设施时发生错误: {}", ex.getMessage(), ex);
                return new ArrayList<>();
            }
        }
    }
    
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<Amenity> getHomestayAmenities(Long homestayId) {
        // 检查房源是否存在
        Homestay homestay = homestayRepository.findById(homestayId)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + homestayId));
        
        // 获取房源包含的所有设施
        return amenityRepository.findByHomestayId(homestayId);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int activateAllAmenities() {
        // 获取所有未激活的设施
        List<Amenity> inactiveAmenities = amenityRepository.findByActive(false);
        
        // 如果没有未激活的设施，直接返回0
        if (inactiveAmenities.isEmpty()) {
            return 0;
        }
        
        // 激活所有设施
        for (Amenity amenity : inactiveAmenities) {
            amenity.setActive(true);
        }
        
        // 保存所有更改
        amenityRepository.saveAll(inactiveAmenities);
        
        return inactiveAmenities.size();
    }
} 