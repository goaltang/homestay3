package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.model.Homestay;
import com.homestay3.homestaybackend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HomestayRepository extends JpaRepository<Homestay, Long>, JpaSpecificationExecutor<Homestay> {
    
    /**
     * 根据状态查找房源
     * @param status 状态
     * @return 房源列表
     */
    List<Homestay> findByStatus(String status);
    
    /**
     * 根据状态和是否推荐查找房源
     * @param status 状态
     * @return 房源列表
     */
    List<Homestay> findByStatusAndFeaturedTrue(String status);
    
    /**
     * 根据状态和是否推荐查找房源
     * @param status 状态
     * @return 房源列表
     */
    List<Homestay> findByStatusAndFeaturedFalse(String status);
    
    /**
     * 根据类型和状态查找房源
     * @param type 类型
     * @param status 状态
     * @return 房源列表
     */
    List<Homestay> findByTypeAndStatus(String type, String status);
    
    /**
     * 查找推荐房源
     * @return 房源列表
     */
    List<Homestay> findByFeaturedTrue();
    
    /**
     * 根据城市查找房源
     * @param city 城市
     * @return 房源列表
     */
    List<Homestay> findByCity(String city);
    
    /**
     * 根据类型查找房源
     * @param type 类型
     * @return 房源列表
     */
    List<Homestay> findByType(String type);
    
    /**
     * 根据价格范围查找房源
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @return 房源列表
     */
    @Query("SELECT h FROM Homestay h WHERE h.price BETWEEN :minPrice AND :maxPrice AND h.status = 'ACTIVE'")
    List<Homestay> findByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);
    
    /**
     * 根据容纳人数查找房源
     * @param guestCount 容纳人数
     * @return 房源列表
     */
    @Query("SELECT h FROM Homestay h WHERE h.maxGuests >= :guestCount AND h.status = 'ACTIVE'")
    List<Homestay> findByGuestCapacity(@Param("guestCount") Integer guestCount);
    
    /**
     * 高级搜索房源
     * @param location 位置（城市或省份）
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param guestCount 容纳人数
     * @param type 类型
     * @return 房源列表
     */
    @Query("SELECT h FROM Homestay h WHERE " +
           "(:location IS NULL OR h.city = :location OR h.province = :location) AND " +
           "(:minPrice IS NULL OR h.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR h.price <= :maxPrice) AND " +
           "(:guestCount IS NULL OR h.maxGuests >= :guestCount) AND " +
           "(:type IS NULL OR h.type = :type) AND " +
           "h.status = 'ACTIVE'")
    List<Homestay> searchHomestays(
            @Param("location") String location,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("guestCount") Integer guestCount,
            @Param("type") String type);
    
    /**
     * 根据所有者查找房源
     * @param owner 所有者
     * @return 房源列表
     */
    List<Homestay> findByOwner(User owner);
    
    /**
     * 根据标题模糊查询房源
     * @param title 标题
     * @param pageable 分页
     * @return 房源分页
     */
    Page<Homestay> findByTitleContaining(String title, Pageable pageable);
    
    /**
     * 根据标题和状态模糊查询房源
     * @param title 标题
     * @param status 状态
     * @param pageable 分页
     * @return 房源分页
     */
    Page<Homestay> findByTitleContainingAndStatus(String title, String status, Pageable pageable);
    
    /**
     * 根据标题和类型模糊查询房源
     * @param title 标题
     * @param type 类型
     * @param pageable 分页
     * @return 房源分页
     */
    Page<Homestay> findByTitleContainingAndType(String title, String type, Pageable pageable);
    
    /**
     * 根据标题、状态和类型模糊查询房源
     * @param title 标题
     * @param status 状态
     * @param type 类型
     * @param pageable 分页
     * @return 房源分页
     */
    Page<Homestay> findByTitleContainingAndStatusAndType(String title, String status, String type, Pageable pageable);
    
    // 统计房东的房源数量
    @Query("SELECT COUNT(h) FROM Homestay h WHERE h.owner.id = :ownerId")
    Long countByOwnerId(@Param("ownerId") Long ownerId);
    
    // 获取房东的所有房源
    @Query("SELECT h FROM Homestay h WHERE h.owner.id = :ownerId")
    Page<Homestay> findByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);
    
    List<Homestay> findByOwnerId(Long ownerId);
    
    @Query("SELECT h FROM Homestay h WHERE h.owner.username = :username")
    List<Homestay> findByOwnerUsername(@Param("username") String username);
    
    @Query("SELECT h FROM Homestay h WHERE h.owner.username = :username AND h.status = :status")
    List<Homestay> findByOwnerUsernameAndStatus(@Param("username") String username, @Param("status") String status);
    
    @Query("SELECT COUNT(h) FROM Homestay h WHERE h.owner.username = :username")
    Long countByOwnerUsername(@Param("username") String username);
    
    @Query("SELECT COUNT(h) FROM Homestay h WHERE h.owner.username = :username AND h.status = :status")
    Long countByOwnerUsernameAndStatus(@Param("username") String username, @Param("status") String status);
    
    Long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT h.province, COUNT(h) FROM Homestay h GROUP BY h.province")
    List<Object[]> countByProvince();
} 