package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.DisputeRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 争议记录Repository
 */
@Repository
public interface DisputeRecordRepository extends JpaRepository<DisputeRecord, Long> {

    /**
     * 根据订单ID查询争议记录
     */
    List<DisputeRecord> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    /**
     * 根据订单ID查询最新的争议记录
     */
    Optional<DisputeRecord> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    /**
     * 根据发起人ID查询争议记录
     */
    Page<DisputeRecord> findByRaisedByOrderByCreatedAtDesc(Long raisedBy, Pageable pageable);

    /**
     * 根据仲裁人ID查询争议记录
     */
    Page<DisputeRecord> findByResolvedByOrderByCreatedAtDesc(Long resolvedBy, Pageable pageable);

    /**
     * 查询所有争议记录
     */
    Page<DisputeRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
