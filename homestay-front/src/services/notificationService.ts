import request from "@/utils/request"; // 导入封装好的 axios 实例
import type {
  NotificationApiResponse,
  NotificationDto,
} from "@/types/notification";
import { normalizeNotification } from "@/types/notification";

// 定义通用的分页参数类型 (如果项目中还没有)
interface PageParams {
  page: number;
  size: number;
}

/**
 * 获取通知列表
 * @param params - 查询参数，包含分页、isRead 状态和可选的 type
 */
export const getNotifications = (
  params: PageParams & { isRead?: boolean; type?: string }
): Promise<NotificationApiResponse> => {
  // 后端分页是从0开始，前端通常从1开始，这里进行转换
  const adjustedParams: any = {
    ...params,
    page: params.page > 0 ? params.page - 1 : 0, // 页码转为0-based
  };
  // 移除 undefined 的参数
  if (!adjustedParams.type) delete adjustedParams.type;
  if (adjustedParams.isRead === undefined) delete adjustedParams.isRead;
  return request({
    url: "/api/notifications",
    method: "get",
    params: adjustedParams,
  }).then((response: NotificationApiResponse) => ({
    ...response,
    data: {
      ...response.data,
      content: (response.data.content || []).map(normalizeNotification),
    },
  }));
};

/**
 * 将单个通知标记为已读
 * @param notificationId - 通知 ID
 */
export const markAsRead = (notificationId: number): Promise<NotificationDto> => {
  return request({
    url: "/api/notifications/" + notificationId + "/read",
    method: "post",
  }).then((response) => normalizeNotification(response.data));
};

/**
 * 将所有未读通知标记为已读
 */
export const markAllAsRead = (): Promise<{ markedCount: number }> => {
  return request({
    url: "/api/notifications/read-all",
    method: "post",
  }).then((response) => response.data);
};

/**
 * 删除单个通知
 * @param notificationId - 通知 ID
 */
export const deleteNotification = (notificationId: number): Promise<void> => {
  return request({
    url: "/api/notifications/" + notificationId,
    method: "delete",
  }).then(() => undefined);
};

/**
 * 获取未读通知数量
 */
export const getUnreadCount = (): Promise<{ unreadCount: number }> => {
  return request({
    url: "/api/notifications/unread-count",
    method: "get",
  }).then((response) => response.data);
};
