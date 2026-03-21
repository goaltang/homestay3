package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.CheckInRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CheckInRecordRepository extends JpaRepository<CheckInRecord, Long> {

    /**
     * 根据订单ID查找入住记录
     */
    List<CheckInRecord> findByOrderId(Long orderId);

    /**
     * 根据订单ID查找最新的入住记录
     */
    Optional<CheckInRecord> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);

    /**
     * 根据入住码查找入住记录
     */
    Optional<CheckInRecord> findByCheckInCode(String checkInCode);

    /**
     * 检查订单是否存在入住记录
     */
    boolean existsByOrderId(Long orderId);

    /**
     * 根据订单ID和状态查找入住记录
     */
    List<CheckInRecord> findByOrderIdAndStatus(Long orderId, String status);
}
