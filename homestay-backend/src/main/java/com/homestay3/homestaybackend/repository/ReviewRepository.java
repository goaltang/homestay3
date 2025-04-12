package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.model.Homestay;
import com.homestay3.homestaybackend.model.Review;
import com.homestay3.homestaybackend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {
    
    // 查找指定房源的评论
    Page<Review> findByHomestay(Homestay homestay, Pageable pageable);
    
    // 查找指定用户的评论
    Page<Review> findByUser(User user, Pageable pageable);
    
    // 查找指定房东的房源评论
    @Query("SELECT r FROM Review r JOIN r.homestay h WHERE h.owner = :owner")
    Page<Review> findByHomestayOwner(@Param("owner") User owner, Pageable pageable);
    
    // 查找指定房源中未回复的评论
    @Query("SELECT r FROM Review r WHERE r.homestay = :homestay AND r.response IS NULL")
    Page<Review> findUnrepliedByHomestay(@Param("homestay") Homestay homestay, Pageable pageable);
    
    // 查找指定房东名下所有房源中未回复的评论
    @Query("SELECT r FROM Review r JOIN r.homestay h WHERE h.owner = :owner AND r.response IS NULL")
    Page<Review> findUnrepliedByHomestayOwner(@Param("owner") User owner, Pageable pageable);
    
    // 计算指定房源的平均评分
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.homestay = :homestay")
    Double calculateAverageRatingByHomestay(@Param("homestay") Homestay homestay);
    
    // 根据用户ID查找评价
    Page<Review> findByUser_Id(Long userId, Pageable pageable);
    
    // 根据房源ID查找评价
    Page<Review> findByHomestay_IdAndIsPublicTrue(Long homestayId, Pageable pageable);
    
    // 根据房源ID和排序条件查找评价
    @Query("SELECT r FROM Review r WHERE r.homestay.id = :homestayId AND r.isPublic = true ORDER BY r.createTime DESC")
    Page<Review> findLatestReviewsByHomestayId(@Param("homestayId") Long homestayId, Pageable pageable);
    
    // 查询房源平均评分
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.homestay.id = :homestayId AND r.isPublic = true")
    Double getAverageRatingByHomestayId(@Param("homestayId") Long homestayId);
    
    // 查询房源评价数量
    @Query("SELECT COUNT(r) FROM Review r WHERE r.homestay.id = :homestayId AND r.isPublic = true")
    Long getReviewCountByHomestayId(@Param("homestayId") Long homestayId);
    
    // 查询房源各项平均评分
    @Query("SELECT AVG(r.cleanlinessRating) as cleanlinessRating, " +
           "AVG(r.accuracyRating) as accuracyRating, " +
           "AVG(r.communicationRating) as communicationRating, " +
           "AVG(r.locationRating) as locationRating, " +
           "AVG(r.checkInRating) as checkInRating, " +
           "AVG(r.valueRating) as valueRating " +
           "FROM Review r WHERE r.homestay.id = :homestayId AND r.isPublic = true")
    List<Object[]> getDetailedRatingsByHomestayId(@Param("homestayId") Long homestayId);
    
    // 获取房东的所有评价
    @Query("SELECT r FROM Review r JOIN r.homestay h WHERE h.owner.id = :hostId")
    Page<Review> findByHostId(@Param("hostId") Long hostId, Pageable pageable);
    
    // 统计房东的评价总数
    @Query("SELECT COUNT(r) FROM Review r JOIN r.homestay h WHERE h.owner.id = :hostId")
    Long countByHostId(@Param("hostId") Long hostId);
    
    // 获取房东的平均评分
    @Query("SELECT AVG(r.rating) FROM Review r JOIN r.homestay h WHERE h.owner.id = :hostId AND r.isPublic = true")
    Double getAverageRatingByHostId(@Param("hostId") Long hostId);
    
    // 获取评价系统的平均评分（管理员）
    @Query("SELECT AVG(r.rating) FROM Review r")
    Double getAverageRating();
    
    // 评分大于等于指定值的评价数量（管理员）
    Long countByRatingGreaterThanEqual(Integer rating);
    
    // 指定评分的评价数量（管理员）
    Long countByRating(Integer rating);
    
    // 评分小于等于指定值的评价数量（管理员）
    Long countByRatingLessThanEqual(Integer rating);
    
    // 已回复的评价数量（管理员）
    Long countByResponseIsNotNull();
    
    // 未回复的评价数量（管理员）
    Long countByResponseIsNull();
} 