import request from "@/utils/request";
import type { User, UserSearchParams } from "@/types";
import {
  adaptPageParams,
  adaptPageResponse,
  adaptUserStatus,
  adaptUserItem,
} from "@/utils/adapter";

// 获取用户列表
export function getUserList(params: UserSearchParams) {
  // 调整前端参数为后端格式
  const adaptedParams = adaptPageParams(params);

  return request<{
    content: any[];
    totalElements: number;
    totalPages: number;
    page: number;
    size: number;
  }>({
    url: "/api/admin/users",
    method: "get",
    params: adaptedParams,
  }).then((response) => {
    // 将后端响应格式转换为前端期望的格式
    return adaptPageResponse(response, adaptUserItem);
  });
}

// 获取用户详情
export function getUserDetail(id: number) {
  return request<any>({
    url: `/api/admin/users/${id}`,
    method: "get",
  }).then((response) => {
    return adaptUserItem(response);
  });
}

// 创建用户
export function createUser(data: Omit<User, "id">) {
  // 转换前端数据为后端格式
  const adaptedData = {
    ...data,
    phoneNumber: data.phone,
    enabled: adaptUserStatus(data.status, false),
  };

  return request<any>({
    url: "/api/admin/users",
    method: "post",
    data: adaptedData,
  }).then((response) => {
    return adaptUserItem(response);
  });
}

// 更新用户
export function updateUser(id: number, data: Partial<User>) {
  // 转换前端数据为后端格式
  const adaptedData: any = { ...data };

  if ("phone" in adaptedData) {
    adaptedData.phoneNumber = adaptedData.phone;
    delete adaptedData.phone;
  }

  if ("status" in adaptedData) {
    adaptedData.enabled = adaptUserStatus(adaptedData.status, false);
    delete adaptedData.status;
  }

  return request<any>({
    url: `/api/admin/users/${id}`,
    method: "put",
    data: adaptedData,
  }).then((response) => {
    return adaptUserItem(response);
  });
}

// 删除用户
export function deleteUser(id: number) {
  return request({
    url: `/api/admin/users/${id}`,
    method: "delete",
  });
}

// 更新用户状态
export function updateUserStatus(id: number, status: boolean) {
  return request({
    url: `/api/admin/users/${id}/status`,
    method: "put",
    data: { enabled: status },
  });
}

// 重置密码
export function resetUserPassword(id: number) {
  return request({
    url: `/api/admin/users/${id}/reset-password`,
    method: "post",
  });
}

// 新增: 批量删除用户
export function batchDeleteUsers(ids: number[]) {
  return request({
    url: `/api/admin/users/batch`,
    method: "delete",
    data: { ids },
  });
}

// 新增: 批量更新用户状态
export function batchUpdateUserStatus(ids: number[], status: string) {
  const enabled = adaptUserStatus(status, false);

  return request({
    url: `/api/admin/users/batch/status`,
    method: "put",
    data: {
      ids,
      enabled,
    },
  });
}

// 新增: 批量重置密码
export function batchResetUserPasswords(ids: number[]) {
  return request<{ [id: string]: string }>({
    url: `/api/admin/users/batch/reset-password`,
    method: "post",
    data: { ids },
  });
}
