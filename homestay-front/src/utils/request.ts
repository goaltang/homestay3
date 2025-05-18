import axios from "axios";
import type {
  AxiosInstance,
  AxiosRequestConfig,
  InternalAxiosRequestConfig,
  AxiosError,
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
  "/uploads/", // 静态资源路径
  "/api/uploads/", // API静态资源路径
  "/api/files/", // 文件资源路径
];

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const method = config.method?.toUpperCase() || "UNKNOWN";
    const url = config.url || "unknown-url";

    // 详细日志，包含请求方法和URL
    console.log(`=====================================`);
    console.log(`准备发送请求: ${method} ${url}`);

    // 检查路径是否在白名单中
    const isPathWhitelisted = whiteList.some((path) => url.startsWith(path));
    console.log(
      `请求路径白名单状态: ${isPathWhitelisted ? "在白名单中" : "需要认证"}`
    );

    // 初始化headers
    config.headers = config.headers || {};

    // 如果请求不在白名单中，尝试添加认证信息
    if (!isPathWhitelisted) {
      // 尝试从不同地方获取token
      let token: string | null = null;

      // 首先从localStorage获取
      token = localStorage.getItem("token");

      // 按顺序尝试不同的token来源
      if (token) {
        console.log("从localStorage获取到token");
      } else {
        // 如果localStorage没有，从sessionStorage获取
        token = sessionStorage.getItem("token");
        if (token) {
          console.log("从sessionStorage获取到token");
        } else {
          // 如果仍然没有，尝试从用户信息获取
          try {
            const userInfo = localStorage.getItem("userInfo");
            if (userInfo) {
              const parsed = JSON.parse(userInfo);
              if (parsed && parsed.token && typeof parsed.token === "string") {
                token = parsed.token;
                console.log("从用户信息中恢复token");
              }
            }
          } catch (e) {
            console.error("解析用户信息失败:", e);
          }
        }
      }

      // 如果找到token，添加到请求头
      if (token) {
        // 显示token预览
        const tokenPreview =
          token.length > 15
            ? `${token.substring(0, 10)}...${token.substring(token.length - 5)}`
            : token;
        console.log(`使用token: ${tokenPreview}`);

        // 添加token到请求头
        config.headers.Authorization = token.startsWith("Bearer ")
          ? token
          : `Bearer ${token}`;

        console.log(`认证头已添加: Bearer ${tokenPreview}`);
      } else {
        // 发出警告
        console.warn(`⚠️ 请求 ${url} 没有携带token，这可能导致认证失败`);

        // 对于需要认证的PUT/DELETE/POST请求，发出特别警告
        if (["PUT", "DELETE", "POST"].includes(method)) {
          console.error(`⚠️⚠️⚠️ ${method}请求没有token，几乎肯定会失败!`);
        }
      }
    }

    // 特殊处理FormData请求，不设置Content-Type，让浏览器自动处理
    if (config.data instanceof FormData) {
      console.log("检测到FormData请求，使用浏览器自动设置的Content-Type");
      delete config.headers["Content-Type"];
    }

    // 输出完整的请求配置
    console.log("完整请求配置:", {
      url: config.url,
      method: config.method,
      hasAuthHeader: !!config.headers.Authorization,
    });

    console.log(`=====================================`);
    return config;
  },
  (error) => {
    console.error("请求拦截器错误:", error);
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const method = response.config.method?.toUpperCase() || "UNKNOWN";
    const url = response.config.url || "unknown-url";

    console.log(
      `响应成功: ${method} ${url}`,
      `状态: ${response.status}`,
      `数据预览: ${JSON.stringify(response.data).substring(0, 100)}...`
    );

    return response;
  },
  (error) => {
    if (error.response) {
      const { status, data, config } = error.response;
      const method = config.method?.toUpperCase() || "UNKNOWN";
      const url = config.url || "unknown-url";

      console.error(
        `响应错误: ${method} ${url}`,
        `状态: ${status}`,
        `错误: ${JSON.stringify(data)}`,
        `认证头: ${config.headers?.Authorization ? "存在" : "不存在"}`
      );

      // 打印完整的错误对象便于调试
      console.error("完整错误对象:", error);

      // 根据状态码处理不同的错误
      switch (status) {
        case 400:
          ElMessage.error(data?.message || "请求参数错误");
          break;
        case 401:
          // 如果是登录接口本身的 401，则不进行全局处理，让错误冒泡给调用者
          if (config.url?.includes("/api/auth/login")) {
            console.warn("登录接口返回 401 (用户名或密码错误)，跳过全局处理");
            // 不做任何操作，错误会继续传递到 userStore.login 的 catch
          } else {
            // 对于其他接口的 401，视为登录过期
            ElMessage.error("登录状态无效或已过期，请重新登录");
            // 清除token
            localStorage.removeItem("token");
            localStorage.removeItem("user"); // 也清除 user 信息
            localStorage.removeItem("userInfo");
            delete axios.defaults.headers.common["Authorization"]; // 如果 axios 实例是共享的
            // 跳转到登录页 (延迟以显示消息)
            setTimeout(() => {
              // 检查是否已在登录页，避免无限循环
              if (router.currentRoute.value.path !== "/login") {
                router.push("/login");
              }
            }, 1500);
          }
          break;
        case 403:
          console.warn(`收到403错误，权限不足，请求URL: ${config.url}`);

          // 记录认证信息
          const authHeader = config.headers?.Authorization;
          console.warn(
            `认证头: ${
              authHeader ? authHeader.substring(0, 20) + "..." : "不存在"
            }`
          );
          console.warn(
            `当前用户token: ${
              localStorage.getItem("token") ? "存在" : "不存在"
            }`
          );

          ElMessage.error("没有权限访问该资源，请重新登录");
          break;
        case 500:
          console.error(`服务器错误: ${config.url}`, data);
          ElMessage.error("服务器错误，请稍后再试");
          break;
        default:
          ElMessage.error(data.message || "未知错误");
      }
    } else if (error.request) {
      console.error("网络错误，请检查您的网络连接或服务器状态", error.request);
      ElMessage.error("网络错误，请检查您的网络连接");
    } else {
      console.error("请求配置错误", error.message);
    }

    return Promise.reject(error);
  }
);

export default request;
