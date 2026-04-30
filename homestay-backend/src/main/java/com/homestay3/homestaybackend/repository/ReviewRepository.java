package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.Review;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    // 查找指定房源的评论
    Page<Review> findByHomestay(Homestay homestay, Pageable pageable);

    // 查找指定用户的评论
    Page<Review> findByUser(User user, Pageable pageable);

    // 查找指定房东的房源评论
    @Query("SELECT r FROM Review r JOIN r.homestay h WHERE h.owner = :owner AND r.deleted = false")
    Page<Review> findByHomestayOwner(@Param("owner") User owner, Pageable pageable);

    // 查找指定房源中未回复的评论（排除已删除）
    @Query("SELECT r FROM Review r WHERE r.homestay = :homestay AND r.response IS NULL AND r.deleted = false")
    Page<Review> findUnrepliedByHomestay(@Param("homestay") Homestay homestay, Pageable pageable);

    // 查找指定房东名下所有房源中未回复的评论（排除已删除）
    @Query("SELECT r FROM Review r JOIN r.homestay h WHERE h.owner = :owner AND r.response IS NULL AND r.deleted = false")
    Page<Review> findUnrepliedByHomestayOwner(@Param("owner") User owner, Pageable pageable);

    // 计算指定房源的平均评分
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.homestay = :homestay AND r.deleted = false")
    Double calculateAverageRatingByHomestay(@Param("homestay") Homestay homestay);

    // 根据用户ID查找评价（排除已删除）
    @Query("SELECT r FROM Review r WHERE r.user.id = :userId AND r.deleted = false")
    Page<Review> findByUser_Id(@Param("userId") Long userId, Pageable pageable);

    // 根据房源ID查找评价（排除已删除和未公开的）
    Page<Review> findByHomestay_IdAndIsPublicTrueAndDeletedFalse(Long homestayId, Pageable pageable);

    // 根据房源ID和排序条件查找评价（排除已删除）
    @Query("SELECT r FROM Review r WHERE r.homestay.id = :homestayId AND r.isPublic = true AND r.deleted = false ORDER BY r.createTime DESC")
    Page<Review> findLatestReviewsByHomestayId(@Param("homestayId") Long homestayId, Pageable pageable);

    // 查询房源平均评分（排除已删除）
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.homestay.id = :homestayId AND r.isPublic = true AND r.deleted = false")
    Double getAverageRatingByHomestayId(@Param("homestayId") Long homestayId);

    // 查询房源评价数量（排除已删除）
    @Query("SELECT COUNT(r) FROM Review r WHERE r.homestay.id = :homestayId AND r.isPublic = true AND r.deleted = false")
    Long getReviewCountByHomestayId(@Param("homestayId") Long homestayId);

    // 查询房源各项平均评分（排除已删除）
    @Query("SELECT AVG(r.cleanlinessRating), AVG(r.accuracyRating), AVG(r.communicationRating), " +
           "AVG(r.locationRating), AVG(r.checkInRating), AVG(r.valueRating) " +
           "FROM Review r WHERE r.homestay.id = :homestayId AND r.isPublic = true AND r.deleted = false")
    List<Object[]> getDetailedRatingsByHomestayId(@Param("homestayId") Long homestayId);

    // 获取房东的所有评价（排除已删除）
    @Query("SELECT r FROM Review r JOIN r.homestay h WHERE h.owner.id = :hostId AND r.deleted = false")
    Page<Review> findByHostId(@Param("hostId") Long hostId, Pageable pageable);

    // 统计房东的评价总数（排除已删除）
    @Query("SELECT COUNT(r) FROM Review r JOIN r.homestay h WHERE h.owner.id = :hostId AND r.deleted = false")
    Long countByHostId(@Param("hostId") Long hostId);

    // 获取房东的平均评分（排除已删除）
    @Query("SELECT AVG(r.rating) FROM Review r JOIN r.homestay h WHERE h.owner.id = :hostId AND r.isPublic = true AND r.deleted = false")
    Double getAverageRatingByHostId(@Param("hostId") Long hostId);

    // 统计房东某评分以上的评价数量（排除已删除）
    @Query("SELECT COUNT(r) FROM Review r JOIN r.homestay h WHERE h.owner.id = :hostId AND r.rating >= :rating AND r.deleted = false")
    Long countByHostIdAndRatingGreaterThanEqual(@Param("hostId") Long hostId, @Param("rating") Integer rating);

    // 统计房东某评分的评价数量（排除已删除）
    @Query("SELECT COUNT(r) FROM Review r JOIN r.homestay h WHERE h.owner.id = :hostId AND r.rating = :rating AND r.deleted = false")
    Long countByHostIdAndRating(@Param("hostId") Long hostId, @Param("rating") Integer rating);

    // 统计房东某评分以下的评价数量（排除已删除）
    @Query("SELECT COUNT(r) FROM Review r JOIN r.homestay h WHERE h.owner.id = :hostId AND r.rating <= :rating AND r.deleted = false")
    Long countByHostIdAndRatingLessThanEqual(@Param("hostId") Long hostId, @Param("rating") Integer rating);

    // 统计房东已回复的评价数量（排除已删除）
    @Query("SELECT COUNT(r) FROM Review r JOIN r.homestay h WHERE h.owner.id = :hostId AND r.response IS NOT NULL AND r.deleted = false")
    Long countByHostIdAndResponseIsNotNull(@Param("hostId") Long hostId);

    // 获取评价系统的平均评分（排除已删除）
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.deleted = false")
    Double getAverageRating();

    // 评分大于等于指定值的评价数量（排除已删除）
    @Query("SELECT COUNT(r) FROM Review r WHERE r.rating >= :rating AND r.deleted = false")
    Long countByRatingGreaterThanEqual(@Param("rating") Integer rating);

    // 指定评分的评价数量（排除已删除）
    @Query("SELECT COUNT(r) FROM Review r WHERE r.rating = :rating AND r.deleted = false")
    Long countByRating(@Param("rating") Integer rating);

    // 评分小于等于指定值的评价数量（排除已删除）
    @Query("SELECT COUNT(r) FROM Review r WHERE r.rating <= :rating AND r.deleted = false")
    Long countByRatingLessThanEqual(@Param("rating") Integer rating);

    // 已回复的评价数量（排除已删除）
    @Query("SELECT COUNT(r) FROM Review r WHERE r.response IS NOT NULL AND r.deleted = false")
    Long countByResponseIsNotNull();

    // 未回复的评价数量（排除已删除）
    @Query("SELECT COUNT(r) FROM Review r WHERE r.response IS NULL AND r.deleted = false")
    Long countByResponseIsNull();

    // 检查指定订单是否已存在评论（排除已删除）
    @Query("SELECT COUNT(r) > 0 FROM Review r WHERE r.order = :order AND r.deleted = false")
    boolean existsByOrder(@Param("order") Order order);

    // 根据订单查找评价（一个订单通常只有一个评价，排除已删除）
    @Query("SELECT r FROM Review r WHERE r.order = :order AND r.deleted = false")
    Optional<Review> findByOrder(@Param("order") Order order);

    // 根据评价ID查找关联的房源标题
    @Query("SELECT r.homestay.title FROM Review r WHERE r.id = :reviewId")
    Optional<String> findHomestayTitleByReviewId(@Param("reviewId") Long reviewId);

    // 批量根据评价ID查找关联的房源标题
    @Query("SELECT r.id, r.homestay.title FROM Review r WHERE r.id IN :reviewIds")
    List<Object[]> findHomestayTitlesByReviewIds(@Param("reviewIds") List<Long> reviewIds);

    // 统计指定房源的评价数量（排除已删除）
    @Query("SELECT COUNT(r) FROM Review r WHERE r.homestay.id = :homestayId AND r.deleted = false")
    Long countByHomestayId(@Param("homestayId") Long homestayId);

    // 统计指定房源在指定时间后的评价数量（排除已删除）
    @Query("SELECT COUNT(r) FROM Review r WHERE r.homestay.id = :homestayId AND r.createTime > :createdAtAfter AND r.deleted = false")
    Long countByHomestayIdAndCreatedAtAfter(@Param("homestayId") Long homestayId, @Param("createdAtAfter") LocalDateTime createdAtAfter);

    /**
     * 批量查询房源平均评分
     */
    @Query("SELECT r.homestay.id, AVG(r.rating) FROM Review r WHERE r.homestay.id IN :ids AND r.isPublic = true AND r.deleted = false GROUP BY r.homestay.id")
    List<Object[]> getAverageRatingByHomestayIds(@Param("ids") List<Long> ids);

    /**
     * 批量查询房源评价数量
     */
    @Query("SELECT r.homestay.id, COUNT(r) FROM Review r WHERE r.homestay.id IN :ids AND r.isPublic = true AND r.deleted = false GROUP BY r.homestay.id")
    List<Object[]> getReviewCountByHomestayIds(@Param("ids") List<Long> ids);

    /**
     * 批量查询房源平均评分和评价数量（合并查询，减少一次数据库往返）
     */
    @Query("SELECT r.homestay.id, AVG(r.rating), COUNT(r) FROM Review r WHERE r.homestay.id IN :ids AND r.isPublic = true AND r.deleted = false GROUP BY r.homestay.id")
    List<Object[]> getRatingAndReviewCountByHomestayIds(@Param("ids") List<Long> ids);
}
