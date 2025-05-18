package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.HostDTO;
import com.homestay3.homestaybackend.dto.HostStatisticsDTO;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import com.homestay3.homestaybackend.dto.HomestayOptionDTO;

public interface HostService {
    /**
     * 获取房东信息
     * @param username 用户名
     * @return 房东信息DTO
     */
    HostDTO getHostInfo(String username);
    
    /**
     * 更新房东信息
     * @param hostDTO 房东信息DTO
     * @param username 用户名
     * @return 更新后的房东信息DTO
     */
    HostDTO updateHostInfo(HostDTO hostDTO, String username);
    
    /**
     * 上传房东头像
     * @param file 头像文件
     * @param username 用户名
     * @return 头像URL
     */
    String uploadAvatar(MultipartFile file, String username);
    
    /**
     * 获取房东统计数据
     * @param username 用户名
     * @return 统计数据DTO
     */
    HostStatisticsDTO getHostStatistics(String username);
    
    /**
     * 获取房东房源列表
     * @param username 用户名
     * @param page 页码
     * @param size 每页大小
     * @return 分页的房源列表
     */
    Page<?> getHostHomestays(String username, int page, int size);
    
    /**
     * 获取房东订单列表
     * @param username 用户名
     * @param page 页码
     * @param size 每页大小
     * @param status 订单状态
     * @return 分页的订单列表
     */
    Page<?> getHostOrders(String username, int page, int size, String status);
    
    /**
     * 获取房东评价列表
     * @param username 用户名
     * @param page 页码
     * @param size 每页大小
     * @return 分页的评价列表
     */
    Page<?> getHostReviews(String username, int page, int size);
    
    /**
     * 获取房东的房源选项列表 (用于下拉菜单)
     * @param username 房东用户名
     * @return 只包含 id 和 title 的房源列表
     */
    List<HomestayOptionDTO> getHostHomestayOptions(String username);
    
    /**
     * 获取房源的房东信息
     * @param homestayId 房源ID
     * @return 房东信息DTO
     */
    HostDTO getHomestayHostInfo(Long homestayId);

    /**
     * 更新房东个人资料
     * @param username 用户名
     * @param profileData 个人资料数据
     * @return 更新后的个人资料
     */
    Map<String, Object> updateHostProfile(String username, Map<String, Object> profileData);

    /**
     * 获取房东个人资料
     * @param username 用户名
     * @return 房东个人资料
     */
    Map<String, Object> getHostProfile(String username);

    /**
     * 上传房东证件照片
     * @param username 用户名
     * @param file 文件
     * @param type 文件类型
     * @return 上传后的文件URL
     */
    String uploadHostDocument(String username, MultipartFile file, String type) throws IOException;
} 