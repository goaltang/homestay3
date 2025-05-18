// 房源状态
export type HomestayStatus = "ACTIVE" | "PENDING" | "INACTIVE" | "REJECTED";

// 订单状态
export type OrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "PAID"
  | "PAYMENT_FAILED"
  | "CHECKED_IN"
  | "COMPLETED"
  | "CANCELLED"
  | "CANCELLED_BY_USER"
  | "CANCELLED_BY_HOST"
  | "CANCELLED_SYSTEM"
  | "REJECTED";

// 支付状态 (新增)
export type PaymentStatus = "UNPAID" | "PAID" | "REFUNDED" | "FAILED"; // 根据实际情况调整

// 支付方式 (新增)
export type PaymentMethod = "ALIPAY" | "WECHAT_PAY" | "OTHER"; // 根据实际情况调整

// 房源类型
export type HomestayType = "ENTIRE" | "PRIVATE";

// 分页响应数据
export interface PaginationResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// 房源基本信息
export interface HomestayBasic {
  id: number;
  title: string;
  coverImage: string;
  type: HomestayType;
  price: number;
  maxGuests: number;
  status: HomestayStatus;
}

// 订单基本信息
export interface OrderBasic {
  orderNumber: string;
  homestayTitle: string;
  guestName: string;
  guestPhone: string;
  checkInDate: string;
  checkOutDate: string;
  nights: number;
  totalAmount: number;
  status: OrderStatus;
  paymentStatus: PaymentStatus; // 新增
  createTime: string;
}
