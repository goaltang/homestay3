import request from "@/utils/request";

/**
 * 获取评价列表
 * @param params 查询参数
 */
export function getReviewList(params: any) {
  return request({
    url: "/api/admin/reviews",
    method: "get",
    params,
  });
}

/**
 * 获取评价详情
 * @param id 评价ID
 */
export function getReviewDetail(id: number) {
  return request({
    url: `/api/admin/reviews/${id}`,
    method: "get",
  });
}

/**
 * 删除评价
 * @param id 评价ID
 */
export function deleteReview(id: number) {
  return request({
    url: `/api/admin/reviews/${id}`,
    method: "delete",
  });
}

/**
 * 审核评价
 * @param id 评价ID
 * @param data 审核数据
 */
export function reviewAction(id: number, data: any) {
  return request({
    url: `/api/admin/reviews/${id}/action`,
    method: "post",
    data,
  });
}

/**
 * 获取评价统计数据
 */
export function getReviewStats() {
  return request({
    url: "/api/admin/reviews/stats",
    method: "get",
  });
}
