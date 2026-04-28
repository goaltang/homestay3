package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.AbAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AbAssignmentRepository extends JpaRepository<AbAssignment, Long> {

    Optional<AbAssignment> findByExperimentIdAndUserId(Long experimentId, Long userId);
}
