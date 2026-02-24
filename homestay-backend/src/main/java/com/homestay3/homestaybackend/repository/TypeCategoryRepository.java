package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.TypeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TypeCategoryRepository extends JpaRepository<TypeCategory, Long> {
    
    List<TypeCategory> findAllByOrderBySortOrderAsc();
    
} 