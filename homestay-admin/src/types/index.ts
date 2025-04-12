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
  name: string;
  price: number;
  address: string;
  status: string;
  createTime?: string;
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
