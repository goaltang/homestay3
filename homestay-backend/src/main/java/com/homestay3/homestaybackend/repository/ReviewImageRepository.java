package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    List<ReviewImage> findByReviewIdOrderBySortOrderAsc(Long reviewId);

    void deleteByReviewId(Long reviewId);

    @Query("SELECT ri.imageUrl FROM ReviewImage ri WHERE ri.review.id = :reviewId ORDER BY ri.sortOrder ASC")
    List<String> findImageUrlsByReviewId(@Param("reviewId") Long reviewId);
}
