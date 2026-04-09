package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.dto.HomestayRequest;
import com.homestay3.homestaybackend.dto.HomestaySearchRequest;
import com.homestay3.homestaybackend.dto.SuggestedFeatureDTO;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.model.HomestayStatus;
import com.homestay3.homestaybackend.entity.HomestayAuditLog;
import com.homestay3.homestaybackend.entity.HomestayType;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.HomestayAuditLogRepository;
import com.homestay3.homestaybackend.repository.HomestayTypeRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.HomestayService;
import com.homestay3.homestaybackend.service.AmenityService;
import com.homestay3.homestaybackend.service.HomestayFeatureAnalysisService;
import com.homestay3.homestaybackend.service.HomestayAvailabilityService;
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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
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
import java.util.stream.Stream;
import java.util.Objects;

import com.homestay3.homestaybackend.entity.Amenity;
import com.homestay3.homestaybackend.repository.AmenityRepository;
import com.homestay3.homestaybackend.dto.AmenityDTO;
import com.homestay3.homestaybackend.util.ImageUrlUtil;
import org.springframework.security.access.AccessDeniedException;
import java.util.Optional;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.entity.Order;
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
    private final HomestayAuditLogRepository homestayAuditLogRepository;
    private final OrderRepository orderRepository;
    private final HomestayAvailabilityService availabilityService;
    private final HomestayFeatureAnalysisService homestayFeatureAnalysisService;
    private final ImageUrlUtil imageUrlUtil;
    private static final Logger log = LoggerFactory.getLogger(HomestayServiceImpl.class);

    @Override
    public List<HomestayDTO> getAllHomestays() {
        log.info("开始获取所有房源");

        try {
            // 只获取状态为ACTIVE的房源
            List<Homestay> homestays = homestayRepository.findByStatus(HomestayStatus.ACTIVE);
            log.info("成功查询到{}个ACTIVE状态的房源", homestays.size());

            if (homestays.isEmpty()) {
                log.warn("没有找到任何ACTIVE状态的房源");
                return new ArrayList<>();
            }

            List<HomestayDTO> result = new ArrayList<>();
            for (Homestay homestay : homestays) {
                try {
                    HomestayDTO dto = convertToDTO(homestay, null);
                    if (dto != null) {
                        result.add(dto);
                    }
                } catch (Exception e) {
                    log.error("转换房源{}为DTO时发生错误: {}", homestay.getId(), e.getMessage(), e);
                    // 继续处理其他房源，不因单个房源错误而中断整个流程
                }
            }

            log.info("成功转换{}个房源为DTO", result.size());
            return result;

        } catch (Exception e) {
            log.error("获取所有房源时发生严重错误: {}", e.getMessage(), e);

            // 返回空列表而不是抛出异常，避免前端500错误
            return new ArrayList<>();
        }
    }

    @Override
    @Deprecated
    public List<HomestayDTO> getFeaturedHomestays() {
        log.info("获取推荐房源");

        // 获取状态为ACTIVE且被标记为featured的房源，最多返回6个
        List<Homestay> featuredHomestays = homestayRepository.findByStatusAndFeaturedTrue(HomestayStatus.ACTIVE);

        List<Homestay> finalHomestayList = new ArrayList<>(); // Placeholder for actual logic
        if (featuredHomestays.size() < 6) {
            List<Homestay> regularHomestays = homestayRepository.findByStatusAndFeaturedFalse(HomestayStatus.ACTIVE);
            int remaining = 6 - featuredHomestays.size();
            if (regularHomestays.size() > remaining) {
                regularHomestays = regularHomestays.subList(0, remaining);
            }
            finalHomestayList.addAll(featuredHomestays);
            finalHomestayList.addAll(regularHomestays);
        } else if (featuredHomestays.size() > 6) {
            finalHomestayList.addAll(featuredHomestays.subList(0, 6));
        } else {
            finalHomestayList.addAll(featuredHomestays);
        }

        return finalHomestayList.stream()
                .map(homestay -> convertToDTO(homestay, null)) // Pass null for criteria
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "homestayDetails", key = "#id")
    public HomestayDTO getHomestayById(Long id, List<String> referringSearchCriteria) { // Signature updated
        log.info("根据ID获取房源详情: id={}, referringSearchCriteria={}", id, referringSearchCriteria);

        Homestay homestay = homestayRepository.findByIdWithAmenities(id)
                .orElseThrow(() -> new ResourceNotFoundException("Homestay not found with id: " + id));

        // Initialize amenities if needed (though findByIdWithAmenities should handle
        // this)
        if (homestay.getAmenities() != null) {
            org.hibernate.Hibernate.initialize(homestay.getAmenities());
        }

        return convertToDTO(homestay, referringSearchCriteria); // Pass criteria to convertToDTO
    }

    @Override
    public List<HomestayDTO> getHomestaysByPropertyType(String propertyType) {
        log.info("根据房源类型获取房源列表: type={}", propertyType);

        List<Homestay> homestays = homestayRepository.findByTypeAndStatus(propertyType, HomestayStatus.ACTIVE);

        return homestays.stream()
                .map(homestay -> convertToDTO(homestay, null)) // Pass null for criteria
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
                log.info("[HomestayService] Found type code '{}' for name '{}'", typeCodeToSearch,
                        request.getPropertyType());
            } else {
                log.warn("[HomestayService] Could not find type code for name '{}'. Ignoring type filter.",
                        request.getPropertyType());
                typeCodeToSearch = null; // 如果找不到代码，则不按类型筛选
            }
        } else {
            typeCodeToSearch = null; // 没有提供类型名称
        }

        Specification<Homestay> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("status"), HomestayStatus.ACTIVE));

            // 关键词搜索（标题、描述 或 详细地址）
            if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                String keyword = "%" + request.getKeyword() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("title"), keyword),
                        criteriaBuilder.like(root.get("description"), keyword),
                        criteriaBuilder.like(root.get("addressDetail"), keyword)));
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
                        criteriaBuilder.equal(root.get("districtCode"), locationExact)));
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

            if (Stream.of(
                    request.getNorthEastLat(),
                    request.getNorthEastLng(),
                    request.getSouthWestLat(),
                    request.getSouthWestLng()
            ).allMatch(Objects::nonNull)) {
                BigDecimal minLatitude = request.getNorthEastLat().min(request.getSouthWestLat());
                BigDecimal maxLatitude = request.getNorthEastLat().max(request.getSouthWestLat());
                BigDecimal minLongitude = request.getNorthEastLng().min(request.getSouthWestLng());
                BigDecimal maxLongitude = request.getNorthEastLng().max(request.getSouthWestLng());

                predicates.add(criteriaBuilder.isNotNull(root.get("latitude")));
                predicates.add(criteriaBuilder.isNotNull(root.get("longitude")));
                predicates.add(criteriaBuilder.between(root.get("latitude"), minLatitude, maxLatitude));
                predicates.add(criteriaBuilder.between(root.get("longitude"), minLongitude, maxLongitude));
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
                    log.warn("请求的部分设施在数据库中未找到，可能导致筛选结果不完全匹配. 请求: {}, 找到: {}", requiredAmenityValues,
                            requiredAmenities.stream().map(Amenity::getValue).collect(Collectors.toList()));
                }

                if (!requiredAmenities.isEmpty()) {
                    log.info("根据以下设施实体进行筛选 (AND): {}",
                            requiredAmenities.stream().map(Amenity::getValue).collect(Collectors.toList()));
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
                log.info("[HomestayService] Adding date availability filter: checkIn={}, checkOut={}", checkInDate,
                        checkOutDate);

                // 创建子查询，查询 Order 表
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<Order> orderRoot = subquery.from(Order.class);
                subquery.select(orderRoot.get("id")); // 选择任意非空字段即可

                // 子查询的 WHERE 条件：
                Predicate homestayMatch = criteriaBuilder.equal(orderRoot.get("homestay"), root); // 订单属于当前房源
                Predicate statusMatch = orderRoot.get("status").in(OrderStatus.CONFIRMED.name(),
                        OrderStatus.PAID.name()); // 状态是已确认或已支付
                // 日期重叠条件： NOT (order.endDate <= checkInDate OR order.startDate >= checkOutDate)
                // 等价于： order.endDate > checkInDate AND order.startDate < checkOutDate
                Predicate dateOverlap = criteriaBuilder.and(
                        criteriaBuilder.greaterThan(orderRoot.get("checkOutDate"), checkInDate), // 预订结束日期 > 请求入住日期
                        criteriaBuilder.lessThan(orderRoot.get("checkInDate"), checkOutDate) // 预订开始日期 < 请求退房日期
                );

                // 组合子查询的 WHERE 条件
                subquery.where(criteriaBuilder.and(homestayMatch, statusMatch, dateOverlap));

                // 将 NOT EXISTS (子查询) 添加为主查询的条件
                predicates.add(criteriaBuilder.not(criteriaBuilder.exists(subquery)));
                log.info("[HomestayService] Added NOT EXISTS subquery for date overlap check.");
            } else {
                log.info(
                        "[HomestayService] Check-in or Check-out date not provided, skipping date availability filter.");
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        List<Homestay> homestays = homestayRepository.findAll(spec);
        log.info("搜索到 {} 个符合条件的房源", homestays.size());

        // When searching, the request.getRequiredAmenities() or other parts of request
        // could form the basis for referringSearchCriteria if we want to highlight
        // *why* these results matched.
        // For now, passing null, but this is a point for future enhancement if needed
        // for search results page.
        List<String> criteriaFromSearch = new ArrayList<>();
        if (request.getRequiredAmenities() != null && !request.getRequiredAmenities().isEmpty()) {
            request.getRequiredAmenities().forEach(
                    amenityCode -> criteriaFromSearch.add("AMENITY_" + amenityCode.toUpperCase().replace(" ", "_")));
        }
        if (typeCodeToSearch != null) {
            criteriaFromSearch.add("PROPERTY_TYPE_" + typeCodeToSearch.toUpperCase());
        }
        // Potentially add other criteria based on request like keywords if they map to
        // review keywords etc.

        final List<String> finalCriteriaFromSearch = criteriaFromSearch.isEmpty() ? null
                : Collections.unmodifiableList(criteriaFromSearch);

        return homestays.stream()
                .map(homestay -> convertToDTO(homestay, finalCriteriaFromSearch)) // Pass criteria from search request
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
            String filename = System.currentTimeMillis() + "_" + (int) (Math.random() * 1000) + extension;

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
        return homestayRepository.findAll(pageable)
                .map(homestay -> convertToDTO(homestay, null)); // Pass null for criteria
    }

    @Override
    public List<HomestayDTO> getHomestaysByOwner(String username) {
        log.info("开始获取用户{}的房源列表", username);

        try {
            // 查找用户
            User owner = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

            log.info("找到用户: ID={}, 用户名={}", owner.getId(), owner.getUsername());

            // 查找房源
            List<Homestay> homestays = homestayRepository.findByOwner(owner);
            log.info("从数据库获取到{}条房源记录", homestays != null ? homestays.size() : 0);

            if (homestays == null || homestays.isEmpty()) {
                log.info("用户{}暂无房源", username);
                return new ArrayList<>();
            }

            // 转换为DTO
            List<HomestayDTO> result = new ArrayList<>();
            for (int i = 0; i < homestays.size(); i++) {
                Homestay homestay = homestays.get(i);
                try {
                    if (homestay == null) {
                        log.warn("发现null房源记录，跳过处理");
                        continue;
                    }

                    log.debug("正在转换房源{}为DTO", homestay.getId());
                    HomestayDTO dto = convertToDTO(homestay, null);

                    if (dto != null) {
                        result.add(dto);
                        log.debug("成功转换房源{}", homestay.getId());
                    } else {
                        log.warn("房源{}转换为DTO失败，返回null", homestay.getId());
                    }
                } catch (Exception e) {
                    log.error("转换房源{}为DTO时发生异常: {}", homestay != null ? homestay.getId() : "null", e.getMessage(), e);
                    // 继续处理其他房源，不因为单个房源出错而影响整体
                }
            }

            log.info("成功转换{}条房源为DTO，用户: {}", result.size(), username);
            return result;

        } catch (ResourceNotFoundException e) {
            log.error("用户不存在: {}", username);
            throw e; // 重新抛出用户不存在异常
        } catch (Exception e) {
            log.error("获取用户{}房源列表时发生未预期异常: {}", username, e.getMessage(), e);
            throw new RuntimeException("获取房源列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public HomestayDTO createHomestay(HomestayDTO homestayDTO, String username) {
        log.info("准备创建新的房源: {} by {}", homestayDTO.getTitle(), username);

        // 确定是否为草稿模式
        boolean isDraft = homestayDTO.getStatus() != null &&
                "DRAFT".equalsIgnoreCase(homestayDTO.getStatus());

        // 根据是否为草稿进行不同的验证
        validateHomestayDTO(homestayDTO, isDraft);

        // 获取房东信息
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + username));

        // 确定房源状态：支持传入状态，默认为草稿
        HomestayStatus initialStatus = HomestayStatus.DRAFT; // 默认为草稿状态
        if (homestayDTO.getStatus() != null && !homestayDTO.getStatus().isEmpty()) {
            try {
                initialStatus = HomestayStatus.valueOf(homestayDTO.getStatus().toUpperCase());
                log.info("使用传入的房源状态: {}", initialStatus);
            } catch (IllegalArgumentException e) {
                log.warn("无效的房源状态: {}, 使用默认状态: {}", homestayDTO.getStatus(), initialStatus);
            }
        }

        // 处理草稿模式下的默认值
        String title = homestayDTO.getTitle();
        String type = homestayDTO.getType();
        BigDecimal price;
        String addressDetail = homestayDTO.getAddressDetail();
        String description = homestayDTO.getDescription();
        Integer maxGuests = homestayDTO.getMaxGuests();
        Integer minNights = homestayDTO.getMinNights();
        Integer maxNights = homestayDTO.getMaxNights();
        Boolean featured = homestayDTO.getFeatured();

        if (isDraft) {
            // 草稿模式下提供默认值
            if (title == null || title.isEmpty()) {
                title = "未命名房源";
            }
            if (type == null || type.isEmpty()) {
                type = "ENTIRE";
            }
            try {
                price = homestayDTO.getPrice() != null ? new BigDecimal(homestayDTO.getPrice()) : BigDecimal.ZERO;
            } catch (NumberFormatException e) {
                price = BigDecimal.ZERO;
            }
            if (addressDetail == null || addressDetail.isEmpty()) {
                addressDetail = "";
            }
            if (description == null || description.isEmpty()) {
                description = "";
            }
            if (maxGuests == null || maxGuests <= 0) {
                maxGuests = 1;
            }
            if (minNights == null || minNights <= 0) {
                minNights = 1;
            }
            if (featured == null) {
                featured = false;
            }
            log.info("草稿模式：已设置默认值");
        } else {
            // 非草稿模式：正常解析
            price = new BigDecimal(homestayDTO.getPrice());
        }

        // 实体构建
        Homestay homestay = Homestay.builder()
                .title(title)
                .type(type)
                .price(price)
                .status(initialStatus)
                .maxGuests(maxGuests)
                .minNights(minNights)
                .maxNights(maxNights)
                .provinceCode(homestayDTO.getProvinceCode())
                .cityCode(homestayDTO.getCityCode())
                .districtCode(homestayDTO.getDistrictCode())
                .addressDetail(addressDetail)
                .description(description)
                .coverImage(homestayDTO.getCoverImage())
                .featured(featured)
                .owner(owner)
                .latitude(homestayDTO.getLatitude() != null ? BigDecimal.valueOf(homestayDTO.getLatitude()) : null)
                .longitude(homestayDTO.getLongitude() != null ? BigDecimal.valueOf(homestayDTO.getLongitude()) : null)
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
        log.info("房源基本信息已保存，ID: {}, 状态: {}, 标题: {}",
                savedHomestay.getId(), savedHomestay.getStatus(), savedHomestay.getTitle());

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
        HomestayDTO result = convertToDTO(savedHomestay, null);
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
    @CacheEvict(value = "homestayDetails", key = "#id")
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
            homestay.setMaxNights(homestayDTO.getMaxNights());

            // --- 地址字段修改 ---
            homestay.setProvinceCode(homestayDTO.getProvinceCode()); // 使用 DTO 的 Code 字段
            homestay.setCityCode(homestayDTO.getCityCode()); // 使用 DTO 的 Code 字段
            homestay.setDistrictCode(homestayDTO.getDistrictCode()); // 使用 DTO 的 Code 字段
            homestay.setAddressDetail(homestayDTO.getAddressDetail()); // 使用 DTO 的 Detail 字段
            // 可选：如果 DTO 包含 Text 字段，也可以在这里设置到实体的 Text 字段
            // homestay.setProvinceText(homestayDTO.getProvinceText());
            // homestay.setCityText(homestayDTO.getCityText());
            // homestay.setDistrictText(homestayDTO.getDistrictText());
            // --- 地址字段修改结束 ---

            // 更新经纬度坐标
            if (homestayDTO.getLatitude() != null) {
                homestay.setLatitude(BigDecimal.valueOf(homestayDTO.getLatitude()));
            }
            if (homestayDTO.getLongitude() != null) {
                homestay.setLongitude(BigDecimal.valueOf(homestayDTO.getLongitude()));
            }

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
                homestay.setStatus(HomestayStatus.valueOf(homestayDTO.getStatus()));
            }

            // 如果DTO有featured值，则更新
            if (homestayDTO.getFeatured() != null) {
                homestay.setFeatured(homestayDTO.getFeatured());
            }

            // 保存更新后的房源
            Homestay updatedHomestay = homestayRepository.save(homestay);
            log.info("房源更新成功: ID={}, 标题={}", updatedHomestay.getId(), updatedHomestay.getTitle());

            // 转换为DTO返回
            HomestayDTO resultDTO = convertToDTO(updatedHomestay, null);

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
        // This is an older search method, less structured.
        // It might be harder to derive specific referringSearchCriteria here compared
        // to HomestaySearchRequest.
        // For now, pass null.
        // Convert Integer prices to BigDecimal for repository call
        BigDecimal minPriceDecimal = minPrice != null ? BigDecimal.valueOf(minPrice) : null;
        BigDecimal maxPriceDecimal = maxPrice != null ? BigDecimal.valueOf(maxPrice) : null;

        // The 'keyword' likely maps to 'location' in the repository method.
        // 'provinceCode' and 'cityCode' are not directly in the repository's
        // searchHomestays signature,
        // that method uses a general 'location' string.
        // For simplicity, we'll use the keyword as location and pass other compatible
        // params.
        List<Homestay> homestays = homestayRepository.searchHomestays(keyword, minPriceDecimal, maxPriceDecimal, guests,
                type);
        return homestays.stream()
                .map(homestay -> convertToDTO(homestay, null))
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

            // 尝试将房源标记为INACTIVE
            try {
                log.info("尝试将房源标记为INACTIVE状态");
                homestay.setStatus(HomestayStatus.INACTIVE);
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
        homestay.setStatus(HomestayStatus.valueOf(status));
        homestay.setUpdatedAt(LocalDateTime.now());

        Homestay updatedHomestay = homestayRepository.save(homestay);
        return convertToDTO(updatedHomestay, null);
    }

    @Override
    @Transactional
    public void updateHomestayStatus(Long id, String status) {
        Homestay homestay = homestayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + id));

        // 验证状态值是否有效
        validateHomestayStatus(status);

        // 更新状态
        homestay.setStatus(HomestayStatus.valueOf(status));
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
                try {
                    HomestayStatus statusEnum = HomestayStatus.valueOf(status);
                    predicates.add(criteriaBuilder.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException e) {
                    log.warn("无效的状态值: {}", status);
                }
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
        log.info("Query executed. Found {} homestays on page {}.", homestaysPage.getNumberOfElements(),
                pageable.getPageNumber());

        // ---- Add Detailed Logging Here ----
        log.info("Raw Homestay entities fetched from DB (Page {}):", homestaysPage.getNumber());
        if (homestaysPage.hasContent()) {
            homestaysPage.getContent()
                    .forEach(h -> log.info(
                            "  Raw Entity -> ID: {}, Title: '{}', Status: {}, Price: {}, CreatedAt: {}, UpdatedAt: {}",
                            h.getId(), h.getTitle(), h.getStatus(), h.getPrice(), h.getCreatedAt(), h.getUpdatedAt()));
        } else {
            log.info("  No entities found on this page.");
        }
        // ---- End Detailed Logging ----

        // Convert to DTO
        Page<HomestayDTO> dtoPage = homestaysPage.map(homestay -> convertToDTO(homestay, null)); // Pass null for
                                                                                                 // criteria

        // Optional: Log DTOs after conversion
        // log.info("Converted HomestayDTOs (Page {}):", dtoPage.getNumber());
        // dtoPage.getContent().forEach(dto -> log.info(" DTO -> ID: {}, Title: '{}',
        // Status: {}", dto.getId(), dto.getTitle(), dto.getStatus()));

        return dtoPage;
    }

    /**
     * 将实体转换为DTO
     */
    private HomestayDTO convertToDTO(Homestay homestay, List<String> referringSearchCriteria) { // Signature updated
        if (homestay == null) {
            log.warn("尝试转换null的Homestay实体为DTO");
            return null;
        }

        try {
            log.debug("开始转换房源{}为DTO", homestay.getId());

            HomestayDTO dto = new HomestayDTO();
            dto.setId(homestay.getId());
            dto.setTitle(homestay.getTitle());
            dto.setType(homestay.getType()); // type code

            // Set Property Type Name with enhanced error handling
            try {
                if (homestay.getType() != null && !homestay.getType().isEmpty()) {
                    Optional<HomestayType> typeEntityOpt = homestayTypeRepository.findByCode(homestay.getType());
                    if (typeEntityOpt.isPresent()) {
                        dto.setPropertyTypeName(typeEntityOpt.get().getName());
                    } else {
                        dto.setPropertyTypeName(homestay.getType()); // 使用类型代码作为默认值
                        log.warn("找不到类型代码 {} 对应的房源类型实体", homestay.getType());
                    }
                } else {
                    dto.setPropertyTypeName("未知类型");
                }
            } catch (Exception e) {
                log.error("设置房源类型名称时发生错误: {}", e.getMessage());
                dto.setPropertyTypeName("未知类型");
            }

            // Price handling with null check
            if (homestay.getPrice() != null) {
                dto.setPrice(homestay.getPrice().toString());
            } else {
                dto.setPrice("0"); // 默认价格
            }

            dto.setStatus(homestay.getStatus() != null ? homestay.getStatus().name() : "UNKNOWN");
            dto.setMaxGuests(homestay.getMaxGuests() != null ? homestay.getMaxGuests() : 1);
            dto.setMinNights(homestay.getMinNights() != null ? homestay.getMinNights() : 1);
            dto.setMaxNights(homestay.getMaxNights());
            dto.setProvinceText(homestay.getProvinceText());
            dto.setCityText(homestay.getCityText());
            dto.setDistrictText(homestay.getDistrictText());
            dto.setAddressDetail(homestay.getAddressDetail());
            dto.setProvinceCode(homestay.getProvinceCode());
            dto.setCityCode(homestay.getCityCode());
            dto.setDistrictCode(homestay.getDistrictCode());
            dto.setDescription(homestay.getDescription());

            // 图片URL处理 - 增强异常处理
            try {
                if (imageUrlUtil != null) {
                    dto.setCoverImage(imageUrlUtil.ensureCompleteImageUrl(homestay.getCoverImage()));
                    if (homestay.getImages() != null) {
                        dto.setImages(imageUrlUtil.ensureCompleteImageUrls(homestay.getImages()));
                    }
                } else {
                    log.warn("ImageUrlUtil为null，跳过图片URL处理");
                    dto.setCoverImage(homestay.getCoverImage());
                    dto.setImages(homestay.getImages());
                }
            } catch (Exception e) {
                log.error("处理图片URL时发生错误: {}", e.getMessage());
                dto.setCoverImage(homestay.getCoverImage());
                dto.setImages(homestay.getImages());
            }

            // Owner信息处理 - 增强空指针检查
            try {
                if (homestay.getOwner() != null) {
                    dto.setOwnerId(homestay.getOwner().getId());
                    dto.setOwnerUsername(homestay.getOwner().getUsername());

                    String ownerName = homestay.getOwner().getFullName();
                    if (ownerName == null || ownerName.isEmpty()) {
                        ownerName = homestay.getOwner().getNickname();
                    }
                    if (ownerName == null || ownerName.isEmpty()) {
                        ownerName = homestay.getOwner().getUsername();
                    }
                    dto.setOwnerName(ownerName);

                    if (imageUrlUtil != null) {
                        dto.setOwnerAvatar(imageUrlUtil.ensureCompleteImageUrl(homestay.getOwner().getAvatar()));
                    } else {
                        dto.setOwnerAvatar(homestay.getOwner().getAvatar());
                    }
                } else {
                    log.warn("房源 {} 的Owner为null", homestay.getId());
                }
            } catch (Exception e) {
                log.error("处理房源所有者信息时发生错误: {}", e.getMessage());
            }

            dto.setFeatured(homestay.getFeatured() != null ? homestay.getFeatured() : false);
            dto.setAutoConfirm(homestay.getAutoConfirm() != null ? homestay.getAutoConfirm() : false);
            dto.setCreatedAt(homestay.getCreatedAt());
            dto.setUpdatedAt(homestay.getUpdatedAt());

            // 经纬度坐标（用于地图找房）
            if (homestay.getLatitude() != null) {
                dto.setLatitude(homestay.getLatitude().doubleValue());
            }
            if (homestay.getLongitude() != null) {
                dto.setLongitude(homestay.getLongitude().doubleValue());
            }

            // 设施转换 - 增强异常处理
            try {
                if (homestay.getAmenities() != null && amenityService != null) {
                    dto.setAmenities(homestay.getAmenities().stream()
                            .filter(Objects::nonNull) // 过滤null的设施
                            .map(amenity -> {
                                try {
                                    return amenityService.convertToDTO(amenity);
                                } catch (Exception e) {
                                    log.error("转换设施 {} 为DTO时发生错误: {}", amenity.getValue(), e.getMessage());
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull) // 过滤转换失败的设施
                            .collect(Collectors.toList()));
                } else {
                    dto.setAmenities(new ArrayList<>());
                }
            } catch (Exception e) {
                log.error("处理房源设施时发生错误: {}", e.getMessage());
                dto.setAmenities(new ArrayList<>());
            }

            // 特征分析 - 增强异常处理
            try {
                if (homestayFeatureAnalysisService != null) {
                    dto.setSuggestedFeatures(
                            homestayFeatureAnalysisService.analyzeFeatures(homestay, referringSearchCriteria));
                } else {
                    dto.setSuggestedFeatures(new ArrayList<>());
                }
            } catch (Exception e) {
                log.error("分析房源特征时发生错误: {}", e.getMessage());
                dto.setSuggestedFeatures(new ArrayList<>());
            }

            log.debug("成功转换房源{}为DTO", homestay.getId());
            return dto;

        } catch (Exception e) {
            log.error("转换房源{}为DTO时发生严重错误: {}", homestay.getId(), e.getMessage(), e);
            return null;
        }
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

    // 验证房源状态是否有效
    private void validateHomestayStatus(String status) {
        try {
            HomestayStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的房源状态: " + status);
        }
    }

    // 验证房源状态枚举是否有效
    private void validateHomestayStatus(HomestayStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("房源状态不能为空");
        }
    }

    // 验证房源DTO是否包含必填字段
    private void validateHomestayDTO(HomestayDTO dto) {
        validateHomestayDTO(dto, false);
    }

    // 验证房源DTO是否包含必填字段（支持草稿模式）
    private void validateHomestayDTO(HomestayDTO dto, boolean isDraft) {
        if (dto == null) {
            throw new IllegalArgumentException("房源数据不能为空");
        }

        // 草稿模式下，只验证基本字段存在即可，允许为空
        if (isDraft) {
            // 草稿状态下只需要确保字段存在，内容可以为空
            log.info("草稿模式验证：允许部分字段为空");
            return;
        }

        // 非草稿模式下的完整验证
        if (dto.getTitle() == null || dto.getTitle().isEmpty()) {
            throw new IllegalArgumentException("房源标题不能为空");
        }
        if (dto.getType() == null || dto.getType().isEmpty()) {
            throw new IllegalArgumentException("房源类型不能为空");
        }
        if (dto.getPrice() == null) {
            throw new IllegalArgumentException("房源价格不能为空");
        }
        if (dto.getAddressDetail() == null || dto.getAddressDetail().isEmpty()) {
            throw new IllegalArgumentException("详细地址不能为空");
        }
        if (dto.getDescription() == null || dto.getDescription().isEmpty()) {
            throw new IllegalArgumentException("房源描述不能为空");
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
        homestay.setMaxNights(dto.getMaxNights());

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
        homestay.setAutoConfirm(dto.getAutoConfirm());
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

    /**
     * 验证房源是否可以从草稿状态提交审核
     */
    private boolean isHomestayReadyForReview(Homestay homestay) {
        if (homestay == null) {
            return false;
        }

        // 检查必填字段
        if (homestay.getTitle() == null || homestay.getTitle().isEmpty() || homestay.getTitle().equals("未命名房源")) {
            log.warn("房源标题未设置，无法提交审核");
            return false;
        }

        if (homestay.getPrice() == null || homestay.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("房源价格未设置或无效，无法提交审核");
            return false;
        }

        if (homestay.getAddressDetail() == null || homestay.getAddressDetail().isEmpty()) {
            log.warn("房源地址未设置，无法提交审核");
            return false;
        }

        if (homestay.getDescription() == null || homestay.getDescription().isEmpty()) {
            log.warn("房源描述未设置，无法提交审核");
            return false;
        }

        if (homestay.getCoverImage() == null || homestay.getCoverImage().isEmpty()) {
            log.warn("房源封面图片未设置，无法提交审核");
            return false;
        }

        log.info("房源ID: {} 满足提交审核的条件", homestay.getId());
        return true;
    }

    /**
     * 获取房源审核就绪状态的详细信息
     */
    private String getHomestayReviewReadinessDetails(Homestay homestay) {
        if (homestay == null) {
            return "房源不存在";
        }

        List<String> missingItems = new ArrayList<>();

        if (homestay.getTitle() == null || homestay.getTitle().isEmpty() || homestay.getTitle().equals("未命名房源")) {
            missingItems.add("房源标题");
        }

        if (homestay.getPrice() == null || homestay.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            missingItems.add("房源价格");
        }

        if (homestay.getAddressDetail() == null || homestay.getAddressDetail().isEmpty()) {
            missingItems.add("详细地址");
        }

        if (homestay.getDescription() == null || homestay.getDescription().isEmpty()) {
            missingItems.add("房源描述");
        }

        if (homestay.getCoverImage() == null || homestay.getCoverImage().isEmpty()) {
            missingItems.add("封面图片");
        }

        if (missingItems.isEmpty()) {
            return "房源信息完整，可以提交审核";
        } else {
            return "缺少必要信息：" + String.join("、", missingItems);
        }
    }

    @Override
    public boolean checkHomestayReadyForReview(Long homestayId) {
        log.info("检查房源是否可以提交审核，ID: {}", homestayId);

        Homestay homestay = homestayRepository.findById(homestayId)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + homestayId));

        // 验证权限：只有房源所有者或管理员可以检查
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String currentUsername = authentication.getName();
            boolean isAdmin = isAdminUser();
            boolean isOwner = homestay.getOwner() != null &&
                    homestay.getOwner().getUsername().equals(currentUsername);

            if (!isAdmin && !isOwner) {
                throw new AccessDeniedException("您没有权限查看此房源的审核状态");
            }
        }

        return isHomestayReadyForReview(homestay);
    }

    @Override
    public String getHomestayReviewReadinessDetails(Long homestayId) {
        log.info("获取房源审核就绪状态详情，ID: {}", homestayId);

        Homestay homestay = homestayRepository.findById(homestayId)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + homestayId));

        // 验证权限：只有房源所有者或管理员可以查看
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String currentUsername = authentication.getName();
            boolean isAdmin = isAdminUser();
            boolean isOwner = homestay.getOwner() != null &&
                    homestay.getOwner().getUsername().equals(currentUsername);

            if (!isAdmin && !isOwner) {
                throw new AccessDeniedException("您没有权限查看此房源的审核状态");
            }
        }

        return getHomestayReviewReadinessDetails(homestay);
    }

    @Override
    public HomestayDTO getHomestayWithOwnerDetails(Long id) {
        log.info("获取房源详情（包含完整房东信息），ID: {}", id);

        Homestay homestay = homestayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + id));

        // 基础转换
        HomestayDTO dto = convertToDTO(homestay, null);

        // 增强房东信息
        if (homestay.getOwner() != null) {
            User owner = homestay.getOwner();

            // 基本房东信息（已在convertToDTO中设置）
            dto.setOwnerId(owner.getId());
            dto.setOwnerUsername(owner.getUsername());
            dto.setOwnerName(owner.getRealName() != null ? owner.getRealName()
                    : (owner.getNickname() != null ? owner.getNickname() : owner.getUsername()));
            dto.setOwnerAvatar(imageUrlUtil.ensureCompleteImageUrl(owner.getAvatar()));

            // 设置扩展的房东信息字段
            dto.setOwnerPhone(owner.getPhone());
            dto.setOwnerEmail(owner.getEmail());
            dto.setOwnerRealName(owner.getRealName());
            dto.setOwnerNickname(owner.getNickname());
            dto.setOwnerOccupation(owner.getOccupation());
            dto.setOwnerIntroduction(owner.getIntroduction());
            dto.setOwnerJoinDate(owner.getCreatedAt());
            dto.setOwnerHostSince(owner.getHostSince());
            dto.setOwnerHostRating(owner.getHostRating());

            // 统计房东的房源数量
            Long homestayCount = homestayRepository.countByOwnerId(owner.getId());
            dto.setOwnerHomestayCount(homestayCount);

            // 计算房东评分（如果有评价系统）
            try {
                // 这里可以从ReviewRepository获取房东的平均评分
                // Double avgRating = reviewRepository.getAverageRatingByHostId(owner.getId());
                // if (avgRating != null) {
                // dto.setOwnerRating(avgRating);
                // }
            } catch (Exception e) {
                log.warn("无法获取房东评分: {}", e.getMessage());
            }

            log.info("房东完整信息 - ID: {}, 用户名: {}, 真实姓名: {}, 手机: {}, 房源数量: {}",
                    owner.getId(), owner.getUsername(), owner.getRealName(),
                    owner.getPhone(), homestayCount);

            // 设置房东评分到现有字段
            if (owner.getHostRating() != null) {
                dto.setOwnerRating(owner.getHostRating());
            }
        }

        log.info("房源详情（包含房东信息）获取成功，房源ID: {}, 房东信息: {}",
                id, dto.getOwnerName());

        return dto;
    }

    @Override
    public List<LocalDate> getUnavailableDates(Long homestayId) {
        log.info("[HomestayService] 获取房源ID: {} 的不可用日期", homestayId);

        try {
            // 验证房源是否存在
            if (!homestayRepository.existsById(homestayId)) {
                log.warn("[HomestayService] 房源不存在，ID: {}", homestayId);
                return new ArrayList<>();
            }

            // 直接从 HomestayAvailability 表查询 BOOKED 状态的日期
            // 这与 BookingConflictService 使用同一个数据源，保证数据一致性
            LocalDate today = LocalDate.now();
            LocalDate endDate = today.plusYears(1); // 查询未来一年的可用性
            List<LocalDate> unavailableDates = availabilityService.getBookedDates(homestayId, today, endDate);

            log.info("[HomestayService] 房源ID: {} 不可用日期总数: {}", homestayId, unavailableDates.size());
            return unavailableDates;

        } catch (Exception e) {
            log.error("[HomestayService] 获取房源不可用日期时发生错误: {}", e.getMessage(), e);
            // 返回空列表而不是抛出异常，避免前端500错误
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public void forceDelistHomestay(Long id, String reason, String notes, String violationType) {
        log.info("[HomestayService] 强制下架房源，ID: {}, 原因: {}, 违规类型: {}", id, reason, violationType);
        
        Homestay homestay = homestayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + id));
        
        // 获取旧状态
        HomestayStatus oldStatus = homestay.getStatus();
        
        // 更新房源状态为已下架
        homestay.setStatus(HomestayStatus.INACTIVE);
        homestay.setUpdatedAt(LocalDateTime.now());
        homestayRepository.save(homestay);
        
        // 获取当前管理员信息
        User reviewer = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            Optional<User> admin = userRepository.findByUsername(auth.getName());
            if (admin.isPresent()) {
                reviewer = admin.get();
            }
        }
        
        // 如果reviewer为null，创建一个临时用户或跳过审核日志
        if (reviewer == null) {
            log.warn("[HomestayService] 无法获取管理员信息，跳过审核日志记录");
            return;
        }
        
        // 记录审核日志 - 使用DEACTIVATE作为操作类型
        HomestayAuditLog auditLog = HomestayAuditLog.builder()
                .homestay(homestay)
                .reviewer(reviewer)
                .oldStatus(oldStatus)
                .newStatus(HomestayStatus.INACTIVE)
                .actionType(HomestayAuditLog.AuditActionType.DEACTIVATE)
                .reviewReason(reason)
                .reviewNotes(notes != null ? notes : "违规类型: " + violationType)
                .build();
        
        homestayAuditLogRepository.save(auditLog);
        
        log.info("[HomestayService] 房源已强制下架，ID: {}, 标题: {}", id, homestay.getTitle());
    }
}
