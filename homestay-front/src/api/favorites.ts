import request from "../utils/request";

// 添加收藏
export function addFavorite(homestayId: number) {
  console.log("添加收藏，民宿ID:", homestayId);
  return request({
    url: `/api/favorites/${homestayId}`,
    method: "post",
  });
}

// 取消收藏
export function removeFavorite(homestayId: number) {
  console.log("取消收藏，民宿ID:", homestayId);
  return request({
    url: `/api/favorites/${homestayId}`,
    method: "delete",
  });
}

// 切换收藏状态
export function toggleFavorite(homestayId: number) {
  console.log("切换收藏状态，民宿ID:", homestayId);
  return request({
    url: `/api/favorites/toggle/${homestayId}`,
    method: "post",
  });
}

// 检查收藏状态
export function checkFavoriteStatus(homestayId: number) {
  return request({
    url: `/api/favorites/check/${homestayId}`,
    method: "get",
  });
}

// 获取用户收藏的民宿列表
export function getUserFavorites() {
  console.log("获取用户收藏列表");
  return request({
    url: "/api/favorites",
    method: "get",
  });
}

// 获取用户收藏的民宿ID列表
export function getUserFavoriteIds() {
  return request({
    url: "/api/favorites/ids",
    method: "get",
  });
}

// 清空用户所有收藏
export function clearUserFavorites() {
  console.log("清空用户所有收藏");
  return request({
    url: "/api/favorites/clear",
    method: "delete",
  });
}

// 获取用户收藏数量
export function getUserFavoriteCount() {
  return request({
    url: "/api/favorites/count",
    method: "get",
  });
}

// 批量检查收藏状态
export function checkFavoritesStatus(homestayIds: number[]) {
  return request({
    url: "/api/favorites/check-batch",
    method: "post",
    data: homestayIds,
  });
}
