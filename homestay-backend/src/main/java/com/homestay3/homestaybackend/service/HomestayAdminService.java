package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.HomestayAdminDetailDTO;
import com.homestay3.homestaybackend.dto.HomestayAdminSummaryDTO;
import com.homestay3.homestaybackend.dto.HomestayDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HomestayAdminService {

    Page<HomestayDTO> getAdminHomestays(Pageable pageable, String title, String status, String type);

    Page<HomestayAdminSummaryDTO> getAdminHomestaySummaries(
            Pageable pageable,
            String title,
            String status,
            String type,
            String provinceCode,
            String cityCode,
            Integer minPrice,
            Integer maxPrice);

    HomestayDTO createHomestay(HomestayDTO homestayDTO);

    void updateHomestayStatus(Long id, String status);

    int batchPopulateCoordinates(int batchSize);

    HomestayDTO getHomestayWithOwnerDetails(Long id);

    HomestayAdminDetailDTO getHomestayAdminDetailWithOwner(Long id);

    void forceDelistHomestay(Long id, String reason, String notes, String violationType);
}
