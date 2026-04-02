import request from "@/utils/request";

/**
 * 获取评价列表 (管理员)
 * @param params 查询参数 (分页, 筛选等)
 */
export function getAdminReviewList(params: any) {
  return request({
    url: "/api/reviews/admin",
    method: "get",
    params,
  });
}

/**
 * 删除评价 (管理员)
 * @param id 评价ID
 */
export function deleteReview(id: number) {
  return request({
    url: `/api/reviews/${id}`,
    method: "delete",
  });
}

/**
 * 设置评价可见性 (管理员)
 * @param id 评价ID
 * @param isVisible 是否可见
 */
export function setReviewVisibility(id: number, isVisible: boolean) {
  return request({
    url: `/api/reviews/${id}/visibility`,
    method: "patch",
    data: { visible: isVisible },
  });
}

/**
 * 获取评价统计数据 (管理员)
 */
export function getAdminReviewStats() {
  return request({
    url: "/api/reviews/stats",
    method: "get",
  });
}

/**
 * 批量设置评价可见性 (管理员)
 * @param ids 评价ID列表
 * @param isVisible 是否可见
 */
export function batchSetVisibility(ids: number[], isVisible: boolean) {
  return request({
    url: "/api/admin/reviews/batch/visibility",
    method: "post",
    data: { ids, isVisible },
  });
}

/**
 * 批量删除评价 (管理员)
 * @param ids 评价ID列表
 */
export function batchDelete(ids: number[]) {
  return request({
    url: "/api/admin/reviews/batch/delete",
    method: "post",
    data: { ids },
  });
}
