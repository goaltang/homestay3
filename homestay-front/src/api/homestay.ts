import request from "../utils/request";
import { ElMessage } from "element-plus";
import type { Homestay } from "../types/homestay";
import {
  removeAllAmenitiesFromHomestayApi,
  addAmenityToHomestayApi,
} from "./amenities";
import { useUserStore } from "../stores/user";
import router from "../router";

// 定义并导出后端返回的 HomestayType 结构
export interface HomestayType {
  id: number;
  code: string;
  name: string;
  icon: string;
  // description?: string;
}

// 定义 API 函数的参数类型 (如果需要)
export interface HomestayListParams {
  page?: number;
  size?: number;
  // ... 其他过滤参数
}

// 定义设施 DTO 结构 (与后端 AmenityDTO 对应)
export interface AmenityOption {
  id?: number; // 后端可能有 ID
  value: string; // 通常是唯一标识符，如 'WiFi'
  label: string; // 显示给用户的名称，如 'WiFi'
  icon?: string; // 可能有图标
}

// 定义按分类分组的设施数据结构
export interface AmenityCategoryOption {
  code?: string;
  name: string; // 分类名称
  icon?: string;
  sortOrder?: number;
  amenities: AmenityOption[]; // 该分类下的设施列表
}

// 获取所有房源列表
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
      return response;
    })
    .catch((error) => {
      console.error("获取房源列表失败(无版本路径):", error);

      // 如果无版本路径失败，尝试v1版本
      console.log(`尝试使用v1 API获取房源列表`);
      return request({
        url: "/api/v1/homestays",
        method: "get",
        params,
      })
        .then((backupResponse) => {
          console.log("获取房源列表成功(v1):", backupResponse.data);
          return backupResponse;
        })
        .catch((backupError) => {
          console.error("获取房源列表失败(v1):", backupError);
          throw backupError;
        });
    });
}

