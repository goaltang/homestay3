package com.homestay3.homestaybackend.model;

/**
 * 房源状态枚举
 * 定义房源的各种状态及其流转关系
 */
public enum HomestayStatus {
    /**
     * 草稿状态 - 房东正在编辑，尚未提交审核
     */
    DRAFT("草稿"),
    
    /**
     * 待审核 - 房东已提交，等待审核员处理
     */
    PENDING("待审核"),
    

    
    /**
     * 已上架 - 审核通过，房源可被用户搜索和预订
     */
    ACTIVE("已上架"),
    
    /**
     * 已下架 - 房源暂时不可预订（可能是房东主动下架或管理员操作）
     */
    INACTIVE("已下架"),
    
    /**
     * 已拒绝 - 审核不通过，需要房东修改后重新提交
     */
    REJECTED("已拒绝"),
    

    
    /**
     * 已暂停 - 因违规等原因被管理员暂停
     */
    SUSPENDED("已暂停");
    
    private final String description;
    
    HomestayStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 获取所有可以直接提交审核的状态
     */
    public static HomestayStatus[] getSubmittableStatuses() {
        return new HomestayStatus[]{DRAFT, REJECTED};
    }
    
    /**
     * 获取所有需要审核的状态
     */
    public static HomestayStatus[] getReviewableStatuses() {
        return new HomestayStatus[]{PENDING};
    }
    
    /**
     * 判断是否可以从当前状态转换到目标状态
     */
    public boolean canTransitionTo(HomestayStatus targetStatus) {
        switch (this) {
            case DRAFT:
                return targetStatus == PENDING;
            case PENDING:
                return targetStatus == ACTIVE || targetStatus == REJECTED || targetStatus == DRAFT;
            case ACTIVE:
                return targetStatus == INACTIVE || targetStatus == SUSPENDED;
            case INACTIVE:
                return targetStatus == ACTIVE || targetStatus == PENDING;
            case REJECTED:
                return targetStatus == PENDING || targetStatus == DRAFT;
            case SUSPENDED:
                return targetStatus == ACTIVE || targetStatus == INACTIVE;
            default:
                return false;
        }
    }
} 