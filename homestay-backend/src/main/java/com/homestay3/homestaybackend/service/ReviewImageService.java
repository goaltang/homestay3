package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.ReviewImageDTO;
import com.homestay3.homestaybackend.entity.Review;
import com.homestay3.homestaybackend.entity.ReviewImage;
import com.homestay3.homestaybackend.repository.ReviewImageRepository;
import com.homestay3.homestaybackend.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ReviewImageService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewImageService.class);
    private static final int MAX_IMAGES_PER_REVIEW = 9;

    private final ReviewImageRepository reviewImageRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public void saveReviewImages(Long reviewId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        if (imageUrls.size() > MAX_IMAGES_PER_REVIEW) {
            throw new IllegalArgumentException("每条评价最多上传" + MAX_IMAGES_PER_REVIEW + "张图片");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("评价不存在: " + reviewId));

        // 删除旧图片
        reviewImageRepository.deleteByReviewId(reviewId);

        // 保存新图片
        List<ReviewImage> images = IntStream.range(0, imageUrls.size())
                .mapToObj(i -> ReviewImage.builder()
                        .review(review)
                        .imageUrl(imageUrls.get(i))
                        .sortOrder(i)
                        .build())
                .collect(Collectors.toList());

        reviewImageRepository.saveAll(images);
        logger.info("保存评价图片, reviewId: {}, count: {}", reviewId, images.size());
    }

    public List<ReviewImageDTO> getImagesByReviewId(Long reviewId) {
        return reviewImageRepository.findByReviewIdOrderBySortOrderAsc(reviewId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<String> getImageUrlsByReviewId(Long reviewId) {
        return reviewImageRepository.findImageUrlsByReviewId(reviewId);
    }

    @Transactional
    public void deleteImagesByReviewId(Long reviewId) {
        reviewImageRepository.deleteByReviewId(reviewId);
    }

    private ReviewImageDTO toDTO(ReviewImage image) {
        return ReviewImageDTO.builder()
                .id(image.getId())
                .reviewId(image.getReview().getId())
                .imageUrl(image.getImageUrl())
                .sortOrder(image.getSortOrder())
                .build();
    }
}
