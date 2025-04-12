package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.model.Order;
import com.homestay3.homestaybackend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    // 查找指定房东的订单
    @Query("SELECT o FROM Order o JOIN o.homestay h WHERE h.owner = :owner")
    Page<Order> findByOwner(@Param("owner") User owner, Pageable pageable);
    
    // 查找指定房东的特定状态订单
    @Query("SELECT o FROM Order o JOIN o.homestay h WHERE h.owner = :owner AND o.status = :status")
    Page<Order> findByOwnerAndStatus(@Param("owner") User owner, @Param("status") String status, Pageable pageable);
    
    // 获取房东待处理订单数量
    @Query("SELECT COUNT(o) FROM Order o JOIN o.homestay h WHERE h.owner = :owner AND o.status = 'PENDING'")
    Long countPendingOrdersByOwner(@Param("owner") User owner);
    
    // 查找指定用户的订单
    Page<Order> findByGuest(User guest, Pageable pageable);
    
    // 查找指定用户的特定状态订单
    Page<Order> findByGuestAndStatus(User guest, String status, Pageable pageable);
    
    // 检查指定房源在日期范围内是否有订单
    @Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.homestay.id = :homestayId " +
           "AND o.status IN ('CONFIRMED', 'PAID') " +
           "AND ((o.checkInDate <= :endDate AND o.checkOutDate >= :startDate))")
    boolean existsOverlappingBooking(@Param("homestayId") Long homestayId, 
                                     @Param("startDate") LocalDate startDate, 
                                     @Param("endDate") LocalDate endDate);
    
    // 获取用户的所有订单
    @Query("SELECT o FROM Order o WHERE o.guest.id = :guestId")
    Page<Order> findByGuestId(@Param("guestId") Long guestId, Pageable pageable);
    
    // 根据状态获取用户的订单
    @Query("SELECT o FROM Order o WHERE o.guest.id = :guestId AND o.status = :status")
    Page<Order> findByGuestIdAndStatus(@Param("guestId") Long guestId, @Param("status") String status, Pageable pageable);
    
    // 获取特定房源的所有订单
    @Query("SELECT o FROM Order o WHERE o.homestay.id = :homestayId")
    Page<Order> findByHomestayId(@Param("homestayId") Long homestayId, Pageable pageable);
    
    // 检查日期范围内的可用性
    @Query("SELECT o FROM Order o WHERE o.homestay.id = :homestayId " +
            "AND o.status != 'CANCELLED' " +
            "AND ((o.checkInDate <= :checkOutDate AND o.checkOutDate >= :checkInDate) " +
            "OR (o.checkInDate >= :checkInDate AND o.checkInDate <= :checkOutDate) " +
            "OR (o.checkOutDate >= :checkInDate AND o.checkOutDate <= :checkOutDate))")
    List<Order> findConflictingOrders(
            @Param("homestayId") Long homestayId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate);
            
    // 房东相关方法
    
    // 获取房东的所有订单
    @Query("SELECT o FROM Order o JOIN o.homestay h WHERE h.owner.id = :hostId")
    Page<Order> findByHostId(@Param("hostId") Long hostId, Pageable pageable);
    
    // 根据状态获取房东的订单
    @Query("SELECT o FROM Order o JOIN o.homestay h WHERE h.owner.id = :hostId AND o.status = :status")
    Page<Order> findByHostIdAndStatus(@Param("hostId") Long hostId, @Param("status") String status, Pageable pageable);
    
    // 统计房东的订单总数
    @Query("SELECT COUNT(o) FROM Order o JOIN o.homestay h WHERE h.owner.id = :hostId")
    Long countByHostId(@Param("hostId") Long hostId);
    
    // 统计房东的特定状态订单数
    @Query("SELECT COUNT(o) FROM Order o JOIN o.homestay h WHERE h.owner.id = :hostId AND o.status = :status")
    Long countByHostIdAndStatus(@Param("hostId") Long hostId, @Param("status") String status);
    
    // 获取房东的总收入
    @Query("SELECT SUM(o.totalAmount) FROM Order o JOIN o.homestay h WHERE h.owner.id = :hostId AND o.status = 'COMPLETED'")
    Double getTotalEarningsByHostId(@Param("hostId") Long hostId);
    
    // 根据房东用户名统计订单总数
    @Query("SELECT COUNT(o) FROM Order o JOIN o.homestay h JOIN h.owner u WHERE u.username = :username")
    Long countByHomestayOwnerUsername(@Param("username") String username);
    
    // 根据房东用户名和订单状态统计订单数
    @Query("SELECT COUNT(o) FROM Order o JOIN o.homestay h JOIN h.owner u WHERE u.username = :username AND o.status = :status")
    Long countByHomestayOwnerUsernameAndStatus(@Param("username") String username, @Param("status") String status);

    List<Order> findByGuest_IdOrderByCreatedAtDesc(Long userId);
    
    Page<Order> findByHomestayIdOrderByCreatedAtDesc(Long homestayId, Pageable pageable);
    
    Optional<Order> findByIdAndGuest_Id(Long id, Long userId);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.homestay.id = :homestayId")
    Long countByHomestayId(@Param("homestayId") Long homestayId);
    
    Long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.createdAt BETWEEN :start AND :end AND o.status = :status")
    Optional<Double> sumTotalPriceByCreatedAtBetweenAndStatus(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        @Param("status") String status
    );
} 