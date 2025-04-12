package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.model.Earning;
import com.homestay3.homestaybackend.model.User;
import com.homestay3.homestaybackend.dto.DailyEarningDTO;
import com.homestay3.homestaybackend.dto.MonthlyEarningDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface EarningRepository extends JpaRepository<Earning, Long>, JpaSpecificationExecutor<Earning> {
    
    // 查找房东收益
    Page<Earning> findByHost(User host, Pageable pageable);
    
    // 计算房东总收益
    @Query("SELECT SUM(e.amount) FROM Earning e WHERE e.host = :host")
    BigDecimal sumTotalEarningsByHost(@Param("host") User host);
    
    // 计算房东月度收益
    @Query("SELECT SUM(e.amount) FROM Earning e WHERE e.host = :host " +
           "AND YEAR(e.checkInDate) = :year AND MONTH(e.checkInDate) = :month")
    BigDecimal sumMonthlyEarningsByHost(
            @Param("host") User host, 
            @Param("year") int year, 
            @Param("month") int month);
    
    // 计算房东指定日期范围内的收益
    @Query("SELECT SUM(e.amount) FROM Earning e WHERE e.host = :host " +
           "AND e.checkInDate BETWEEN :startDate AND :endDate")
    BigDecimal sumEarningsByHostAndDateRange(
            @Param("host") User host, 
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate);
    
    // 获取房东指定房源的收益
    @Query("SELECT e FROM Earning e WHERE e.host = :host AND e.homestay.id = :homestayId")
    Page<Earning> findByHostAndHomestayId(
            @Param("host") User host, 
            @Param("homestayId") Long homestayId, 
            Pageable pageable);
    
    // 获取房东日收益趋势数据
    @Query("SELECT DATE_FORMAT(e.checkInDate, '%Y-%m-%d') as date, SUM(e.amount) as amount " +
           "FROM Earning e WHERE e.host = :host " +
           "AND e.checkInDate BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE_FORMAT(e.checkInDate, '%Y-%m-%d') " +
           "ORDER BY DATE_FORMAT(e.checkInDate, '%Y-%m-%d')")
    List<Object[]> getDailyEarningsTrend(
            @Param("host") User host, 
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate);
    
    // 获取房东月收益趋势数据
    @Query("SELECT CONCAT(YEAR(e.checkInDate), '-', MONTH(e.checkInDate)) as month, SUM(e.amount) as amount " +
           "FROM Earning e WHERE e.host = :host " +
           "AND e.checkInDate BETWEEN :startDate AND :endDate " +
           "GROUP BY YEAR(e.checkInDate), MONTH(e.checkInDate) " +
           "ORDER BY YEAR(e.checkInDate), MONTH(e.checkInDate)")
    List<Object[]> getMonthlyEarningsTrend(
            @Param("host") User host, 
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate);
    
    // 查询指定房东在给定日期范围内的收益
    @Query("SELECT e FROM Earning e WHERE e.host.id = :hostId " +
            "AND (:startDate IS NULL OR e.checkInDate >= :startDate) " +
            "AND (:endDate IS NULL OR e.checkOutDate <= :endDate) " +
            "AND (:homestayId IS NULL OR e.homestay.id = :homestayId)")
    Page<Earning> findByHostAndDateRange(
            @Param("hostId") Long hostId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("homestayId") Long homestayId,
            Pageable pageable);
    
    // 查询指定房东的总收益
    @Query("SELECT SUM(e.amount) FROM Earning e WHERE e.host.id = :hostId " +
            "AND (:startDate IS NULL OR e.checkInDate >= :startDate) " +
            "AND (:endDate IS NULL OR e.checkOutDate <= :endDate) " +
            "AND (:homestayId IS NULL OR e.homestay.id = :homestayId)")
    BigDecimal sumAmountByHostAndDateRange(
            @Param("hostId") Long hostId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("homestayId") Long homestayId);
    
    // 查询指定房东的待结算收益
    @Query("SELECT SUM(e.amount) FROM Earning e WHERE e.host.id = :hostId AND e.status = 'PENDING'")
    BigDecimal sumPendingAmountByHost(@Param("hostId") Long hostId);
    
    // 按日期统计收益趋势
    @Query("SELECT NEW com.homestay3.homestaybackend.dto.DailyEarningDTO(e.checkInDate, SUM(e.amount)) " +
            "FROM Earning e WHERE e.host.id = :hostId " +
            "AND e.checkInDate BETWEEN :startDate AND :endDate " +
            "GROUP BY e.checkInDate " +
            "ORDER BY e.checkInDate")
    List<DailyEarningDTO> getDailyEarningsByHost(
            @Param("hostId") Long hostId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    // 按月份统计收益趋势
    @Query("SELECT NEW com.homestay3.homestaybackend.dto.MonthlyEarningDTO(FUNCTION('YEAR', e.checkInDate), FUNCTION('MONTH', e.checkInDate), SUM(e.amount)) " +
            "FROM Earning e WHERE e.host.id = :hostId " +
            "AND e.checkInDate BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('YEAR', e.checkInDate), FUNCTION('MONTH', e.checkInDate) " +
            "ORDER BY FUNCTION('YEAR', e.checkInDate), FUNCTION('MONTH', e.checkInDate)")
    List<MonthlyEarningDTO> getMonthlyEarningsByHost(
            @Param("hostId") Long hostId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
} 