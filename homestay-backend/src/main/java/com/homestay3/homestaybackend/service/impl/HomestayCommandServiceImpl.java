package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.entity.Amenity;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.HomestayStatus;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.AmenityService;
import com.homestay3.homestaybackend.service.HomestayCommandService;
import com.homestay3.homestaybackend.service.search.HomestayIndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomestayCommandServiceImpl implements HomestayCommandService {

    private final HomestayRepository homestayRepository;
    private final UserRepository userRepository;
    private final AmenityService amenityService;
    private final HomestayDtoAssembler homestayDtoAssembler;
    private final HomestayMutationSupport homestayMutationSupport;
    private final ObjectProvider<HomestayIndexingService> homestayIndexingServiceProvider;

    @Override
    @Transactional
    public String uploadHomestayImage(MultipartFile file) {
        try {
            String uploadDir = "uploads/homestays";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = System.currentTimeMillis() + "_" + (int) (Math.random() * 1000) + extension;

            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            return "/uploads/homestays/" + filename;
        } catch (IOException exception) {
            throw new RuntimeException("Failed to upload homestay image", exception);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "homestayList", allEntries = true)
    public HomestayDTO createHomestay(HomestayDTO homestayDTO, String username) {
        boolean isDraft = homestayDTO.getStatus() != null
                && "DRAFT".equalsIgnoreCase(homestayDTO.getStatus());
        homestayMutationSupport.validateHomestayDTO(homestayDTO, isDraft);

        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + username));

        HomestayStatus initialStatus = HomestayStatus.DRAFT;
        if (homestayDTO.getStatus() != null && !homestayDTO.getStatus().isEmpty()) {
            try {
                initialStatus = HomestayStatus.valueOf(homestayDTO.getStatus().toUpperCase());
            } catch (IllegalArgumentException exception) {
                log.warn("Invalid homestay status {}, fallback to {}", homestayDTO.getStatus(), initialStatus);
            }
        }

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
            if (title == null || title.isEmpty()) {
                title = "未命名房源";
            }
            if (type == null || type.isEmpty()) {
                type = "ENTIRE";
            }
            try {
                price = homestayDTO.getPrice() != null ? new BigDecimal(homestayDTO.getPrice()) : BigDecimal.ZERO;
            } catch (NumberFormatException exception) {
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
        } else {
            price = new BigDecimal(homestayDTO.getPrice());
        }

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

        homestayMutationSupport.populateCoordinatesIfMissing(homestay);

        if (homestayDTO.getImages() != null && !homestayDTO.getImages().isEmpty()) {
            homestay.setImages(new ArrayList<>());
            homestayDTO.getImages().stream()
                    .filter(image -> image != null && !image.trim().isEmpty())
                    .forEach(image -> homestay.getImages().add(image));
        }

        Homestay savedHomestay = homestayRepository.save(homestay);

        if (homestayDTO.getAmenities() != null && !homestayDTO.getAmenities().isEmpty()) {
            Set<Amenity> amenities = homestayMutationSupport.resolveAmenities(homestayDTO.getAmenities());
            if (!amenities.isEmpty()) {
                savedHomestay.setAmenities(new HashSet<>(amenities));
                savedHomestay = homestayRepository.save(savedHomestay);
                for (Amenity amenity : amenities) {
                    try {
                        amenityService.updateAmenityUsageCount(amenity.getValue(), true);
                    } catch (Exception exception) {
                        log.warn("Failed to update amenity usage count for {}", amenity.getValue(), exception);
                    }
                }
            }
        }

        syncHomestayToElasticsearch(savedHomestay);
        return homestayDtoAssembler.toDTO(savedHomestay, null);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "homestayDetails", key = "#id"),
            @CacheEvict(value = "homestayList", allEntries = true)
    })
    public HomestayDTO updateHomestay(Long id, HomestayDTO homestayDTO) {
        if (homestayDTO == null) {
            throw new IllegalArgumentException("房源数据不能为空");
        }

        Homestay homestay = homestayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("认证失败，无法获取当前用户信息");
        }

        String currentUsername = authentication.getName();
        boolean isAdmin = hasAdminAuthority(authentication);
        boolean isOwner = homestay.getOwner() != null
                && homestay.getOwner().getUsername().equals(currentUsername);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("您没有权限更新此房源");
        }

        try {
            boolean addressChanged = homestayMutationSupport.hasAddressChanged(homestay, homestayDTO);

            homestay.setTitle(homestayDTO.getTitle());
            homestay.setType(homestayDTO.getType());
            try {
                if (homestayDTO.getPrice() != null) {
                    homestay.setPrice(new BigDecimal(homestayDTO.getPrice()));
                }
            } catch (NumberFormatException exception) {
                log.warn("Invalid price {}, keep original value", homestayDTO.getPrice());
            }

            homestay.setMaxGuests(homestayDTO.getMaxGuests());
            homestay.setMinNights(homestayDTO.getMinNights());
            homestay.setMaxNights(homestayDTO.getMaxNights());
            homestay.setProvinceCode(homestayDTO.getProvinceCode());
            homestay.setCityCode(homestayDTO.getCityCode());
            homestay.setDistrictCode(homestayDTO.getDistrictCode());
            homestay.setAddressDetail(homestayDTO.getAddressDetail());

            if (homestayMutationSupport.hasExplicitCoordinates(homestayDTO)) {
                homestay.setLatitude(BigDecimal.valueOf(homestayDTO.getLatitude()));
                homestay.setLongitude(BigDecimal.valueOf(homestayDTO.getLongitude()));
            } else {
                if (addressChanged) {
                    homestay.setLatitude(null);
                    homestay.setLongitude(null);
                }
                homestayMutationSupport.populateCoordinatesIfMissing(homestay);
            }

            homestay.setDescription(homestayDTO.getDescription());

            if (homestayDTO.getCoverImage() != null) {
                homestay.setCoverImage(homestayDTO.getCoverImage());
            }

            if (homestayDTO.getImages() != null) {
                if (homestay.getImages() == null) {
                    homestay.setImages(new ArrayList<>());
                } else {
                    homestay.getImages().clear();
                }

                homestayDTO.getImages().stream()
                        .filter(image -> image != null && !image.trim().isEmpty())
                        .forEach(image -> homestay.getImages().add(image));
            }

            homestayMutationSupport.updateHomestayAmenities(homestay, homestayDTO);

            if (homestayDTO.getStatus() != null) {
                homestay.setStatus(HomestayStatus.valueOf(homestayDTO.getStatus()));
            }
            if (homestayDTO.getFeatured() != null) {
                homestay.setFeatured(homestayDTO.getFeatured());
            }

            Homestay updatedHomestay = homestayRepository.save(homestay);
            syncHomestayToElasticsearch(updatedHomestay);
            return homestayDtoAssembler.toDTO(updatedHomestay, null);
        } catch (Exception exception) {
            log.error("Failed to update homestay {}", id, exception);
            throw new RuntimeException("更新房源失败: " + exception.getMessage(), exception);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"homestayDetails", "homestayList"}, allEntries = true)
    public void deleteHomestay(Long id) {
        Homestay homestay = homestayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID为 " + id + " 的房源"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("未授权操作");
        }

        String currentUsername = authentication.getName();
        boolean isAdmin = hasAdminAuthority(authentication);
        boolean isOwner = homestay.getOwner() != null
                && currentUsername.equals(homestay.getOwner().getUsername());

        if (!isAdmin && !isOwner) {
            throw new RuntimeException("Access Denied");
        }

        try {
            if (homestay.getAmenities() != null && !homestay.getAmenities().isEmpty()) {
                homestay.getAmenities().clear();
                homestayRepository.save(homestay);
            }

            deleteHomestayFromElasticsearch(homestay.getId());
            homestayRepository.delete(homestay);
        } catch (Exception exception) {
            log.error("Failed to delete homestay {}", id, exception);
            try {
                homestay.setStatus(HomestayStatus.INACTIVE);
                homestayRepository.save(homestay);
            } catch (Exception nestedException) {
                log.error("Failed to mark homestay {} inactive after delete failure", id, nestedException);
                throw new RuntimeException("删除房源失败: " + exception.getMessage());
            }
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "homestayList", allEntries = true)
    public HomestayDTO updateHomestayStatus(Long id, String status, String ownerUsername) {
        Homestay homestay = homestayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在"));

        if (homestay.getOwner() == null || !homestay.getOwner().getUsername().equals(ownerUsername)) {
            throw new IllegalArgumentException("您不是此房源的拥有者，无权更新状态");
        }

        homestayMutationSupport.validateHomestayStatus(status);
        HomestayStatus newStatus = HomestayStatus.valueOf(status);

        // 状态机校验：禁止非法状态转换
        if (!homestay.getStatus().canTransitionTo(newStatus)) {
            throw new IllegalArgumentException(
                    String.format("无法将房源从'%s'状态变更为'%s'状态",
                            homestay.getStatus().getDescription(),
                            newStatus.getDescription()));
        }

        homestay.setStatus(newStatus);
        homestay.setUpdatedAt(LocalDateTime.now());

        Homestay updatedHomestay = homestayRepository.save(homestay);
        if (newStatus == HomestayStatus.ACTIVE) {
            syncHomestayToElasticsearch(updatedHomestay);
        } else {
            deleteHomestayFromElasticsearch(updatedHomestay.getId());
        }
        return homestayDtoAssembler.toDTO(updatedHomestay, null);
    }

    private boolean hasAdminAuthority(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private void syncHomestayToElasticsearch(Homestay homestay) {
        if (homestay == null || homestay.getId() == null) {
            return;
        }
        try {
            HomestayIndexingService indexingService = homestayIndexingServiceProvider.getIfAvailable();
            if (indexingService != null) {
                indexingService.syncHomestay(homestay.getId());
                log.debug("Queued homestay {} for ES sync", homestay.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to queue ES sync for homestay {}: {}", homestay.getId(), e.getMessage());
        }
    }

    private void deleteHomestayFromElasticsearch(Long homestayId) {
        if (homestayId == null) {
            return;
        }
        try {
            HomestayIndexingService indexingService = homestayIndexingServiceProvider.getIfAvailable();
            if (indexingService != null) {
                indexingService.deleteHomestay(homestayId);
                log.debug("Queued homestay {} for ES deletion", homestayId);
            }
        } catch (Exception e) {
            log.warn("Failed to queue ES deletion for homestay {}: {}", homestayId, e.getMessage());
        }
    }
}
