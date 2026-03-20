package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    Optional<SystemConfig> findByConfigKey(String configKey);

    List<SystemConfig> findByCategory(String category);

    boolean existsByConfigKey(String configKey);

    void deleteByConfigKey(String configKey);
}
