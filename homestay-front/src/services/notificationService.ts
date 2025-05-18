import request from "@/utils/request"; // 导入封装好的 axios 实例
import type {
  NotificationApiResponse,
  NotificationDto,
} from "@/types/notification";

// 定义通用的分页参数类型 (如果项目中还没有)
interface PageParams {
  page: number;
  size: number;
}

/**
 * 获取通知列表
 * @param params - 查询参数，包含分页和可选的 isRead 状态
 */
export const getNotifications = (
  params: PageParams & { isRead?: boolean }
): Promise<NotificationApiResponse> => {
  // 后端分页是从0开始，前端通常从1开始，这里进行转换
  const adjustedParams = {
    ...params,
    page: params.page > 0 ? params.page - 1 : 0, // 页码转为0-based
  };
  return request({
    url: "/api/notifications",
    method: "get",
    params: adjustedParams,
  });
};

/**
 * 将单个通知标记为已读
 * @param notificationId - 通知 ID
 */
export const markAsRead = (notificationId: number): Promise<void> => {
  return request({
    url: "/api/notifications/" + notificationId + "/read",
    method: "post",
  });
};

/**
 * 将所有未读通知标记为已读
 */
export const markAllAsRead = (): Promise<{ markedCount: number }> => {
  return request({
    url: "/api/notifications/read-all",
    method: "post",
  });
};

/**
 * 删除单个通知
 * @param notificationId - 通知 ID
 */
export const deleteNotification = (notificationId: number): Promise<void> => {
  return request({
    url: "/api/notifications/" + notificationId,
    method: "delete",
  });
};

/**
 * 获取未读通知数量
 */
export const getUnreadCount = (): Promise<{ unreadCount: number }> => {
  return request({
    url: "/api/notifications/unread-count",
    method: "get",
  });
};
