import { ref, onUnmounted } from 'vue';
import { useNotificationStore } from '@/stores/notification';
import { useChatStore } from '@/stores/chat';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

let stompClient: any = null;
const isConnected = ref(false);

/**
 * 初始化WebSocket连接
 * @param userId 用户ID
 */
export const initWebSocket = (userId: number | null) => {
  if (!userId) {
    console.warn('用户ID为空，无法初始化WebSocket连接');
    return;
  };

  // 如果已经有连接，先断开
  if (stompClient) {
    disconnectWebSocket();
  };

  try {
    // 创建SockJS连接
    const socket = new SockJS((import.meta.env.VITE_API_URL || 'http://localhost:8080') + '/ws');
    stompClient = Stomp.over(socket);

    // 调试模式下打印日志
    stompClient.debug = (str: string) => {
      // console.log(str);
    };

    // 连接到WebSocket
    stompClient.connect({}, function(frame: any) {
      console.log('WebSocket已连接: ' + frame);
      isConnected.value = true;

      // 订阅用户的通知主题
      stompClient.subscribe(`/topic/notifications/${userId}`, function(notification: any) {
        try {
          const notificationDTO = JSON.parse(notification.body);
          console.log('收到实时通知:', notificationDTO);
          
          // 使用Pinia store更新通知状态
          const notificationStore = useNotificationStore();
          
          // 将新通知添加到列表开头（最新的在前面）
          notificationStore.notifications.unshift(notificationDTO as any);
          
          // 如果是未读通知，增加未读计数
          if (!notificationDTO.isRead) {
            notificationStore.unreadCount += 1;
          }
          
          // 同时更新分页中的通知（如果存在）
          if (notificationStore.pagination?.content) {
            notificationStore.pagination.content.unshift(notificationDTO as any);
          }
          
          // 可以在这里触发全局事件或显示通知提示
          // 例如: window.dispatchEvent(new CustomEvent('new-notification', { detail: notificationDTO }));
        } catch (error) {
          console.error('解析通知消息失败:', error);
        }
      });

      // 订阅未读数量更新主题
      stompClient.subscribe(`/topic/unread-count/${userId}`, function(countMessage: any) {
        try {
          const count = JSON.parse(countMessage.body);
          console.log('收到未读数量更新:', count);

          // 使用Pinia store更新未读数量
          const notificationStore = useNotificationStore();
          notificationStore.unreadCount = count;

          // 同时更新所有通知的已读状态（可选，取决于业务需求）
          // 这里我们只是更新数量，保持通知本身的状态不变
          // 如果需要同步所有通知的状态，可以遍历通知列表并标记为已读
        } catch (error) {
          console.error('解析未读数量消息失败:', error);
        }
      });

      // 订阅用户的聊天消息主题
      stompClient.subscribe(`/topic/chat/user/${userId}`, function(chatMessage: any) {
        try {
          const message = JSON.parse(chatMessage.body);
          console.log('收到聊天消息:', message);

          // 使用Pinia store更新聊天状态
          const chatStore = useChatStore();
          chatStore.handleNewMessage(message);
        } catch (error) {
          console.error('解析聊天消息失败:', error);
        }
      });
    }, function(error: any) {
      console.error('WebSocket连接错误:', error);
      isConnected.value = false;
      // 可以在这里实现重连逻辑
    });
  } catch (error) {
    console.error('初始化WebSocket失败:', error);
    isConnected.value = false;
  }
};

/**
 * 断开WebSocket连接
 */
export const disconnectWebSocket = () => {
  if (stompClient && stompClient.connected) {
    stompClient.disconnect(function() {
      console.log('WebSocket已断开连接');
      isConnected.value = false;
    });
  };
  stompClient = null;
};

/**
 * 检查WebSocket是否已连接
 */
export const isWebSocketConnected = (): boolean => {
  return isConnected.value && stompClient && stompClient.connected;
};

// 在组件卸载时自动断开连接的辅助函数
export const useWebSocket = (userId: number | null) => {
  onUnmounted(() => {
    disconnectWebSocket();
  });
  
  return {
    initWebSocket,
    disconnectWebSocket,
    isWebSocketConnected
  };
};