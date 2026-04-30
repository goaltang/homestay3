package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.ReferralRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface ReferralRecordRepository extends JpaRepository<ReferralRecord, Long> {

    Optional<ReferralRecord> findByReferralCode(String referralCode);

    Optional<ReferralRecord> findByReferralCodeAndStatusAndExpireAtAfter(String referralCode, String status, LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rr FROM ReferralRecord rr WHERE rr.referralCode = :code AND rr.status = :status AND rr.expireAt > :now")
    Optional<ReferralRecord> findByReferralCodeAndStatusAndExpireAtAfterForUpdate(
            @Param("code") String referralCode,
            @Param("status") String status,
            @Param("now") LocalDateTime now);

    List<ReferralRecord> findByInviterIdOrderByCreatedAtDesc(Long inviterId);
}
