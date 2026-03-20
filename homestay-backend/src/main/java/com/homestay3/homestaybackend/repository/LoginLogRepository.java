package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.LoginLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {

    Page<LoginLog> findByUsernameContaining(String username, Pageable pageable);

    Page<LoginLog> findByLoginTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<LoginLog> findByLoginTypeAndUsernameContaining(String loginType, String username, Pageable pageable);

    @Query("SELECT l FROM LoginLog l WHERE " +
           "(:username IS NULL OR l.username LIKE %:username%) AND " +
           "(:loginType IS NULL OR l.loginType = :loginType) AND " +
           "(:startTime IS NULL OR l.loginTime >= :startTime) AND " +
           "(:endTime IS NULL OR l.loginTime <= :endTime) AND " +
           "(:loginStatus IS NULL OR l.loginStatus = :loginStatus)")
    Page<LoginLog> searchLogs(@Param("username") String username,
                              @Param("loginType") String loginType,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime,
                              @Param("loginStatus") String loginStatus,
                              Pageable pageable);
}
