package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 根据邮箱查找用户
     * @param email 邮箱
     * @return 用户
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 根据重置令牌查找用户
     * @param resetToken 重置令牌
     * @return 用户
     */
    Optional<User> findByResetToken(String resetToken);
    
    // 根据角色查询用户
    List<User> findByRole(String role);
    
    Long countByCreatedAtBefore(LocalDateTime dateTime);
    
    Long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
} 