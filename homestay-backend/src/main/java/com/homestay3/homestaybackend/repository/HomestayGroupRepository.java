package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.HomestayGroup;
import com.homestay3.homestaybackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HomestayGroupRepository extends JpaRepository<HomestayGroup, Long> {

    List<HomestayGroup> findByOwner(User owner);

    List<HomestayGroup> findByOwnerId(Long ownerId);

    Optional<HomestayGroup> findByOwnerIdAndId(Long ownerId, Long id);

    Optional<HomestayGroup> findByOwnerIdAndCode(Long ownerId, String code);

    boolean existsByOwnerIdAndName(Long ownerId, String name);

    boolean existsByOwnerIdAndCode(Long ownerId, String code);

    @Query("SELECT COUNT(hg) FROM HomestayGroup hg WHERE hg.owner.id = :ownerId")
    Long countByOwnerId(@Param("ownerId") Long ownerId);

    List<HomestayGroup> findByOwnerIdAndEnabledTrueOrderBySortOrderAsc(Long ownerId);

    List<HomestayGroup> findByOwnerIdAndIsDefaultTrue(Long ownerId);

    void deleteByOwnerIdAndId(Long ownerId, Long id);

    @Query("SELECT hg FROM HomestayGroup hg ORDER BY hg.createdAt DESC")
    Page<HomestayGroup> findAllOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT hg FROM HomestayGroup hg WHERE hg.owner.username LIKE %:keyword% OR hg.name LIKE %:keyword%")
    Page<HomestayGroup> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT hg FROM HomestayGroup hg WHERE hg.owner.id = :ownerId")
    Page<HomestayGroup> findByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);
}
