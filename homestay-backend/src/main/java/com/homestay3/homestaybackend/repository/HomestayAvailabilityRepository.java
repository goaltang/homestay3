package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.HomestayAvailability;
import com.homestay3.homestaybackend.entity.HomestayAvailability.AvailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HomestayAvailabilityRepository extends JpaRepository<HomestayAvailability, Long> {

    Optional<HomestayAvailability> findByHomestayIdAndDate(Long homestayId, LocalDate date);

    List<HomestayAvailability> findByHomestayIdAndDateBetween(Long homestayId, LocalDate startDate, LocalDate endDate);

    List<HomestayAvailability> findByHomestayIdAndDateIn(Long homestayId, List<LocalDate> dates);

    @Query("SELECT ha FROM HomestayAvailability ha WHERE ha.homestayId = :homestayId " +
           "AND ha.date >= :startDate AND ha.date < :endDate AND ha.status = :status")
    List<HomestayAvailability> findByHomestayIdAndDateRangeAndStatus(
            @Param("homestayId") Long homestayId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") AvailabilityStatus status);

    @Query("SELECT COUNT(ha) > 0 FROM HomestayAvailability ha WHERE ha.homestayId = :homestayId " +
           "AND ha.date >= :startDate AND ha.date < :endDate AND ha.status = 'BOOKED'")
    boolean hasBookedDates(
            @Param("homestayId") Long homestayId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Modifying
    @Query("UPDATE HomestayAvailability ha SET ha.status = :newStatus, ha.orderId = :orderId, " +
           "ha.locked = false, ha.lockExpiresAt = null WHERE ha.homestayId = :homestayId " +
           "AND ha.date >= :startDate AND ha.date < :endDate")
    int updateAvailabilityStatus(
            @Param("homestayId") Long homestayId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("newStatus") AvailabilityStatus newStatus,
            @Param("orderId") Long orderId);

    @Modifying
    @Query("UPDATE HomestayAvailability ha SET ha.locked = true, ha.lockExpiresAt = :expiresAt " +
           "WHERE ha.homestayId = :homestayId AND ha.date >= :startDate AND ha.date < :endDate " +
           "AND ha.status = 'AVAILABLE' AND (ha.locked = false OR ha.lockExpiresAt < :now)")
    int acquireLocks(
            @Param("homestayId") Long homestayId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("expiresAt") LocalDateTime expiresAt,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE HomestayAvailability ha SET ha.locked = false, ha.lockExpiresAt = null " +
           "WHERE ha.homestayId = :homestayId AND ha.date >= :startDate AND ha.date < :endDate")
    int releaseLocks(
            @Param("homestayId") Long homestayId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT ha FROM HomestayAvailability ha WHERE ha.homestayId = :homestayId " +
           "AND ha.date >= :startDate AND ha.date < :endDate AND ha.locked = true " +
           "AND ha.lockExpiresAt > :now")
    List<HomestayAvailability> findLockedDates(
            @Param("homestayId") Long homestayId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("now") LocalDateTime now);

    @Query("SELECT ha FROM HomestayAvailability ha WHERE ha.homestayId IN :homestayIds " +
           "AND ha.date >= :startDate AND ha.date < :endDate")
    List<HomestayAvailability> findByHomestayIdsAndDateRange(
            @Param("homestayIds") List<Long> homestayIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Modifying
    @Query("UPDATE HomestayAvailability ha SET ha.status = :status, ha.source = 'HOST', " +
           "ha.reason = :reason, ha.note = :note, ha.createdBy = :createdBy " +
           "WHERE ha.homestayId = :homestayId AND ha.date >= :startDate AND ha.date < :endDate " +
           "AND ha.status != 'BOOKED'")
    int setManualAvailability(
            @Param("homestayId") Long homestayId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") HomestayAvailability.AvailabilityStatus status,
            @Param("reason") String reason,
            @Param("note") String note,
            @Param("createdBy") Long createdBy);

    @Query("SELECT COUNT(ha) > 0 FROM HomestayAvailability ha WHERE ha.homestayId = :homestayId " +
           "AND ha.date >= :startDate AND ha.date < :endDate AND ha.status = 'BOOKED' " +
           "AND ha.locked = true AND ha.lockExpiresAt > :now")
    boolean hasOccupiedDates(
            @Param("homestayId") Long homestayId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("now") LocalDateTime now);
}
