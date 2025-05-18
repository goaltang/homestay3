import request from "@/utils/request";

// 获取评论列表
export function getReviews(params?: any) {
  return request({
    url: "/api/reviews",
    method: "get",
    params,
  });
}

// 获取评论详情
export function getReviewDetail(id: number) {
  return request({
    url: `/api/reviews/${id}`,
    method: "get",
  });
}

// 创建评论
export function createReview(data: any) {
  console.warn("createReview方法已弃用，请使用submitReview方法");
  return submitReview(data);
}

// 更新评论
export function updateReview(id: number, data: any) {
  return request({
    url: `/api/reviews/${id}`,
    method: "put",
    data,
  });
}

// 删除评论
export function deleteReview(id: number) {
  return request({
    url: `/api/reviews/${id}`,
    method: "delete",
  });
}

/**
 * 获取房源的评价列表
 * @param homestayId 房源ID
 * @param params 分页参数
 */
export function getHomestayReviews(
  homestayId: number,
  params?: { page?: number; size?: number }
) {
  console.log(`获取房源评价，ID: ${homestayId}，参数:`, params);
  return request({
    url: `/api/reviews/homestay/${homestayId}`,
    method: "get",
    params,
  })
    .then((response) => {
      console.log(`房源评价获取成功，ID: ${homestayId}`, response.data);
      return response;
    })
    .catch((error) => {
      console.error(`房源评价获取失败，ID: ${homestayId}`, error);

      // 如果是开发环境，返回模拟数据
      if (process.env.NODE_ENV === "development") {
        console.log("使用评价模拟数据");
        const mockReviews = [
          {
            id: 1,
            userId: 101,
            userName: "张先生",
            rating: 5,
            content:
              "非常棒的住宿体验，房间干净整洁，设施齐全，位置也很方便。房东很热情，给了我们很多当地的旅游建议。下次来还会选择这里。",
            createTime: "2023-03-15T14:30:00",
            avatarUrl:
              "https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png",
          },
          {
            id: 2,
            userId: 102,
            userName: "李女士",
            rating: 4.5,
            content:
              "房间比照片上看起来要小一些，但是整体还是很满意的。床很舒适，周围环境也很安静。",
            createTime: "2023-02-22T10:15:00",
            avatarUrl:
              "https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png",
            response:
              "感谢您的评价，关于房间大小的问题我们已经更新了更准确的描述，希望您下次再来入住！",
          },
          {
            id: 3,
            userId: 103,
            userName: "王先生",
            rating: 4,
            content:
              "位置很好，离地铁站很近，购物也方便。就是空调有点吵，希望能改进。",
            createTime: "2023-01-10T18:45:00",
            avatarUrl:
              "https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png",
          },
        ];

        const page = params?.page || 0;
        const size = params?.size || 10;
        const start = page * size;
        const end = start + size;

        return {
          data: {
            content: mockReviews.slice(start, end),
            totalElements: mockReviews.length,
            totalPages: Math.ceil(mockReviews.length / size),
            size: size,
            number: page,
            first: page === 0,
            last: end >= mockReviews.length,
          },
        };
      }

      throw error;
    });
}

/**
 * 获取房源评价统计信息
 * @param homestayId 房源ID
 */
export function getHomestayReviewStats(homestayId: number) {
  return request({
    url: `/api/reviews/homestay/${homestayId}/stats`,
    method: "get",
  })
    .then((response) => {
      console.log(`房源评价统计获取成功，ID: ${homestayId}`, response.data);
      return response;
    })
    .catch((error) => {
      console.error(`房源评价统计获取失败，ID: ${homestayId}`, error);

      // 如果是开发环境，返回模拟数据
      if (process.env.NODE_ENV === "development") {
        console.log("使用评价统计模拟数据");
        return {
          data: {
            averageRating: 4.7,
            reviewCount: 68,
            detailedRatings: {
              cleanlinessRating: 4.8,
              accuracyRating: 4.7,
              communicationRating: 4.9,
              locationRating: 4.6,
              checkInRating: 4.8,
              valueRating: 4.5,
            },
          },
        };
      }

      throw error;
    });
}

/**
 * 获取用户的评价列表
 * @param params 分页参数
 */
export function getUserReviews(params?: { page?: number; size?: number }) {
  return request({
    url: "/api/reviews/user",
    method: "get",
    params,
  });
}

/**
 * 提交房源评价
 * @param data 评价数据
 */
export function submitReview(data: {
  orderId: number;
  homestayId: number;
  rating: number;
  content: string;
  cleanlinessRating?: number;
  accuracyRating?: number;
  communicationRating?: number;
  locationRating?: number;
  checkInRating?: number;
  valueRating?: number;
}) {
  console.log("Submitting review with data:", data);
  return request({
    url: "/api/reviews",
    method: "post",
    data,
  });
}

// 房东回复评价
export function respondToReview(reviewId: number, response: string) {
  return request({
    url: `/api/reviews/${reviewId}/response`,
    method: "post",
    data: { response },
  });
}

// --- 新增：删除房东回复 ---
/**
 * 删除房东对评价的回复
 * @param reviewId 评价ID
 */
export function deleteReviewResponse(reviewId: number) {
  return request<void>({
    url: `/api/reviews/${reviewId}/response`,
    method: "delete",
  });
}
// --- 结束新增 ---

/**
 * 获取房东的评价列表（带筛选和分页）
 * @param params 筛选和分页参数
 */
export function getHostReviews(params?: {
  homestayId?: number | null;
  rating?: number | null;
  replyStatus?: string | null;
  page?: number; // 0-based
  size?: number;
  sort?: string; // <-- 新增: 允许传递 sort 参数 (e.g., "createTime,desc")
}) {
  // 过滤掉 null 或 undefined 的参数
  const filteredParams = Object.entries(params || {})
    .filter(([_, value]) => value !== null && value !== undefined)
    .reduce((acc, [key, value]) => ({ ...acc, [key]: value }), {});

  return request({
    url: "/api/reviews/host",
    method: "get",
    params: filteredParams,
  });
}

/**
 * 获取房东的评价统计数据
 */
export function getHostReviewStats() {
  return request({
    url: "/api/reviews/host/stats",
    method: "get",
  });
}
