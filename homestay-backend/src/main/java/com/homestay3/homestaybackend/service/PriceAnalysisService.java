package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.PriceCompetitivenessDTO;
import com.homestay3.homestaybackend.entity.Homestay;

/**
 * 价格分析服务接口
 */
public interface PriceAnalysisService {
    
    /**
     * 分析房源的价格竞争力
     * @param homestay 要分析的房源
     * @return 价格竞争力分析结果
     */
    PriceCompetitivenessDTO analyzePriceCompetitiveness(Homestay homestay);
    
    /**
     * 判断当前是否为旺季
     * @param cityCode 城市代码
     * @return 是否为旺季
     */
    boolean isSeasonalPeak(String cityCode);
    
    /**
     * 获取季节性调整因子
     * @param cityCode 城市代码
     * @param month 月份
     * @return 调整因子 (1.0为基准)
     */
    double getSeasonalAdjustmentFactor(String cityCode, int month);
} 