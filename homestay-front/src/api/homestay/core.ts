import request from "../../utils/request";
import type { Homestay } from "../../types/homestay";

/**
 * 获取所有房源列表
 */
export function getHomestays(params?: {
  page?: number;
  size?: number;
  status?: string;
  type?: string;
}) {
  console.log("获取房源列表，参数:", params);
  return request({
    url: "/api/homestays",
    method: "get",
    params,
  })
    .then((response) => {
      console.log("获取房源列表成功(无版本路径):", response.data);

      if (
        response.data &&
        typeof response.data === "object" &&
        response.data.success === false
      ) {
        console.warn("后端返回错误响应:", response.data);
        const fallbackResponse = {
          ...response,
          data: response.data.data || [],
        };
        return fallbackResponse;
      }

      return response;
    })
    .catch((error) => {
      console.error("获取房源列表失败(无版本路径):", error);

      console.log(`尝试使用v1 API获取房源列表`);
      return request({
        url: "/api/v1/homestays",
        method: "get",
        params,
      })
        .then((backupResponse) => {
          console.log("获取房源列表成功(v1):", backupResponse.data);

          if (
            backupResponse.data &&
            typeof backupResponse.data === "object" &&
            backupResponse.data.success === false
          ) {
            console.warn("v1版本也返回错误响应:", backupResponse.data);
            const fallbackResponse = {
              ...backupResponse,
              data: backupResponse.data.data || [],
            };
            return fallbackResponse;
          }

          return backupResponse;
        })
        .catch((backupError) => {
          console.error("获取房源列表失败(v1):", backupError);
          console.log("使用模拟数据作为降级方案");
          return {
            data: [],
            status: 200,
            statusText: "OK",
            headers: {},
            config: {},
            request: {},
          };
        });
    });
}

/**
 * 获取房源详情
 */
export function getHomestayById(id: number, referringCriteria?: string) {
  console.log(`开始获取房源详情, ID: ${id}, 搜索条件: ${referringCriteria}`);

  const params: Record<string, any> = {};
  if (referringCriteria) {
    params.referring_criteria = referringCriteria;
  }

  return request({
    url: `/api/homestays/${id}`,
    method: "get",
    params: params,
  })
    .then((response) => {
      console.log(
        `房源详情获取成功(无版本路径), ID: ${id}, 数据:`,
        response.data
      );
      return response;
    })
    .catch((error) => {
      console.error(`房源详情获取失败(无版本路径), ID: ${id}, 错误:`, error);

      console.log(`尝试使用v1 API获取房源详情, ID: ${id}`);
      return request({
        url: `/api/v1/homestays/${id}`,
        method: "get",
        params: params,
      })
        .then((backupResponse) => {
          console.log(
            `房源详情获取成功(v1), ID: ${id}, 数据:`,
            backupResponse.data
          );
          return backupResponse;
        })
        .catch((backupError) => {
          console.error(`房源详情获取失败(v1), ID: ${id}, 错误:`, backupError);
          throw backupError;
        });
    });
}

/**
 * 获取房东的房源列表
 */
export function getOwnerHomestays(params?: {
  page?: number;
  size?: number;
  status?: string;
  type?: string;
}) {
  return request({
    url: "/api/homestays/owner",
    method: "get",
    params,
  }).catch((error) => {
    console.error(`获取房东房源列表失败(无版本路径)`, error);
    console.log(`尝试使用v1 API获取房东房源列表`);
    return request({
      url: "/api/v1/homestays/owner",
      method: "get",
      params,
    });
  });
}

/**
 * 创建新房源
 */
export function createHomestay(data: Omit<Homestay, "id">) {
  const cleanData: Record<string, any> = { ...data };

  if (cleanData.amenities && Array.isArray(cleanData.amenities)) {
    if (
      cleanData.amenities.length > 0 &&
      typeof cleanData.amenities[0] === "object"
    ) {
      cleanData.amenities = cleanData.amenities
        .map((amenity: any) =>
          typeof amenity === "string"
            ? amenity
            : amenity.value || amenity.code || ""
        )
        .filter(Boolean);
    }
  }

  if (typeof cleanData.price === "number") {
    cleanData.price = String(cleanData.price);
  }

  cleanData.maxGuests = Number(cleanData.maxGuests) || 1;
  cleanData.minNights = Number(cleanData.minNights) || 1;

  if (!cleanData.status) {
    cleanData.status = "INACTIVE";
  }

  if (!cleanData.images || !Array.isArray(cleanData.images)) {
    cleanData.images = [];
  }

  Object.keys(cleanData).forEach((key) => {
    if (cleanData[key] === null || cleanData[key] === undefined) {
      delete cleanData[key];
    }
  });

  console.log(`开始创建房源(处理后数据):`, cleanData);

  return request({
    url: "/api/homestays",
    method: "post",
    data: cleanData,
    timeout: 30000,
  })
    .then((response) => {
      console.log(`房源创建成功:`, response.data);

      const newHomestayId = response.data?.id;
      if (
        newHomestayId &&
        cleanData.amenities &&
        cleanData.amenities.length > 0 &&
        (!response.data.amenities || response.data.amenities.length === 0)
      ) {
        console.log(`设施可能未成功保存，尝试使用addAllAmenitiesToHomestay...`);

        return import("../amenities").then(
          ({ addAllAmenitiesToHomestayApi }) => {
            return addAllAmenitiesToHomestayApi(newHomestayId)
              .then(() => {
                console.log(`使用addAllAmenitiesToHomestay添加设施成功`);
                return response;
              })
              .catch((error) => {
                console.error(
                  `使用addAllAmenitiesToHomestay添加设施失败:`,
                  error
                );
                return response;
              });
          }
        );
      }

      return response;
    })
    .catch((error) => {
      console.error(`房源创建失败:`, error);
      throw error;
    });
}

