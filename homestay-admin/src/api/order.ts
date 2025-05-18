import request from "@/utils/request";
import type { Order, OrderSearchParams } from "@/types";
import {
  adaptPageParams,
  adaptPageResponse,
  adaptOrderStatus,
  adaptOrderItem,
} from "@/utils/adapter";

// --- 定义更具体的类型 ---
// 管理员订单搜索参数类型 (匹配 AdminOrderController)
export interface AdminOrderSearchParams {
  page?: number;
  size?: number;
  orderNumber?: string;
  guestName?: string;
  homestayTitle?: string;
  status?: string;
  paymentStatus?: string;
  paymentMethod?: string;
  hostName?: string;
  checkInDateStart?: string; // YYYY-MM-DD
  checkInDateEnd?: string; // YYYY-MM-DD
  createTimeStart?: string; // YYYY-MM-DD
  createTimeEnd?: string; // YYYY-MM-DD
  sort?: string; // e.g., "createdAt,desc"
}

// 管理员订单列表项类型 (匹配后端 OrderDTO)
export interface AdminOrderListItem {
  id: number;
  orderNumber: string;
  homestayId: number;
  homestayTitle: string;
  guestId: number;
  guestName: string;
  guestPhone: string;
  hostId: number;
  hostName: string;
  checkInDate: string;
  checkOutDate: string;
  nights: number;
  guestCount: number;
  price: number;
  totalAmount: number;
  status: string;
  paymentStatus: string | null;
  paymentMethod: string | null;
  remark: string | null;
  createTime: string;
  updateTime: string;
}

// 分页响应类型
interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number; // 后端返回的是 0-based
  size: number;
}
// --- 类型定义结束 ---

// 获取管理员订单列表 (新)
export function getAdminOrders(params: AdminOrderSearchParams) {
  // 参数处理: 移除空值或 undefined 值，后端 @RequestParam(required = false) 会处理
  const filteredParams = Object.entries(params).reduce((acc, [key, value]) => {
    if (value !== null && value !== undefined && value !== "") {
      // @ts-ignore
      acc[key] = value;
    }
    return acc;
  }, {} as AdminOrderSearchParams);

  return request<PageResponse<AdminOrderListItem>>({
    url: "/api/admin/orders",
    method: "get",
    params: filteredParams,
  }).then((response) => {
    // 后端返回的已经是所需结构，可能不需要 adaptPageResponse
    // 但如果需要前端页码是 1-based，可以在这里调整
    // response.page = response.page + 1;
    // 同时，如果列表项需要转换 (如日期格式)，可以在此处理
    // response.content = response.content.map(adaptOrderItem); // adaptOrderItem 需要更新
    return response; // 直接返回后端数据结构
  });
}

// 获取订单详情 (可以保留，但可能需要更新 adaptOrderItem)
export function getOrderDetail(id: number) {
  return request<any>({
    url: `/api/admin/orders/${id}`,
    method: "get",
  }).then((response) => {
    // return adaptOrderItem(response);
    return response; // 返回原始数据
  });
}

// 更新订单状态 (可以保留，注意只更新 status 字段)
export function updateOrderStatus(id: number, status: string) {
  // 注意：这个函数现在只应该用来更新订单的生命周期状态(status)
  // 而不是支付状态(paymentStatus)
  return request({
    url: `/api/admin/orders/${id}/status`,
    method: "put",
    data: { status: status }, // 直接传递后端接受的字符串状态
  });
}

// 新增: 管理员手动确认支付
export function confirmPayment(orderId: number) {
  return request<AdminOrderListItem>({
    url: `/api/admin/orders/${orderId}/confirm-payment`,
    method: "put",
  });
}

// 新增: 管理员发起退款
export function initiateRefund(orderId: number) {
  // 后端目前不需要请求体，所以 data 为空
  return request<AdminOrderListItem>({
    url: `/api/admin/orders/${orderId}/refund`,
    method: "post", // 注意后端是 POST
  });
}

// 删除订单
export function deleteOrder(id: number) {
  return request({
    url: `/api/admin/orders/${id}`,
    method: "delete",
  });
}

// 导出订单
export function exportOrders(params: OrderSearchParams) {
  // 转换参数
  const adaptedParams = adaptPageParams(params);

  // 转换状态
  if (adaptedParams.status) {
    adaptedParams.status = adaptOrderStatus(adaptedParams.status, false);
  }

  return request({
    url: "/api/admin/orders/export",
    method: "get",
    params: adaptedParams,
    responseType: "blob",
  });
}

// 新增: 批量删除订单
export function batchDeleteOrders(ids: number[]) {
  return request({
    url: `/api/admin/orders/batch`,
    method: "delete",
    data: { ids },
  });
}

// 新增: 批量更新订单状态
export function batchUpdateOrderStatus(ids: number[], status: string) {
  const backendStatus = adaptOrderStatus(status, false);

  return request({
    url: `/api/admin/orders/batch/status`,
    method: "put",
    data: {
      ids,
      status: backendStatus,
    },
  });
}

// 新增: 批量导出订单
export function batchExportOrders(ids: number[]) {
  return request({
    url: "/api/admin/orders/batch/export",
    method: "post",
    data: { ids },
    responseType: "blob",
  });
}

// 订单状态转换函数
function convertOrderStatus(backendStatus: string): string {
  switch (backendStatus) {
    case "PENDING":
      return "0"; // 待支付
    case "PAID":
      return "1"; // 已支付
    case "CANCELLED":
      return "2"; // 已取消
    case "COMPLETED":
      return "3"; // 已完成
    default:
      return "0"; // 默认为待支付
  }
}
