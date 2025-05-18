import { defineStore } from "pinia";
import { ref, computed } from "vue";
import api from "@/utils/request"; // 你的 axios 实例
import type { Page } from "@/types/page"; // 假设你有一个 Page 类型

// 定义通知的数据结构 (与后端的 NotificationDto 对应)
export interface Notification {
  id: number;
  userId: number;
  actorId?: number;
  type: string; // 后续可以考虑使用枚举字符串
  entityType?: string;
  entityId?: string;
  content: string;
  isRead: boolean;
  readAt?: string; // ISO 8601 格式的日期时间字符串
  createdAt: string; // ISO 8601 格式的日期时间字符串
  // 可以添加 actorUsername, entityTitle 等前端需要的附加信息
  // actorUsername?: string;
  // entityTitle?: string;
}

// 定义后端返回的未读数量接口响应结构
interface UnreadCountResponse {
  unreadCount: number;
}

export const useNotificationStore = defineStore("notification", () => {
  // --- State ---
  const unreadCount = ref<number>(0);
  const notifications = ref<Notification[]>([]); // 用于下拉列表或通知中心
  const loading = ref<boolean>(false);
  const pagination = ref<Page<Notification> | null>(null); // 用于通知中心分页

  // --- Getters ---
  const hasUnread = computed(() => unreadCount.value > 0);

  // --- Actions ---

  /**
   * 获取当前用户的未读通知数量
   */
  const fetchUnreadCount = async () => {
    // 不需要设置 loading，这个请求通常比较快，在后台静默执行
    try {
      const response = await api.get<UnreadCountResponse>(
        "/api/notifications/unread-count"
      );
      unreadCount.value = response.data.unreadCount || 0;
      console.log("获取到未读通知数量:", unreadCount.value);
    } catch (error) {
      console.error("获取未读通知数量失败:", error);
      // 失败时不重置为0，避免误导用户
    }
  };

  /**
   * 获取通知列表（分页）
   * @param page 页码 (0-based)
   * @param size 每页数量
   * @param isReadFilter 筛选条件 (true: 只看已读, false: 只看未读, null: 所有)
   * @param target 'dropdown' | 'center' - 用于区分是加载少量给下拉还是加载分页给中心
   */
  const fetchNotifications = async (
    page: number = 0,
    size: number = 5, // 下拉默认少一点，中心可以多一点
    isReadFilter: boolean | null = null,
    target: "dropdown" | "center" = "dropdown"
  ) => {
    loading.value = true;
    try {
      const params: Record<string, any> = { page, size };
      if (isReadFilter !== null) {
        params.isRead = isReadFilter;
      }
      console.log(
        `请求通知列表: page=${page}, size=${size}, isRead=${isReadFilter}, target=${target}`
      );

      const response = await api.get<Page<Notification>>("/api/notifications", {
        params,
      });

      if (target === "dropdown") {
        // 更新下拉列表（通常是最新的几条）
        notifications.value = response.data.content || [];
        console.log("更新通知下拉列表:", notifications.value);
      } else {
        // 更新通知中心的分页数据
        pagination.value = response.data;
        notifications.value = response.data.content || []; // 也更新列表，方便直接使用
        console.log("更新通知中心数据:", pagination.value);
      }
    } catch (error) {
      console.error(`获取通知列表 (${target}) 失败:`, error);
      // 清空数据或显示错误提示
      if (target === "dropdown") {
        notifications.value = [];
      } else {
        pagination.value = null;
        notifications.value = [];
      }
    } finally {
      loading.value = false;
    }
  };

  /**
   * 将单条通知标记为已读
   * @param notificationId 通知 ID
   */
  const markAsRead = async (notificationId: number) => {
    try {
      await api.post(`/api/notifications/${notificationId}/read`);
      console.log(`通知 ${notificationId} 已标记为已读 (API 调用成功)`);

      // 更新前端状态
      const notification = notifications.value.find(
        (n) => n.id === notificationId
      );
      if (notification && !notification.isRead) {
        notification.isRead = true;
        notification.readAt = new Date().toISOString(); // 设置为当前时间
        // 更新未读计数 (如果之前是未读的话)
        if (unreadCount.value > 0) {
          unreadCount.value--;
        }
        console.log(
          `前端状态更新: 通知 ${notificationId} 标记已读，未读数: ${unreadCount.value}`
        );
      }
      // 如果是在通知中心操作，可能需要更新分页对象中的数据
      if (pagination.value) {
        const notificationInPage = pagination.value.content.find(
          (n) => n.id === notificationId
        );
        if (notificationInPage && !notificationInPage.isRead) {
          notificationInPage.isRead = true;
          notificationInPage.readAt = new Date().toISOString();
        }
      }
    } catch (error) {
      console.error(`标记通知 ${notificationId} 为已读失败:`, error);
      // 可以考虑显示错误消息
    }
  };

  /**
   * 将所有未读通知标记为已读
   */
  const markAllAsRead = async () => {
    if (unreadCount.value === 0) {
      console.log("没有未读通知，无需操作");
      return;
    }
    try {
      const response = await api.post<{ markedCount: number }>(
        "/api/notifications/read-all"
      );
      const markedCount = response.data.markedCount || 0;
      console.log(`API 响应: ${markedCount} 条通知已标记为已读`);

      // 更新前端状态
      unreadCount.value = 0; // 直接设置为 0
      notifications.value.forEach((n) => {
        if (!n.isRead) {
          n.isRead = true;
          n.readAt = new Date().toISOString();
        }
      });
      if (pagination.value) {
        pagination.value.content.forEach((n) => {
          if (!n.isRead) {
            n.isRead = true;
            n.readAt = new Date().toISOString();
          }
        });
      }
      console.log("前端状态更新: 所有通知标记已读，未读数: 0");
    } catch (error) {
      console.error("标记所有通知为已读失败:", error);
      // 可以考虑显示错误消息
    }
  };

  // --- 初始化 ---
  // 应用启动时获取一次未读数量 (可以在 App.vue 或布局组件中调用)
  // fetchUnreadCount();

  return {
    unreadCount,
    notifications,
    loading,
    pagination,
    hasUnread,
    fetchUnreadCount,
    fetchNotifications,
    markAsRead,
    markAllAsRead,
  };
});

// 定义分页类型 (如果还没有的话)
// 可以放在 @/types/page.ts
/*
export interface Page<T> {
    content: T[];
    pageable: {
        pageNumber: number;
        pageSize: number;
        sort: {
            sorted: boolean;
            unsorted: boolean;
            empty: boolean;
        };
        offset: number;
        paged: boolean;
        unpaged: boolean;
    };
    totalPages: number;
    totalElements: number;
    last: boolean;
    size: number;
    number: number; // 当前页码 (0-based)
    sort: {
        sorted: boolean;
        unsorted: boolean;
        empty: boolean;
    };
    numberOfElements: number; // 当前页元素数量
    first: boolean;
    empty: boolean;
}
*/
