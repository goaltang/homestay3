package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.model.HomestayType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HomestayTypeRepository extends JpaRepository<HomestayType, Long> {
    
    Optional<HomestayType> findByCode(String code);
    
    List<HomestayType> findByActiveTrue();
    
    List<HomestayType> findByActiveTrueOrderBySortOrderAsc();
    
    List<HomestayType> findByCategoryIdOrderBySortOrderAsc(Long categoryId);
    
    List<HomestayType> findByCategoryIdAndActiveTrueOrderBySortOrderAsc(Long categoryId);

    Optional<HomestayType> findByNameIgnoreCase(String name);
} 