import type { AxiosResponse } from "axios";

export type NotificationCategory =
  | "order"
  | "message"
  | "review"
  | "homestay"
  | "coupon"
  | "system";

export interface NotificationDto {
  id: number;
  userId: number;
  actorId: number | null;
  type: string;
  rawType?: string | null;
  entityType: string | null;
  rawEntityType?: string | null;
  entityId: string | null;
  content: string;
  read: boolean;
  isRead?: boolean;
  readAt: string | null;
  createdAt: string;
  actorUsername?: string;
  entityTitle?: string;
  category?: NotificationCategory | string | null;
  title?: string | null;
  deepLink?: string | null;
  payload?: Record<string, unknown> | null;
}

export interface NotificationPageData {
  content: NotificationDto[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export type NotificationApiResponse = AxiosResponse<NotificationPageData>;

export const normalizeNotification = (notification: NotificationDto): NotificationDto => {
  const read = notification.read ?? notification.isRead ?? false;
  const type = notification.type || notification.rawType || "UNKNOWN";
  const entityType = notification.entityType || notification.rawEntityType || null;
  const normalized = {
    ...notification,
    type,
    entityType,
    read,
    isRead: read,
  };

  return {
    ...normalized,
    category: notification.category || resolveNotificationCategory(normalized),
    title: notification.title || resolveNotificationTitle(normalized),
    deepLink: notification.deepLink || null,
    payload: notification.payload || null,
  };
};

export const resolveNotificationCategory = (notification: Pick<NotificationDto, "type" | "entityType" | "category">): NotificationCategory => {
  if (notification.category) {
    return notification.category as NotificationCategory;
  }

  const type = notification.type;
  if (type?.startsWith("BOOKING") || type?.startsWith("ORDER") || type?.startsWith("REFUND") || type?.startsWith("PAYMENT")
    || ["PAID", "CANCELLED", "CANCELLED_BY_HOST", "CANCELLED_BY_USER", "COMPLETED", "CONFIRMED", "PENDING", "REFUNDED"].includes(type || "")) {
    return "order";
  }
  if (type?.startsWith("NEW_MESSAGE")) return "message";
  if (type?.startsWith("NEW_REVIEW") || type?.startsWith("REVIEW")) return "review";
  if (type?.startsWith("HOMESTAY")) return "homestay";
  if (type?.startsWith("COUPON")) return "coupon";

  switch (notification.entityType) {
    case "BOOKING":
    case "ORDER":
      return "order";
    case "MESSAGE":
    case "MESSAGE_THREAD":
      return "message";
    case "REVIEW":
      return "review";
    case "HOMESTAY":
      return "homestay";
    case "COUPON":
      return "coupon";
    default:
      return "system";
  }
};

export const resolveNotificationTitle = (notification: Pick<NotificationDto, "type" | "title">): string => {
  if (notification.title) {
    return notification.title;
  }

  const map: Record<string, string> = {
    BOOKING_REQUEST: "预订请求",
    BOOKING_ACCEPTED: "预订确认",
    BOOKING_REJECTED: "预订被拒",
    BOOKING_CANCELLED: "预订取消",
    BOOKING_REMINDER: "入住提醒",
    REVIEW_REMINDER: "评价提醒",
    ORDER_CONFIRMED: "订单确认",
    PAYMENT_RECEIVED: "收款通知",
    ORDER_CANCELLED_BY_HOST: "订单取消",
    ORDER_CANCELLED_BY_GUEST: "订单取消",
    ORDER_COMPLETED: "订单完成",
    ORDER_STATUS_CHANGED: "订单状态",
    REFUND_REQUESTED: "退款申请",
    REFUND_APPROVED: "退款通过",
    REFUND_REJECTED: "退款被拒",
    REFUND_COMPLETED: "退款完成",
    NEW_MESSAGE: "新消息",
    NEW_REVIEW: "新评价",
    REVIEW_REPLIED: "评价回复",
    HOMESTAY_APPROVED: "房源通过",
    HOMESTAY_REJECTED: "房源被拒",
    HOMESTAY_SUBMITTED: "房源审核",
    PASSWORD_CHANGED: "账号安全",
    EMAIL_VERIFIED: "账号安全",
    SYSTEM_ANNOUNCEMENT: "系统公告",
    WELCOME_MESSAGE: "欢迎",
    COUPON_EXPIRING: "优惠券",
    COUPON_ISSUED: "优惠券",
    PAID: "已付款",
    CANCELLED: "已取消",
    CANCELLED_BY_HOST: "房东取消",
    CANCELLED_BY_USER: "用户取消",
    COMPLETED: "已完成",
    CONFIRMED: "已确认",
    PENDING: "待处理",
    REFUNDED: "已退款",
    UNKNOWN: "系统通知",
  };
  return map[notification.type || ""] || "系统通知";
};

export const resolveNotificationDeepLink = (
  notification: NotificationDto,
  options: { isLandlord?: boolean; fallback?: string } = {},
): string | null => {
  if (notification.deepLink) {
    return notification.deepLink;
  }

  const fallback = options.fallback ?? null;
  const entityId = notification.entityId;
  const isLandlord = options.isLandlord ?? false;

  switch (notification.entityType) {
    case "BOOKING":
    case "ORDER":
      return entityId ? (isLandlord ? `/host/orders?highlightOrderId=${entityId}` : `/orders/${entityId}`) : fallback;
    case "HOMESTAY":
      return entityId ? (isLandlord ? `/host/homestay/edit/${entityId}` : `/homestays/${entityId}`) : fallback;
    case "REVIEW":
      return entityId ? (isLandlord ? `/host/reviews?highlightReviewId=${entityId}` : "/user/reviews") : fallback;
    case "MESSAGE":
    case "MESSAGE_THREAD":
      return isLandlord ? "/host/messages" : "/user/notifications";
    case "USER":
      return "/user/profile";
    case "COUPON":
      return "/user/coupons";
    default:
      if (notification.type === "NEW_MESSAGE") return isLandlord ? "/host/messages" : "/user/notifications";
      if (notification.type?.startsWith("COUPON")) return "/user/coupons";
      return fallback;
  }
};
