package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.CheckOutRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CheckOutRecordRepository extends JpaRepository<CheckOutRecord, Long> {

    /**
     * 根据订单ID查找退房记录
     */
    List<CheckOutRecord> findByOrderId(Long orderId);

    /**
     * 根据订单ID查找最新的退房记录
     */
    Optional<CheckOutRecord> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);

    /**
     * 根据状态查找退房记录
     */
    List<CheckOutRecord> findByStatus(String status);

    /**
     * 检查订单是否存在退房记录
     */
    boolean existsByOrderId(Long orderId);
}
