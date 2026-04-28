package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.AbEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AbEventRepository extends JpaRepository<AbEvent, Long> {

    @Query("SELECT e.variantId, e.eventType, COUNT(*), COUNT(DISTINCT e.userId), COALESCE(SUM(e.eventValue), 0) " +
           "FROM AbEvent e WHERE e.experimentId = :experimentId AND e.createdAt BETWEEN :start AND :end " +
           "GROUP BY e.variantId, e.eventType")
    List<Object[]> aggregateByExperiment(@Param("experimentId") Long experimentId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    long countByExperimentIdAndVariantIdAndEventType(Long experimentId, Long variantId, String eventType);
}
