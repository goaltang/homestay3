package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.dto.HomestaySearchRequest;
import com.homestay3.homestaybackend.dto.MapClusterDTO;
import com.homestay3.homestaybackend.service.HomestayAdminService;
import com.homestay3.homestaybackend.service.HomestayCommandService;
import com.homestay3.homestaybackend.service.HomestayQueryService;
import com.homestay3.homestaybackend.service.HomestaySearchService;
import com.homestay3.homestaybackend.service.HomestayService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomestayServiceImpl implements HomestayService {

    private final HomestayQueryService homestayQueryService;
    private final HomestayCommandService homestayCommandService;
    private final HomestaySearchService homestaySearchService;
    private final HomestayAdminService homestayAdminService;

    @Override
    public List<HomestayDTO> getAllHomestays() {
        return homestayQueryService.getAllHomestays();
    }

    @Override
    @Deprecated
    public List<HomestayDTO> getFeaturedHomestays() {
        return homestayQueryService.getFeaturedHomestays();
    }

    @Override
    public HomestayDTO getHomestayById(Long id, List<String> referringSearchCriteria) {
        return homestayQueryService.getHomestayById(id, referringSearchCriteria);
    }

    @Override
    public List<HomestayDTO> getHomestaysByPropertyType(String propertyType) {
        return homestayQueryService.getHomestaysByPropertyType(propertyType);
    }

    @Override
    public List<HomestayDTO> searchHomestays(HomestaySearchRequest request) {
        return homestaySearchService.searchHomestays(request);
    }

    @Override
    public List<MapClusterDTO> getMapClusters(HomestaySearchRequest request) {
        return homestaySearchService.getMapClusters(request);
    }

    @Override
    public List<HomestayDTO> getNearbyHomestays(HomestaySearchRequest request) {
        return homestaySearchService.getNearbyHomestays(request);
    }

    @Override
    public List<HomestayDTO> searchHomestaysNearLandmark(HomestaySearchRequest request) {
        return homestaySearchService.searchHomestaysNearLandmark(request);
    }

    @Override
    public String uploadHomestayImage(MultipartFile file) {
        return homestayCommandService.uploadHomestayImage(file);
    }

    @Override
    public Page<HomestayDTO> getHomestaysByPage(Pageable pageable) {
        return homestayQueryService.getHomestaysByPage(pageable);
    }

    @Override
    public List<HomestayDTO> getHomestaysByOwner(String username) {
        return homestayQueryService.getHomestaysByOwner(username);
    }

    @Override
    public HomestayDTO createHomestay(HomestayDTO homestayDTO, String ownerUsername) {
        return homestayCommandService.createHomestay(homestayDTO, ownerUsername);
    }

    @Override
    public HomestayDTO updateHomestay(Long id, HomestayDTO homestayDTO) {
        return homestayCommandService.updateHomestay(id, homestayDTO);
    }

    @Override
    public List<HomestayDTO> searchHomestays(
            String keyword,
            String provinceCode,
            String cityCode,
            Integer minPrice,
            Integer maxPrice,
            Integer guests,
            String type) {
        return homestaySearchService.searchHomestays(
                keyword,
                provinceCode,
                cityCode,
                minPrice,
                maxPrice,
                guests,
                type);
    }

    @Override
    public void deleteHomestay(Long id) {
        homestayCommandService.deleteHomestay(id);
    }

    @Override
    public HomestayDTO updateHomestayStatus(Long id, String status, String ownerUsername) {
        return homestayCommandService.updateHomestayStatus(id, status, ownerUsername);
    }

    @Override
    public int batchPopulateCoordinates(int batchSize) {
        return homestayAdminService.batchPopulateCoordinates(batchSize);
    }

    @Override
    public Page<HomestayDTO> getAdminHomestays(Pageable pageable, String title, String status, String type) {
        return homestayAdminService.getAdminHomestays(pageable, title, status, type);
    }

    @Override
    public HomestayDTO createHomestay(HomestayDTO homestayDTO) {
        return homestayAdminService.createHomestay(homestayDTO);
    }

    @Override
    public void updateHomestayStatus(Long id, String status) {
        homestayAdminService.updateHomestayStatus(id, status);
    }

    @Override
    public boolean checkHomestayReadyForReview(Long homestayId) {
        return homestayQueryService.checkHomestayReadyForReview(homestayId);
    }

    @Override
    public String getHomestayReviewReadinessDetails(Long homestayId) {
        return homestayQueryService.getHomestayReviewReadinessDetails(homestayId);
    }

    @Override
    public HomestayDTO getHomestayWithOwnerDetails(Long id) {
        return homestayAdminService.getHomestayWithOwnerDetails(id);
    }

    @Override
    public List<LocalDate> getUnavailableDates(Long homestayId) {
        return homestayQueryService.getUnavailableDates(homestayId);
    }

    @Override
    public void forceDelistHomestay(Long id, String reason, String notes, String violationType) {
        homestayAdminService.forceDelistHomestay(id, reason, notes, violationType);
    }
}
