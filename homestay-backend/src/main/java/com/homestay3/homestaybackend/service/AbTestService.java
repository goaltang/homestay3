package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.entity.AbExperiment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface AbTestService {

    AbExperiment createExperiment(AbExperiment experiment, java.util.List<Map<String, Object>> variants);

    AbExperiment updateExperiment(Long id, AbExperiment experiment);

    void deleteExperiment(Long id);

    AbExperiment startExperiment(Long id);

    AbExperiment stopExperiment(Long id);

    Page<AbExperiment> getExperiments(String status, Pageable pageable);

    Map<String, Object> getExperimentDetail(Long id);

    /**
     * 获取用户所属的实验组
     */
    Map<String, Object> assignVariant(Long experimentId, Long userId);

    /**
     * 记录实验事件
     */
    void trackEvent(Long experimentId, Long variantId, Long userId, String eventType, java.math.BigDecimal eventValue);

    /**
     * 获取实验统计报告
     */
    Map<String, Object> getExperimentReport(Long id);
}
