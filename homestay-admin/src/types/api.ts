/**
 * 后端API接口类型定义
 */

// 通用分页响应格式
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  empty: boolean;
  first: boolean;
  last: boolean;
}

// 用户相关API
export namespace UserAPI {
  // 后端用户对象
  export interface UserDTO {
    id: number;
    username: string;
    nickname?: string;
    email?: string;
    phoneNumber?: string;
    enabled: boolean;
    role?: string;
    createdAt?: string;
    updatedAt?: string;
  }

  // 用户状态更新请求
  export interface UpdateStatusRequest {
    enabled: boolean;
  }

  // 批量操作请求
  export interface BatchRequest {
    ids: number[];
  }

  // 批量状态更新请求
  export interface BatchStatusRequest extends BatchRequest {
    enabled: boolean;
  }

  // 密码重置响应
  export interface ResetPasswordResponse {
    newPassword: string;
  }

  // 批量密码重置响应
  export interface BatchResetPasswordResponse {
    [id: string]: string;
  }
}

// 房源相关API
export namespace HomestayAPI {
  // 后端房源对象
  export interface HomestayDTO {
    id: number;
    title: string;
    description?: string;
    price: number;
    address?: string;
    province?: string;
    city?: string;
    district?: string;
    location?: string;
    featuredImage?: string;
    images?: string[];
    status: "ACTIVE" | "INACTIVE";
    host?: UserAPI.UserDTO;
    createdAt?: string;
    updatedAt?: string;
  }

  // 房源状态更新请求
  export interface UpdateStatusRequest {
    status: "ACTIVE" | "INACTIVE";
  }

  // 批量操作请求
  export interface BatchRequest {
    ids: number[];
  }

  // 批量状态更新请求
  export interface BatchStatusRequest extends BatchRequest {
    status: "ACTIVE" | "INACTIVE";
  }
}

// 订单相关API
export namespace OrderAPI {
  // 后端订单对象
  export interface OrderDTO {
    id: number;
    orderNumber: string;
    totalAmount: number;
    status: "PENDING" | "PAID" | "CANCELLED" | "COMPLETED";
    checkInDate?: string;
    checkOutDate?: string;
    homestay?: HomestayAPI.HomestayDTO;
    guest?: UserAPI.UserDTO;
    createdAt?: string;
    updatedAt?: string;
  }

  // 订单状态更新请求
  export interface UpdateStatusRequest {
    status: "PENDING" | "PAID" | "CANCELLED" | "COMPLETED";
  }

  // 批量操作请求
  export interface BatchRequest {
    ids: number[];
  }

  // 批量状态更新请求
  export interface BatchStatusRequest extends BatchRequest {
    status: "PENDING" | "PAID" | "CANCELLED" | "COMPLETED";
  }
}
