package com.homestay3.homestaybackend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.homestay3.homestaybackend.model.AmenityCategory;

@Repository
public interface AmenityCategoryRepository extends JpaRepository<AmenityCategory, String> {
    
    List<AmenityCategory> findByNameContainingIgnoreCase(String keyword);
    
    List<AmenityCategory> findAllByOrderBySortOrderAsc();
} 