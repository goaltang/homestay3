import axios, { AxiosRequestConfig, InternalAxiosRequestConfig } from "axios";

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
    const token = localStorage.getItem("token");
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // 添加详细日志，特别是注册请求
    if (config.url && config.url.includes("/auth/register")) {
      console.log("发送注册请求:", {
        url: config.url,
        method: config.method,
        data: config.data,
        headers: config.headers,
      });
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
    // 记录注册相关的响应
    if (response.config.url && response.config.url.includes("/auth/register")) {
      console.log("注册请求响应:", {
        status: response.status,
        data: response.data,
        url: response.config.url,
      });
    }
    return response;
  },
  (error) => {
    if (error.response) {
      // 更详细地记录错误
      console.error("API响应错误:", {
        status: error.response.status,
        url: error.config?.url,
        method: error.config?.method,
        data: error.response.data,
      });

      // 处理401错误（未授权）
      if (error.response.status === 401) {
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        localStorage.removeItem("userInfo");
        window.location.href = "/login";
      }

      // 处理其他错误
      let errorMessage = "请求失败";

      // 提取错误消息 - 考虑多种可能的数据结构
      if (error.response.data) {
        if (typeof error.response.data === "string") {
          errorMessage = error.response.data;
        } else if (error.response.data.message) {
          errorMessage = error.response.data.message;
        } else if (error.response.data.error) {
          errorMessage = error.response.data.error;
        } else if (error.response.data.msg) {
          errorMessage = error.response.data.msg;
        }
      }

      // 将提取的错误消息保存到error对象中，方便上层调用者使用
      error.displayMessage = errorMessage;

      console.error("API错误消息:", errorMessage);
    } else if (error.request) {
      console.error("网络错误 - 没有收到响应:", error.message);
      error.displayMessage = "网络连接失败，请检查您的网络连接";
    } else {
      console.error("请求配置错误:", error.message);
      error.displayMessage = "请求配置错误: " + error.message;
    }

    return Promise.reject(error);
  }
);

export default api;
