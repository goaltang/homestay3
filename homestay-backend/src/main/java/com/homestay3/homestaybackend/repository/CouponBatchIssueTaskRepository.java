package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.CouponBatchIssueTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface CouponBatchIssueTaskRepository extends JpaRepository<CouponBatchIssueTask, Long> {

    Page<CouponBatchIssueTask> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
