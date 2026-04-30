package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.BannerDTO;
import com.homestay3.homestaybackend.dto.HomeStatsDTO;
import com.homestay3.homestaybackend.entity.Banner;
import com.homestay3.homestaybackend.model.HomestayStatus;
import com.homestay3.homestaybackend.repository.BannerRepository;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.ReviewRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final HomestayRepository homestayRepository;
    private final ReviewRepository reviewRepository;
    private final BannerRepository bannerRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public HomeStatsDTO getStats() {
        Long homestayCount = homestayRepository.countByStatus(HomestayStatus.ACTIVE);
        Long cityCount = homestayRepository.countDistinctCityTextByStatus(HomestayStatus.ACTIVE);

        Long totalReviews = reviewRepository.countPublicReviews();
        Long positiveReviews = reviewRepository.countPublicPositiveReviews(4);

        Double positiveRate = 0.0;
        if (totalReviews != null && totalReviews > 0 && positiveReviews != null) {
            positiveRate = Math.round((positiveReviews.doubleValue() / totalReviews.doubleValue()) * 1000) / 10.0;
        }

        Long totalUsers = userRepository.count();
        Long totalOrders = orderRepository.count();

        return HomeStatsDTO.builder()
                .homestayCount(homestayCount != null ? homestayCount : 0L)
                .cityCount(cityCount != null ? cityCount : 0L)
                .positiveRate(positiveRate)
                .totalUsers(totalUsers != null ? totalUsers : 0L)
                .totalOrders(totalOrders != null ? totalOrders : 0L)
                .build();
    }

    public List<BannerDTO> getActiveBanners() {
        return bannerRepository.findByEnabledTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toBannerDTO)
                .collect(Collectors.toList());
    }

    // ========== Banner 管理接口 ==========

    public List<BannerDTO> getAllBanners() {
        return bannerRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder"))
                .stream()
                .map(this::toBannerDTO)
                .collect(Collectors.toList());
    }

    public Page<BannerDTO> getBannersPage(String keyword, Pageable pageable) {
        Page<Banner> page;
        if (keyword != null && !keyword.trim().isEmpty()) {
            page = bannerRepository.findByTitleContainingIgnoreCase(keyword.trim(), pageable);
        } else {
            page = bannerRepository.findAll(pageable);
        }
        List<BannerDTO> content = page.getContent().stream()
                .map(this::toBannerDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    public BannerDTO getBannerById(Long id) {
        return bannerRepository.findById(id)
                .map(this::toBannerDTO)
                .orElse(null);
    }

    @Transactional
    public BannerDTO createBanner(Banner banner) {
        if (banner.getSortOrder() == null) {
            banner.setSortOrder(0);
        }
        if (banner.getEnabled() == null) {
            banner.setEnabled(true);
        }
        Banner saved = bannerRepository.save(banner);
        return toBannerDTO(saved);
    }

    @Transactional
    public BannerDTO updateBanner(Long id, Banner update) {
        Banner existing = bannerRepository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }
        if (update.getTitle() != null) existing.setTitle(update.getTitle());
        if (update.getSubtitle() != null) existing.setSubtitle(update.getSubtitle());
        if (update.getImageUrl() != null) existing.setImageUrl(update.getImageUrl());
        if (update.getLinkUrl() != null) existing.setLinkUrl(update.getLinkUrl());
        if (update.getBgGradient() != null) existing.setBgGradient(update.getBgGradient());
        if (update.getSortOrder() != null) existing.setSortOrder(update.getSortOrder());
        if (update.getEnabled() != null) existing.setEnabled(update.getEnabled());
        Banner saved = bannerRepository.save(existing);
        return toBannerDTO(saved);
    }

    @Transactional
    public void deleteBanner(Long id) {
        bannerRepository.deleteById(id);
    }

    @Transactional
    public BannerDTO toggleBannerEnabled(Long id) {
        Banner existing = bannerRepository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }
        existing.setEnabled(!Boolean.TRUE.equals(existing.getEnabled()));
        Banner saved = bannerRepository.save(existing);
        return toBannerDTO(saved);
    }

    private BannerDTO toBannerDTO(Banner banner) {
        if (banner == null) {
            return null;
        }
        return BannerDTO.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .subtitle(banner.getSubtitle())
                .imageUrl(banner.getImageUrl())
                .linkUrl(banner.getLinkUrl())
                .bgGradient(banner.getBgGradient())
                .sortOrder(banner.getSortOrder())
                .enabled(banner.getEnabled())
                .createdAt(banner.getCreatedAt())
                .updatedAt(banner.getUpdatedAt())
                .build();
    }
}
