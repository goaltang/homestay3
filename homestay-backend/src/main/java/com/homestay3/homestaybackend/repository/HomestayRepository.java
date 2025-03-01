package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.model.Homestay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface HomestayRepository extends JpaRepository<Homestay, Long> {
    
    List<Homestay> findByFeaturedTrue();
    
    List<Homestay> findByCity(String city);
    
    List<Homestay> findByCountry(String country);
    
    List<Homestay> findByPropertyType(String propertyType);
    
    @Query("SELECT h FROM Homestay h WHERE h.pricePerNight BETWEEN :minPrice AND :maxPrice")
    List<Homestay> findByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);
    
    @Query("SELECT h FROM Homestay h WHERE h.maxGuests >= :guestCount")
    List<Homestay> findByGuestCapacity(@Param("guestCount") Integer guestCount);
    
    @Query("SELECT h FROM Homestay h WHERE " +
           "(:location IS NULL OR LOWER(h.city) LIKE LOWER(CONCAT('%', :location, '%')) OR LOWER(h.country) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:minPrice IS NULL OR h.pricePerNight >= :minPrice) AND " +
           "(:maxPrice IS NULL OR h.pricePerNight <= :maxPrice) AND " +
           "(:guestCount IS NULL OR h.maxGuests >= :guestCount) AND " +
           "(:propertyType IS NULL OR h.propertyType = :propertyType)")
    List<Homestay> searchHomestays(
            @Param("location") String location,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("guestCount") Integer guestCount,
            @Param("propertyType") String propertyType
    );
} 