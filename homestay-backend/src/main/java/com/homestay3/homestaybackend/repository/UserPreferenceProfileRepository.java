package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.UserPreferenceProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserPreferenceProfileRepository extends JpaRepository<UserPreferenceProfile, Long> {

    /**
     * 查找用户画像
     */
    Optional<UserPreferenceProfile> findByUserId(Long userId);

    /**
     * 查找需要更新的画像（长时间未更新）
     */
    List<UserPreferenceProfile> findByUpdatedAtBefore(LocalDateTime before);

    /**
     * 查找最近活跃的用户画像
     */
    List<UserPreferenceProfile> findByLastActiveAtAfter(LocalDateTime since);
}
