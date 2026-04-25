import request from "@/utils/request";
import { handleApiError } from "@/utils/errorHandler";

/**
 * 统一报价接口
 */
export function getPricingQuote(data: {
  homestayId: number;
  checkInDate: string;
  checkOutDate: string;
  guestCount: number;
  couponIds?: number[];
}) {
  return request({
    url: "/api/pricing/quote",
    method: "post",
    data,
  }).catch((error) => {
    handleApiError(error, "获取报价失败");
    throw error;
  });
}
