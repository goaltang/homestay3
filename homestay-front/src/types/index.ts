// 房源状态
export type HomestayStatus = "ACTIVE" | "PENDING" | "INACTIVE" | "REJECTED";

// 订单状态
export type OrderStatus = "PENDING" | "CONFIRMED" | "COMPLETED" | "CANCELLED";

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
  createTime: string;
}
