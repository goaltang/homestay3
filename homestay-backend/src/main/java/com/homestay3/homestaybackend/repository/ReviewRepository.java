package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.model.Homestay;
import com.homestay3.homestaybackend.model.Review;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
    
    // 统计房东某评分以上的评价数量
    @Query("SELECT COUNT(r) FROM Review r JOIN r.homestay h WHERE h.owner.id = :hostId AND r.rating >= :rating")
    Long countByHostIdAndRatingGreaterThanEqual(@Param("hostId") Long hostId, @Param("rating") Integer rating);

    // 统计房东某评分的评价数量
    @Query("SELECT COUNT(r) FROM Review r JOIN r.homestay h WHERE h.owner.id = :hostId AND r.rating = :rating")
    Long countByHostIdAndRating(@Param("hostId") Long hostId, @Param("rating") Integer rating);

    // 统计房东某评分以下的评价数量
    @Query("SELECT COUNT(r) FROM Review r JOIN r.homestay h WHERE h.owner.id = :hostId AND r.rating <= :rating")
    Long countByHostIdAndRatingLessThanEqual(@Param("hostId") Long hostId, @Param("rating") Integer rating);

    // 统计房东已回复的评价数量
    @Query("SELECT COUNT(r) FROM Review r JOIN r.homestay h WHERE h.owner.id = :hostId AND r.response IS NOT NULL")
    Long countByHostIdAndResponseIsNotNull(@Param("hostId") Long hostId);
    
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

    // 检查指定订单是否已存在评论
    boolean existsByOrder(Order order);
    
    // 根据订单查找评价（一个订单通常只有一个评价）
    Optional<Review> findByOrder(Order order);

    // 根据评价ID查找关联的房源标题
    @Query("SELECT r.homestay.title FROM Review r WHERE r.id = :reviewId")
    Optional<String> findHomestayTitleByReviewId(@Param("reviewId") Long reviewId);
} 