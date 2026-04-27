import request from "@/utils/request";

// ========== 节假日管理 ==========

export function getHolidays(params?: {
  year?: number;
  regionCode?: string;
}) {
  return request({
    url: "/api/admin/holidays",
    method: "get",
    params,
  });
}

export function createHoliday(data: any) {
  return request({
    url: "/api/admin/holidays",
    method: "post",
    data,
  });
}

export function updateHoliday(id: number, data: any) {
  return request({
    url: `/api/admin/holidays/${id}`,
    method: "put",
    data,
  });
}

export function deleteHoliday(id: number) {
  return request({
    url: `/api/admin/holidays/${id}`,
    method: "delete",
  });
}

// ========== 价格规则管理 ==========

export function getPricingRules(params?: {
  page?: number;
  size?: number;
  ruleType?: string;
}) {
  return request({
    url: "/api/admin/pricing-rules",
    method: "get",
    params,
  });
}

export function createPricingRule(data: any) {
  return request({
    url: "/api/admin/pricing-rules",
    method: "post",
    data,
  });
}

export function updatePricingRule(id: number, data: any) {
  return request({
    url: `/api/admin/pricing-rules/${id}`,
    method: "put",
    data,
  });
}

export function deletePricingRule(id: number) {
  return request({
    url: `/api/admin/pricing-rules/${id}`,
    method: "delete",
  });
}

export function togglePricingRule(id: number) {
  return request({
    url: `/api/admin/pricing-rules/${id}/toggle`,
    method: "patch",
  });
}
