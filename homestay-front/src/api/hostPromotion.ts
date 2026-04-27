import request from "@/utils/request";

export interface HostCampaign {
  id: number;
  name: string;
  campaignType: string;
  status: string;
  startAt: string;
  endAt: string;
  priority: number;
  budgetTotal: number;
  budgetUsed: number;
  budgetAlertThreshold: number;
  budgetAlertTriggered: boolean;
  subsidyBearer: string;
  rules: HostCampaignRule[];
  createdAt: string;
}

export interface HostCampaignRule {
  id: number;
  ruleType: string;
  discountAmount: number;
  discountRate: number;
  maxDiscount: number;
  thresholdAmount: number;
  minNights: number;
  maxNights: number;
}

export interface HostCampaignForm {
  name: string;
  campaignType: string;
  startAt: string;
  endAt: string;
  priority: number;
  budgetTotal: number | null;
  budgetAlertThreshold?: number | null;
  stackable?: boolean;
  rules: HostCampaignRuleForm[];
}

export interface HostCampaignRuleForm {
  ruleType: string;
  discountAmount: number | null;
  discountRate: number | null;
  maxDiscount: number | null;
  thresholdAmount: number | null;
  minNights: number | null;
  maxNights: number | null;
  firstOrderOnly?: boolean;
}

export interface HostPromotionStats {
  totalCampaigns: number;
  activeCampaigns: number;
  totalDiscount: number;
  totalUsageCount: number;
  usedCount: number;
}

export interface HostRoiOverview {
  totalGmv: number;
  totalDiscountCost: number;
  totalOrders: number;
  totalUsageCount: number;
  usedCount: number;
  platformCost: number;
  hostCost: number;
  roi: number;
  usageRate: number;
  avgDiscountPerOrder: number;
  avgOrderValue: number;
}

export interface HostRoiCampaign {
  campaignId: number;
  campaignName: string;
  campaignType: string;
  subsidyBearer: string;
  gmv: number;
  discountCost: number;
  orderCount: number;
  usageCount: number;
  usedCount: number;
  roi: number;
  usageRate: number;
  budgetUsageRate: number;
  budgetTotal: number;
  budgetUsed: number;
}

export function getHostCampaigns(params?: { page?: number; size?: number }) {
  return request({
    url: "/api/host/promotions/campaigns",
    method: "get",
    params,
  });
}

export function createHostCampaign(data: HostCampaignForm) {
  return request({
    url: "/api/host/promotions/campaigns",
    method: "post",
    data,
  });
}

export function updateHostCampaign(id: number, data: HostCampaignForm) {
  return request({
    url: `/api/host/promotions/campaigns/${id}`,
    method: "put",
    data,
  });
}

export function pauseHostCampaign(id: number) {
  return request({
    url: `/api/host/promotions/campaigns/${id}/pause`,
    method: "post",
  });
}

export function endHostCampaign(id: number) {
  return request({
    url: `/api/host/promotions/campaigns/${id}/end`,
    method: "post",
  });
}

export function getHostPromotionStats() {
  return request({
    url: "/api/host/promotions/statistics",
    method: "get",
  });
}

export function getHostRoiOverview(params?: { startDate?: string; endDate?: string }) {
  return request({
    url: "/api/host/promotions/roi/overview",
    method: "get",
    params,
  });
}

export function getHostRoiCampaigns(params?: { startDate?: string; endDate?: string }) {
  return request({
    url: "/api/host/promotions/roi/campaigns",
    method: "get",
    params,
  });
}