/**
 * 更新房源信息
 */
export function updateHomestay(
  id: number,
  data: Partial<Homestay>
): Promise<any> {
  console.log(`开始更新房源, ID: ${id}`, data);

  const token = localStorage.getItem("token");
  if (!token) {
    console.error("⚠️ 更新房源失败：没有找到token，请先登录！");
    return Promise.reject(new Error("未登录，无法更新房源"));
  }

  console.log(`使用token更新房源: ${token.substring(0, 15)}...`);

  let userInfo;
  let userId = "";
  let username = "";

  try {
    const userInfoStr = localStorage.getItem("userInfo");
    if (userInfoStr) {
      userInfo = JSON.parse(userInfoStr);
      userId = userInfo.id || "";
      username = userInfo.username || "";
      console.log(`从userInfo获取到用户信息: ID=${userId}, 用户名=${username}`);
    }

    if (!username) {
      const user = JSON.parse(localStorage.getItem("user") || "{}");
      if (user.username) {
        username = user.username;
        userId = user.id || "";
        console.log(
          `从user对象获取到用户信息: ID=${userId}, 用户名=${username}`
        );
      }
    }
  } catch (e) {
    console.error("解析用户信息失败", e);
  }

  const cleanData: Record<string, any> = { ...data };

  cleanData.ownerUsername = username;
  console.log(`设置房源所有者: ${username}`);

  if (cleanData.amenities && Array.isArray(cleanData.amenities)) {
    cleanData.amenities = cleanData.amenities
      .map((amenity: any) => {
        if (typeof amenity === "string") {
          return amenity;
        } else if (typeof amenity === "object" && amenity !== null) {
          return amenity.value || amenity.code || "";
        }
        return null;
      })
      .filter(Boolean);

    console.log("格式化后的设施数据:", cleanData.amenities);
  } else {
    cleanData.amenities = [];
  }

  if (typeof cleanData.price === "number") {
    cleanData.price = String(cleanData.price);
  }

  delete cleanData.id;
  delete cleanData.createdAt;
  delete cleanData.updatedAt;
  delete cleanData.ownerInfo;

  const headers = {
    "Content-Type": "application/json",
    Authorization: token.startsWith("Bearer ") ? token : `Bearer ${token}`,
    "X-Username": username,
    "X-User-Id": userId,
  };

  console.log(`发送PUT请求到 /api/homestays/${id}`);
  console.log(
    `认证信息: 用户名=${username}, 用户ID=${userId}, Token格式正确=${token.startsWith(
      "Bearer "
    )}`
  );
  console.log(`请求数据:`, JSON.stringify(cleanData, null, 2));

  return request({
    url: `/api/homestays/${id}`,
    method: "put",
    data: cleanData,
    headers: headers,
    timeout: 30000,
  })
    .then((response) => {
      console.log(`房源更新成功, ID: ${id}`, response.data);
      return { success: true, data: response.data };
    })
    .catch((error) => {
      console.error(`更新房源失败, ID: ${id}, 错误:`, error);

      if (
        error.status === 403 ||
        (error.response && error.response.status === 403)
      ) {
        const message = "您没有权限更新此房源，请确认您已登录并有相应权限";
        console.error(message);

        if (typeof window !== "undefined") {
          setTimeout(() => {
            window.location.href = "/login";
          }, 1500);
        }
      }

      return Promise.reject({ success: false, error: error });
    });
}

/**
 * 删除房源
 */
export function deleteHomestay(id: number) {
  console.log(`开始删除房源, ID: ${id}`);

  const token = localStorage.getItem("token") || "";

  let username = "";
  try {
    const userInfo = JSON.parse(localStorage.getItem("userInfo") || "{}");
    username = userInfo.username || "";
  } catch (e) {
    console.warn("无法获取用户信息", e);
  }

  const headers = {
    Authorization: `Bearer ${token}`,
    "X-Username": username,
  };

  console.log(`尝试删除房源, ID: ${id}`);
  return request({
    url: `/api/homestays/${id}`,
    method: "delete",
    headers,
    timeout: 15000,
  })
    .then((response) => {
      console.log(`房源删除成功, ID: ${id}`, response);
      return response;
    })
    .catch((error) => {
      console.error(`删除房源失败, ID: ${id}`, error);
      throw error;
    });
}

