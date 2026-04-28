import axios, { InternalAxiosRequestConfig } from "axios";

const baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const api = axios.create({
  baseURL,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
});

// 请求拦截器
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem("homestay_token") || localStorage.getItem("token");
    if (token) {
      // 使用 set 方法确保 header 正确设置（兼容 axios v1.x）
      config.headers.set("Authorization", `Bearer ${token}`);
      console.log(`[API] ${config.method?.toUpperCase()} ${config.url} 已附加 token`);
    } else {
      console.warn(`[API] ${config.method?.toUpperCase()} ${config.url} 未找到 token`);
    }

    return config;
  },
  (error) => {
    console.error("请求拦截器错误:", error);
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response) {
      const status = error.response.status;
      const url = error.config?.url;

      console.error(`[API] 响应错误: ${status} ${url}`, error.response.data);

      // 提取错误消息
      let errorMessage = "请求失败";
      const data = error.response.data;
      if (data) {
        if (typeof data === "string") {
          errorMessage = data;
        } else if (data.message) {
          errorMessage = data.message;
        } else if (data.error) {
          errorMessage = data.error;
        } else if (data.msg) {
          errorMessage = data.msg;
        }
      }
      error.displayMessage = errorMessage;

      // 401 处理：仅清除存储，不自动跳转（避免干扰登录流程）
      // 页面跳转由路由守卫或调用方负责
      if (status === 401) {
        console.warn("[API] 收到 401，清除本地认证状态");
        localStorage.removeItem("homestay_token");
        localStorage.removeItem("homestay_user");
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        localStorage.removeItem("userInfo");
      }
    } else if (error.request) {
      console.error("[API] 网络错误:", error.message);
      error.displayMessage = "网络连接失败，请检查您的网络连接";
    } else {
      console.error("[API] 请求配置错误:", error.message);
      error.displayMessage = "请求配置错误: " + error.message;
    }

    return Promise.reject(error);
  }
);

export default api;
