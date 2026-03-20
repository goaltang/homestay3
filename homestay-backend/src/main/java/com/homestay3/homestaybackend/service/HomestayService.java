package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.dto.HomestayRequest;
import com.homestay3.homestaybackend.dto.HomestaySearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface HomestayService {
    
    /**
     * 获取所有房源
     */
    List<HomestayDTO> getAllHomestays();
    
    /**
     * 获取推荐房源 (已废弃，请使用HomestayRecommendationService)
     * @deprecated 使用 HomestayRecommendationService.getRecommendedHomestays() 替代
     */
    @Deprecated
    List<HomestayDTO> getFeaturedHomestays();
    
    /**
     * 根据ID获取房源详情
     * @param id 房源ID
     * @param referringSearchCriteria 用户找到此房源时使用的搜索条件，用于优先显示匹配的特色
     * @return 房源DTO
     */
    HomestayDTO getHomestayById(Long id, List<String> referringSearchCriteria);
    
    /**
     * 根据房源类型获取房源列表
     */
    List<HomestayDTO> getHomestaysByPropertyType(String propertyType);
    
    /**
     * 搜索房源
     */
    List<HomestayDTO> searchHomestays(HomestaySearchRequest request);
    
    /**
     * 上传民宿图片
     */
    String uploadHomestayImage(MultipartFile file);
    
    /**
     * 分页获取民宿
     */
    Page<HomestayDTO> getHomestaysByPage(Pageable pageable);
    
    /**
     * 获取用户拥有的民宿
     */
    List<HomestayDTO> getHomestaysByOwner(String username);
    
    /**
     * 创建民宿
     */
    HomestayDTO createHomestay(HomestayDTO homestayDTO, String ownerUsername);
    
    /**
     * 更新民宿
     */
    HomestayDTO updateHomestay(Long id, HomestayDTO homestayDTO);
    
    /**
     * 搜索民宿
     */
    List<HomestayDTO> searchHomestays(String keyword, String provinceCode, String cityCode,
                                     Integer minPrice, Integer maxPrice,
                                     Integer guests, String type);
    
    /**
     * 删除民宿
     */
    void deleteHomestay(Long id);
    
    /**
     * 更新民宿状态
     */
    HomestayDTO updateHomestayStatus(Long id, String status, String ownerUsername);
    
    /**
     * 管理员获取民宿列表（分页和筛选）
     */
    Page<HomestayDTO> getAdminHomestays(Pageable pageable, String title, String status, String type);
    
    /**
     * 管理员创建民宿
     */
    HomestayDTO createHomestay(HomestayDTO homestayDTO);
    
    /**
     * 管理员更新民宿状态
     */
    void updateHomestayStatus(Long id, String status);
    
    /**
     * 检查房源是否可以提交审核
     */
    boolean checkHomestayReadyForReview(Long homestayId);
    
    /**
     * 获取房源审核就绪状态的详细信息
     */
    String getHomestayReviewReadinessDetails(Long homestayId);
    
    /**
     * 获取房源详情（包含完整房东信息）
     * @param id 房源ID
     * @return 包含完整房东信息的房源DTO
     */
    HomestayDTO getHomestayWithOwnerDetails(Long id);
    
    /**
     * 获取房源不可用日期
     * @param homestayId 房源ID
     * @return 不可用日期列表
     */
    List<LocalDate> getUnavailableDates(Long homestayId);
    
    /**
     * 强制下架房源（因违规）
     * @param id 房源ID
     * @param reason 下架原因
     * @param notes 备注
     * @param violationType 违规类型
     */
    void forceDelistHomestay(Long id, String reason, String notes, String violationType);
} 