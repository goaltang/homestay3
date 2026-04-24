package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.AmenityDTO;
import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.entity.Amenity;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.model.HomestayStatus;
import com.homestay3.homestaybackend.repository.AmenityRepository;
import com.homestay3.homestaybackend.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class HomestayMutationSupport {

    private final AmenityRepository amenityRepository;
    private final GeocodingService geocodingService;

    public void populateCoordinatesIfMissing(Homestay homestay) {
        if (homestay == null || hasCompleteCoordinates(homestay)) {
            return;
        }

        geocodingService.geocode(
                        homestay.getProvinceCode(),
                        homestay.getCityCode(),
                        homestay.getDistrictCode(),
                        homestay.getAddressDetail())
                .ifPresent(coordinates -> {
                    homestay.setLatitude(coordinates.latitude());
                    homestay.setLongitude(coordinates.longitude());
                    log.info(
                            "Auto populated coordinates for homestay title={}, latitude={}, longitude={}",
                            homestay.getTitle(),
                            coordinates.latitude(),
                            coordinates.longitude());
                });
    }

    public boolean hasCompleteCoordinates(Homestay homestay) {
        return homestay.getLatitude() != null && homestay.getLongitude() != null;
    }

    public boolean hasExplicitCoordinates(HomestayDTO homestayDTO) {
        return homestayDTO.getLatitude() != null && homestayDTO.getLongitude() != null;
    }

    public boolean hasAddressChanged(Homestay homestay, HomestayDTO homestayDTO) {
        return !Objects.equals(normalizeAddressPart(homestay.getProvinceCode()), normalizeAddressPart(homestayDTO.getProvinceCode()))
                || !Objects.equals(normalizeAddressPart(homestay.getCityCode()), normalizeAddressPart(homestayDTO.getCityCode()))
                || !Objects.equals(normalizeAddressPart(homestay.getDistrictCode()), normalizeAddressPart(homestayDTO.getDistrictCode()))
                || !Objects.equals(normalizeAddressPart(homestay.getAddressDetail()), normalizeAddressPart(homestayDTO.getAddressDetail()));
    }

    public Set<Amenity> resolveAmenities(Collection<AmenityDTO> amenityDTOs) {
        Set<Amenity> amenities = new HashSet<>();
        if (amenityDTOs == null || amenityDTOs.isEmpty()) {
            return amenities;
        }

        for (AmenityDTO amenityDTO : amenityDTOs) {
            if (amenityDTO == null || !StringUtils.hasText(amenityDTO.getValue())) {
                continue;
            }

            try {
                Amenity amenity = amenityRepository.findById(amenityDTO.getValue()).orElse(null);
                if (amenity != null) {
                    amenities.add(amenity);
                } else {
                    log.warn("Amenity not found: {}", amenityDTO.getValue());
                }
            } catch (Exception exception) {
                log.error("Failed to resolve amenity {}", amenityDTO.getValue(), exception);
            }
        }

        return amenities;
    }

    public void updateHomestayAmenities(Homestay homestay, HomestayDTO homestayDTO) {
        if (homestayDTO.getAmenities() == null) {
            log.info("No amenities provided, keep current amenities for homestay {}", homestay.getId());
            return;
        }

        try {
            Set<Amenity> amenities = resolveAmenities(homestayDTO.getAmenities());
            if (homestay.getAmenities() == null) {
                homestay.setAmenities(new HashSet<>());
            } else {
                homestay.getAmenities().clear();
            }
            homestay.getAmenities().addAll(amenities);
            log.info("Updated {} amenities for homestay {}", amenities.size(), homestay.getId());
        } catch (Exception exception) {
            log.error("Failed to update amenities for homestay {}", homestay.getId(), exception);
        }
    }

    public boolean isAdminUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || authentication.getAuthorities() == null) {
                return false;
            }

            return authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        } catch (Exception exception) {
            log.error("Failed to inspect admin authority", exception);
            return false;
        }
    }

    public boolean isHomestayReadyForReview(Homestay homestay) {
        if (homestay == null) {
            return false;
        }

        if (!StringUtils.hasText(homestay.getTitle()) || "未命名房源".equals(homestay.getTitle())) {
            return false;
        }
        if (homestay.getPrice() == null || homestay.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (!StringUtils.hasText(homestay.getAddressDetail())) {
            return false;
        }
        if (!StringUtils.hasText(homestay.getDescription())) {
            return false;
        }
        return StringUtils.hasText(homestay.getCoverImage());
    }

    public String getHomestayReviewReadinessDetails(Homestay homestay) {
        if (homestay == null) {
            return "房源不存在";
        }

        java.util.List<String> missingItems = new ArrayList<>();

        if (!StringUtils.hasText(homestay.getTitle()) || "未命名房源".equals(homestay.getTitle())) {
            missingItems.add("房源标题");
        }
        if (homestay.getPrice() == null || homestay.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            missingItems.add("房源价格");
        }
        if (!StringUtils.hasText(homestay.getAddressDetail())) {
            missingItems.add("详细地址");
        }
        if (!StringUtils.hasText(homestay.getDescription())) {
            missingItems.add("房源描述");
        }
        if (!StringUtils.hasText(homestay.getCoverImage())) {
            missingItems.add("封面图片");
        }

        if (missingItems.isEmpty()) {
            return "房源信息完整，可以提交审核";
        }

        return "缺少必要信息: " + String.join("、", missingItems);
    }

    public void validateHomestayStatus(String status) {
        try {
            HomestayStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("无效的房源状态: " + status);
        }
    }

    public void validateHomestayDTO(HomestayDTO dto) {
        validateHomestayDTO(dto, false);
    }

    public void validateHomestayDTO(HomestayDTO dto, boolean isDraft) {
        if (dto == null) {
            throw new IllegalArgumentException("房源数据不能为空");
        }

        if (isDraft) {
            return;
        }

        if (!StringUtils.hasText(dto.getTitle())) {
            throw new IllegalArgumentException("房源标题不能为空");
        }
        if (!StringUtils.hasText(dto.getType())) {
            throw new IllegalArgumentException("房源类型不能为空");
        }
        if (dto.getPrice() == null) {
            throw new IllegalArgumentException("房源价格不能为空");
        }
        if (!StringUtils.hasText(dto.getAddressDetail())) {
            throw new IllegalArgumentException("详细地址不能为空");
        }
        if (!StringUtils.hasText(dto.getDescription())) {
            throw new IllegalArgumentException("房源描述不能为空");
        }
    }

    private String normalizeAddressPart(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
