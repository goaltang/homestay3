import request from "@/utils/request";
import { normalizePageResponse, unwrapApiData } from "@/api/response";

// 争议记录类型
export interface DisputeRecord {
  id: number;
  orderId: number;
  orderNumber: string;
  homestayTitle: string;
  disputeReason: string;
  raisedBy: number;
  raisedByUsername: string;
  raisedAt: string;
  resolution: string;
  resolvedBy: number;
  resolvedByUsername: string;
  resolvedAt: string;
  resolutionNote: string;
}

// 获取争议记录列表（分页）
export function getDisputeRecords(params: { page?: number; size?: number; orderId?: string }) {
  return request({
    url: "/api/admin/disputes",
    method: "get",
    params: {
      page: params.page ?? 0,
      size: params.size ?? 10,
      ...(params.orderId ? { orderId: params.orderId } : {}),
    },
  }).then((res) => normalizePageResponse<DisputeRecord>(res, { singleObjectAsList: true }));
}

// 获取争议记录详情
export function getDisputeRecordDetail(orderId: number) {
  return request<DisputeRecord>({
    url: `/api/admin/disputes/${orderId}`,
    method: "get",
  }).then((res) => unwrapApiData<DisputeRecord>(res));
}
