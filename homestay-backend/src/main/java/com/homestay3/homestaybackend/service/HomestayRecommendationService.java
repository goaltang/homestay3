package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.HomestaySummaryDTO;
import com.homestay3.homestaybackend.dto.PagedResponse;
import com.homestay3.homestaybackend.dto.UserRecommendationRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 民宿推荐服务接口
 */
public interface HomestayRecommendationService {
    
    /**
     * 获取热门民宿
     * 基于近期预订量、评分等因素排序
     * 
     * @param limit 返回数量限制
     * @return 热门民宿列表
     */
    List<HomestaySummaryDTO> getPopularHomestays(int limit);
    
    /**
     * 获取热门民宿（分页版本）
     * 基于近期预订量、评分等因素排序
     * 
     * @param pageable 分页参数
     * @return 热门民宿分页结果
     */
    PagedResponse<HomestaySummaryDTO> getPopularHomestaysPage(Pageable pageable);
    
    /**
     * 获取推荐民宿
     * 基于综合算法推荐的优质民宿
     * 
     * @param limit 返回数量限制
     * @return 推荐民宿列表
     */
    List<HomestaySummaryDTO> getRecommendedHomestays(int limit);
    
    /**
     * 获取推荐民宿（分页版本）
     * 基于综合算法推荐的优质民宿
     * 
     * @param pageable 分页参数
     * @return 推荐民宿分页结果
     */
    PagedResponse<HomestaySummaryDTO> getRecommendedHomestaysPage(Pageable pageable);
    
    /**
     * 获取个性化推荐民宿
     * 基于用户历史行为和偏好
     * 
     * @param userId 用户ID
     * @param limit 返回数量限制
     * @return 个性化推荐民宿列表
     */
    List<HomestaySummaryDTO> getPersonalizedRecommendations(Long userId, int limit);
    
    /**
     * 获取基于位置的推荐民宿
     * 
     * @param provinceCode 省份代码
     * @param cityCode 城市代码
     * @param limit 返回数量限制
     * @return 基于位置的推荐民宿列表
     */
    List<HomestaySummaryDTO> getLocationBasedRecommendations(String provinceCode, String cityCode, int limit);
    
    /**
     * 获取相似民宿推荐
     * 
     * @param homestayId 基准民宿ID
     * @param limit 返回数量限制
     * @return 相似民宿列表
     */
    List<HomestaySummaryDTO> getSimilarHomestays(Long homestayId, int limit);
    
    /**
     * 基于用户请求获取推荐
     * 
     * @param request 用户推荐请求
     * @return 推荐民宿列表
     */
    List<HomestaySummaryDTO> getRecommendationsByRequest(UserRecommendationRequest request);
    
    /**
     * 刷新推荐缓存
     */
    void refreshRecommendationCache();
} 
