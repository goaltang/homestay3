import axios from "axios";
import { ElMessage, ElNotification } from "element-plus";
import { useUserStore } from "@/stores/user";
import type { AxiosRequestConfig, AxiosResponse } from "axios";

// 创建一个带类型的request实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080",
  timeout: 10000,
});

// 日志级别
const logLevels = {
  none: 0,
  error: 1,
  warn: 2,
  info: 3,
  debug: 4,
};

// 当前日志级别
const currentLogLevel = import.meta.env.VITE_LOG_LEVEL
  ? logLevels[import.meta.env.VITE_LOG_LEVEL as keyof typeof logLevels]
  : logLevels.error;

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore();
    const token = userStore.token;
    if (token) {
      config.headers["Authorization"] = `Bearer ${token}`;
    }

    // 开发环境下记录请求日志
    if (currentLogLevel >= logLevels.debug) {
      console.log(
        "Request:",
        config.method?.toUpperCase(),
        config.url,
        config.params || config.data
      );
    }

    return config;
  },
  (error) => {
    console.error("请求错误:", error);
    return Promise.reject(error);
  }
);

// 错误信息映射
const errorMessages: Record<number, string> = {
  400: "请求参数错误",
  401: "未授权，请重新登录",
  403: "没有权限执行此操作",
  404: "请求的资源不存在",
  405: "请求方法不允许",
  408: "请求超时",
  409: "数据冲突",
  410: "请求的资源已被永久删除",
  422: "验证错误",
  429: "请求过于频繁，请稍后再试",
  500: "服务器内部错误",
  501: "服务未实现",
  502: "网关错误",
  503: "服务不可用",
  504: "网关超时",
};

// 响应数据检查
function checkResponseData(data: any): boolean {
  return data !== null && data !== undefined;
}

// 格式化错误消息
function formatErrorMessage(error: any): string {
  if (!error.response) {
    return "网络连接失败，请检查您的网络连接";
  }

  const status = error.response.status;
  const defaultMsg = errorMessages[status] || "未知错误";

  // 尝试从响应体中获取详细错误信息
  return (
    error.response?.data?.error ||
    error.response?.data?.message ||
    error.response?.data?.msg ||
    defaultMsg
  );
}

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // 开发环境下记录响应日志
    if (currentLogLevel >= logLevels.debug) {
      console.log("Response:", response.config.url, response.data);
    }

    // 检查响应数据
    if (!checkResponseData(response.data)) {
      if (currentLogLevel >= logLevels.warn) {
        console.warn("响应数据为空或格式不正确:", response.config.url);
      }
    }

    // 直接返回响应数据
    return response.data;
  },
  (error) => {
    // 记录错误日志
    if (currentLogLevel >= logLevels.error) {
      console.error("响应错误:", error);
    }

    if (!error.response) {
      // 网络错误或请求被取消
      ElMessage.error("网络连接失败，请检查您的网络连接");
      return Promise.reject(error);
    }

    const status = error.response.status;
    const errorMsg = formatErrorMessage(error);

    // 处理不同的HTTP状态码
    switch (status) {
      case 401:
        // 未授权，清除用户状态并重定向到登录页
        const userStore = useUserStore();
        userStore.logout();
        window.location.href = "/login";
        ElMessage.error("登录已过期，请重新登录");
        break;
      case 403:
        ElMessage.error("没有权限执行此操作");
        break;
      case 422:
        // 验证错误，可能包含多个字段的错误信息
        if (error.response.data?.errors) {
          const validationErrors = error.response.data.errors;
          // 显示验证错误的通知
          ElNotification({
            title: "输入验证错误",
            message: Object.entries(validationErrors)
              .map(([field, msgs]) => `${field}: ${msgs}`)
              .join("<br>"),
            type: "error",
            dangerouslyUseHTMLString: true,
            duration: 5000,
          });
        } else {
          ElMessage.error(errorMsg);
        }
        break;
      case 500:
        ElMessage.error(errorMsg);
        // 记录严重错误到日志系统
        if (currentLogLevel >= logLevels.error) {
          console.error("服务器错误:", error.response?.data);
        }
        break;
      default:
        ElMessage.error(errorMsg);
    }

    return Promise.reject(error);
  }
);

// 增强的错误处理包装函数
async function safeRequest<T = any>(config: AxiosRequestConfig): Promise<T> {
  try {
    const result = (await request(config)) as T;
    return result;
  } catch (error: any) {
    // 这里可以添加全局的错误处理逻辑
    if (error.name === "AxiosError" && !error.response) {
      // 网络错误的额外处理
      console.error("网络错误详情:", error);
    }

    throw error; // 重新抛出错误，让调用者可以进行特定处理
  }
}

// 定义一个类型安全的请求函数
export default function <T = any>(config: AxiosRequestConfig): Promise<T> {
  return safeRequest<T>(config);
}
