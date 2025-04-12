import request from "@/utils/request";

/**
 * 获取房东的订单列表
 * @param params 分页和过滤参数
 */
export function getHostOrders(params?: {
  page?: number;
  size?: number;
  status?: string;
}) {
  return request({
    url: "/api/orders/host",
    method: "get",
    params,
  });
}

/**
 * 更新订单状态
 * @param id 订单ID
 * @param status 新状态（CONFIRMED, CHECKED_IN, COMPLETED等）
 */
export function updateOrderStatus(id: number, status: string) {
  return request({
    url: `/api/orders/${id}/status`,
    method: "put",
    data: { status },
  });
}

/**
 * 确认订单
 * @param id 订单ID
 */
export function confirmOrder(id: number) {
  return request({
    url: `/api/orders/${id}/status`,
    method: "put",
    data: { status: "CONFIRMED" },
  });
}

/**
 * 拒绝订单
 * @param id 订单ID
 * @param reason 拒绝原因
 */
export function rejectOrder(id: number, reason: string) {
  return request({
    url: `/api/orders/${id}/reject`,
    method: "put",
    data: { reason },
  });
}

/**
 * 获取房东待处理订单数量
 */
export function getPendingOrderCount() {
  return request({
    url: "/api/orders/pending/count",
    method: "get",
  });
}

/**
 * 根据房源ID获取该房源的订单
 * @param homestayId 房源ID
 * @param params 查询参数
 */
export function getOrdersByHomestay(
  homestayId: number,
  params?: {
    page?: number;
    size?: number;
    status?: string;
  }
) {
  return request({
    url: `/api/orders/homestay/${homestayId}`,
    method: "get",
    params,
  });
}

/**
 * 获取房东订单统计信息
 */
export function getHostOrderStats() {
  return request({
    url: "/api/orders/host/stats",
    method: "get",
  });
}
