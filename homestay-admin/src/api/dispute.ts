import request from "@/utils/request";

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

// 分页响应类型
interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

// 获取争议记录列表（分页）
export function getDisputeRecords(params: { page?: number; size?: number; orderId?: string }) {
  return request<PageResponse<DisputeRecord>>({
    url: "/api/admin/disputes",
    method: "get",
    params: {
      page: params.page ?? 0,
      size: params.size ?? 10,
      ...(params.orderId ? { orderId: params.orderId } : {}),
    },
  });
}

// 获取争议记录详情
export function getDisputeRecordDetail(orderId: number) {
  return request<DisputeRecord>({
    url: `/api/admin/disputes/${orderId}`,
    method: "get",
  });
}
