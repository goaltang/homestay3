import request from "@/utils/request";
import type { Homestay } from "@/types/homestay";

// 获取所有房源列表
export function getHomestays(params?: {
  page?: number;
  size?: number;
  status?: string;
  type?: string;
}) {
  console.log("获取房源列表，参数:", params);
  return request({
    url: "/api/v1/homestays",
    method: "get",
    params,
  })
    .then((response) => {
      console.log("获取房源列表成功:", response.data);
      return response;
    })
    .catch((error) => {
      console.error("获取房源列表失败:", error);
      throw error;
    });
}

// 获取房源详情
export function getHomestayById(id: number) {
  console.log(`开始获取房源详情, ID: ${id}`);

  // 尝试两个可能的API端点，先尝试v1版本
  return request({
    url: `/api/v1/homestays/${id}`,
    method: "get",
  })
    .then((response) => {
      console.log(`房源详情获取成功(v1), ID: ${id}, 数据:`, response.data);
      return response;
    })
    .catch((error) => {
      console.error(`房源详情获取失败(v1), ID: ${id}, 错误:`, error);

      // 如果v1失败，尝试不带版本号的端点
      console.log(`尝试使用备用API获取房源详情, ID: ${id}`);
      return request({
        url: `/api/homestays/${id}`,
        method: "get",
      })
        .then((backupResponse) => {
          console.log(
            `房源详情获取成功(备用), ID: ${id}, 数据:`,
            backupResponse.data
          );
          return backupResponse;
        })
        .catch((backupError) => {
          console.error(
            `房源详情获取失败(备用), ID: ${id}, 错误:`,
            backupError
          );
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
    url: "/api/v1/homestays/owner",
    method: "get",
    params,
  });
}

// 创建新房源
export function createHomestay(data: Omit<Homestay, "id">) {
  return request({
    url: "/api/v1/homestays",
    method: "post",
    data,
  });
}

// 更新房源信息
export function updateHomestay(id: number, data: Partial<Homestay>) {
  console.log(`开始更新房源, ID: ${id}, 数据:`, data);
  return request({
    url: `/api/v1/homestays/${id}`,
    method: "put",
    data,
  })
    .then((response) => {
      console.log(`房源更新成功, ID: ${id}`, response.data);
      return response;
    })
    .catch((error) => {
      console.error(`房源更新失败, ID: ${id}`, error);
      throw error;
    });
}

// 删除房源
export function deleteHomestay(id: number) {
  return request({
    url: `/api/v1/homestays/${id}`,
    method: "delete",
  });
}

// 更改房源状态：激活
export function activateHomestay(id: number) {
  return request({
    url: `/api/v1/homestays/${id}/activate`,
    method: "patch",
    data: {},
  });
}

// 更改房源状态：停用
export function deactivateHomestay(id: number) {
  return request({
    url: `/api/v1/homestays/${id}/deactivate`,
    method: "patch",
    data: {},
  });
}

// 上传房源图片
export function uploadHomestayImage(file: File, imageType: string = "gallery") {
  console.log(
    `开始上传房源${imageType === "cover" ? "封面" : "图片集"}图片:`,
    file.name,
    "大小:",
    (file.size / 1024).toFixed(2),
    "KB"
  );

  const formData = new FormData();
  formData.append("file", file);
  formData.append("type", "homestay");
  // 添加额外参数标识是封面还是普通图片
  formData.append("imageType", imageType);

  return request({
    url: "/api/files/upload",
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
    timeout: 30000,
  })
    .then((response) => {
      console.log(
        `${imageType === "cover" ? "封面" : "图片集"}图片上传成功:`,
        response.data
      );

      // 确保响应中包含imageType字段
      if (response.data && response.data.data) {
        response.data.data.imageType = imageType;
      }

      return response;
    })
    .catch((error) => {
      console.error(
        `${imageType === "cover" ? "封面" : "图片集"}图片上传失败:`,
        error
      );
      // 重新抛出错误以便调用者处理
      throw error;
    });
}

// 获取省份列表
export function getProvinces() {
  return request({
    url: "/api/v1/locations/provinces",
    method: "get",
  });
}

// 获取城市列表
export function getCities(provinceCode: string) {
  return request({
    url: "/api/v1/locations/cities",
    method: "get",
    params: { provinceCode },
  });
}

// 获取区县列表
export function getDistricts(cityCode: string) {
  return request({
    url: "/api/v1/locations/districts",
    method: "get",
    params: { cityCode },
  });
}

// 获取房源类型列表
export function getHomestayTypes() {
  return request({
    url: "/api/v1/homestays/types",
    method: "get",
  });
}

// 获取房源设施列表
export function getHomestayAmenities() {
  return request({
    url: "/api/v1/homestays/amenities",
    method: "get",
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
    url: "/api/v1/homestays",
    method: "get",
    params: {
      ...params,
      status: "ACTIVE",
    },
  })
    .then((response) => {
      console.log("获取已上架房源列表成功:", response.data);
      return response;
    })
    .catch((error) => {
      console.error("获取已上架房源列表失败:", error);
      throw error;
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
    url: "/api/v1/homestays/batch/activate",
    method: "post",
    data: { ids },
  });
}

// 批量下架房源
export function batchDeactivateHomestays(ids: number[]) {
  return request({
    url: "/api/v1/homestays/batch/deactivate",
    method: "post",
    data: { ids },
  });
}

// 批量删除房源
export function batchDeleteHomestays(ids: number[]) {
  return request({
    url: "/api/v1/homestays/batch",
    method: "delete",
    data: { ids },
  });
}
