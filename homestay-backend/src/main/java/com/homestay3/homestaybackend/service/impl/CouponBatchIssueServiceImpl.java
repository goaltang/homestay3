package com.homestay3.homestaybackend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay3.homestaybackend.entity.CouponBatchIssueItem;
import com.homestay3.homestaybackend.entity.CouponBatchIssueTask;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.repository.CouponBatchIssueItemRepository;
import com.homestay3.homestaybackend.repository.CouponBatchIssueTaskRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.CouponBatchIssueService;
import com.homestay3.homestaybackend.service.CouponService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponBatchIssueServiceImpl implements CouponBatchIssueService {

    private final CouponBatchIssueTaskRepository taskRepository;
    private final CouponBatchIssueItemRepository itemRepository;
    private final UserRepository userRepository;
    private final CouponService couponService;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public CouponBatchIssueTask createBatchTask(Long templateId, String name, String filterType,
                                                 Map<String, Object> filterParams, Long createdBy) {
        // 1. 查询目标用户
        List<Long> userIds = queryTargetUsers(filterType, filterParams);

        CouponBatchIssueTask task = CouponBatchIssueTask.builder()
                .templateId(templateId)
                .name(name)
                .filterType(filterType)
                .filterParamsJson(toJson(filterParams))
                .totalCount(userIds.size())
                .successCount(0)
                .failCount(0)
                .status("PENDING")
                .createdBy(createdBy)
                .build();
        task = taskRepository.save(task);

        // 2. 批量创建明细（每 500 条一批插入）
        if (!userIds.isEmpty()) {
            List<CouponBatchIssueItem> items = new ArrayList<>();
            for (Long userId : userIds) {
                items.add(CouponBatchIssueItem.builder()
                        .taskId(task.getId())
                        .userId(userId)
                        .status("PENDING")
                        .build());
                if (items.size() >= 500) {
                    itemRepository.saveAll(items);
                    items.clear();
                }
            }
            if (!items.isEmpty()) {
                itemRepository.saveAll(items);
            }
        }

        return task;
    }

    @Override
    @Async("taskExecutor")
    @Transactional
    public void executeBatchTask(Long taskId) {
        CouponBatchIssueTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null || !"PENDING".equals(task.getStatus())) {
            log.warn("批量发券任务不存在或状态不正确，taskId={}", taskId);
            return;
        }

        task.setStatus("PROCESSING");
        taskRepository.save(task);
        log.info("开始执行批量发券任务，taskId={}, totalCount={}", taskId, task.getTotalCount());

        int success = 0;
        int fail = 0;
        int pageSize = 200;

        while (true) {
            // 始终从 offset=0 查询，因为已处理的记录状态已变更，会自然"上浮"
            List<CouponBatchIssueItem> items = entityManager.createQuery(
                            "SELECT i FROM CouponBatchIssueItem i WHERE i.taskId = :taskId AND i.status = 'PENDING'",
                            CouponBatchIssueItem.class)
                    .setParameter("taskId", taskId)
                    .setFirstResult(0)
                    .setMaxResults(pageSize)
                    .getResultList();

            if (items.isEmpty()) {
                break;
            }

            for (CouponBatchIssueItem item : items) {
                try {
                    couponService.claimCoupon(item.getUserId(), task.getTemplateId());
                    item.setStatus("SUCCESS");
                    success++;
                } catch (Exception e) {
                    item.setStatus("FAILED");
                    item.setErrorMsg(truncate(e.getMessage(), 500));
                    fail++;
                    log.warn("批量发券失败，taskId={}, userId={}, 原因={}", taskId, item.getUserId(), e.getMessage());
                }
            }
            itemRepository.saveAll(items);

            // 更新任务进度
            task.setSuccessCount(success);
            task.setFailCount(fail);
            taskRepository.save(task);
        }

        task.setStatus("COMPLETED");
        task.setSuccessCount(success);
        task.setFailCount(fail);
        taskRepository.save(task);
        log.info("批量发券任务完成，taskId={}, success={}, fail={}", taskId, success, fail);
    }

    @Override
    public Page<CouponBatchIssueTask> getTaskList(String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return taskRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return taskRepository.findAll(pageable);
    }

    @Override
    public Map<String, Object> getTaskDetail(Long taskId) {
        CouponBatchIssueTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("task", task);
        result.put("pendingCount", itemRepository.countByTaskIdAndStatus(taskId, "PENDING"));
        result.put("successCount", itemRepository.countByTaskIdAndStatus(taskId, "SUCCESS"));
        result.put("failCount", itemRepository.countByTaskIdAndStatus(taskId, "FAILED"));
        return result;
    }

    @Override
    public Page<CouponBatchIssueItem> getTaskItems(Long taskId, String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return itemRepository.findByTaskIdAndStatus(taskId, status, pageable);
        }
        return itemRepository.findByTaskId(taskId, pageable);
    }

    @Override
    @Async("taskExecutor")
    @Transactional
    public void retryFailedItems(Long taskId) {
        CouponBatchIssueTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null || !"COMPLETED".equals(task.getStatus())) {
            log.warn("任务不存在或未完成，无法重试，taskId={}", taskId);
            return;
        }

        List<CouponBatchIssueItem> failedItems = itemRepository.findByTaskIdAndStatus(taskId, "FAILED");
        if (failedItems.isEmpty()) {
            log.info("没有失败项需要重试，taskId={}", taskId);
            return;
        }

        log.info("开始重试失败项，taskId={}, failedCount={}", taskId, failedItems.size());
        int success = 0;
        int fail = 0;

        for (CouponBatchIssueItem item : failedItems) {
            try {
                couponService.claimCoupon(item.getUserId(), task.getTemplateId());
                item.setStatus("SUCCESS");
                item.setErrorMsg(null);
                success++;
            } catch (Exception e) {
                item.setStatus("FAILED");
                item.setErrorMsg(e.getMessage());
                fail++;
                log.warn("重试发券失败，taskId={}, userId={}, 原因={}", taskId, item.getUserId(), e.getMessage());
            }
        }
        itemRepository.saveAll(failedItems);

        // 更新任务统计：所有失败项都被重新处理，fail 就是当前失败总数
        task.setSuccessCount(task.getSuccessCount() + success);
        task.setFailCount(fail);
        taskRepository.save(task);
        log.info("重试完成，taskId={}, success={}, stillFail={}", taskId, success, fail);
    }

    @SuppressWarnings("unchecked")
    private List<Long> queryTargetUsers(String filterType, Map<String, Object> filterParams) {
        Query query;
        switch (filterType) {
            case "ALL":
                query = entityManager.createNativeQuery("SELECT id FROM users WHERE status = 'ACTIVE'");
                break;
            case "NEW_USER": {
                int newUserDays = getIntParam(filterParams, "days", 7);
                query = entityManager.createNativeQuery(
                        "SELECT id FROM users WHERE status = 'ACTIVE' AND created_at >= :cutoff");
                query.setParameter("cutoff", LocalDateTime.now().minusDays(newUserDays));
                break;
            }
            case "AT_RISK": {
                int inactiveDays = getIntParam(filterParams, "days", 30);
                query = entityManager.createNativeQuery(
                        "SELECT u.id FROM users u LEFT JOIN user_preference_profile p ON u.id = p.user_id " +
                        "WHERE u.status = 'ACTIVE' AND (p.last_active_at IS NULL OR p.last_active_at < :cutoff)");
                query.setParameter("cutoff", LocalDateTime.now().minusDays(inactiveDays));
                break;
            }
            case "NO_ORDER_30D": {
                int noOrderDays = getIntParam(filterParams, "days", 30);
                query = entityManager.createNativeQuery(
                        "SELECT u.id FROM users u WHERE u.status = 'ACTIVE' AND u.id NOT IN " +
                        "(SELECT DISTINCT guest_id FROM orders WHERE created_at >= :cutoff AND guest_id IS NOT NULL)");
                query.setParameter("cutoff", LocalDateTime.now().minusDays(noOrderDays));
                break;
            }
            case "HIGH_VALUE": {
                BigDecimal minAmount = getBigDecimalParam(filterParams, "minAmount", new BigDecimal("1000"));
                query = entityManager.createNativeQuery(
                        "SELECT guest_id FROM orders WHERE status IN ('PAID','CHECKED_IN','CHECKED_OUT','COMPLETED') " +
                        "GROUP BY guest_id HAVING SUM(total_amount) >= :minAmount");
                query.setParameter("minAmount", minAmount);
                break;
            }
            default:
                query = entityManager.createNativeQuery("SELECT id FROM users WHERE status = 'ACTIVE'");
        }

        List<Number> result = query.getResultList();
        return result.stream().map(Number::longValue).distinct().toList();
    }

    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        if (params == null) return defaultValue;
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private BigDecimal getBigDecimalParam(Map<String, Object> params, String key, BigDecimal defaultValue) {
        if (params == null) return defaultValue;
        Object value = params.get(key);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null || str.length() <= maxLen) return str;
        return str.substring(0, maxLen);
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}
