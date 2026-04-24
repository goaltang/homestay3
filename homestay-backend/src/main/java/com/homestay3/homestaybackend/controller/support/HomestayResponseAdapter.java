package com.homestay3.homestaybackend.controller.support;

import com.homestay3.homestaybackend.dto.AmenityDTO;
import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.dto.HomestayDetailDTO;
import com.homestay3.homestaybackend.dto.HomestayStatusResponse;
import com.homestay3.homestaybackend.dto.HomestaySummaryDTO;
import com.homestay3.homestaybackend.dto.HomestayWriteResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class HomestayResponseAdapter {

    public List<HomestaySummaryDTO> toSummaryDTOs(Collection<HomestayDTO> source) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }

        return source.stream()
                .map(this::toSummaryDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public HomestaySummaryDTO toSummaryDTO(HomestayDTO dto) {
        if (dto == null) {
            return null;
        }

        return HomestaySummaryDTO.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .type(dto.getType())
                .propertyTypeName(dto.getPropertyTypeName())
                .price(dto.getPrice())
                .status(dto.getStatus())
                .maxGuests(dto.getMaxGuests())
                .provinceText(dto.getProvinceText())
                .cityText(dto.getCityText())
                .districtText(dto.getDistrictText())
                .addressDetail(dto.getAddressDetail())
                .coverImage(dto.getCoverImage())
                .images(copyList(dto.getImages()))
                .ownerId(dto.getOwnerId())
                .ownerUsername(dto.getOwnerUsername())
                .ownerName(dto.getOwnerName())
                .ownerAvatar(dto.getOwnerAvatar())
                .ownerRating(dto.getOwnerRating())
                .featured(dto.getFeatured())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .build();
    }

    public HomestayWriteResponse toWriteResponse(HomestayDTO dto) {
        if (dto == null) {
            return null;
        }

        return HomestayWriteResponse.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .type(dto.getType())
                .propertyTypeName(dto.getPropertyTypeName())
                .price(dto.getPrice())
                .status(dto.getStatus())
                .maxGuests(dto.getMaxGuests())
                .minNights(dto.getMinNights())
                .maxNights(dto.getMaxNights())
                .provinceText(dto.getProvinceText())
                .cityText(dto.getCityText())
                .districtText(dto.getDistrictText())
                .addressDetail(dto.getAddressDetail())
                .provinceCode(dto.getProvinceCode())
                .cityCode(dto.getCityCode())
                .districtCode(dto.getDistrictCode())
                .description(dto.getDescription())
                .coverImage(dto.getCoverImage())
                .images(copyList(dto.getImages()))
                .amenities(copyList(dto.getAmenities()))
                .ownerId(dto.getOwnerId())
                .ownerUsername(dto.getOwnerUsername())
                .ownerName(dto.getOwnerName())
                .ownerAvatar(dto.getOwnerAvatar())
                .featured(dto.getFeatured())
                .autoConfirm(dto.getAutoConfirm())
                .checkInTime(dto.getCheckInTime())
                .checkOutTime(dto.getCheckOutTime())
                .cancelPolicyType(dto.getCancelPolicyType())
                .houseRules(dto.getHouseRules())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .build();
    }

    public HomestayStatusResponse toStatusResponse(HomestayDTO dto) {
        if (dto == null) {
            return null;
        }

        return HomestayStatusResponse.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .status(dto.getStatus())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    public HomestayStatusResponse toStatusResponse(HomestayDetailDTO dto) {
        if (dto == null) {
            return null;
        }

        return HomestayStatusResponse.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .status(dto.getStatus())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    private <T> List<T> copyList(List<T> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }
}
