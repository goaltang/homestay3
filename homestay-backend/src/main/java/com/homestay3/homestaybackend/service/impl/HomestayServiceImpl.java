package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.dto.HomestayRequest;
import com.homestay3.homestaybackend.dto.HomestaySearchRequest;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.Homestay;
import com.homestay3.homestaybackend.model.HomestayType;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.HomestayTypeRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.HomestayService;
import com.homestay3.homestaybackend.service.AmenityService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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

import com.homestay3.homestaybackend.model.Amenity;
import com.homestay3.homestaybackend.repository.AmenityRepository;
import com.homestay3.homestaybackend.dto.AmenityDTO;
import org.springframework.security.access.AccessDeniedException;
import java.util.Optional;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.model.Order;
import com.homestay3.homestaybackend.model.OrderStatus;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class HomestayServiceImpl implements HomestayService {

    private final HomestayRepository homestayRepository;
    private final UserRepository userRepository;
    private final AmenityRepository amenityRepository;
    private final AmenityService amenityService;
    private final HomestayTypeRepository homestayTypeRepository;
    private final OrderRepository orderRepository;
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
        
        // 使用新的查询方法，确保加载amenities
        Homestay homestay = homestayRepository.findByIdWithAmenities(id)
                .orElseThrow(() -> new ResourceNotFoundException("Homestay not found with id: " + id));
        
        if (homestay.getAmenities() != null) {
            log.info("房源 {} 关联的设施数量: {}", id, homestay.getAmenities().size());
            // 确保amenities已初始化
            org.hibernate.Hibernate.initialize(homestay.getAmenities());
            
            // 记录每个设施的信息用于调试
            homestay.getAmenities().forEach(amenity -> {
                log.debug("设施: value={}, label={}, icon={}", 
                    amenity.getValue(), amenity.getLabel(), amenity.getIcon());
            });
        } else {
            log.warn("房源 {} 的amenities集合为null", id);
        }
        
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
        log.info("搜索房源 (添加日期筛选): {}", request);
        log.info("[HomestayService] Received propertyType NAME for search: '{}'", request.getPropertyType());
        
        // 根据前端传来的类型名称查找对应的类型代码
        final String typeCodeToSearch; // 使用 final 确保 effectively final
        if (request.getPropertyType() != null && !request.getPropertyType().isEmpty()) {
            Optional<HomestayType> typeOpt = homestayTypeRepository.findByNameIgnoreCase(request.getPropertyType());
            if (typeOpt.isPresent()) {
                typeCodeToSearch = typeOpt.get().getCode();
                log.info("[HomestayService] Found type code '{}' for name '{}'", typeCodeToSearch, request.getPropertyType());
            } else {
                log.warn("[HomestayService] Could not find type code for name '{}'. Ignoring type filter.", request.getPropertyType());
                typeCodeToSearch = null; // 如果找不到代码，则不按类型筛选
            }
        } else {
            typeCodeToSearch = null; // 没有提供类型名称
        }
        
        Specification<Homestay> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("status"), "ACTIVE"));
            
            // 关键词搜索（标题、描述 或 详细地址）
            if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                String keyword = "%" + request.getKeyword() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("title"), keyword),
                        criteriaBuilder.like(root.get("description"), keyword),
                        criteriaBuilder.like(root.get("addressDetail"), keyword)
                ));
            }
            
            // 位置搜索 (修改为同时搜索文本字段和编码字段)
            if (request.getLocation() != null && !request.getLocation().isEmpty()) {
                String locationLike = "%" + request.getLocation() + "%";
                String locationExact = request.getLocation(); // Use the exact value for code matching
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("provinceText"), locationLike),
                        criteriaBuilder.like(root.get("cityText"), locationLike),
                        criteriaBuilder.like(root.get("districtText"), locationLike),
                        criteriaBuilder.like(root.get("addressDetail"), locationLike),
                        // Also check against the code fields directly
                        criteriaBuilder.equal(root.get("provinceCode"), locationExact),
                        criteriaBuilder.equal(root.get("cityCode"), locationExact),
                        criteriaBuilder.equal(root.get("districtCode"), locationExact)
                ));
            }

            // 新增：根据编码精确筛选
            if (StringUtils.hasText(request.getProvinceCode())) {
                predicates.add(criteriaBuilder.equal(root.get("provinceCode"), request.getProvinceCode()));
            }
            if (StringUtils.hasText(request.getCityCode())) {
                predicates.add(criteriaBuilder.equal(root.get("cityCode"), request.getCityCode()));
            }
            if (StringUtils.hasText(request.getDistrictCode())) {
                predicates.add(criteriaBuilder.equal(root.get("districtCode"), request.getDistrictCode()));
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
            
            // 房源类型 (使用获取到的 typeCodeToSearch 进行比较)
            if (typeCodeToSearch != null) { 
                log.info("[HomestayService] Adding predicate for propertyType CODE: '{}'", typeCodeToSearch);
                // 比较 homestays 表的 type 字段 (存储的是代码) 和查找到的 typeCodeToSearch
                predicates.add(criteriaBuilder.equal(root.get("type"), typeCodeToSearch)); 
            }
            
            // 设施过滤 (使用 requiredAmenities 列表)
            List<String> requiredAmenityValues = request.getRequiredAmenities();
            if (requiredAmenityValues != null && !requiredAmenityValues.isEmpty()) {
                log.info("接收到需要筛选的设施代码: {}", requiredAmenityValues);
                // 根据收集到的 Value/Code 查询 Amenity 实体
                List<Amenity> requiredAmenities = amenityRepository.findByValueInIgnoreCase(requiredAmenityValues);
                
                // 确保所有请求的设施都找到了对应的实体 (可选的严格检查)
                if (requiredAmenities.size() != requiredAmenityValues.size()) {
                   log.warn("请求的部分设施在数据库中未找到，可能导致筛选结果不完全匹配. 请求: {}, 找到: {}", requiredAmenityValues, requiredAmenities.stream().map(Amenity::getValue).collect(Collectors.toList()));
                }

                if (!requiredAmenities.isEmpty()) {
                   log.info("根据以下设施实体进行筛选 (AND): {}", requiredAmenities.stream().map(Amenity::getValue).collect(Collectors.toList()));
                   // 为每个找到的 Amenity 实体添加 isMember 条件
                   for (Amenity amenity : requiredAmenities) {
                       predicates.add(criteriaBuilder.isMember(amenity, root.get("amenities")));
                   }
                } else {
                    log.warn("请求的所有设施值 ({}) 在数据库中均未找到，将返回空结果集", requiredAmenityValues);
                    predicates.add(criteriaBuilder.disjunction()); // 添加恒为 false 的条件
                }
            } else {
                 log.info("未指定任何设施进行筛选");
            }
            
            // 日期可用性筛选 (关键修改)
            LocalDate checkInDate = request.getCheckInDate();
            LocalDate checkOutDate = request.getCheckOutDate();

            if (checkInDate != null && checkOutDate != null) {
                log.info("[HomestayService] Adding date availability filter: checkIn={}, checkOut={}", checkInDate, checkOutDate);

                // 创建子查询，查询 Order 表
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<Order> orderRoot = subquery.from(Order.class);
                subquery.select(orderRoot.get("id")); // 选择任意非空字段即可

                // 子查询的 WHERE 条件：
                Predicate homestayMatch = criteriaBuilder.equal(orderRoot.get("homestay"), root); // 订单属于当前房源
                Predicate statusMatch = orderRoot.get("status").in(OrderStatus.CONFIRMED.name(), OrderStatus.PAID.name()); // 状态是已确认或已支付
                // 日期重叠条件： NOT (order.endDate <= checkInDate OR order.startDate >= checkOutDate)
                // 等价于： order.endDate > checkInDate AND order.startDate < checkOutDate
                Predicate dateOverlap = criteriaBuilder.and(
                    criteriaBuilder.greaterThan(orderRoot.get("checkOutDate"), checkInDate), // 预订结束日期 > 请求入住日期
                    criteriaBuilder.lessThan(orderRoot.get("checkInDate"), checkOutDate)      // 预订开始日期 < 请求退房日期
                );

                // 组合子查询的 WHERE 条件
                subquery.where(criteriaBuilder.and(homestayMatch, statusMatch, dateOverlap));

                // 将 NOT EXISTS (子查询) 添加为主查询的条件
                predicates.add(criteriaBuilder.not(criteriaBuilder.exists(subquery)));
                log.info("[HomestayService] Added NOT EXISTS subquery for date overlap check.");
            } else {
                log.info("[HomestayService] Check-in or Check-out date not provided, skipping date availability filter.");
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        List<Homestay> homestays = homestayRepository.findAll(spec);
        log.info("搜索到 {} 个符合条件的房源", homestays.size());
        
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
    public HomestayDTO createHomestay(HomestayDTO homestayDTO, String username) {
        log.info("准备创建新的房源: {} by {}", homestayDTO.getTitle(), username);
        
        // 获取房东信息
        User owner = userRepository.findByUsername(username).orElseThrow(() -> 
            new ResourceNotFoundException("用户不存在: " + username));
        
        // 实体构建
        Homestay homestay = Homestay.builder()
            .title(homestayDTO.getTitle())
            .type(homestayDTO.getType())
            .price(new BigDecimal(homestayDTO.getPrice()))
            .status("PENDING")
            .maxGuests(homestayDTO.getMaxGuests())
            .minNights(homestayDTO.getMinNights())
            .provinceCode(homestayDTO.getProvinceCode())
            .cityCode(homestayDTO.getCityCode())
            .districtCode(homestayDTO.getDistrictCode())
            .addressDetail(homestayDTO.getAddressDetail())
            .description(homestayDTO.getDescription())
            .coverImage(homestayDTO.getCoverImage())
            .featured(homestayDTO.getFeatured())
            .owner(owner)
            .build();
        
        // 添加图片集合
        if (homestayDTO.getImages() != null && !homestayDTO.getImages().isEmpty()) {
            // 确保images集合已初始化
            if (homestay.getImages() == null) {
                homestay.setImages(new ArrayList<>());
            }
            
            log.info("添加{}张图片到房源", homestayDTO.getImages().size());
            // 清除空值并添加所有图片URL
            homestayDTO.getImages().stream()
                .filter(img -> img != null && !img.trim().isEmpty())
                .forEach(img -> {
                    homestay.getImages().add(img);
                    log.info("添加图片URL: {}", img);
                });
        }
        
        // 保存房源基本信息
        Homestay savedHomestay = homestayRepository.save(homestay);
        log.info("房源基本信息已保存，ID: {}", savedHomestay.getId());
        
        // 处理设施数据
        if (homestayDTO.getAmenities() != null && !homestayDTO.getAmenities().isEmpty()) {
            log.info("处理设施数据，数量: {}", homestayDTO.getAmenities().size());
            
            Set<Amenity> amenities = new HashSet<>();
            
            for (AmenityDTO amenityDTO : homestayDTO.getAmenities()) {
                String amenityValue = amenityDTO.getValue();
                if (amenityValue != null && !amenityValue.isEmpty()) {
                    try {
                        // 查找设施
                        Amenity amenity = amenityRepository.findById(amenityValue)
                            .orElse(null);
                            
                        if (amenity != null) {
                            amenities.add(amenity);
                            log.info("添加设施: {}", amenityValue);
                        } else {
                            log.warn("找不到设施: {}", amenityValue);
                        }
                    } catch (Exception e) {
                        log.error("添加设施时出错: {} - {}", amenityValue, e.getMessage());
                    }
                } else {
                    log.warn("收到无效的设施值: {}", amenityDTO);
                }
            }
            
            if (!amenities.isEmpty()) {
                // 设置房源设施并保存
                savedHomestay.setAmenities(amenities);
                savedHomestay = homestayRepository.save(savedHomestay);
                log.info("设施数据已保存，数量: {}", amenities.size());
                
                // 更新设施使用计数
                for (Amenity amenity : amenities) {
                    try {
                        amenityService.updateAmenityUsageCount(amenity.getValue(), true);
                    } catch (Exception e) {
                        log.warn("更新设施使用计数失败: {}", e.getMessage());
                    }
                }
            } else {
                log.warn("未找到任何有效设施，跳过设施保存");
            }
        }
        
        // 转换为DTO并返回
        HomestayDTO result = convertToDTO(savedHomestay);
        result.setOwnerUsername(owner.getUsername());
        result.setOwnerName(owner.getFullName());
        
        // 格式化价格为字符串
        result.setPrice(savedHomestay.getPrice().toString());
        
        // 添加设施信息到DTO
        if (savedHomestay.getAmenities() != null && !savedHomestay.getAmenities().isEmpty()) {
            result.setAmenities(savedHomestay.getAmenities().stream()
                .map(amenity -> {
                    AmenityDTO dto = new AmenityDTO();
                    dto.setValue(amenity.getValue());
                    dto.setLabel(amenity.getLabel());
                    dto.setIcon(amenity.getIcon());
                    return dto;
                })
                .collect(Collectors.toList()));
        }
        
        return result;
    }

    @Override
    @Transactional
    public HomestayDTO updateHomestay(Long id, HomestayDTO homestayDTO) {
        log.info("开始处理更新房源请求，ID: {}", id);
        
        // 确保DTO不为空
        if (homestayDTO == null) {
            throw new IllegalArgumentException("房源数据不能为空");
        }
        
        // 查找房源
        Homestay homestay = homestayRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("更新房源失败 - 找不到房源，ID: {}", id);
                    return new ResourceNotFoundException("房源不存在，ID: " + id);
                });
        
        log.info("找到房源，ID: {}, 标题: {}, 所有者: {}", 
                 id, homestay.getTitle(), 
                 homestay.getOwner() != null ? homestay.getOwner().getUsername() : "未知");
        
        // 获取当前用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.error("更新房源失败 - 未获取到认证信息，ID: {}", id);
            throw new RuntimeException("认证失败，无法获取当前用户信息");
        }
        
        String currentUsername = authentication.getName();
        log.info("当前用户: {}", currentUsername);
        
        // 权限检查
        boolean isAdmin = false;
        boolean isOwner = false;
        
        // 检查是否为管理员
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String auth = authority.getAuthority();
            if ("ROLE_ADMIN".equals(auth)) {
                isAdmin = true;
                log.info("当前用户是管理员(ROLE_ADMIN)，有权限更新任何房源");
                break;
            }
        }
        
        // 输出用户所有权限，用于诊断
        log.info("用户 {} 的权限: {}", currentUsername, 
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(", ")));
        
        // 检查是否为房源所有者
        if (homestay.getOwner() != null && homestay.getOwner().getUsername().equals(currentUsername)) {
            isOwner = true;
            log.info("当前用户是房源所有者，有权限更新此房源");
        } else {
            log.warn("当前用户 {} 不是房源所有者 {}", currentUsername, 
                    homestay.getOwner() != null ? homestay.getOwner().getUsername() : "未知");
        }
        
        // 权限验证
        if (!isAdmin && !isOwner) {
            log.error("更新房源失败 - 权限不足，用户: {}, 房源ID: {}, 房源所有者: {}", 
                currentUsername, id, homestay.getOwner() != null ? homestay.getOwner().getUsername() : "未知");
            throw new AccessDeniedException("您没有权限更新此房源");
        }
        
        log.info("权限验证通过，开始更新房源信息，ID: {}", id);
        
        try {
            // 更新基本信息
            homestay.setTitle(homestayDTO.getTitle());
            homestay.setType(homestayDTO.getType());
            
            // 确保价格是有效数字
            try {
                if (homestayDTO.getPrice() != null) {
                    homestay.setPrice(new BigDecimal(homestayDTO.getPrice()));
                }
            } catch (NumberFormatException e) {
                log.warn("价格格式转换错误: {}", homestayDTO.getPrice());
                // 保持原价格不变
            }
            
            homestay.setMaxGuests(homestayDTO.getMaxGuests());
            homestay.setMinNights(homestayDTO.getMinNights());

            // --- 地址字段修改 ---
            homestay.setProvinceCode(homestayDTO.getProvinceCode()); // 使用 DTO 的 Code 字段
            homestay.setCityCode(homestayDTO.getCityCode());         // 使用 DTO 的 Code 字段
            homestay.setDistrictCode(homestayDTO.getDistrictCode()); // 使用 DTO 的 Code 字段
            homestay.setAddressDetail(homestayDTO.getAddressDetail()); // 使用 DTO 的 Detail 字段
            // 可选：如果 DTO 包含 Text 字段，也可以在这里设置到实体的 Text 字段
            // homestay.setProvinceText(homestayDTO.getProvinceText());
            // homestay.setCityText(homestayDTO.getCityText());
            // homestay.setDistrictText(homestayDTO.getDistrictText());
            // --- 地址字段修改结束 ---

            homestay.setDescription(homestayDTO.getDescription());
            
            // 更新封面图片
            if (homestayDTO.getCoverImage() != null) {
                homestay.setCoverImage(homestayDTO.getCoverImage());
                log.info("更新封面图片: {}", homestayDTO.getCoverImage());
            }
            
            // 更新图片集合
            if (homestayDTO.getImages() != null) {
                // 确保images集合已初始化
                if (homestay.getImages() == null) {
                    homestay.setImages(new ArrayList<>());
                } else {
                    // 清空现有图片
                    homestay.getImages().clear();
                }
                
                // 添加新的图片URLs
                log.info("更新房源图片集合，数量: {}", homestayDTO.getImages().size());
                homestayDTO.getImages().stream()
                    .filter(img -> img != null && !img.trim().isEmpty())
                    .forEach(img -> {
                        homestay.getImages().add(img);
                        log.info("添加图片URL: {}", img);
                    });
            }
            
            // 更新设施数据
            updateHomestayAmenities(homestay, homestayDTO);
            
            // 如果DTO有状态值，则更新
            if (homestayDTO.getStatus() != null) {
                homestay.setStatus(homestayDTO.getStatus());
            }
            
            // 如果DTO有featured值，则更新
            if (homestayDTO.getFeatured() != null) {
                homestay.setFeatured(homestayDTO.getFeatured());
            }
            
            // 保存更新后的房源
            Homestay updatedHomestay = homestayRepository.save(homestay);
            log.info("房源更新成功: ID={}, 标题={}", updatedHomestay.getId(), updatedHomestay.getTitle());
            
            // 转换为DTO返回
            HomestayDTO resultDTO = convertToDTO(updatedHomestay);
            
            // 添加额外的所有者信息
            if (updatedHomestay.getOwner() != null) {
                resultDTO.setOwnerUsername(updatedHomestay.getOwner().getUsername());
                resultDTO.setOwnerName(updatedHomestay.getOwner().getFullName());
            }
            
            return resultDTO;
        } catch (Exception e) {
            log.error("更新房源出错: {}", e.getMessage(), e);
            throw new RuntimeException("更新房源失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新房源的设施信息
     */
    private void updateHomestayAmenities(Homestay homestay, HomestayDTO homestayDTO) {
        // 处理设施数据
        if (homestayDTO.getAmenities() != null) {
            try {
                log.info("开始更新设施，房源ID: {}, 接收到 {} 个设施", 
                         homestay.getId(), homestayDTO.getAmenities().size());
                
                Set<Amenity> amenities = new HashSet<>();
                
                // 处理AmenityDTO列表
                for (AmenityDTO amenityDTO : homestayDTO.getAmenities()) {
                    if (amenityDTO != null && amenityDTO.getValue() != null && !amenityDTO.getValue().isEmpty()) {
                        String amenityId = amenityDTO.getValue();
                        try {
                            Amenity amenity = amenityRepository.findById(amenityId)
                                    .orElse(null);
                            
                            if (amenity != null) {
                                amenities.add(amenity);
                                log.debug("成功添加设施: {}", amenityId);
                            } else {
                                log.warn("找不到设施: {}", amenityId);
                            }
                        } catch (Exception e) {
                            log.error("处理设施时出错: {} - {}", amenityId, e.getMessage());
                        }
                    }
                }
                
                // 清除现有设施并设置新的设施集合
                homestay.getAmenities().clear();
                homestay.getAmenities().addAll(amenities);
                log.info("成功更新 {} 个设施", amenities.size());
            } catch (Exception e) {
                log.error("处理设施时出现异常: {}", e.getMessage(), e);
                // 出错时至少保留原有设施
            }
        } else {
            log.info("没有接收到设施数据，保持原有设施不变");
        }
    }

    @Override
    public List<HomestayDTO> searchHomestays(String keyword, String provinceCode, String cityCode,
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
            
            // 省份编码筛选 (修改)
            if (provinceCode != null && !provinceCode.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("provinceCode"), provinceCode)); // <-- 修改实体字段名
            }
            
            // 城市编码筛选 (修改)
            if (cityCode != null && !cityCode.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("cityCode"), cityCode)); // <-- 修改实体字段名
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
        log.info("开始处理删除房源请求，ID: {}", id);
        
        // 1. 检查房源是否存在
        Homestay homestay = homestayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID为 " + id + " 的房源"));
        
        log.info("房源存在，ID: {}, 标题: {}, 所有者: {}", id, homestay.getTitle(), 
                 homestay.getOwner() != null ? homestay.getOwner().getUsername() : "未知");
        
        // 2. 获取当前用户和权限信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.error("无法获取认证信息，拒绝删除");
            throw new RuntimeException("未授权操作");
        }
        
        String currentUsername = authentication.getName();
        log.info("当前用户: {}", currentUsername);
        
        // 3. 权限检查 - 使用更直接的方式判断
        boolean isAdmin = false;
        boolean isOwner = false;
        
        // 检查是否管理员
        for (var authority : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                isAdmin = true;
                log.info("当前用户是管理员");
                break;
            }
        }
        
        // 检查是否房源所有者
        if (homestay.getOwner() != null && currentUsername.equals(homestay.getOwner().getUsername())) {
            isOwner = true;
            log.info("当前用户是房源所有者");
        } else {
            log.info("当前用户不是房源所有者，房源所有者是: {}", 
                    homestay.getOwner() != null ? homestay.getOwner().getUsername() : "未知");
        }
        
        if (!isAdmin && !isOwner) {
            log.error("权限不足：用户'{}'尝试删除不属于自己的房源，所有者为'{}'", 
                     currentUsername, homestay.getOwner() != null ? homestay.getOwner().getUsername() : "未知");
            throw new RuntimeException("Access Denied");
        }
        
        log.info("权限验证通过，准备删除房源");
        
        // 4. 删除房源
        try {
            // 先删除所有关联的设施
            log.info("移除房源关联的设施");
            if (homestay.getAmenities() != null && !homestay.getAmenities().isEmpty()) {
                homestay.getAmenities().clear();
                homestayRepository.save(homestay);
            }
            
            // 执行删除
            log.info("执行房源删除");
            homestayRepository.delete(homestay);
            log.info("房源删除成功，ID: {}", id);
        } catch (Exception e) {
            log.error("删除房源时发生异常，ID: {}, 错误: {}", id, e.getMessage(), e);
            
            // 尝试将房源标记为DELETED
            try {
                log.info("尝试将房源标记为DELETED状态");
                homestay.setStatus("DELETED");
                homestayRepository.save(homestay);
                log.info("房源已标记为DELETED状态");
            } catch (Exception ex) {
                log.error("标记房源为DELETED状态也失败，ID: {}", id, ex);
                throw new RuntimeException("删除房源失败：" + e.getMessage());
            }
        }
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
        // Create dynamic query specification
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
        
        // Execute query
        log.info("Executing admin homestay query with spec and pageable: {}, {}", spec, pageable);
        Page<Homestay> homestaysPage = homestayRepository.findAll(spec, pageable);
        log.info("Query executed. Found {} homestays on page {}.", homestaysPage.getNumberOfElements(), pageable.getPageNumber());

        // ---- Add Detailed Logging Here ----
        log.info("Raw Homestay entities fetched from DB (Page {}):", homestaysPage.getNumber());
        if (homestaysPage.hasContent()) {
            homestaysPage.getContent().forEach(h -> 
                log.info("  Raw Entity -> ID: {}, Title: '{}', Status: {}, Price: {}, CreatedAt: {}, UpdatedAt: {}", 
                         h.getId(), h.getTitle(), h.getStatus(), h.getPrice(), h.getCreatedAt(), h.getUpdatedAt())
            );
        } else {
            log.info("  No entities found on this page.");
        }
        // ---- End Detailed Logging ----
        
        // Convert to DTO
        Page<HomestayDTO> dtoPage = homestaysPage.map(this::convertToDTO);

        // Optional: Log DTOs after conversion
        // log.info("Converted HomestayDTOs (Page {}):", dtoPage.getNumber());
        // dtoPage.getContent().forEach(dto -> log.info("  DTO -> ID: {}, Title: '{}', Status: {}", dto.getId(), dto.getTitle(), dto.getStatus()));

        return dtoPage;
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
        
        // 处理设施集合，避免空指针异常
        List<AmenityDTO> amenitiesList = new ArrayList<>();
        if (homestay.getAmenities() != null && !homestay.getAmenities().isEmpty()) {
            log.info("处理房源设施，ID: {}, 设施数量: {}", homestay.getId(), homestay.getAmenities().size());
            amenitiesList = homestay.getAmenities().stream()
                    .map(amenity -> {
                        com.homestay3.homestaybackend.dto.AmenityDTO dto = new com.homestay3.homestaybackend.dto.AmenityDTO();
                        dto.setValue(amenity.getValue());
                        dto.setLabel(amenity.getLabel());
                        dto.setDescription(amenity.getDescription());
                        dto.setIcon(amenity.getIcon());
                        dto.setActive(amenity.isActive());
                        dto.setUsageCount(amenity.getUsageCount());
                        if (amenity.getCategory() != null) {
                            dto.setCategoryCode(amenity.getCategory().getCode());
                            dto.setCategoryName(amenity.getCategory().getName());
                            dto.setCategoryIcon(amenity.getCategory().getIcon());
                        }
                        return dto;
                    })
                    .collect(Collectors.toList());
            log.info("房源 {} 的设施已转换完成，转换后数量: {}", homestay.getId(), amenitiesList.size());
        } else {
            log.warn("房源 {} 没有关联的设施", homestay.getId());
        }
        
        return HomestayDTO.builder()
                .id(homestay.getId())
                .title(homestay.getTitle())
                .type(homestay.getType())
                .price(homestay.getPrice().toString())
                .status(homestay.getStatus())
                .maxGuests(homestay.getMaxGuests())
                .minNights(homestay.getMinNights())
                .provinceCode(homestay.getProvinceCode())
                .cityCode(homestay.getCityCode())
                .districtCode(homestay.getDistrictCode())
                .addressDetail(homestay.getAddressDetail())
                .amenities(amenitiesList)
                .description(homestay.getDescription())
                .coverImage(ensureCompleteImageUrl(homestay.getCoverImage()))
                .images(processedImages)
                .ownerUsername(homestay.getOwner().getUsername())
                .ownerName(homestay.getOwner().getFullName())
                .ownerAvatar(ensureCompleteImageUrl(homestay.getOwner().getAvatar()))
                .ownerRating(homestay.getOwner().getHostRating())
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
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                log.warn("没有找到认证信息");
                return null;
            }
            
            String username = authentication.getName();
            if (username == null || username.isEmpty() || username.equals("anonymousUser")) {
                log.warn("无效的用户名: {}", username);
                return null;
            }
            
            return userRepository.findByUsername(username)
                    .orElse(null);
        } catch (Exception e) {
            log.error("获取当前用户时发生错误: {}", e.getMessage(), e);
            return null;
        }
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
        log.info("从DTO更新Homestay对象，标题: {}", dto.getTitle());
        
        homestay.setTitle(dto.getTitle());
        homestay.setDescription(dto.getDescription());
        homestay.setType(dto.getType());
        
        // 价格从String转为BigDecimal
        if (dto.getPrice() != null) {
            homestay.setPrice(new BigDecimal(dto.getPrice()));
        }
        
        // --- 地址字段映射修改 ---
        // 移除旧的映射
        // homestay.setAddress(dto.getAddress());
        // homestay.setProvince(dto.getProvince());
        // homestay.setCity(dto.getCity());
        // homestay.setDistrict(dto.getDistrict());

        // 添加新的映射 (主要使用编码和详细地址)
        homestay.setProvinceCode(dto.getProvinceCode());
        homestay.setCityCode(dto.getCityCode());
        homestay.setDistrictCode(dto.getDistrictCode()); // DTO中的districtCode可能为null
        homestay.setAddressDetail(dto.getAddressDetail());

        // 可选：如果DTO也包含文本字段，并且实体也需要保存，可以取消注释以下行
        // homestay.setProvinceText(dto.getProvinceText());
        // homestay.setCityText(dto.getCityText());
        // homestay.setDistrictText(dto.getDistrictText());
        // --- 地址字段映射修改结束 ---

        homestay.setMaxGuests(dto.getMaxGuests());
        homestay.setMinNights(dto.getMinNights());
        
        // 将AmenityDTO转换为Amenity
        try {
            if (dto.getAmenities() != null && !dto.getAmenities().isEmpty()) {
                log.info("处理房源设施，数量: {}", dto.getAmenities().size());
                Set<Amenity> amenities = new HashSet<>();
                
                for (AmenityDTO amenityDTO : dto.getAmenities()) {
                    try {
                        String value = amenityDTO.getValue();
                        if (value != null && !value.isEmpty()) {
                            log.debug("查找设施: {}", value);
                            Amenity amenity = amenityRepository.findById(value)
                                    .orElseThrow(() -> new ResourceNotFoundException("设施不存在: " + value));
                            amenities.add(amenity);
                        }
                    } catch (Exception e) {
                        log.warn("处理单个设施时出错: {}, 错误: {}", amenityDTO.getValue(), e.getMessage());
                    }
                }
                
                // 确保清除旧的设施集合并设置新的
                homestay.getAmenities().clear();
                homestay.getAmenities().addAll(amenities);
                log.info("成功设置房源设施，最终数量: {}", homestay.getAmenities().size());
            } else {
                log.info("房源没有设施或设施列表为空");
            }
        } catch (Exception e) {
            log.error("处理房源设施时发生异常: {}", e.getMessage(), e);
        }
        
        // 设置图片
        if (dto.getImages() != null) {
            homestay.setImages(new ArrayList<>(dto.getImages()));
        }
        
        homestay.setCoverImage(dto.getCoverImage());
        homestay.setFeatured(dto.getFeatured());
        log.info("Homestay对象更新完成");
    }

    @Override
    @Transactional
    public HomestayDTO createHomestay(HomestayDTO homestayDTO) {
        // 获取当前登录用户的用户名
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        // 调用已有的createHomestay(HomestayDTO, String)方法
        return createHomestay(homestayDTO, username);
    }

    // 判断当前用户是否为管理员
    private boolean isAdminUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || authentication.getAuthorities() == null) {
                log.warn("无法获取认证信息或权限列表");
                return false;
            }
            
            return authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        } catch (Exception e) {
            log.error("检查管理员权限时发生错误: {}", e.getMessage(), e);
            return false;
        }
    }
}