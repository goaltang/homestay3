package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.AbExperiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AbExperimentRepository extends JpaRepository<AbExperiment, Long> {

    List<AbExperiment> findByStatus(String status);

    List<AbExperiment> findByTargetIdAndExperimentType(Long targetId, String experimentType);
}
