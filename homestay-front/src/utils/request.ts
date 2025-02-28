import axios from "axios";
import type { AxiosInstance } from "axios";
import router from "@/router";

// 创建 axios 实例
const request: AxiosInstance = axios.create({
  baseURL: "http://localhost:8080",
  timeout: 5000,
  headers: {
    "Content-Type": "application/json",
  },
});

// 添加不需要token的白名单路径
const whiteList = [
  "/api/auth/login",
  "/api/auth/register",
  "/api/auth/forgot-password",
  "/api/auth/reset-password",
];

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 确保 /api 前缀
    if (!config.url?.startsWith("/api")) {
      config.url = `/api${config.url}`;
    }

    // 移除重复的 /api 前缀
    config.url = config.url.replace(/\/api\/api\//, "/api/");

    // 检查是否在白名单中
    const isWhitelisted = whiteList.some((path) => config.url?.includes(path));
    console.log("请求路径:", config.url, "是否在白名单中:", isWhitelisted);

    const token = localStorage.getItem("token");
    // 只有不在白名单中的请求才需要token
    if (token && !isWhitelisted) {
      config.headers["Authorization"] = `Bearer ${token.trim()}`;
    }

    console.log("最终请求配置:", {
      url: config.url,
      method: config.method,
      headers: config.headers,
      data: config.data,
      isWhitelisted,
    });

    return config;
  },
  (error) => {
    console.error("请求错误:", error);
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    console.log("收到响应:", {
      status: response.status,
      data: response.data,
      headers: response.headers,
    });
    return response;
  },
  (error) => {
    if (error.response) {
      const status = error.response.status;
      const data = error.response.data;
      const config = error.config;

      console.error(`请求失败: ${status}`, {
        data,
        config,
        headers: error.response.headers,
      });

      // 检查是否在白名单中
      const isWhitelisted = whiteList.some((path) =>
        config.url?.includes(path)
      );
      console.log(
        "错误请求路径:",
        config.url,
        "是否在白名单中:",
        isWhitelisted
      );

      switch (status) {
        case 401:
          // 只有在非白名单路径且非登录页面时才清除token并重定向
          if (!isWhitelisted && router.currentRoute.value.path !== "/login") {
            console.log("清除token并重定向到登录页");
            localStorage.removeItem("token");
            router.push("/login");
          }
          break;
        case 403:
          console.error("没有权限访问");
          break;
        case 404:
          console.error("请求的资源不存在");
          break;
        case 500:
          console.error("服务器错误");
          break;
        default:
          console.error("请求失败:", error.response.data);
      }
    } else if (error.request) {
      console.error("没有收到响应:", error.request);
    } else {
      console.error("请求配置错误:", error.message);
    }
    return Promise.reject(error);
  }
);

export default request;
