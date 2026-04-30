import request from "@/utils/request";
import { handleApiError } from "@/utils/errorHandler";

/**
 * 获取当前可用优惠券列表
 */
export function getAvailableCoupons() {
  return request({
    url: "/api/coupons/available",
    method: "get",
  }).catch((error) => {
    handleApiError(error, "获取优惠券失败");
    throw error;
  });
}

/**
 * 获取我的优惠券
 * @param status 状态筛选：ALL / AVAILABLE / USED / EXPIRED
 */
export function getMyCoupons(status?: string) {
  return request({
    url: "/api/coupons/mine",
    method: "get",
    params: status && status !== "ALL" ? { status } : undefined,
  }).catch((error) => {
    handleApiError(error, "获取我的优惠券失败");
    throw error;
  });
}

/**
 * 领取优惠券
 */
export function claimCoupon(templateId: number) {
  return request({
    url: `/api/coupons/${templateId}/claim`,
    method: "post",
  }).catch((error) => {
    handleApiError(error, "领取优惠券失败");
    throw error;
  });
}
