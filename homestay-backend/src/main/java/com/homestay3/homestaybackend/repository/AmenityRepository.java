package com.homestay3.homestaybackend.repository;

import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.homestay3.homestaybackend.model.Amenity;
import com.homestay3.homestaybackend.model.AmenityCategory;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, String> {
    
    List<Amenity> findByLabelContainingIgnoreCase(String keyword);
    
    List<Amenity> findByValueContainingIgnoreCaseOrLabelContainingIgnoreCase(String valueKeyword, String labelKeyword);
    
    List<Amenity> findByCategory(AmenityCategory category);
    
    List<Amenity> findByCategoryAndActive(AmenityCategory category, boolean active);
    
    List<Amenity> findByActive(boolean active);
    
    List<Amenity> findByValueContainingIgnoreCaseOrLabelContainingIgnoreCaseAndActive(
            String valueKeyword, String labelKeyword, boolean active);
    
    @Modifying
    @Query("UPDATE Amenity a SET a.usageCount = a.usageCount + 1 WHERE a.value = :value")
    void incrementUsageCount(@Param("value") String value);
    
    @Modifying
    @Query("UPDATE Amenity a SET a.usageCount = a.usageCount - 1 WHERE a.value = :value AND a.usageCount > 0")
    void decrementUsageCount(@Param("value") String value);
    
    @Query(value = "SELECT a.* FROM amenities a WHERE a.value IN " +
           "(SELECT amenity_id FROM homestay_amenity WHERE homestay_id = :homestayId)", 
           nativeQuery = true)
    List<Amenity> findByHomestayId(@Param("homestayId") Long homestayId);
    
    @Query("SELECT COUNT(h) FROM Homestay h JOIN h.amenities a WHERE a.value = :value")
    long countHomestaysUsingAmenity(@Param("value") String value);

    List<Amenity> findByValueInIgnoreCase(Collection<String> values);
} 