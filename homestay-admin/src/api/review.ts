import request from "@/utils/request";

/**
 * 获取评价列表 (管理员)
 * @param params 查询参数 (分页, 筛选等)
 */
export function getAdminReviewList(params: any) {
  // 确认后端管理员获取评价列表的实际 URL
  return request({
    url: "/api/reviews/admin", // <-- Update URL to use the new admin endpoint
    method: "get",
    params, // 参数名 rating, status, page, size, sort 应能被后端正确接收
  });
}

/**
 * 获取评价详情 (如果需要)
 * @param id 评价ID
 */
// export function getReviewDetail(id: number) { ... }

/**
 * 删除评价 (管理员)
 * @param id 评价ID
 */
export function deleteReview(id: number) {
  // 确认后端删除评价的实际 URL
  return request({
    url: `/api/reviews/${id}`, // 使用标准的删除路径
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
  // 确认后端管理员获取统计的实际 URL
  return request({
    // url: "/api/reviews/admin/stats", // 如果后端是这个路径
    url: "/api/reviews/stats", // 假设是这个路径
    method: "get",
  });
}

// 移除或注释掉不再使用的旧函数，例如 reviewAction (如果不需要)
/**
 * 审核评价 (旧函数，可能不再需要)
 * @param id 评价ID
 * @param data 审核数据
 */
// export function reviewAction(id: number, data: any) { ... }
