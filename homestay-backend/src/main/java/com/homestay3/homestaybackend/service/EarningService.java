package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.EarningDTO;
import com.homestay3.homestaybackend.dto.EarningsSummaryDTO;
import com.homestay3.homestaybackend.dto.EarningsTrendDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface EarningService {
    
    // 获取收益明细
    Page<EarningDTO> getEarningsDetail(String hostUsername, LocalDate startDate, LocalDate endDate, 
                                        Long homestayId, Pageable pageable);
    
    // 获取收益汇总
    EarningsSummaryDTO getEarningsSummary(String hostUsername, LocalDate startDate, LocalDate endDate, 
                                          Long homestayId);
    
    // 获取收益趋势
    EarningsTrendDTO getEarningsTrend(String hostUsername, LocalDate startDate, LocalDate endDate, 
                                      String type);
    
    // 获取月度收益
    BigDecimal getMonthlyEarnings(String hostUsername);
    
    // 获取待结算收益
    BigDecimal getPendingEarnings(String hostUsername);
    
    // 根据订单创建收益记录（通常在订单完成时调用）
    EarningDTO generatePendingEarningForOrder(Long orderId);
    
    // 结算指定房东的合格收益
    int settleHostEarnings(String hostUsername); // 返回结算的记录条数
} 