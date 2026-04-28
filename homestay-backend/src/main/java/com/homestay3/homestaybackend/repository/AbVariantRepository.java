package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.AbVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AbVariantRepository extends JpaRepository<AbVariant, Long> {

    List<AbVariant> findByExperimentId(Long experimentId);
}
