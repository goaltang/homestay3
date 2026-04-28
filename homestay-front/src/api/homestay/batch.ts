import request from "../../utils/request";

/**
 * 批量激活房源
 */
export function batchActivateHomestays(ids: number[]) {
  return request({
    url: "/api/homestays/batch/activate",
    method: "post",
    data: { ids },
  }).catch((error) => {
    console.error(`批量激活房源失败`, error);
    throw error;
  });
}

/**
 * 批量下架房源
 */
export function batchDeactivateHomestays(ids: number[]) {
  return request({
    url: "/api/homestays/batch/deactivate",
    method: "post",
    data: { ids },
  }).catch((error) => {
    console.error(`批量下架房源失败`, error);
    throw error;
  });
}

/**
 * 批量删除房源
 */
export function batchDeleteHomestays(ids: number[]) {
  return request({
    url: "/api/homestays/batch",
    method: "delete",
    data: { ids },
  }).catch((error) => {
    console.error(`批量删除房源失败`, error);
    throw error;
  });
}

/**
 * 批量分配房源到分组
 */
export function batchAssignToGroup(groupId: number, homestayIds: number[]) {
  return request({
    url: `/api/host/groups/${groupId}/assign`,
    method: "post",
    data: { homestayIds },
  });
}

/**
 * 批量移除房源分组
 */
export function batchRemoveFromGroup(homestayIds: number[]) {
  return request({
    url: "/api/host/groups/remove",
    method: "post",
    data: { homestayIds },
  });
}
