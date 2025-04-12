import axios from "axios";
import type {
  AxiosInstance,
  AxiosRequestConfig,
  InternalAxiosRequestConfig,
} from "axios";
import router from "../router";
import { ElMessage } from "element-plus";
import { useUserStore } from "../stores/user";

// 扩展 AxiosRequestConfig 类型，添加 isWhitelisted 属性
declare module "axios" {
  interface InternalAxiosRequestConfig {
    isWhitelisted?: boolean;
  }
}

// 从环境变量获取 API 基础 URL
const baseURL = import.meta.env.VITE_API_BASE_URL || "";

// 创建 axios 实例
const request: AxiosInstance = axios.create({
  baseURL,
  timeout: 15000,
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
  "/api/auth/current", // 添加获取当前用户信息的路径
  "/api/users/current", // 添加获取用户信息的备用路径
  "/api/homestays",
  "/api/homestays/featured",
  "/uploads/", // 静态资源路径
  "/api/uploads/", // API静态资源路径
  "/api/files/", // 文件资源路径
  "/homestays/", // 房源图片路径
  "/api/homestays/", // 房源图片路径
  "/avatars/", // 头像路径
  "/api/avatars/", // API头像路径
];

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    console.log(`请求: ${config.method?.toUpperCase()} ${config.url}`);

    // 获取token
    const token = localStorage.getItem("token");

    // 如果有token，添加到请求头
    if (token) {
      console.log(
        "已添加Authorization头: Bearer " + token.substring(0, 10) + "..."
      );
      config.headers.Authorization = `Bearer ${token}`;
    }

    // 特殊处理FormData请求，不设置Content-Type，让浏览器自动处理
    if (config.data instanceof FormData) {
      console.log("检测到FormData请求，使用浏览器自动设置的Content-Type");
      // 删除可能存在的Content-Type，让浏览器自动设置
      delete config.headers["Content-Type"];
    }

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
    console.log(
      `响应成功: ${response.config.method?.toUpperCase()} ${
        response.config.url
      }`,
      `状态: ${response.status}`
    );
    // 如果响应成功，直接返回数据
    return response;
  },
  (error) => {
    if (error.response) {
      const { status, data, config } = error.response;
      console.error(
        `响应错误: ${config.method?.toUpperCase()} ${config.url}`,
        `状态: ${status}`,
        data
      );

      // 根据状态码处理不同的错误
      switch (status) {
        case 400:
          ElMessage.error(data.message || "请求参数错误");
          break;
        case 401:
          // 检查是否是白名单路径
          const isWhitelisted = whiteList.some((path) =>
            error.config?.url?.startsWith(path)
          );

          if (!isWhitelisted) {
            ElMessage.error("登录已过期，请重新登录");
            // 清除token
            localStorage.removeItem("token");
            // 跳转到登录页
            setTimeout(() => {
              router.push("/login");
            }, 1500);
          }
          break;
        case 403:
          ElMessage.error("没有权限访问该资源");
          break;
        case 404:
          console.warn(`资源不存在: ${error.config?.url}`);
          break;
        case 500:
          ElMessage.error("服务器错误，请稍后再试");
          break;
        default:
          ElMessage.error(data.message || "未知错误");
      }
    } else if (error.request) {
      // 请求发出但没有收到响应
      console.error("网络错误，请检查您的网络连接或服务器状态", error.request);
      ElMessage.error("网络错误，请检查您的网络连接");
    } else {
      // 请求配置出错
      console.error("请求配置错误", error.message);
    }

    return Promise.reject(error);
  }
);

export default request;
