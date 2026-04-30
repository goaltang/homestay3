package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findByEnabledTrueOrderBySortOrderAsc();

    Page<Banner> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
