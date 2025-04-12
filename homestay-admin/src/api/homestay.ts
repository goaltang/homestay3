import request from "@/utils/request";
import type { Homestay, HomestaySearchParams } from "@/types";
import {
  adaptPageParams,
  adaptPageResponse,
  adaptHomestayStatus,
  adaptHomestayItem,
} from "@/utils/adapter";

// 获取房源列表
export function getHomestayList(params: HomestaySearchParams) {
  // 调整前端参数为后端格式
  const adaptedParams = adaptPageParams(params);

  // 处理特殊参数
  if ("name" in adaptedParams) {
    adaptedParams.title = adaptedParams.name;
    delete adaptedParams.name;
  }

  // 状态转换
  if (adaptedParams.status) {
    adaptedParams.status = adaptHomestayStatus(adaptedParams.status, false);
  }

  return request<{
    content: any[];
    totalElements: number;
    totalPages: number;
    page: number;
    size: number;
  }>({
    url: "/api/admin/homestays",
    method: "get",
    params: adaptedParams,
  }).then((response) => {
    // 将后端响应格式转换为前端期望的格式
    return adaptPageResponse(response, adaptHomestayItem);
  });
}

// 获取房源详情
export function getHomestayDetail(id: number) {
  return request<any>({
    url: `/api/admin/homestays/${id}`,
    method: "get",
  }).then((response) => {
    return adaptHomestayItem(response);
  });
}

// 创建房源
export function createHomestay(data: Omit<Homestay, "id">) {
  // 转换前端数据为后端格式
  const adaptedData = {
    ...data,
    title: data.name,
    status: adaptHomestayStatus(data.status, false),
  };

  return request<any>({
    url: "/api/admin/homestays",
    method: "post",
    data: adaptedData,
  }).then((response) => {
    return adaptHomestayItem(response);
  });
}

// 更新房源
export function updateHomestay(id: number, data: Partial<Homestay>) {
  // 转换前端数据为后端格式
  const adaptedData: any = { ...data };

  if ("name" in adaptedData) {
    adaptedData.title = adaptedData.name;
    delete adaptedData.name;
  }

  if ("status" in adaptedData) {
    adaptedData.status = adaptHomestayStatus(adaptedData.status, false);
  }

  return request<any>({
    url: `/api/admin/homestays/${id}`,
    method: "put",
    data: adaptedData,
  }).then((response) => {
    return adaptHomestayItem(response);
  });
}

// 删除房源
export function deleteHomestay(id: number) {
  return request({
    url: `/api/admin/homestays/${id}`,
    method: "delete",
  });
}

// 更新房源状态
export function updateHomestayStatus(id: number, status: string) {
  const backendStatus = adaptHomestayStatus(status, false);

  return request({
    url: `/api/admin/homestays/${id}/status`,
    method: "put",
    data: { status: backendStatus },
  });
}

// 新增: 批量删除房源
export function batchDeleteHomestays(ids: number[]) {
  return request({
    url: `/api/admin/homestays/batch`,
    method: "delete",
    data: { ids },
  });
}

// 新增: 批量更新房源状态
export function batchUpdateHomestayStatus(ids: number[], status: string) {
  const backendStatus = adaptHomestayStatus(status, false);

  return request({
    url: `/api/admin/homestays/batch/status`,
    method: "put",
    data: {
      ids,
      status: backendStatus,
    },
  });
}
