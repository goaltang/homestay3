package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.ReferralRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ReferralRecordRepository extends JpaRepository<ReferralRecord, Long> {

    Optional<ReferralRecord> findByReferralCode(String referralCode);

    Optional<ReferralRecord> findByReferralCodeAndStatusAndExpireAtAfter(String referralCode, String status, LocalDateTime now);
}
