import request from "@/utils/request";

/**
 * 获取统计数据
 * @param params 查询参数
 */
export function getStatisticsData(params: any) {
  return request({
    url: "/api/admin/statistics",
    method: "get",
    params,
  });
}

/**
 * 导出统计数据
 * @param params 查询参数
 */
export function exportStatistics(params: any) {
  return request({
    url: "/api/admin/statistics/export",
    method: "get",
    params,
    responseType: "blob",
  });
}

/**
 * 获取订单趋势
 * @param params 查询参数
 */
export function getOrderTrend(params: any) {
  return request({
    url: "/api/admin/statistics/order-trend",
    method: "get",
    params,
  });
}

/**
 * 获取收入趋势
 * @param params 查询参数
 */
export function getIncomeTrend(params: any) {
  return request({
    url: "/api/admin/statistics/income-trend",
    method: "get",
    params,
  });
}

/**
 * 获取用户增长趋势
 * @param params 查询参数
 */
export function getUserTrend(params: any) {
  return request({
    url: "/api/admin/statistics/user-trend",
    method: "get",
    params,
  });
}

/**
 * 获取民宿地区分布
 */
export function getHomestayDistribution() {
  return request({
    url: "/api/admin/statistics/homestay-distribution",
    method: "get",
  });
}
