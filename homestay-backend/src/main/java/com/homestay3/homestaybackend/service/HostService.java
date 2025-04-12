package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.HostDTO;
import com.homestay3.homestaybackend.dto.HostStatisticsDTO;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

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
     * 获取房源的房东信息
     * @param homestayId 房源ID
     * @return 房东信息DTO
     */
    HostDTO getHomestayHostInfo(Long homestayId);
} 