package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.RefundRecord;
import com.homestay3.homestaybackend.model.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRecordRepository extends JpaRepository<RefundRecord, Long> {
    
    /**
     * 根据订单ID查找退款记录
     */
    List<RefundRecord> findByOrderId(Long orderId);
    
    /**
     * 根据订单ID查找最新的退款记录
     */
    Optional<RefundRecord> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);
    
    /**
     * 根据商户订单号查找退款记录
     */
    Optional<RefundRecord> findByOutTradeNo(String outTradeNo);
    
    /**
     * 根据退款交易号查找退款记录
     */
    Optional<RefundRecord> findByRefundTradeNo(String refundTradeNo);
    
    /**
     * 根据订单ID和状态查找退款记录
     */
    List<RefundRecord> findByOrderIdAndRefundStatus(Long orderId, RefundStatus refundStatus);
}
