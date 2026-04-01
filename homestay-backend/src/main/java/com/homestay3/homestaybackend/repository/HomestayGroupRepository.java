package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.HomestayGroup;
import com.homestay3.homestaybackend.entity.User;
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
}