/**
 * 更改房源状态：激活
 */
export function activateHomestay(id: number) {
  return request({
    url: `/api/homestays/${id}/activate`,
    method: "patch",
    data: {},
  }).catch((error) => {
    console.error(`激活房源失败(无版本路径), ID: ${id}`, error);
    console.log(`尝试使用v1 API激活房源, ID: ${id}`);
    return request({
      url: `/api/v1/homestays/${id}/activate`,
      method: "patch",
      data: {},
    });
  });
}

/**
 * 更改房源状态：停用
 */
export function deactivateHomestay(id: number) {
  return request({
    url: `/api/homestays/${id}/deactivate`,
    method: "patch",
    data: {},
  }).catch((error) => {
    console.error(`停用房源失败(无版本路径), ID: ${id}`, error);
    console.log(`尝试使用v1 API停用房源, ID: ${id}`);
    return request({
      url: `/api/v1/homestays/${id}/deactivate`,
      method: "patch",
      data: {},
    });
  });
}

/**
 * 获取已上架房源列表
 */
export function getActiveHomestays(params?: {
  page?: number;
  size?: number;
  featured?: boolean;
}) {
  console.log("获取已上架房源列表，参数:", params);
  return request({
    url: "/api/homestays/status/ACTIVE",
    method: "get",
    params: params,
  })
    .then((response) => {
      console.log("获取已上架房源列表成功(分页接口):", response.data);
      return response;
    })
    .catch((error) => {
      console.error("获取已上架房源列表失败(分页接口):", error);

      console.log(`回退到原始API接口`);
      return request({
        url: "/api/homestays",
        method: "get",
        params: {
          ...params,
          status: "ACTIVE",
        },
      })
        .then((backupResponse) => {
          console.log("获取已上架房源列表成功(原始接口):", backupResponse.data);
          const data = backupResponse.data || [];
          return {
            ...backupResponse,
            data: {
              data: data,
              total: data.length,
              page: 0,
              size: data.length,
              pages: 1,
            },
          };
        })
        .catch((backupError) => {
          console.error("获取已上架房源列表失败(原始接口):", backupError);
          throw backupError;
        });
    });
}

/**
 * 根据ID列表批量获取房源
 */
export function getHomestaysByIds(ids: number[]) {
  console.log("根据ID列表获取房源，IDs:", ids);

  return request({
    url: "/api/homestays/by-ids",
    method: "post",
    data: { ids },
  })
    .then((response) => {
      console.log("批量获取房源成功:", response.data);
      return response;
    })
    .catch((error) => {
      console.error("批量获取房源失败，尝试备用API:", error);

      return Promise.all(
        ids.map((id) =>
          request({
            url: `/api/homestays/${id}`,
            method: "get",
          })
            .then((response) => response.data)
            .catch((error) => {
              console.warn(`获取ID为${id}的房源失败:`, error);
              return null;
            })
        )
      ).then((results) => {
        const validResults = results.filter((r) => r !== null);
        console.log("批量获取房源成功(备用方法):", validResults);
        return { data: validResults };
      });
    });
}

/**
 * 验证当前用户是否拥有特定房源的权限
 */
export function checkHomestayOwnership(id: number): Promise<boolean> {
  console.log(`检查房源ID:${id}的所有权`);
  const token = localStorage.getItem("token");

  if (!token) {
    console.error("未找到token，无法验证所有权");
    return Promise.resolve(false);
  }

  return request({
    url: `/api/homestays/${id}`,
    method: "get",
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })
    .then((response) => {
      const homestayData = response.data;
      const userInfo = JSON.parse(localStorage.getItem("userInfo") || "{}");
      const currentUsername = userInfo.username;

      console.log(`房源所有者: ${homestayData.ownerUsername}`);
      console.log(`当前用户: ${currentUsername}`);

      const isOwner = homestayData.ownerUsername === currentUsername;

      if (!isOwner) {
        console.error(
          `权限不足: 该房源属于 ${homestayData.ownerUsername}，而非当前用户 ${currentUsername}`
        );
      } else {
        console.log("所有权验证通过，当前用户是该房源的拥有者");
      }

      return isOwner;
    })
    .catch((error) => {
      console.error(`检查房源所有权失败:`, error);
      return false;
    });
}

/**
 * 获取房源不可用日期
 */
export function getHomestayUnavailableDates(id: number) {
  return request({
    url: `/api/homestays/${id}/unavailable-dates`,
    method: "get",
  });
}
