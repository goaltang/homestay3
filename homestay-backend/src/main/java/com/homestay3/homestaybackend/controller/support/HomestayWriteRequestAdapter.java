package com.homestay3.homestaybackend.controller.support;

import com.homestay3.homestaybackend.dto.AmenityDTO;
import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.dto.HomestayWriteRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class HomestayWriteRequestAdapter {

    public HomestayDTO toDTO(HomestayWriteRequest request) {
        if (request == null) {
            return null;
        }

        HomestayDTO dto = new HomestayDTO();
        dto.setTitle(request.getTitle());
        dto.setType(request.getType());
        dto.setPrice(request.getPrice());
        dto.setStatus(request.getStatus());
        dto.setMaxGuests(request.getMaxGuests());
        dto.setMinNights(request.getMinNights());
        dto.setMaxNights(request.getMaxNights());
        dto.setProvinceText(request.getProvinceText());
        dto.setCityText(request.getCityText());
        dto.setDistrictText(request.getDistrictText());
        dto.setAddressDetail(request.getAddressDetail());
        dto.setProvinceCode(request.getProvinceCode());
        dto.setCityCode(request.getCityCode());
        dto.setDistrictCode(request.getDistrictCode());
        dto.setDescription(request.getDescription());
        dto.setCoverImage(request.getCoverImage());
        dto.setImages(request.getImages() != null ? new ArrayList<>(request.getImages()) : new ArrayList<>());
        dto.setAmenities(normalizeAmenities(request.getAmenities()));
        dto.setOwnerUsername(request.getOwnerUsername());
        dto.setFeatured(request.getFeatured());
        dto.setAutoConfirm(request.getAutoConfirm());
        dto.setCheckInTime(request.getCheckInTime());
        dto.setCheckOutTime(request.getCheckOutTime());
        dto.setCancelPolicyType(request.getCancelPolicyType());
        dto.setHouseRules(request.getHouseRules());
        dto.setLatitude(request.getLatitude());
        dto.setLongitude(request.getLongitude());
        return dto;
    }

    private List<AmenityDTO> normalizeAmenities(List<Object> amenities) {
        List<AmenityDTO> normalized = new ArrayList<>();
        if (amenities == null || amenities.isEmpty()) {
            return normalized;
        }

        for (Object item : amenities) {
            if (item instanceof AmenityDTO amenityDTO) {
                normalized.add(amenityDTO);
                continue;
            }

            if (item instanceof String value) {
                AmenityDTO dto = new AmenityDTO();
                dto.setValue(value);
                dto.setLabel(value);
                normalized.add(dto);
                continue;
            }

            if (item instanceof Map<?, ?> map) {
                AmenityDTO dto = new AmenityDTO();
                Object value = map.get("value");
                Object label = map.get("label");
                Object icon = map.get("icon");
                dto.setValue(value instanceof String ? (String) value : null);
                dto.setLabel(label instanceof String ? (String) label : dto.getValue());
                dto.setIcon(icon instanceof String ? (String) icon : null);
                normalized.add(dto);
            }
        }

        return normalized;
    }
}
