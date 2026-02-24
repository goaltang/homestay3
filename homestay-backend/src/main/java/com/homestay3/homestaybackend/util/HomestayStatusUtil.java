package com.homestay3.homestaybackend.util;

import com.homestay3.homestaybackend.model.HomestayStatus;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 房源状态管理工具类
 * 提供状态转换验证、状态描述等功能
 */
@UtilityClass
public class HomestayStatusUtil {

    /**
     * 状态中文描述映射
     */
    private static final Map<HomestayStatus, String> STATUS_DESCRIPTIONS = Map.of(
        HomestayStatus.DRAFT, "草稿",
        HomestayStatus.PENDING, "待审核",
        HomestayStatus.ACTIVE, "已上架",
        HomestayStatus.INACTIVE, "已下架",
        HomestayStatus.REJECTED, "已拒绝",
        HomestayStatus.SUSPENDED, "已暂停"
    );

    /**
     * 获取状态的中文描述
     */
    public static String getStatusDescription(HomestayStatus status) {
        return STATUS_DESCRIPTIONS.getOrDefault(status, "未知状态");
    }

    /**
     * 根据字符串获取状态枚举
     */
    public static HomestayStatus fromString(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            return HomestayStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 检查状态是否为有效状态
     */
    public static boolean isValidStatus(String statusStr) {
        return fromString(statusStr) != null;
    }

    /**
     * 获取所有可用状态
     */
    public static List<HomestayStatus> getAllStatuses() {
        return Arrays.asList(HomestayStatus.values());
    }

    /**
     * 获取可以提交审核的状态
     */
    public static List<HomestayStatus> getSubmittableStatuses() {
        return Arrays.asList(HomestayStatus.DRAFT, HomestayStatus.REJECTED);
    }

    /**
     * 获取需要审核的状态
     */
    public static List<HomestayStatus> getReviewableStatuses() {
        return Arrays.asList(HomestayStatus.PENDING);
    }

    /**
     * 获取正常运营的状态
     */
    public static List<HomestayStatus> getActiveStatuses() {
        return Arrays.asList(HomestayStatus.ACTIVE);
    }

    /**
     * 检查状态是否允许用户预订
     */
    public static boolean isBookableStatus(HomestayStatus status) {
        return status == HomestayStatus.ACTIVE;
    }

    /**
     * 检查状态是否需要管理员处理
     */
    public static boolean requiresAdminAction(HomestayStatus status) {
        return status == HomestayStatus.PENDING;
    }

    /**
     * 获取状态对应的CSS类名（用于前端显示）
     */
    public static String getStatusCssClass(HomestayStatus status) {
        switch (status) {
            case DRAFT:
                return "status-draft";
            case PENDING:
                return "status-warning";
            case ACTIVE:
                return "status-success";
            case INACTIVE:
                return "status-secondary";
            case REJECTED:
                return "status-danger";
            case SUSPENDED:
                return "status-dark";
            default:
                return "status-default";
        }
    }

    /**
     * 获取状态流转的建议下一步操作
     */
    public static String getNextActionSuggestion(HomestayStatus currentStatus) {
        switch (currentStatus) {
            case DRAFT:
                return "完善房源信息后提交审核";
            case PENDING:
                return "等待管理员审核";
            case ACTIVE:
                return "房源正常运营中";
            case INACTIVE:
                return "可以重新上架或编辑房源";
            case REJECTED:
                return "根据拒绝原因修改后重新提交";
            case SUSPENDED:
                return "联系客服了解暂停原因";
            default:
                return "状态异常，请联系客服";
        }
    }

    /**
     * 获取状态的优先级（用于排序）
     */
    public static int getStatusPriority(HomestayStatus status) {
        switch (status) {
            case PENDING:
                return 1;
            case REJECTED:
                return 2;
            case ACTIVE:
                return 3;
            case INACTIVE:
                return 4;
            case SUSPENDED:
                return 5;
            case DRAFT:
                return 6;
            default:
                return 99;
        }
    }
} 