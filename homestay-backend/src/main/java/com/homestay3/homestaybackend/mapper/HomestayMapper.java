package com.homestay3.homestaybackend.mapper;

import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.dto.AmenityDTO;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.Amenity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HomestayMapper {

    public HomestayDTO toDTO(Homestay homestay) {
        if (homestay == null) {
            return null;
        }

        HomestayDTO dto = new HomestayDTO();
        dto.setId(homestay.getId());
        dto.setTitle(homestay.getTitle());
        dto.setDescription(homestay.getDescription());
        dto.setType(homestay.getType());
        dto.setStatus(homestay.getStatus().name());
        dto.setMaxGuests(homestay.getMaxGuests());
        dto.setMinNights(homestay.getMinNights());
        dto.setFeatured(homestay.getFeatured());
        
        // 价格转换：BigDecimal 转 String
        if (homestay.getPrice() != null) {
            dto.setPrice(homestay.getPrice().toString());
        }
        
        // 地址信息
        dto.setProvinceText(homestay.getProvinceText());
        dto.setCityText(homestay.getCityText());
        dto.setDistrictText(homestay.getDistrictText());
        dto.setAddressDetail(homestay.getAddressDetail());
        dto.setProvinceCode(homestay.getProvinceCode());
        dto.setCityCode(homestay.getCityCode());
        dto.setDistrictCode(homestay.getDistrictCode());
        
        // 图片信息
        dto.setCoverImage(homestay.getCoverImage());
        dto.setImages(homestay.getImages());
        
        // 设施转换
        if (homestay.getAmenities() != null && !homestay.getAmenities().isEmpty()) {
            List<AmenityDTO> amenityDTOs = homestay.getAmenities().stream()
                .map(this::amenityToDTO)
                .collect(Collectors.toList());
            dto.setAmenities(amenityDTOs);
        }
        
        // 房东信息
        if (homestay.getOwner() != null) {
            dto.setOwnerId(homestay.getOwner().getId());
            dto.setOwnerUsername(homestay.getOwner().getUsername());
            dto.setOwnerName(homestay.getOwner().getRealName() != null ? 
                           homestay.getOwner().getRealName() : homestay.getOwner().getUsername());
            dto.setOwnerAvatar(homestay.getOwner().getAvatar());
        }
        
        // 时间信息
        dto.setCreatedAt(homestay.getCreatedAt());
        dto.setUpdatedAt(homestay.getUpdatedAt());
        
        return dto;
    }

    public Homestay toEntity(HomestayDTO dto) {
        if (dto == null) {
            return null;
        }

        Homestay homestay = new Homestay();
        homestay.setId(dto.getId());
        homestay.setTitle(dto.getTitle());
        homestay.setDescription(dto.getDescription());
        homestay.setType(dto.getType());
        if (dto.getStatus() != null) {
            try {
                homestay.setStatus(com.homestay3.homestaybackend.model.HomestayStatus.valueOf(dto.getStatus()));
            } catch (IllegalArgumentException e) {
                homestay.setStatus(com.homestay3.homestaybackend.model.HomestayStatus.DRAFT);
            }
        }
        homestay.setMaxGuests(dto.getMaxGuests());
        homestay.setMinNights(dto.getMinNights());
        homestay.setFeatured(dto.getFeatured());
        
        // 价格转换：String 转 BigDecimal
        if (dto.getPrice() != null && !dto.getPrice().isEmpty()) {
            try {
                homestay.setPrice(new BigDecimal(dto.getPrice()));
            } catch (NumberFormatException e) {
                // 如果转换失败，设置默认值或记录错误
                homestay.setPrice(BigDecimal.ZERO);
            }
        }
        
        // 地址信息
        homestay.setProvinceText(dto.getProvinceText());
        homestay.setCityText(dto.getCityText());
        homestay.setDistrictText(dto.getDistrictText());
        homestay.setAddressDetail(dto.getAddressDetail());
        homestay.setProvinceCode(dto.getProvinceCode());
        homestay.setCityCode(dto.getCityCode());
        homestay.setDistrictCode(dto.getDistrictCode());
        
        // 图片信息
        homestay.setCoverImage(dto.getCoverImage());
        homestay.setImages(dto.getImages());
        
        return homestay;
    }

    private AmenityDTO amenityToDTO(Amenity amenity) {
        if (amenity == null) {
            return null;
        }
        
        AmenityDTO dto = new AmenityDTO();
        dto.setValue(amenity.getValue());
        dto.setLabel(amenity.getLabel());
        dto.setIcon(amenity.getIcon());
        dto.setActive(amenity.isActive());
        
        if (amenity.getCategory() != null) {
            dto.setCategoryName(amenity.getCategory().getName());
        }
        
        return dto;
    }
} 