package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.RoiCampaignDTO;
import com.homestay3.homestaybackend.dto.RoiOverviewDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ROI 分析服务
 */
public interface RoiAnalysisService {

    /**
     * 平台级 ROI 概览
     *
     * @param startDate 开始时间（可选）
     * @param endDate   结束时间（可选）
     * @return ROI 概览数据
     */
    RoiOverviewDTO getPlatformRoiOverview(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 平台级活动 ROI 排行
     *
     * @param startDate 开始时间（可选）
     * @param endDate   结束时间（可选）
     * @param limit     返回条数
     * @return 活动 ROI 列表
     */
    List<RoiCampaignDTO> getPlatformCampaignRoi(LocalDateTime startDate, LocalDateTime endDate, int limit);

    /**
     * 房东级 ROI 概览
     *
     * @param hostId    房东ID
     * @param startDate 开始时间（可选）
     * @param endDate   结束时间（可选）
     * @return ROI 概览数据
     */
    RoiOverviewDTO getHostRoiOverview(Long hostId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 房东级活动 ROI 列表
     *
     * @param hostId    房东ID
     * @param startDate 开始时间（可选）
     * @param endDate   结束时间（可选）
     * @return 活动 ROI 列表
     */
    List<RoiCampaignDTO> getHostCampaignRoi(Long hostId, LocalDateTime startDate, LocalDateTime endDate);
}
