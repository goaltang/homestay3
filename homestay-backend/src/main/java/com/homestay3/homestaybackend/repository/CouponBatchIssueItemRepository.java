package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.CouponBatchIssueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface CouponBatchIssueItemRepository extends JpaRepository<CouponBatchIssueItem, Long> {

    List<CouponBatchIssueItem> findByTaskId(Long taskId);

    List<CouponBatchIssueItem> findByTaskIdAndStatus(Long taskId, String status);

    Page<CouponBatchIssueItem> findByTaskIdAndStatus(Long taskId, String status, Pageable pageable);

    long countByTaskIdAndStatus(Long taskId, String status);

    @Query("SELECT i FROM CouponBatchIssueItem i WHERE i.taskId = :taskId ORDER BY i.createdAt DESC")
    Page<CouponBatchIssueItem> findByTaskId(@Param("taskId") Long taskId, Pageable pageable);
}
