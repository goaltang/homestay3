package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.HomestayAdminDetailDTO;
import com.homestay3.homestaybackend.dto.HomestayAdminSummaryDTO;
import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.HomestayAuditLog;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.HomestayStatus;
import com.homestay3.homestaybackend.repository.HomestayAuditLogRepository;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.HomestayAdminService;
import com.homestay3.homestaybackend.service.HomestayCommandService;
import com.homestay3.homestaybackend.service.search.HomestayIndexingService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomestayAdminServiceImpl implements HomestayAdminService {

    private final HomestayRepository homestayRepository;
    private final UserRepository userRepository;
    private final HomestayAuditLogRepository homestayAuditLogRepository;
    private final HomestayDtoAssembler homestayDtoAssembler;
    private final HomestayMutationSupport homestayMutationSupport;
    private final HomestaySpecificationSupport homestaySpecificationSupport;
    private final HomestayCommandService homestayCommandService;
    private final ObjectProvider<HomestayIndexingService> homestayIndexingServiceProvider;

    @Override
    public Page<HomestayDTO> getAdminHomestays(Pageable pageable, String title, String status, String type) {
        Page<Homestay> homestaysPage = findAdminHomestays(pageable, title, status, type, null, null, null, null);
        return homestayDtoAssembler.toDTOPage(homestaysPage, null);
    }

    @Override
    public Page<HomestayAdminSummaryDTO> getAdminHomestaySummaries(
            Pageable pageable,
            String title,
            String status,
            String type,
            String provinceCode,
            String cityCode,
            Integer minPrice,
            Integer maxPrice) {
        Page<Homestay> homestaysPage = findAdminHomestays(pageable, title, status, type, provinceCode, cityCode, minPrice, maxPrice);
        return homestayDtoAssembler.toAdminSummaryDTOPage(homestaysPage, null);
    }

    private Page<Homestay> findAdminHomestays(Pageable pageable, String title, String status, String type,
                                               String provinceCode, String cityCode, Integer minPrice, Integer maxPrice) {
        Specification<Homestay> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(title)) {
                predicates.add(criteriaBuilder.like(root.get("title"), "%" + title + "%"));
            }

            if (StringUtils.hasText(status)) {
                try {
                    HomestayStatus statusEnum = HomestayStatus.valueOf(status);
                    predicates.add(criteriaBuilder.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException exception) {
                    log.warn("Ignore invalid homestay status filter {}", status);
                }
            }

            if (StringUtils.hasText(type)) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }

            if (StringUtils.hasText(provinceCode)) {
                predicates.add(root.get("provinceCode")
                        .in(homestaySpecificationSupport.getProvinceCodeCandidates(provinceCode)));
            }

            if (StringUtils.hasText(cityCode)) {
                predicates.add(root.get("cityCode")
                        .in(homestaySpecificationSupport.getCityCodeCandidates(cityCode)));
            }

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), BigDecimal.valueOf(minPrice)));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), BigDecimal.valueOf(maxPrice)));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Homestay> homestaysPage = homestayRepository.findAll(
                homestaySpecificationSupport.withOwnerFetch(specification),
                pageable);
        return homestaysPage;
    }

    @Override
    public HomestayDTO createHomestay(HomestayDTO homestayDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return homestayCommandService.createHomestay(homestayDTO, username);
    }

    @Override
    @Transactional
    @CacheEvict(value = "homestayList", allEntries = true)
    public void updateHomestayStatus(Long id, String status) {
        Homestay homestay = homestayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + id));

        homestayMutationSupport.validateHomestayStatus(status);
        HomestayStatus newStatus = HomestayStatus.valueOf(status);
        homestay.setStatus(newStatus);
        homestay.setUpdatedAt(LocalDateTime.now());
        homestayRepository.save(homestay);

        // 同步 ES 索引
        syncHomestayToElasticsearch(homestay, newStatus);
    }

    @Override
    @Transactional
    public int batchPopulateCoordinates(int batchSize) {
        List<Homestay> missing = homestayRepository.findByLatitudeIsNullOrLongitudeIsNull();
        if (missing.isEmpty()) {
            return 0;
        }

        int limit = batchSize > 0 ? Math.min(missing.size(), batchSize) : missing.size();
        int successCount = 0;

        for (int index = 0; index < limit; index++) {
            Homestay homestay = missing.get(index);
            try {
                homestayMutationSupport.populateCoordinatesIfMissing(homestay);
                if (homestayMutationSupport.hasCompleteCoordinates(homestay)) {
                    successCount++;
                }
            } catch (Exception exception) {
                log.warn("Failed to populate coordinates for homestay {}", homestay.getId(), exception);
            }
        }

        return successCount;
    }

    @Override
    public HomestayDTO getHomestayWithOwnerDetails(Long id) {
        Homestay homestay = homestayRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + id));

        HomestayDTO dto = homestayDtoAssembler.toDTO(homestay, null);
        if (homestay.getOwner() != null) {
            User owner = homestay.getOwner();
            dto.setOwnerId(owner.getId());
            dto.setOwnerUsername(owner.getUsername());
            dto.setOwnerName(owner.getRealName() != null
                    ? owner.getRealName()
                    : (owner.getNickname() != null ? owner.getNickname() : owner.getUsername()));
            dto.setOwnerPhone(owner.getPhone());
            dto.setOwnerEmail(owner.getEmail());
            dto.setOwnerRealName(owner.getRealName());
            dto.setOwnerNickname(owner.getNickname());
            dto.setOwnerOccupation(owner.getOccupation());
            dto.setOwnerIntroduction(owner.getIntroduction());
            dto.setOwnerJoinDate(owner.getCreatedAt());
            dto.setOwnerHostSince(owner.getHostSince());
            dto.setOwnerHostRating(owner.getHostRating());
            dto.setOwnerHomestayCount(homestayRepository.countByOwnerId(owner.getId()));
            if (owner.getHostRating() != null) {
                dto.setOwnerRating(owner.getHostRating());
            }
        }

        return dto;
    }

    @Override
    public HomestayAdminDetailDTO getHomestayAdminDetailWithOwner(Long id) {
        return homestayDtoAssembler.toAdminDetailDTO(getHomestayWithOwnerDetails(id));
    }

    @Override
    @Transactional
    @CacheEvict(value = {"homestayDetails", "homestayList"}, allEntries = true)
    public void forceDelistHomestay(Long id, String reason, String notes, String violationType) {
        Homestay homestay = homestayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在，ID: " + id));

        HomestayStatus oldStatus = homestay.getStatus();
        homestay.setStatus(HomestayStatus.INACTIVE);
        homestay.setUpdatedAt(LocalDateTime.now());
        homestayRepository.save(homestay);

        // 强制下架后从 ES 删除
        deleteHomestayFromElasticsearch(homestay.getId());

        User reviewer = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            Optional<User> admin = userRepository.findByUsername(authentication.getName());
            if (admin.isPresent()) {
                reviewer = admin.get();
            }
        }

        if (reviewer == null) {
            log.warn("Skip audit log for force delist because current admin user cannot be resolved");
            return;
        }

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
    }

    private void syncHomestayToElasticsearch(Homestay homestay, HomestayStatus status) {
        if (homestay == null || homestay.getId() == null) {
            return;
        }
        try {
            HomestayIndexingService indexingService = homestayIndexingServiceProvider.getIfAvailable();
            if (indexingService != null) {
                if (status == HomestayStatus.ACTIVE) {
                    indexingService.syncHomestay(homestay.getId());
                    log.debug("Synced homestay {} to ES after admin status update", homestay.getId());
                } else {
                    indexingService.deleteHomestay(homestay.getId());
                    log.debug("Deleted homestay {} from ES after admin status update", homestay.getId());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to sync homestay {} to ES after admin status update: {}", homestay.getId(), e.getMessage());
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
                log.debug("Deleted homestay {} from ES after admin force delist", homestayId);
            }
        } catch (Exception e) {
            log.warn("Failed to delete homestay {} from ES after admin force delist: {}", homestayId, e.getMessage());
        }
    }
}
