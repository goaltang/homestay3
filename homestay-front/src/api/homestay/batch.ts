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
    console.error(`批量激活房源失败(无版本路径)`, error);
    console.log(`尝试使用v1 API批量激活房源`);
    return request({
      url: "/api/v1/homestays/batch/activate",
      method: "post",
      data: { ids },
    });
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
    console.error(`批量下架房源失败(无版本路径)`, error);
    console.log(`尝试使用v1 API批量下架房源`);
    return request({
      url: "/api/v1/homestays/batch/deactivate",
      method: "post",
      data: { ids },
    });
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
    console.error(`批量删除房源失败(无版本路径)`, error);
    console.log(`尝试使用v1 API批量删除房源`);
    return request({
      url: "/api/v1/homestays/batch",
      method: "delete",
      data: { ids },
    });
  });
}
