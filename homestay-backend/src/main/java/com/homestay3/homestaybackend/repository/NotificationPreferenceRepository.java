package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    Optional<NotificationPreference> findByUserIdAndDomain(Long userId, String domain);

    List<NotificationPreference> findByUserId(Long userId);
}