// 获取房源详情
export function getHomestayById(id: number) {
  console.log(`开始获取房源详情, ID: ${id}`);

  // 先尝试无版本路径，这个似乎总是能正常工作
  return request({
    url: `/api/homestays/${id}`,
    method: "get",
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

      // 如果无版本路径失败，再尝试v1版本
      console.log(`尝试使用v1 API获取房源详情, ID: ${id}`);
      return request({
        url: `/api/v1/homestays/${id}`,
        method: "get",
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

// 获取房东的房源列表
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

    // 如果无版本路径失败，尝试v1版本
    console.log(`尝试使用v1 API获取房东房源列表`);
    return request({
      url: "/api/v1/homestays/owner",
      method: "get",
      params,
    });
  });
}

// 创建新房源
export function createHomestay(data: Omit<Homestay, "id">) {
  // 处理设施数据，确保是字符串数组
  const cleanData: Record<string, any> = { ...data };

  // 处理amenities字段，确保它是字符串数组
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

  // 确保price是字符串类型
  if (typeof cleanData.price === "number") {
    cleanData.price = String(cleanData.price);
  }

  // 确保必填字段存在且格式正确
  cleanData.maxGuests = Number(cleanData.maxGuests) || 1;
  cleanData.minNights = Number(cleanData.minNights) || 1;

  // 确保状态字段有默认值
  if (!cleanData.status) {
    cleanData.status = "INACTIVE";
  }

  // 确保图片数组是有效的
  if (!cleanData.images || !Array.isArray(cleanData.images)) {
    cleanData.images = [];
  }

  // 移除null和undefined值
  Object.keys(cleanData).forEach((key) => {
    if (cleanData[key] === null || cleanData[key] === undefined) {
      delete cleanData[key];
    }
  });

  console.log(`开始创建房源(处理后数据):`, cleanData);

  // 尝试直接创建房源，包含设施数据
  return request({
    url: "/api/homestays",
    method: "post",
    data: cleanData,
    timeout: 30000, // 增加超时时间
  })
    .then((response) => {
      console.log(`房源创建成功:`, response.data);

      // 如果房源创建成功，但没有保存设施，尝试使用addAllAmenitiesToHomestay
      const newHomestayId = response.data?.id;
      if (
        newHomestayId &&
        cleanData.amenities &&
        cleanData.amenities.length > 0 &&
        (!response.data.amenities || response.data.amenities.length === 0)
      ) {
        console.log(`设施可能未成功保存，尝试使用addAllAmenitiesToHomestay...`);

        // 导入设施相关API
        return import("./amenities").then(
          ({ addAllAmenitiesToHomestayApi }) => {
            // 尝试一键添加所有设施
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

                // 即使添加设施失败，仍返回原始响应，让用户知道房源已创建
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

// 更新房源信息
export function updateHomestay(
  id: number,
  data: Partial<Homestay>
): Promise<any> {
  console.log(`开始更新房源, ID: ${id}`, data);

  // 确保当前有有效token
  const token = localStorage.getItem("token");
  if (!token) {
    console.error("⚠️ 更新房源失败：没有找到token，请先登录！");
    ElMessage.error("登录已过期，请重新登录后再试");
    return Promise.reject(new Error("未登录，无法更新房源"));
  }

  console.log(`使用token更新房源: ${token.substring(0, 15)}...`);

  // 获取用户信息
  let userInfo;
  let userId = "";
  let username = "";

  try {
    // 尝试获取完整用户信息
    const userInfoStr = localStorage.getItem("userInfo");
    if (userInfoStr) {
      userInfo = JSON.parse(userInfoStr);
      userId = userInfo.id || "";
      username = userInfo.username || "";
      console.log(`从userInfo获取到用户信息: ID=${userId}, 用户名=${username}`);
    }

    // 如果无法从userInfo获取，尝试从其他来源获取
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

  // 处理设施数据，确保格式正确
  const cleanData: Record<string, any> = { ...data };

  // 确保设置了ownerUsername (重要！权限验证依赖此字段)
  cleanData.ownerUsername = username;
  console.log(`设置房源所有者: ${username}`);

  // 格式化设施数据，转换为简单字符串数组格式
  if (cleanData.amenities && Array.isArray(cleanData.amenities)) {
    // 将复杂对象转为简单字符串值
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
    // 确保amenities始终是数组
    cleanData.amenities = [];
  }

  // 确保price是字符串类型
  if (typeof cleanData.price === "number") {
    cleanData.price = String(cleanData.price);
  }

  // 删除多余字段，避免后端验证错误
  delete cleanData.id; // 避免ID冲突
  delete cleanData.createdAt;
  delete cleanData.updatedAt;
  delete cleanData.ownerInfo;

  // 设置明确的请求头，包含所有可能需要的认证信息
  const headers = {
    "Content-Type": "application/json",
    Authorization: token.startsWith("Bearer ") ? token : `Bearer ${token}`, // 确保正确的格式
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

  // 发送PUT请求更新房源
  return request({
    url: `/api/homestays/${id}`,
    method: "put",
    data: cleanData,
    headers: headers,
    timeout: 30000,
  })
    .then((response) => {
      console.log(`房源更新成功, ID: ${id}`, response.data);
      ElMessage.success("房源信息已成功更新");
      return { success: true, data: response.data };
    })
    .catch((error) => {
      console.error(`更新房源失败, ID: ${id}, 错误:`, error);

      // 检查是否是权限问题
      if (
        error.status === 403 ||
        (error.response && error.response.status === 403)
      ) {
        ElMessage.error("您没有权限更新此房源，请确认您已登录并有相应权限");

        // 提示用户刷新页面或重新登录
        ElMessage.warning("请刷新页面或重新登录后再尝试此操作");

        // 如果浏览器环境下，可以尝试重定向到登录
        if (typeof window !== "undefined") {
          setTimeout(() => {
            window.location.href = "/login";
          }, 1500);
        }
      } else {
        ElMessage.error("更新房源失败，请检查网络或联系管理员");
      }

      return Promise.reject({ success: false, error: error });
    });
}

// 删除房源
export function deleteHomestay(id: number) {
  console.log(`开始删除房源, ID: ${id}`);

  // 确保当前有有效token
  const token = localStorage.getItem("token") || "";

  // 获取用户名
  let username = "";
  try {
    const userInfo = JSON.parse(localStorage.getItem("userInfo") || "{}");
    username = userInfo.username || "";
  } catch (e) {
    console.warn("无法获取用户信息", e);
  }

  const headers = {
    Authorization: `Bearer ${token}`,
    "X-Username": username, // 确保包含用户名在请求头中
  };

  // 直接尝试删除房源，简化逻辑
  console.log(`尝试删除房源, ID: ${id}`);
  return request({
    url: `/api/homestays/${id}`,
    method: "delete",
    headers,
    timeout: 15000,
  })
    .then((response) => {
      console.log(`房源删除成功, ID: ${id}`, response);

      // 检查响应是否包含成功标志
      if (response && response.data && response.data.success) {
        ElMessage.success(response.data.message || "房源已成功删除");
      } else {
        ElMessage.success("房源已删除");
      }

      return response;
    })
    .catch((error) => {
      console.error(`删除房源失败, ID: ${id}`, error);

      // 获取错误信息
      let errorMessage = "删除房源失败，请稍后再试";

      if (error.response && error.response.data) {
        if (error.response.data.message) {
          errorMessage = error.response.data.message;
        } else if (error.response.data.error) {
          errorMessage = error.response.data.error;
        }
      }

      ElMessage.error(errorMessage);
      return Promise.reject(error);
    });
}

// 更改房源状态：激活
export function activateHomestay(id: number) {
  return request({
    url: `/api/homestays/${id}/activate`,
    method: "patch",
    data: {},
  }).catch((error) => {
    console.error(`激活房源失败(无版本路径), ID: ${id}`, error);

    // 如果无版本路径失败，尝试v1版本
    console.log(`尝试使用v1 API激活房源, ID: ${id}`);
    return request({
      url: `/api/v1/homestays/${id}/activate`,
      method: "patch",
      data: {},
    });
  });
}

// 更改房源状态：停用
export function deactivateHomestay(id: number) {
  return request({
    url: `/api/homestays/${id}/deactivate`,
    method: "patch",
    data: {},
  }).catch((error) => {
    console.error(`停用房源失败(无版本路径), ID: ${id}`, error);

    // 如果无版本路径失败，尝试v1版本
    console.log(`尝试使用v1 API停用房源, ID: ${id}`);
    return request({
      url: `/api/v1/homestays/${id}/deactivate`,
      method: "patch",
      data: {},
    });
  });
}

// 上传房源图片
export function uploadHomestayImage(
  file: File,
  imageType: string = "gallery",
  homestayId?: number
) {
  console.log(
    `开始上传房源${imageType === "cover" ? "封面" : "图片集"}图片:`,
    file.name,
    "大小:",
    (file.size / 1024).toFixed(2),
    "KB",
    homestayId ? `民宿ID: ${homestayId}` : ""
  );

  const formData = new FormData();
  formData.append("file", file);

  let url;

  // 根据是否有homestayId决定使用哪个API端点
  if (homestayId) {
    url = "/api/homestay-images/upload";
    formData.append("homestayId", homestayId.toString());
  } else {
    url = "/api/files/upload";
    formData.append("type", "homestay");
    formData.append("imageType", imageType);
  }

  // 获取API基础URL
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";

  return request({
    url: `${API_BASE_URL}${url}`,
    method: "post",
    data: formData,
    headers: {
      "Content-Type": "multipart/form-data",
    },
    // 上传进度处理
    onUploadProgress: (progressEvent) => {
      const percentCompleted = Math.round(
        (progressEvent.loaded * 100) / (progressEvent.total || 1)
      );
      console.log(`上传进度: ${percentCompleted}%`);
    },
    // 确保请求超时时间足够长
    timeout: 60000,
  })
    .then((response) => {
      console.log(
        `${imageType === "cover" ? "封面" : "图片集"}图片上传成功:`,
        response.data
      );

      // 确保响应中包含imageType字段
      if (response.data && response.data.data) {
        // 处理不同响应格式
        if (typeof response.data.data === "object") {
          if (!response.data.data.imageType) {
            response.data.data.imageType = imageType;
          }
        } else if (typeof response.data.data === "string") {
          // 字符串URL响应，转换为对象
          const url = response.data.data;
          response.data.data = {
            url: url,
            imageType: imageType,
          };
        }
      } else if (response.data && response.data.status === "success") {
        // 兼容不同响应格式
        if (!response.data.data) {
          response.data.data = {
            url: response.data.downloadUrl || "",
            imageType: imageType,
          };
        }
        // 添加success字段保持一致性
        response.data.success = true;
      }

      return response;
    })
    .catch((error) => {
      console.error(
        `${imageType === "cover" ? "封面" : "图片集"}图片上传失败:`,
        error
      );

      // 详细错误信息
      if (error.response) {
        console.error(
          "服务器响应错误:",
          error.response.status,
          error.response.data
        );
      } else if (error.request) {
        console.error("未收到响应，请检查服务器是否运行或网络连接");
      } else {
        console.error("请求配置错误:", error.message);
      }

      // 重新抛出错误以便调用者处理
      throw error;
    });
}

// 批量上传民宿图片
export function uploadHomestayImages(files: File[], homestayId: number) {
  const formData = new FormData();
  files.forEach((file) => {
    formData.append("files", file);
  });
  formData.append("homestayId", homestayId.toString());

  console.log(
    `开始批量上传民宿图片，民宿ID: ${homestayId}, 图片数量: ${files.length}`
  );

  return request({
    url: "/api/homestay-images/upload-multiple",
    method: "post",
    data: formData,
    headers: {
      "Content-Type": "multipart/form-data",
    },
    timeout: 120000, // 批量上传需要更长的超时时间
  })
    .then((response) => {
      console.log(`批量上传民宿图片成功:`, response.data);
      return response;
    })
    .catch((error) => {
      console.error(`批量上传民宿图片失败:`, error);
      throw error;
    });
}

// 获取民宿图片
export function getHomestayImages(homestayId: number) {
  return request({
    url: `/api/homestay-images/homestay/${homestayId}`,
    method: "get",
  })
    .then((response) => {
      console.log(`获取民宿图片成功，民宿ID: ${homestayId}`, response.data);
      return response;
    })
    .catch((error) => {
      console.error(`获取民宿图片失败，民宿ID: ${homestayId}`, error);
      throw error;
    });
}

// 删除民宿图片
export function deleteHomestayImage(imageId: number) {
  return request({
    url: `/api/homestay-images/${imageId}`,
    method: "delete",
  })
    .then((response) => {
      console.log(`删除民宿图片成功，图片ID: ${imageId}`, response.data);
      return response;
    })
    .catch((error) => {
      console.error(`删除民宿图片失败，图片ID: ${imageId}`, error);
      throw error;
    });
}

// 删除民宿所有图片
export function deleteAllHomestayImages(homestayId: number) {
  return request({
    url: `/api/homestay-images/homestay/${homestayId}`,
    method: "delete",
  })
    .then((response) => {
      console.log(`删除民宿所有图片成功，民宿ID: ${homestayId}`, response.data);
      return response;
    })
    .catch((error) => {
      console.error(`删除民宿所有图片失败，民宿ID: ${homestayId}`, error);
      throw error;
    });
}

// 获取省份列表
export function getProvinces() {
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";
  return request({
    url: `${API_BASE_URL}/api/locations/provinces`,
    method: "get",
  }).catch((error) => {
    console.error(`获取省份列表失败(无版本路径)`, error);

    // 如果无版本路径失败，尝试v1版本
    console.log(`尝试使用v1 API获取省份列表`);
    return request({
      url: `${API_BASE_URL}/api/v1/locations/provinces`,
      method: "get",
    });
  });
}

// 获取城市列表
export function getCities(provinceCode: string) {
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";
  return request({
    url: `${API_BASE_URL}/api/locations/cities`,
    method: "get",
    params: { provinceCode },
  }).catch((error) => {
    console.error(`获取城市列表失败(无版本路径)`, error);

    // 如果无版本路径失败，尝试v1版本
    console.log(`尝试使用v1 API获取城市列表`);
    return request({
      url: `${API_BASE_URL}/api/v1/locations/cities`,
      method: "get",
      params: { provinceCode },
    });
  });
}

// 获取区县列表
export function getDistricts(cityCode: string) {
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";
  return request({
    url: `${API_BASE_URL}/api/locations/districts`,
    method: "get",
    params: { cityCode },
  }).catch((error) => {
    console.error(`获取区县列表失败(无版本路径)`, error);

    // 如果无版本路径失败，尝试v1版本
    console.log(`尝试使用v1 API获取区县列表`);
    return request({
      url: `${API_BASE_URL}/api/v1/locations/districts`,
      method: "get",
      params: { cityCode },
    });
  });
}

// 获取房源类型列表
export function getHomestayTypes(): Promise<HomestayType[]> {
  return request({
    url: "/api/homestay-types",
    method: "get",
  })
    .then((response) => {
      // 假设后端返回的数据结构是正确的数组，直接返回 response.data
      // 让 TypeScript 类型断言和后续代码处理类型问题
      // 如果 response.data 不是预期的数组，后续的 v-for 会失败或显示空
      if (response && response.data) {
        // console.log("API response.data:", response.data); // 可以取消注释来调试具体数据
        // 确保即使后端返回的不是严格数组，只要结构类似，也能尝试使用
        return response.data as HomestayType[];
      } else {
        console.warn(
          "/api/homestay-types response or response.data is missing."
        );
        return []; // 返回空数组以防万一
      }
    })
    .catch((error) => {
      console.error("获取房源类型失败 (API):", error);
      // 发生错误时也返回空数组，防止页面崩溃
      return [];
    });
}

// 获取按分类分组的房源类型
export function getHomestayTypesByCategory() {
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";
  return request({
    url: `${API_BASE_URL}/api/homestay-types/by-category`,
    method: "get",
  }).catch((error) => {
    console.error(`获取分组房源类型失败(无版本路径)`, error);

    // 如果无版本路径失败，尝试v1版本
    console.log(`尝试使用v1 API获取分组房源类型`);
    return request({
      url: `${API_BASE_URL}/api/v1/homestay-types/by-category`,
      method: "get",
    });
  });
}

// 获取所有房源类型分类
export function getHomestayCategories() {
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";
  return request({
    url: `${API_BASE_URL}/api/homestay-types/categories`,
    method: "get",
  }).catch((error) => {
    console.error(`获取房源类型分类失败(无版本路径)`, error);

    // 如果无版本路径失败，尝试v1版本
    console.log(`尝试使用v1 API获取房源类型分类`);
    return request({
      url: `${API_BASE_URL}/api/v1/homestay-types/categories`,
      method: "get",
    });
  });
}

// 获取房源设施列表
export function getHomestayAmenities() {
  return request({
    url: "/api/homestays/amenities",
    method: "get",
  }).catch((error) => {
    console.error(`获取房源设施列表失败(无版本路径)`, error);

    // 如果无版本路径失败，尝试v1版本
    console.log(`尝试使用v1 API获取房源设施列表`);
    return request({
      url: "/api/v1/homestays/amenities",
      method: "get",
    });
  });
}

// 获取已上架房源列表（供首页展示）
export function getActiveHomestays(params?: {
  page?: number;
  size?: number;
  featured?: boolean;
}) {
  console.log("获取已上架房源列表，参数:", params);
  return request({
    url: "/api/homestays",
    method: "get",
    params: {
      ...params,
      status: "ACTIVE",
    },
  })
    .then((response) => {
      console.log("获取已上架房源列表成功(无版本路径):", response.data);
      return response;
    })
    .catch((error) => {
      console.error("获取已上架房源列表失败(无版本路径):", error);

      // 如果无版本路径失败，尝试v1版本
      console.log(`尝试使用v1 API获取已上架房源列表`);
      return request({
        url: "/api/v1/homestays",
        method: "get",
        params: {
          ...params,
          status: "ACTIVE",
        },
      })
        .then((backupResponse) => {
          console.log("获取已上架房源列表成功(v1):", backupResponse.data);
          return backupResponse;
        })
        .catch((backupError) => {
          console.error("获取已上架房源列表失败(v1):", backupError);
          throw backupError;
        });
    });
}

// 根据ID列表批量获取房源
export function getHomestaysByIds(ids: number[]) {
  console.log("根据ID列表获取房源，IDs:", ids);

  // 尝试两个可能的API端点
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

      // 使用备用API，如果有多个ID，就按顺序请求
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
        // 过滤掉请求失败的结果
        const validResults = results.filter((r) => r !== null);
        console.log("批量获取房源成功(备用方法):", validResults);
        return { data: validResults };
      });
    });
}

// 批量激活房源
export function batchActivateHomestays(ids: number[]) {
  return request({
    url: "/api/homestays/batch/activate",
    method: "post",
    data: { ids },
  }).catch((error) => {
    console.error(`批量激活房源失败(无版本路径)`, error);

    // 如果无版本路径失败，尝试v1版本
    console.log(`尝试使用v1 API批量激活房源`);
    return request({
      url: "/api/v1/homestays/batch/activate",
      method: "post",
      data: { ids },
    });
  });
}

// 批量下架房源
export function batchDeactivateHomestays(ids: number[]) {
  return request({
    url: "/api/homestays/batch/deactivate",
    method: "post",
    data: { ids },
  }).catch((error) => {
    console.error(`批量下架房源失败(无版本路径)`, error);

    // 如果无版本路径失败，尝试v1版本
    console.log(`尝试使用v1 API批量下架房源`);
    return request({
      url: "/api/v1/homestays/batch/deactivate",
      method: "post",
      data: { ids },
    });
  });
}

// 批量删除房源
export function batchDeleteHomestays(ids: number[]) {
  return request({
    url: "/api/homestays/batch",
    method: "delete",
    data: { ids },
  }).catch((error) => {
    console.error(`批量删除房源失败(无版本路径)`, error);

    // 如果无版本路径失败，尝试v1版本
    console.log(`尝试使用v1 API批量删除房源`);
    return request({
      url: "/api/v1/homestays/batch",
      method: "delete",
      data: { ids },
    });
  });
}

// 验证当前用户是否拥有特定房源的权限
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
      // 获取当前用户信息
      const userInfo = JSON.parse(localStorage.getItem("userInfo") || "{}");
      const currentUsername = userInfo.username;

      console.log(`房源所有者: ${homestayData.ownerUsername}`);
      console.log(`当前用户: ${currentUsername}`);

      // 检查房源所有者是否是当前用户
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

// 获取按分类分组的可用设施列表
export function getAvailableAmenitiesGrouped(): Promise<
  AmenityCategoryOption[]
> {
  console.log("尝试获取按分类分组的可用设施列表...");
  return request({
    url: "/api/amenities/by-categories", // 调用新的端点
    method: "get",
    params: { onlyActive: true }, // 确保只获取激活的设施
  })
    .then((response) => {
      // 检查返回的数据结构，期望是 { success: true, data: List<AmenityCategoryDTO> }
      if (
        response &&
        response.data &&
        response.data.success === true &&
        Array.isArray(response.data.data)
      ) {
        console.log("成功获取按分类分组的可用设施列表:", response.data.data);
        // 直接返回 data 部分，类型断言为前端定义的接口
        return response.data.data as AmenityCategoryOption[];
      } else {
        console.warn(
          "获取按分类分组的设施列表时返回的数据格式不符合预期:",
          response?.data
        );
        return []; // 返回空数组
      }
    })
    .catch((error) => {
      console.error("获取按分类分组的设施列表失败:", error);
      ElMessage.error("加载设施选项失败");
      return []; // 出错时返回空数组
    });
}

// 获取房源类型（用于筛选）
export const getHomestayTypesForFilter = () => {
  return request({
    url: "/api/homestay-types/legacy-types", // 添加 /api 前缀
    method: "get",
  });
};
