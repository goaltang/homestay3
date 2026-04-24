package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.dto.HomestayDetailDTO;
import com.homestay3.homestaybackend.dto.HomestaySummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface HomestayQueryService {

    List<HomestayDTO> getAllHomestays();

    List<HomestaySummaryDTO> getAllHomestaySummaries();

    @Deprecated
    List<HomestayDTO> getFeaturedHomestays();

    HomestayDTO getHomestayById(Long id, List<String> referringSearchCriteria);

    HomestayDetailDTO getHomestayDetailById(Long id, List<String> referringSearchCriteria);

    List<HomestayDTO> getHomestaysByPropertyType(String propertyType);

    List<HomestaySummaryDTO> getHomestaySummariesByPropertyType(String propertyType);

    Page<HomestayDTO> getHomestaysByPage(Pageable pageable);

    Page<HomestaySummaryDTO> getHomestaySummaryPage(Pageable pageable);

    List<HomestayDTO> getHomestaysByOwner(String username);

    List<HomestaySummaryDTO> getHomestaySummariesByOwner(String username);

    boolean checkHomestayReadyForReview(Long homestayId);

    String getHomestayReviewReadinessDetails(Long homestayId);

    List<LocalDate> getUnavailableDates(Long homestayId);
}
