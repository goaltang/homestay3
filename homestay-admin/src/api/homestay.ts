import request from "@/utils/request";
import type {
  Homestay,
  HomestaySearchParams,
  PageResult, // 导入 PageResult 以获得更强的类型检查
} from "@/types";
import {
  adaptPageParams,
  adaptPageResponse,
  adaptHomestayItem,
} from "@/utils/adapter";

// Temporary type definition if not available in @/types
interface HomestayTypeDTO {
  code: string;
  name: string;
  // Add other fields from backend DTO if needed (icon, description etc.)
}

interface HomestayTypeApiResponse {
  success: boolean;
  data: HomestayTypeDTO[];
  message?: string;
}

// 获取房源列表
export function getHomestayList(
  params: HomestaySearchParams
): Promise<PageResult<Homestay>> {
  const adaptedParams = adaptPageParams(params);

  // 使用 title 字段进行搜索 (如果存在)
  if ("title" in params && params.title) {
    // 直接检查原始 params
    adaptedParams.title = params.title;
    // 移除旧的 name (如果 HomestaySearchParams 中还存在)
    if ("name" in adaptedParams) {
      delete adaptedParams.name;
    }
  } else if ("name" in adaptedParams) {
    // 如果仍然是 name, 做兼容转换，但建议前端统一用 title
    adaptedParams.title = adaptedParams.name;
    delete adaptedParams.name;
  }

  // 状态转换 (保持不变)
  if (adaptedParams.status) {
    adaptedParams.status = adaptedParams.status;
  }

  // 明确返回类型
  return request<{
    content: any[]; // 后端原始数据
    totalElements: number;
    totalPages: number;
    page: number;
    size: number;
  }>({
    url: "/api/admin/homestays",
    method: "get",
    params: adaptedParams,
  }).then((response) => {
    // adaptPageResponse 应该返回 PageResult<Homestay>
    return adaptPageResponse<Homestay>(response, adaptHomestayItem);
  });
}

// 获取房源详情
export function getHomestayDetail(id: number): Promise<Homestay> {
  return request<any>({
    url: `/api/admin/homestays/${id}`,
    method: "get",
  }).then((response) => {
    // adaptHomestayItem 应该返回 Homestay 类型
    return adaptHomestayItem(response.data || response); // 处理可能的包装
  });
}

// 创建房源
export function createHomestay(data: Omit<Homestay, "id">): Promise<Homestay> {
  // 直接使用传入的 data (假设已包含 title 和新的地址字段)
  // 移除旧的 name -> title 转换
  const adaptedData = {
    ...data, // 包含 title, provinceCode, cityCode, districtCode, addressDetail 等
    // status: data.status, // status 应该已包含在 data 中
  };
  // 显式移除旧的 address (如果存在于 data)
  delete (adaptedData as any).address;

  return request<any>({
    url: "/api/admin/homestays",
    method: "post",
    data: adaptedData,
  }).then((response) => {
    return adaptHomestayItem(response.data || response);
  });
}

// 更新房源
export function updateHomestay(
  id: number,
  data: Partial<Homestay>
): Promise<Homestay> {
  // 直接使用传入的 data (假设已包含 title 和新的地址字段)
  const adaptedData: Partial<Homestay> = { ...data };

  // 移除旧的 name -> title 转换
  if ("name" in adaptedData) {
    delete (adaptedData as any).name;
  }
  // 移除旧的 address
  if ("address" in adaptedData) {
    delete (adaptedData as any).address;
  }

  // 确保传递了所有需要的字段，包括新的地址字段

  return request<any>({
    url: `/api/admin/homestays/${id}`,
    method: "put",
    data: adaptedData,
  }).then((response) => {
    return adaptHomestayItem(response.data || response);
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
  console.log(`Updating status for homestay ${id} to: ${status}`);

  return request({
    url: `/api/admin/homestays/${id}/status`,
    method: "put",
    data: { status: status },
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
  console.log(`Batch updating status for ids ${ids} to: ${status}`);

  return request({
    url: `/api/admin/homestays/batch/status`,
    method: "put",
    data: {
      ids,
      status: status,
    },
  });
}

/**
 * 获取所有激活的房源类型列表
 */
export function getActiveHomestayTypes(): Promise<HomestayTypeDTO[]> {
  return request<HomestayTypeDTO[]>({
    url: "/api/homestay-types",
    method: "get",
  }).then((response) => {
    if (Array.isArray(response)) {
      return response;
    } else {
      console.error(
        "Invalid response received for active homestay types (expected array):",
        response
      );
      return [];
    }
  });
}
