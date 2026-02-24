package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {
    
    /**
     * 查找用户的所有收藏
     */
    List<UserFavorite> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 查找用户收藏的民宿ID列表
     */
    @Query("SELECT uf.homestayId FROM UserFavorite uf WHERE uf.userId = :userId ORDER BY uf.createdAt DESC")
    List<Long> findHomestayIdsByUserId(@Param("userId") Long userId);
    
    /**
     * 检查用户是否收藏了某个民宿
     */
    boolean existsByUserIdAndHomestayId(Long userId, Long homestayId);
    
    /**
     * 查找特定的收藏记录
     */
    Optional<UserFavorite> findByUserIdAndHomestayId(Long userId, Long homestayId);
    
    /**
     * 删除用户对某个民宿的收藏
     */
    @Modifying
    @Query("DELETE FROM UserFavorite uf WHERE uf.userId = :userId AND uf.homestayId = :homestayId")
    void deleteByUserIdAndHomestayId(@Param("userId") Long userId, @Param("homestayId") Long homestayId);
    
    /**
     * 统计用户的收藏数量
     */
    long countByUserId(Long userId);
    
    /**
     * 清空用户的所有收藏
     */
    @Modifying
    @Query("DELETE FROM UserFavorite uf WHERE uf.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
    
    /**
     * 获取收藏了某个民宿的用户数量
     */
    long countByHomestayId(Long homestayId);
} 