// 分页参数
export interface PageParams {
  page: number;
  pageSize: number;
}

// 分页响应
export interface PageResult<T> {
  list: T[];
  total: number;
}

// 房源
export interface Homestay {
  id: number;
  title: string;
  price?: number;
  pricePerNight?: number;
  status: string;
  createTime?: string;
  type?: string;
  maxGuests?: number;
  minNights?: number;
  bedrooms?: number;
  beds?: number;
  bathrooms?: number;
  amenities?: string[];
  images?: string[];
  coverImage?: string;
  description?: string;
  featured?: boolean;
  rating?: number;
  reviewCount?: number;
  hostId?: number;
  hostName?: string;
  ownerName?: string;
  ownerUsername?: string;
  provinceCode?: string;
  cityCode?: string;
  districtCode?: string;
  addressDetail?: string;
  updatedAt?: string;
}

// 订单
export interface Order {
  orderNo: string;
  homestayName: string;
  userName: string;
  amount: number;
  status: string;
  createTime: string;
}

// 用户
export interface User {
  id: number;
  username: string;
  nickname: string;
  phone: string;
  email: string;
  status: string;
  createTime: string;
  userType?: string;
  verificationStatus?: string;
}

// 房源搜索参数
export interface HomestaySearchParams extends PageParams {
  name?: string;
  status?: string;
}

// 订单搜索参数
export interface OrderSearchParams extends PageParams {
  orderNo?: string;
  status?: string;
}

// 用户搜索参数
export interface UserSearchParams extends PageParams {
  username?: string;
  phone?: string;
  status?: string;
}

// 状态映射
export interface StatusMap {
  [key: string]: {
    text: string;
    type: string;
  };
}

// 身份验证
export interface IdentityVerification {
  id: number;
  userId: number;
  username: string;
  realName: string;
  idCard: string;
  idCardFront: string;
  idCardBack: string;
  status: string; // 'PENDING', 'VERIFIED', 'REJECTED'
  submitTime: string;
  reviewTime?: string;
  reviewNote?: string;
}

// 身份验证搜索参数
export interface VerificationSearchParams extends PageParams {
  username?: string;
  status?: string;
}
