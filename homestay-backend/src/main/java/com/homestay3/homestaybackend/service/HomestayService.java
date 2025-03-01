package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.dto.HomestaySearchRequest;
import com.homestay3.homestaybackend.model.Homestay;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomestayService {
    
    private final HomestayRepository homestayRepository;
    
    public List<HomestayDTO> getAllHomestays() {
        return homestayRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<HomestayDTO> getFeaturedHomestays() {
        return homestayRepository.findByFeaturedTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public HomestayDTO getHomestayById(Long id) {
        return homestayRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("民宿不存在"));
    }
    
    public List<HomestayDTO> searchHomestays(HomestaySearchRequest request) {
        log.info("搜索民宿: {}", request);
        
        return homestayRepository.searchHomestays(
                request.getLocation(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getGuestCount(),
                request.getPropertyType()
        ).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<HomestayDTO> getHomestaysByPropertyType(String propertyType) {
        return homestayRepository.findByPropertyType(propertyType).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    private HomestayDTO convertToDTO(Homestay homestay) {
        return HomestayDTO.builder()
                .id(homestay.getId())
                .title(homestay.getTitle())
                .description(homestay.getDescription())
                .location(homestay.getLocation())
                .city(homestay.getCity())
                .country(homestay.getCountry())
                .pricePerNight(homestay.getPricePerNight())
                .maxGuests(homestay.getMaxGuests())
                .bedrooms(homestay.getBedrooms())
                .beds(homestay.getBeds())
                .bathrooms(homestay.getBathrooms())
                .amenities(homestay.getAmenities())
                .images(homestay.getImages())
                .rating(homestay.getRating())
                .reviewCount(homestay.getReviewCount())
                .latitude(homestay.getLatitude())
                .longitude(homestay.getLongitude())
                .hostName(homestay.getHostName())
                .hostId(homestay.getHost() != null ? homestay.getHost().getId() : null)
                .featured(homestay.isFeatured())
                .propertyType(homestay.getPropertyType())
                .distanceFromCenter(homestay.getDistanceFromCenter())
                .build();
    }
} 