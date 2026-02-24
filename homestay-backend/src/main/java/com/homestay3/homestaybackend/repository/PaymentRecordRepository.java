package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 支付记录Repository
 */
@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    
    /**
     * 根据商户订单号查找支付记录
     */
    Optional<PaymentRecord> findByOutTradeNo(String outTradeNo);
    
    /**
     * 根据订单ID查找最新的支付记录
     */
    Optional<PaymentRecord> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);
    
    /**
     * 根据支付平台交易号查找支付记录
     */
    Optional<PaymentRecord> findByTransactionId(String transactionId);
} 