package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    Page<OperationLog> findByOperator(String operator, Pageable pageable);

    Page<OperationLog> findByOperationType(String operationType, Pageable pageable);

    Page<OperationLog> findByResource(String resource, Pageable pageable);

    @Query("SELECT o FROM OperationLog o WHERE o.operateTime BETWEEN :startTime AND :endTime")
    Page<OperationLog> findByOperateTimeBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    @Query("SELECT o FROM OperationLog o WHERE " +
           "(:operator IS NULL OR o.operator LIKE %:operator%) AND " +
           "(:operationType IS NULL OR o.operationType = :operationType) AND " +
           "(:resource IS NULL OR o.resource = :resource) AND " +
           "(:startTime IS NULL OR o.operateTime >= :startTime) AND " +
           "(:endTime IS NULL OR o.operateTime <= :endTime)")
    Page<OperationLog> searchLogs(
            @Param("operator") String operator,
            @Param("operationType") String operationType,
            @Param("resource") String resource,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    List<OperationLog> findTop50ByOrderByOperateTimeDesc();
}
