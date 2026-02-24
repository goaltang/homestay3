import request from "@/utils/request";

// 违规举报相关接口

/**
 * 获取违规举报列表
 */
export function getViolationReports(params: {
  page?: number;
  size?: number;
  status?: string;
  violationType?: string;
  keyword?: string;
}) {
  return request({
    url: "/api/admin/violations",
    method: "get",
    params,
  });
}

/**
 * 获取待处理的违规举报
 */
export function getPendingReports(params: { page?: number; size?: number }) {
  return request({
    url: "/api/admin/violations/pending",
    method: "get",
    params,
  });
}

/**
 * 获取违规举报详情
 */
export function getReportDetail(id: number) {
  return request({
    url: `/api/admin/violations/${id}`,
    method: "get",
  });
}

/**
 * 处理违规举报
 */
export function processReport(
  id: number,
  data: {
    actionType: string;
    reason: string;
    notes?: string;
    suspendDays?: number;
  }
) {
  return request({
    url: `/api/admin/violations/${id}/process`,
    method: "post",
    data,
  });
}

/**
 * 忽略违规举报
 */
export function dismissReport(
  id: number,
  data: {
    reason: string;
  }
) {
  return request({
    url: `/api/admin/violations/${id}/dismiss`,
    method: "post",
    data,
  });
}

/**
 * 获取房源的举报历史
 */
export function getHomestayReports(homestayId: number) {
  return request({
    url: `/api/admin/violations/homestay/${homestayId}`,
    method: "get",
  });
}

/**
 * 获取违规统计数据
 */
export function getViolationStatistics() {
  return request({
    url: "/api/admin/violations/statistics",
    method: "get",
  });
}

/**
 * 扫描检测可能的违规房源
 */
export function scanForViolations() {
  return request({
    url: "/api/admin/violations/scan",
    method: "post",
  });
}

/**
 * 批量处理违规举报
 */
export function batchProcessReports(data: {
  reportIds: number[];
  actionType: string;
  reason: string;
}) {
  return request({
    url: "/api/admin/violations/batch-process",
    method: "post",
    data,
  });
}

/**
 * 获取多次被举报的房源
 */
export function getHomestaysWithMultipleReports(minReportCount = 2) {
  return request({
    url: "/api/admin/violations/multiple-reports",
    method: "get",
    params: { minReportCount },
  });
}
