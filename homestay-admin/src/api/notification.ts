import request from "@/utils/request";

export interface NotificationDto {
  id: number;
  userId: number;
  actorId: number | null;
  type: string;
  entityType: string | null;
  entityId: string | null;
  content: string;
  read: boolean;
  readAt: string | null;
  createdAt: string;
  actorUsername?: string;
  entityTitle?: string;
}

export interface NotificationListResponse {
  content: NotificationDto[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface UnreadCountResponse {
  unreadCount: number;
}

/**
 * 获取用户通知列表
 */
export function getNotifications(params: {
  page?: number;
  size?: number;
  isRead?: boolean;
  type?: string;
}) {
  return request<NotificationListResponse>({
    url: "/api/notifications",
    method: "get",
    params,
  });
}

/**
 * 获取未读通知数量
 */
export function getUnreadCount() {
  return request<UnreadCountResponse>({
    url: "/api/notifications/unread-count",
    method: "get",
  });
}

/**
 * 标记通知为已读
 */
export function markAsRead(notificationId: number) {
  return request({
    url: `/api/notifications/${notificationId}/read`,
    method: "post",
  });
}

/**
 * 标记所有通知为已读
 */
export function markAllAsRead() {
  return request({
    url: "/api/notifications/read-all",
    method: "post",
  });
}

/**
 * 删除通知
 */
export function deleteNotification(notificationId: number) {
  return request({
    url: `/api/notifications/${notificationId}`,
    method: "delete",
  });
}

/**
 * 获取通知类型的显示名称
 */
export function getNotificationTypeDisplayName(type: string): string {
  const typeMap: Record<string, string> = {
    BOOKING_REQUEST: "预订请求",
    BOOKING_ACCEPTED: "预订已接受",
    BOOKING_REJECTED: "预订已拒绝",
    BOOKING_CANCELLED: "预订已取消",
    NEW_REVIEW: "新评价",
    HOMESTAY_APPROVED: "房源审核通过",
    HOMESTAY_REJECTED: "房源审核未通过",
    HOMESTAY_SUBMITTED: "房源提交审核",
    REVIEWER_ASSIGNED: "审核员分配",
    HOMESTAY_UNDER_REVIEW: "房源审核中",
    HOMESTAY_RESUBMITTED: "房源重新提交",
    SYSTEM_ANNOUNCEMENT: "系统公告",
    WELCOME_MESSAGE: "欢迎消息",
  };
  return typeMap[type] || type;
}

/**
 * 格式化相对时间
 */
export function formatRelativeTime(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

  if (diffInSeconds < 60) {
    return "刚刚";
  } else if (diffInSeconds < 3600) {
    return `${Math.floor(diffInSeconds / 60)}分钟前`;
  } else if (diffInSeconds < 86400) {
    return `${Math.floor(diffInSeconds / 3600)}小时前`;
  } else if (diffInSeconds < 2592000) {
    return `${Math.floor(diffInSeconds / 86400)}天前`;
  } else {
    return date.toLocaleDateString("zh-CN");
  }
}
