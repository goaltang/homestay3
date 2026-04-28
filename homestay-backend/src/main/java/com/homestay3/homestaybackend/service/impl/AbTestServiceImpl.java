package com.homestay3.homestaybackend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay3.homestaybackend.entity.*;
import com.homestay3.homestaybackend.repository.*;
import com.homestay3.homestaybackend.service.AbTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbTestServiceImpl implements AbTestService {

    private final AbExperimentRepository experimentRepository;
    private final AbVariantRepository variantRepository;
    private final AbAssignmentRepository assignmentRepository;
    private final AbEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AbExperiment createExperiment(AbExperiment experiment, List<Map<String, Object>> variants) {
        experiment.setStatus("DRAFT");
        AbExperiment saved = experimentRepository.save(experiment);

        if (variants != null) {
            for (Map<String, Object> v : variants) {
                AbVariant variant = AbVariant.builder()
                        .experimentId(saved.getId())
                        .variantKey((String) v.get("variantKey"))
                        .name((String) v.get("name"))
                        .trafficRatio((Integer) v.getOrDefault("trafficRatio", 50))
                        .configJson(toJson(v.get("config")))
                        .build();
                variantRepository.save(variant);
            }
        }
        return saved;
    }

    @Override
    @Transactional
    public AbExperiment updateExperiment(Long id, AbExperiment experiment) {
        AbExperiment existing = experimentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("实验不存在"));
        if (!"DRAFT".equals(existing.getStatus())) {
            throw new IllegalArgumentException("只有草稿状态的实验可以编辑");
        }
        existing.setName(experiment.getName());
        existing.setDescription(experiment.getDescription());
        existing.setTargetId(experiment.getTargetId());
        existing.setExperimentType(experiment.getExperimentType());
        existing.setStartAt(experiment.getStartAt());
        existing.setEndAt(experiment.getEndAt());
        existing.setTrafficPercent(experiment.getTrafficPercent());
        existing.setPrimaryMetric(experiment.getPrimaryMetric());
        return experimentRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteExperiment(Long id) {
        AbExperiment exp = experimentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("实验不存在"));
        if (!"DRAFT".equals(exp.getStatus()) && !"ENDED".equals(exp.getStatus())) {
            throw new IllegalArgumentException("只能删除草稿或已结束状态的实验");
        }
        experimentRepository.delete(exp);
    }

    @Override
    @Transactional
    public AbExperiment startExperiment(Long id) {
        AbExperiment exp = experimentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("实验不存在"));
        List<AbVariant> variants = variantRepository.findByExperimentId(id);
        if (variants.size() < 2) {
            throw new IllegalArgumentException("至少需要2个实验组才能启动");
        }
        int totalRatio = variants.stream().mapToInt(AbVariant::getTrafficRatio).sum();
        if (totalRatio != 100) {
            throw new IllegalArgumentException("实验组流量占比之和必须等于100");
        }
        exp.setStatus("RUNNING");
        if (exp.getStartAt() == null) {
            exp.setStartAt(LocalDateTime.now());
        }
        return experimentRepository.save(exp);
    }

    @Override
    @Transactional
    public AbExperiment stopExperiment(Long id) {
        AbExperiment exp = experimentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("实验不存在"));
        exp.setStatus("ENDED");
        exp.setEndAt(LocalDateTime.now());
        return experimentRepository.save(exp);
    }

    @Override
    public Page<AbExperiment> getExperiments(String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            // 简单实现，可用 Specification
            return experimentRepository.findAll(pageable);
        }
        return experimentRepository.findAll(pageable);
    }

    @Override
    public Map<String, Object> getExperimentDetail(Long id) {
        AbExperiment exp = experimentRepository.findById(id).orElse(null);
        if (exp == null) return Collections.emptyMap();
        Map<String, Object> result = new HashMap<>();
        result.put("experiment", exp);
        result.put("variants", variantRepository.findByExperimentId(id));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> assignVariant(Long experimentId, Long userId) {
        AbExperiment exp = experimentRepository.findById(experimentId).orElse(null);
        if (exp == null || !"RUNNING".equals(exp.getStatus())) {
            return Map.of("assigned", false, "reason", "实验不存在或未运行");
        }

        // 检查用户是否已有分配记录
        Optional<AbAssignment> existing = assignmentRepository.findByExperimentIdAndUserId(experimentId, userId);
        if (existing.isPresent()) {
            AbVariant variant = variantRepository.findById(existing.get().getVariantId()).orElse(null);
            return Map.of("assigned", true, "variantId", existing.get().getVariantId(),
                    "variantKey", variant != null ? variant.getVariantKey() : null);
        }

        // 流量控制：根据 userId hash 判断是否进入实验
        int userHash = (userId.hashCode() & 0x7fffffff) % 100;
        if (userHash >= exp.getTrafficPercent()) {
            return Map.of("assigned", false, "reason", "未命中实验流量");
        }

        List<AbVariant> variants = variantRepository.findByExperimentId(experimentId);
        if (variants.isEmpty()) {
            return Map.of("assigned", false, "reason", "实验组配置为空");
        }

        // 按 trafficRatio 分配
        int bucket = ((userId + "_" + experimentId).hashCode() & 0x7fffffff) % 100;
        int cumulative = 0;
        AbVariant selected = variants.get(0);
        for (AbVariant v : variants) {
            cumulative += v.getTrafficRatio();
            if (bucket < cumulative) {
                selected = v;
                break;
            }
        }

        AbAssignment assignment = AbAssignment.builder()
                .experimentId(experimentId)
                .variantId(selected.getId())
                .userId(userId)
                .build();
        assignmentRepository.save(assignment);

        return Map.of("assigned", true, "variantId", selected.getId(),
                "variantKey", selected.getVariantKey());
    }

    @Override
    @Transactional
    public void trackEvent(Long experimentId, Long variantId, Long userId, String eventType, BigDecimal eventValue) {
        try {
            AbEvent event = AbEvent.builder()
                    .experimentId(experimentId)
                    .variantId(variantId)
                    .userId(userId)
                    .eventType(eventType)
                    .eventValue(eventValue)
                    .build();
            eventRepository.save(event);
        } catch (Exception e) {
            log.warn("AB实验埋点失败: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getExperimentReport(Long id) {
        AbExperiment exp = experimentRepository.findById(id).orElse(null);
        if (exp == null) return Collections.emptyMap();

        List<AbVariant> variants = variantRepository.findByExperimentId(id);
        LocalDateTime start = exp.getStartAt() != null ? exp.getStartAt() : LocalDateTime.now().minusDays(30);
        LocalDateTime end = exp.getEndAt() != null ? exp.getEndAt() : LocalDateTime.now();

        List<Object[]> rows = eventRepository.aggregateByExperiment(id, start, end);

        // 整理数据: variantId -> eventType -> {count, distinctUsers, sumValue}
        Map<Long, Map<String, Map<String, Number>>> data = new HashMap<>();
        for (Object[] row : rows) {
            Long vId = ((Number) row[0]).longValue();
            String eventType = (String) row[1];
            Long count = ((Number) row[2]).longValue();
            Long distinctUsers = ((Number) row[3]).longValue();
            BigDecimal sumValue = (BigDecimal) row[4];

            data.computeIfAbsent(vId, k -> new HashMap<>())
                    .put(eventType, Map.of("count", count, "distinctUsers", distinctUsers, "sumValue", sumValue));
        }

        // 组装报告
        Map<String, Object> controlStats = null;
        List<Map<String, Object>> variantReports = new ArrayList<>();

        for (AbVariant variant : variants) {
            Map<String, Map<String, Number>> events = data.getOrDefault(variant.getId(), Collections.emptyMap());
            long impression = getDistinctUsers(events, "IMPRESSION");
            long click = getDistinctUsers(events, "CLICK");
            long convert = getDistinctUsers(events, "CONVERT");
            long order = getDistinctUsers(events, "ORDER");
            BigDecimal gmv = getSumValue(events, "ORDER");

            double conversionRate = impression > 0 ? (double) convert / impression : 0;
            double orderRate = impression > 0 ? (double) order / impression : 0;

            Map<String, Object> report = new LinkedHashMap<>();
            report.put("variantId", variant.getId());
            report.put("variantKey", variant.getVariantKey());
            report.put("name", variant.getName());
            report.put("trafficRatio", variant.getTrafficRatio());
            report.put("impression", impression);
            report.put("click", click);
            report.put("convert", convert);
            report.put("order", order);
            report.put("gmv", gmv);
            report.put("conversionRate", roundRate(conversionRate));
            report.put("orderRate", roundRate(orderRate));

            if ("control".equals(variant.getVariantKey())) {
                controlStats = report;
            }
            variantReports.add(report);
        }

        // 计算相对提升和p-value
        if (controlStats != null) {
            long controlImp = ((Number) controlStats.get("impression")).longValue();
            long controlConv = ((Number) controlStats.get("convert")).longValue();
            for (Map<String, Object> report : variantReports) {
                if ("control".equals(report.get("variantKey"))) continue;

                long vImp = ((Number) report.get("impression")).longValue();
                long vConv = ((Number) report.get("convert")).longValue();

                double controlRate = controlImp > 0 ? (double) controlConv / controlImp : 0;
                double variantRate = vImp > 0 ? (double) vConv / vImp : 0;
                double lift = controlRate > 0 ? (variantRate - controlRate) / controlRate : 0;

                report.put("liftRate", roundRate(lift));
                report.put("pValue", calcPValue(controlImp, controlConv, vImp, vConv));
                report.put("isSignificant", report.get("pValue") != null && ((BigDecimal) report.get("pValue")).doubleValue() < 0.05);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("experiment", exp);
        result.put("variants", variantReports);
        result.put("control", controlStats);
        return result;
    }

    private long getDistinctUsers(Map<String, Map<String, Number>> events, String eventType) {
        Map<String, Number> event = events.get(eventType);
        if (event == null) return 0;
        return ((Number) event.get("distinctUsers")).longValue();
    }

    private BigDecimal getSumValue(Map<String, Map<String, Number>> events, String eventType) {
        Map<String, Number> event = events.get(eventType);
        if (event == null) return BigDecimal.ZERO;
        return (BigDecimal) event.get("sumValue");
    }

    private BigDecimal roundRate(double rate) {
        return BigDecimal.valueOf(rate).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 两比例Z检验计算p-value
     */
    private BigDecimal calcPValue(long n1, long x1, long n2, long x2) {
        if (n1 == 0 || n2 == 0) return BigDecimal.ONE;
        double p1 = (double) x1 / n1;
        double p2 = (double) x2 / n2;
        double p = (double) (x1 + x2) / (n1 + n2);
        double se = Math.sqrt(p * (1 - p) * (1.0 / n1 + 1.0 / n2));
        if (se == 0) return BigDecimal.ONE;
        double z = (p2 - p1) / se;
        NormalDistribution nd = new NormalDistribution(0, 1);
        double pValue = 2 * (1 - nd.cumulativeProbability(Math.abs(z)));
        return BigDecimal.valueOf(pValue).setScale(4, RoundingMode.HALF_UP);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
