import request from "@/utils/request";

// 获取房东收益统计
export function getHostEarnings(params?: any) {
  return request({
    url: "/api/host/earnings",
    method: "get",
    params,
  });
}

// 获取房东信息
export function getHostInfo() {
  return request({
    url: "/api/host/info",
    method: "get",
  });
}

// 获取房源的房东信息
export function getHomestayHostInfo(homestayId: number) {
  console.log(`开始获取房源ID为${homestayId}的房东信息`);

  return request({
    url: `/api/host/info/${homestayId}`,
    method: "get",
  })
    .then((response) => {
      console.log(`获取房源房东信息成功，房源ID: ${homestayId}`, response.data);
      return response;
    })
    .catch((error) => {
      console.error(`获取房源房东信息失败，房源ID: ${homestayId}`, error);

      // 如果是开发环境且请求失败，返回模拟数据
      if (process.env.NODE_ENV === "development") {
        console.log("使用房东模拟数据");
        return {
          data: {
            id: 101,
            name: "张晓明",
            avatar:
              "https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png",
            rating: 4.92,
            accommodations: 156,
            years: 5,
            responseRate: "99%",
            responseTime: "1小时内",
            companions: [
              {
                name: "李华",
                avatar:
                  "https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png",
              },
              {
                name: "王芳",
                avatar:
                  "https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png",
              },
            ],
          },
        };
      }

      // 如果不是开发环境，继续抛出错误
      throw error;
    });
}

// 更新房东信息
export function updateHostInfo(data: any) {
  return request({
    url: "/api/host/info",
    method: "put",
    data,
  });
}

// 成为房东
export function becomeHost(data: any) {
  return request({
    url: "/api/host/register",
    method: "post",
    data,
  });
}

// 获取房东统计数据
export function getHostStatistics() {
  return request({
    url: "/api/host/statistics",
    method: "get",
  });
}

// 上传房东头像
export function uploadHostAvatar(data: FormData) {
  return request({
    url: "/api/host/avatar",
    method: "post",
    data,
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
}

// 获取房东房源列表
export function getHostHomestays(params?: any) {
  return request({
    url: "/api/host/homestays",
    method: "get",
    params,
  });
}

// 获取房东订单列表
export function getHostOrders(params?: any) {
  return request({
    url: "/api/host/orders",
    method: "get",
    params,
  });
}

// 获取房东评价列表
export function getHostReviews(params?: any) {
  return request({
    url: "/api/host/reviews",
    method: "get",
    params,
  });
}
