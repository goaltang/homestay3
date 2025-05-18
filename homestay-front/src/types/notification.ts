// 定义后端 NotificationDto 的类型
export interface NotificationDto {
  id: number;
  userId: number;
  actorId: number | null; // 触发者ID，可能为 null (系统通知)
  type: string; // 通知类型，例如 'BOOKING_REQUEST', 'WELCOME_MESSAGE'
  entityType: string; // 关联实体类型，例如 'BOOKING', 'USER'
  entityId: string; // 关联实体ID (字符串类型，因为可能不是数字)
  content: string; // 通知内容
  read: boolean; // <--- 修改为与 JSON 一致的字段名
  readAt: string | null; // ISO 格式的日期字符串 或 null
  createdAt: string; // ISO 格式的日期字符串

  // 可选的增强字段 (如果后端添加了)
  // actorUsername?: string;
  // entityTitle?: string;
}

// 定义后端实际返回的分页数据结构
export interface NotificationPageData {
  content: NotificationDto[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // 当前页码 (0-based)
  // 其他分页字段...
}

// 定义 getNotifications API 的 axios 响应类型
// 它包含一个 data 属性，其类型是 NotificationPageData
export interface NotificationApiResponse {
  data: NotificationPageData;
  status: number;
  statusText: string;
  headers: any;
  config: any;
  request?: any;
}
