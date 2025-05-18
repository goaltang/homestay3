import { PaymentMethod, PaymentStatus } from "./index";

// 房源类型
export interface Homestay {
  id: number;
  title: string;
  type: string;
  price: number;
  maxGuests: number;
  minNights: number;
  province: string;
  city: string;
  district: string;
  address: string;
  amenities: string[];
  description: string;
  coverImage: string;
  images: string[];
  status: string;
  featured: boolean;
  createdAt: string;
  updatedAt: string;
}

// 订单类型
export interface Order {
  id: number;
  orderNumber: string;
  homestayId: number;
  homestayTitle: string;
  hostId: number;
  hostName: string;
  guestId: number;
  guestName: string;
  guestPhone: string;
  checkInDate: string;
  checkOutDate: string;
  nights: number;
  guestCount: number;
  price: number;
  totalAmount: number;
  status: string;
  paymentStatus: PaymentStatus;
  paymentMethod?: PaymentMethod;
  remark: string;
  createTime: string;
  updateTime: string;
}

// 评价类型
export interface Review {
  id: number;
  homestayId: number;
  homestayTitle: string;
  userId: number;
  userName: string;
  userAvatar: string;
  rating: number;
  content: string;
  images: string[];
  response: string;
  responseTime: string;
  createTime: string;
}

// 收益类型
export interface Earning {
  id: number;
  orderNumber: string;
  homestayId: number;
  homestayTitle: string;
  guestName: string;
  checkInDate: string;
  checkOutDate: string;
  nights: number;
  amount: number;
  status: string;
  createTime: string;
}

// 收益汇总类型
export interface EarningSummary {
  totalEarnings: number;
  totalOrders: number;
  averagePerOrder: number;
}

// 收益趋势类型
export interface EarningTrend {
  labels: string[];
  values: number[];
}

// 分页查询结果类型
export interface PageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// 用户信息类型
export interface HostInfo {
  username: string;
  nickname: string;
  email: string;
  avatar: string;
  phone: string;
  realName: string;
  idCard: string;
  gender?: string;
  birthday?: string;
  occupation?: string;
  languages?: string[];
  introduction?: string;
  companions?: HostCompanion[];
  homestayCount?: number;
  orderCount?: number;
  reviewCount?: number;
  rating?: number;
  hostSince?: string;
}

export interface HostCompanion {
  name: string;
  avatar?: string;
}

export interface HostStatistics {
  homestayCount: number;
  orderCount: number;
  reviewCount: number;
  rating: number;
  totalEarnings: number;
  pendingOrders: number;
  completedOrders: number;
  cancelledOrders: number;
}

export interface HostHomestay {
  id: number;
  title: string;
  status: string;
  pricePerNight: number;
  bookingCount: number;
  reviewCount: number;
  rating: number;
  income: number;
  createdAt: string;
  coverImage: string;
  location: string;
}

export interface HostOrder {
  id: number;
  orderNumber: string;
  homestayId: number;
  homestayTitle: string;
  guestName: string;
  guestPhone?: string;
  guestAvatar?: string;
  checkInDate: string;
  checkOutDate: string;
  guestCount: number;
  totalAmount: number;
  status: string;
  paymentStatus: PaymentStatus;
  paymentMethod?: PaymentMethod;
  createdAt: string;
}

export interface HostReview {
  id: number;
  homestayId: number;
  homestayTitle: string;
  userId: number;
  userName: string;
  userAvatar?: string;
  rating: number;
  content: string;
  images?: string[];
  createdAt: string;
  response?: string;
}

export interface HostPasswordChange {
  oldPassword: string;
  newPassword: string;
}

// 筛选参数类型
export interface FilterParams {
  page?: number;
  size?: number;
  startDate?: string;
  endDate?: string;
  homestayId?: number;
  status?: string;
  type?: string;
  rating?: number;
  responseStatus?: string;
}
