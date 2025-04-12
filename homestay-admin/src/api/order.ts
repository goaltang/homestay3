import request from "@/utils/request";
import type { Order, OrderSearchParams } from "@/types";
import {
  adaptPageParams,
  adaptPageResponse,
  adaptOrderStatus,
  adaptOrderItem,
} from "@/utils/adapter";

// 获取订单列表
export function getOrderList(params: OrderSearchParams) {
  // 调整前端参数为后端格式
  const adaptedParams = adaptPageParams(params);

  // 订单状态转换
  if (adaptedParams.status) {
    adaptedParams.status = adaptOrderStatus(adaptedParams.status, false);
  }

  return request<{
    content: any[];
    totalElements: number;
    totalPages: number;
    page: number;
    size: number;
  }>({
    url: "/api/admin/orders",
    method: "get",
    params: adaptedParams,
  }).then((response) => {
    // 将后端响应格式转换为前端期望的格式
    return adaptPageResponse(response, adaptOrderItem);
  });
}

// 获取订单详情
export function getOrderDetail(id: number) {
  return request<any>({
    url: `/api/admin/orders/${id}`,
    method: "get",
  }).then((response) => {
    return adaptOrderItem(response);
  });
}

// 更新订单状态
export function updateOrderStatus(id: number, status: string) {
  const backendStatus = adaptOrderStatus(status, false);

  return request({
    url: `/api/admin/orders/${id}/status`,
    method: "put",
    data: { status: backendStatus },
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
