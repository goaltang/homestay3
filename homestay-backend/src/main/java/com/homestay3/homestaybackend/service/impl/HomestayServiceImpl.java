package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.dto.HomestayRequest;
import com.homestay3.homestaybackend.dto.HomestaySearchRequest;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.Homestay;
import com.homestay3.homestaybackend.model.User;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.HomestayService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomestayServiceImpl implements HomestayService {

    private final HomestayRepository homestayRepository;
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(HomestayServiceImpl.class);

    @Override
    public List<HomestayDTO> getAllHomestays() {
        log.info("获取所有房源");
        
        // 只获取状态为ACTIVE的房源
        List<Homestay> homestays = homestayRepository.findByStatus("ACTIVE");
        
        return homestays.stream()
                .map(this::convertToDTO)
                .map(this::addTestImageIfEmpty)
                .collect(Collectors.toList());
    }

    @Override
    public List<HomestayDTO> getFeaturedHomestays() {
        log.info("获取推荐房源");
        
        // 获取状态为ACTIVE且被标记为featured的房源，最多返回6个
        List<Homestay> featuredHomestays = homestayRepository.findByStatusAndFeaturedTrue("ACTIVE");
        
        if (featuredHomestays.size() < 6) {
            // 如果推荐房源不足6个，补充一些普通房源
            List<Homestay> regularHomestays = homestayRepository.findByStatusAndFeaturedFalse("ACTIVE");
            int remaining = 6 - featuredHomestays.size();
            
            if (regularHomestays.size() > remaining) {
                regularHomestays = regularHomestays.subList(0, remaining);
            }
            
            featuredHomestays.addAll(regularHomestays);
        } else if (featuredHomestays.size() > 6) {
            // 如果推荐房源超过6个，只取前6个
            featuredHomestays = featuredHomestays.subList(0, 6);
        }
        
        return featuredHomestays.stream()
                .map(this::convertToDTO)
                .map(this::addTestImageIfEmpty)
                .collect(Collectors.toList());
    }

    @Override
    public HomestayDTO getHomestayById(Long id) {
        log.info("根据ID获取房源详情: id={}", id);
        
        Homestay homestay = homestayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Homestay not found with id: " + id));
        
        return convertToDTO(homestay);
    }

    @Override
    public List<HomestayDTO> getHomestaysByPropertyType(String propertyType) {
        log.info("根据房源类型获取房源列表: type={}", propertyType);
        
        List<Homestay> homestays = homestayRepository.findByTypeAndStatus(propertyType, "ACTIVE");
        
        return homestays.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<HomestayDTO> searchHomestays(HomestaySearchRequest request) {
        log.info("搜索房源: {}", request);
        
        // 创建动态查询条件
        Specification<Homestay> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 只查询状态为ACTIVE的房源
            predicates.add(criteriaBuilder.equal(root.get("status"), "ACTIVE"));
            
            // 关键词搜索（标题或描述）
            if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                String keyword = "%" + request.getKeyword() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("title"), keyword),
                        criteriaBuilder.like(root.get("description"), keyword)
                ));
            }
            
            // 位置搜索（省份、城市或区县）
            if (request.getLocation() != null && !request.getLocation().isEmpty()) {
                String location = "%" + request.getLocation() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("province"), location),
                        criteriaBuilder.like(root.get("city"), location),
                        criteriaBuilder.like(root.get("district"), location)
                ));
            }
            
            // 房源类型
            if (request.getPropertyType() != null && !request.getPropertyType().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("type"), request.getPropertyType()));
            }
            
            // 价格范围
            if (request.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), request.getMinPrice()));
            }
            if (request.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), request.getMaxPrice()));
            }
            
            // 入住人数
            if (request.getMinGuests() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("maxGuests"), request.getMinGuests()));
            }
            
            // 设施过滤
            if (Boolean.TRUE.equals(request.getHasWifi())) {
                predicates.add(criteriaBuilder.isMember("WIFI", root.get("amenities")));
            }
            if (Boolean.TRUE.equals(request.getHasAirConditioning())) {
                predicates.add(criteriaBuilder.isMember("AIR_CONDITIONING", root.get("amenities")));
            }
            if (Boolean.TRUE.equals(request.getHasKitchen())) {
                predicates.add(criteriaBuilder.isMember("KITCHEN", root.get("amenities")));
            }
            if (Boolean.TRUE.equals(request.getHasWasher())) {
                predicates.add(criteriaBuilder.isMember("WASHER", root.get("amenities")));
            }
            if (Boolean.TRUE.equals(request.getHasParking())) {
                predicates.add(criteriaBuilder.isMember("PARKING", root.get("amenities")));
            }
            if (Boolean.TRUE.equals(request.getHasPool())) {
                predicates.add(criteriaBuilder.isMember("POOL", root.get("amenities")));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        // 执行查询
        List<Homestay> homestays = homestayRepository.findAll(spec);
        
        return homestays.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String uploadHomestayImage(MultipartFile file) {
        try {
            // 确保上传目录存在
            String uploadDir = "uploads/homestays";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = System.currentTimeMillis() + "_" + (int)(Math.random() * 1000) + extension;
            
            // 保存文件
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);
            
            return "/uploads/homestays/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload homestay image", e);
        }
    }

    @Override
    public Page<HomestayDTO> getHomestaysByPage(Pageable pageable) {
        return homestayRepository.findAll(pageable).map(this::convertToDTO);
    }

    @Override
    public List<HomestayDTO> getHomestaysByOwner(String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        
        return homestayRepository.findByOwner(owner).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HomestayDTO createHomestay(HomestayDTO homestayDTO) {
        // 验证必填字段
        validateHomestayDTO(homestayDTO);
        
        // 创建新的Homestay对象
        Homestay homestay = new Homestay();
        
        // 设置房源信息
        updateHomestayFromDTO(homestay, homestayDTO);
        
        // 默认使用当前登录用户作为房东
        homestay.setOwner(getCurrentUser());
        
        // 设置初始状态为PENDING
        homestay.setStatus("PENDING");
        homestay.setCreatedAt(LocalDateTime.now());
        homestay.setUpdatedAt(LocalDateTime.now());
        
        // 保存到数据库
        Homestay savedHomestay = homestayRepository.save(homestay);
        
        return convertToDTO(savedHomestay);
    }

    @Override
    @Transactional
    public HomestayDTO createHomestay(HomestayDTO homestayDTO, String ownerUsername) {
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在，用户名: " + ownerUsername));
        
        // 验证必填字段
        validateHomestayDTO(homestayDTO);
        
        // 创建新的Homestay对象
        Homestay homestay = new Homestay();
        
        // 设置房源信息
        updateHomestayFromDTO(homestay, homestayDTO);
        
        // 设置房东
        homestay.setOwner(owner);
        
        // 设置初始状态为PENDING
        homestay.setStatus("PENDING");
        homestay.setCreatedAt(LocalDateTime.now());
        homestay.setUpdatedAt(LocalDateTime.now());
        
        // 保存到数据库
        Homestay savedHomestay = homestayRepository.save(homestay);
        
        return convertToDTO(savedHomestay);
    }

    @Override
    @Transactional
    public HomestayDTO updateHomestay(Long id, HomestayDTO homestayDTO) {
        Homestay homestay = homestayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Homestay not found with id: " + id));
        
        // 检查权限（只有房源所有者可以更新）
        User currentUser = getCurrentUser();
        if (!homestay.getOwner().equals(currentUser)) {
            throw new RuntimeException("You don't have permission to update this homestay");
        }
        
        homestay.setTitle(homestayDTO.getTitle());
        homestay.setType(homestayDTO.getType());
        homestay.setPrice(new BigDecimal(homestayDTO.getPrice()));
        homestay.setMaxGuests(homestayDTO.getMaxGuests());
        homestay.setMinNights(homestayDTO.getMinNights());
        homestay.setProvince(homestayDTO.getProvince());
        homestay.setCity(homestayDTO.getCity());
        homestay.setDistrict(homestayDTO.getDistrict());
        homestay.setAddress(homestayDTO.getAddress());
        homestay.setAmenities(new HashSet<>(homestayDTO.getAmenities()));
        homestay.setDescription(homestayDTO.getDescription());
        homestay.setCoverImage(homestayDTO.getCoverImage());
        homestay.setImages(new ArrayList<>(homestayDTO.getImages()));
        homestay.setUpdatedAt(LocalDateTime.now());
        
        Homestay updatedHomestay = homestayRepository.save(homestay);
        return convertToDTO(updatedHomestay);
    }

    @Override
    public List<HomestayDTO> searchHomestays(String keyword, String province, String city, 
                                            Integer minPrice, Integer maxPrice, 
                                            Integer guests, String type) {
        // 创建动态查询条件
        Specification<Homestay> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 只查询状态为ACTIVE的房源
            predicates.add(criteriaBuilder.equal(root.get("status"), "ACTIVE"));
            
            // 关键词搜索（标题或描述）
            if (keyword != null && !keyword.isEmpty()) {
                String keywordPattern = "%" + keyword + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("title"), keywordPattern),
                        criteriaBuilder.like(root.get("description"), keywordPattern)
                ));
            }
            
            // 省份
            if (province != null && !province.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("province"), province));
            }
            
            // 城市
            if (city != null && !city.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("city"), city));
            }
            
            // 价格范围
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), new BigDecimal(minPrice)));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), new BigDecimal(maxPrice)));
            }
            
            // 入住人数
            if (guests != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("maxGuests"), guests));
            }
            
            // 房源类型
            if (type != null && !type.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        // 执行查询
        List<Homestay> homestays = homestayRepository.findAll(spec);
        
        return homestays.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteHomestay(Long id) {
        Homestay homestay = homestayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Homestay not found with id: " + id));
        
        // 检查权限（只有房源所有者可以删除）
        User currentUser = getCurrentUser();
        if (!homestay.getOwner().equals(currentUser)) {
            throw new RuntimeException("You don't have permission to delete this homestay");
        }
        
        homestayRepository.delete(homestay);
    }

    @Override
    @Transactional
    public HomestayDTO updateHomestayStatus(Long id, String status, String ownerUsername) {
        Homestay homestay = homestayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在"));
        
        // 检查权限（只有房源的拥有者才能更新状态）
        if (!homestay.getOwner().getUsername().equals(ownerUsername)) {
            throw new IllegalArgumentException("您不是此房源的拥有者，无权更新状态");
        }
        
        // 验证状态值是否有效
        validateHomestayStatus(status);
        
        // 更新状态
        homestay.setStatus(status);
        homestay.setUpdatedAt(LocalDateTime.now());
        
        Homestay updatedHomestay = homestayRepository.save(homestay);
        return convertToDTO(updatedHomestay);
    }
    
    @Override
    @Transactional
    public void updateHomestayStatus(Long id, String status) {
        Homestay homestay = homestayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + id));
        
        // 验证状态值是否有效
        validateHomestayStatus(status);
        
        // 更新状态
        homestay.setStatus(status);
        homestay.setUpdatedAt(LocalDateTime.now());
        
        homestayRepository.save(homestay);
    }
    
    @Override
    public Page<HomestayDTO> getAdminHomestays(Pageable pageable, String title, String status, String type) {
        // 创建动态查询条件
        Specification<Homestay> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 标题筛选
            if (title != null && !title.isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("title"), "%" + title + "%"));
            }
            
            // 状态筛选
            if (status != null && !status.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            
            // 类型筛选
            if (type != null && !type.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        // 执行查询
        Page<Homestay> homestaysPage = homestayRepository.findAll(spec, pageable);
        
        // 转换为DTO
        return homestaysPage.map(this::convertToDTO);
    }

    /**
     * 将实体转换为DTO
     */
    private HomestayDTO convertToDTO(Homestay homestay) {
        // 处理图片路径，确保是完整的URL
        List<String> processedImages = new ArrayList<>();
        if (homestay.getImages() != null && !homestay.getImages().isEmpty()) {
            processedImages = homestay.getImages().stream()
                    .map(this::ensureCompleteImageUrl)
                    .collect(Collectors.toList());
        }
        
        return HomestayDTO.builder()
                .id(homestay.getId())
                .title(homestay.getTitle())
                .type(homestay.getType())
                .price(homestay.getPrice().toString())
                .status(homestay.getStatus())
                .maxGuests(homestay.getMaxGuests())
                .minNights(homestay.getMinNights())
                .province(homestay.getProvince())
                .city(homestay.getCity())
                .district(homestay.getDistrict())
                .address(homestay.getAddress())
                .amenities(new ArrayList<>(homestay.getAmenities()))
                .description(homestay.getDescription())
                .coverImage(ensureCompleteImageUrl(homestay.getCoverImage()))
                .images(processedImages)
                .ownerUsername(homestay.getOwner().getUsername())
                .ownerName(homestay.getOwner().getFullName())
                .featured(homestay.getFeatured())
                .createdAt(homestay.getCreatedAt())
                .updatedAt(homestay.getUpdatedAt())
                .build();
    }
    
    /**
     * 确保图片URL是完整的，包含域名
     */
    private String ensureCompleteImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        
        // 如果已经是完整URL，直接返回
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return imagePath;
        }
        
        // 确保路径以/开头
        if (!imagePath.startsWith("/")) {
            imagePath = "/" + imagePath;
        }
        
        // 返回完整URL
        return "http://localhost:8080" + imagePath;
    }

    /**
     * 获取当前用户
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * 为没有图片的房源添加测试图片
     */
    private HomestayDTO addTestImageIfEmpty(HomestayDTO homestayDTO) {
        if (homestayDTO.getImages() == null || homestayDTO.getImages().isEmpty()) {
            List<String> testImages = new ArrayList<>();
            // 添加一个本地测试图片路径（确保uploads/homestays目录中存在此文件）
            testImages.add("/uploads/homestays/fcefb873-2ed7-4284-b1b9-b9145dc2188a.jpg");
            homestayDTO.setImages(testImages);
            
            // 如果封面图片也为空，也设置一个
            if (homestayDTO.getCoverImage() == null || homestayDTO.getCoverImage().isEmpty()) {
                homestayDTO.setCoverImage("/uploads/homestays/fcefb873-2ed7-4284-b1b9-b9145dc2188a.jpg");
            }
        }
        return homestayDTO;
    }

    // 验证房源状态是否有效
    private void validateHomestayStatus(String status) {
        List<String> validStatuses = Arrays.asList("PENDING", "ACTIVE", "INACTIVE", "REJECTED");
        if (!validStatuses.contains(status)) {
            throw new IllegalArgumentException("无效的房源状态: " + status);
        }
    }
    
    // 验证房源DTO是否包含必填字段
    private void validateHomestayDTO(HomestayDTO dto) {
        if (dto.getTitle() == null || dto.getTitle().isEmpty()) {
            throw new IllegalArgumentException("房源标题不能为空");
        }
        if (dto.getType() == null || dto.getType().isEmpty()) {
            throw new IllegalArgumentException("房源类型不能为空");
        }
        if (dto.getPrice() == null) {
            throw new IllegalArgumentException("房源价格不能为空");
        }
    }
    
    // 从DTO更新Homestay对象
    private void updateHomestayFromDTO(Homestay homestay, HomestayDTO dto) {
        homestay.setTitle(dto.getTitle());
        homestay.setDescription(dto.getDescription());
        homestay.setType(dto.getType());
        
        // 价格从String转为BigDecimal
        if (dto.getPrice() != null) {
            homestay.setPrice(new BigDecimal(dto.getPrice()));
        }
        
        homestay.setAddress(dto.getAddress());
        homestay.setProvince(dto.getProvince());
        homestay.setCity(dto.getCity());
        homestay.setDistrict(dto.getDistrict());
        homestay.setMaxGuests(dto.getMaxGuests());
        homestay.setMinNights(dto.getMinNights());
        
        // 转换amenities，确保是Set类型
        if (dto.getAmenities() != null) {
            homestay.setAmenities(new HashSet<>(dto.getAmenities()));
        }
        
        // 设置图片
        if (dto.getImages() != null) {
            homestay.setImages(new ArrayList<>(dto.getImages()));
        }
        
        homestay.setCoverImage(dto.getCoverImage());
        homestay.setFeatured(dto.isFeatured());
    }
}