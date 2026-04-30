import axios from "axios";
import type { AxiosInstance } from "axios";
import router from "../router";
import { ElMessage } from "element-plus";

declare module "axios" {
  interface InternalAxiosRequestConfig {
    isWhitelisted?: boolean;
  }
}

const baseURL = import.meta.env.VITE_API_BASE_URL || "";
const debugRequests = import.meta.env.DEV && import.meta.env.VITE_DEBUG_REQUESTS === "true";

const requestLog = (...args: unknown[]) => {
  if (debugRequests) console.log(...args);
};

const requestWarn = (...args: unknown[]) => {
  if (debugRequests) console.warn(...args);
};

const request: AxiosInstance = axios.create({
  baseURL,
  timeout: 30000,
  headers: {
    "Content-Type": "application/json",
  },
});

const whiteList = [
  "/api/auth/login",
  "/api/auth/register",
  "/api/auth/forgot-password",
  "/api/auth/reset-password",
  "/api/homestays/map-search",
  "/api/homestays/map-clusters",
  "/api/homestays/nearby",
  "/api/homestays/landmark-search",
  "/uploads/",
  "/api/uploads/",
  "/api/files/",
  "/api/homestays/search",
  "/api/homestay-types",
  "/api/homestays/amenities",
  "/api/amenities/",
  "/api/homestays/",
  "/api/locations/",
  "/api/recommendations/popular",
  "/api/recommendations/recommended",
  "/api/recommendations/location",
  "/api/recommendations/similar",
];

const readOnlyPaths = ["/api/files/", "/api/homestays/"];
const publicPostPaths = [
  "/api/homestays/search",
  "/api/homestays/map-search",
  "/api/homestays/map-clusters",
  "/api/homestays/nearby",
  "/api/homestays/landmark-search",
];
const authRequiredPaths = [
  "/api/homestays/owner",
  "/api/homestays/batch/",
  "/api/homestays/submit-review",
  "/api/homestays/withdraw-review",
];

const getStoredToken = () => {
  let token = localStorage.getItem("homestay_token") || localStorage.getItem("token");
  if (token) return token;

  token = sessionStorage.getItem("homestay_token") || sessionStorage.getItem("token");
  if (token) return token;

  try {
    const userInfo = localStorage.getItem("homestay_user") || localStorage.getItem("userInfo");
    if (!userInfo) return null;
    const parsed = JSON.parse(userInfo);
    return typeof parsed?.token === "string" ? parsed.token : null;
  } catch (error) {
    console.error("Failed to parse stored user info:", error);
    return null;
  }
};

request.interceptors.request.use(
  (config) => {
    const method = config.method?.toUpperCase() || "UNKNOWN";
    const url = config.url || "unknown-url";
    let isPathWhitelisted = whiteList.some((path) => url.startsWith(path));

    if (
      readOnlyPaths.some((path) => url.startsWith(path)) &&
      method !== "GET" &&
      !(method === "POST" && publicPostPaths.some((path) => url.startsWith(path)))
    ) {
      isPathWhitelisted = false;
    }

    if (authRequiredPaths.some((path) => url.includes(path))) {
      isPathWhitelisted = false;
    }

    config.headers = config.headers || {};

    if (!isPathWhitelisted) {
      const token = getStoredToken();
      if (token) {
        config.headers.Authorization = token.startsWith("Bearer ") ? token : `Bearer ${token}`;
      } else {
        requestWarn(`Request ${method} ${url} has no auth token.`);
      }
    }

    if (config.data instanceof FormData) {
      delete config.headers["Content-Type"];
    }

    requestLog("request", {
      method,
      url,
      whitelisted: isPathWhitelisted,
      hasAuthHeader: Boolean(config.headers.Authorization),
    });

    return config;
  },
  (error) => {
    console.error("Request interceptor error:", error);
    return Promise.reject(error);
  },
);

request.interceptors.response.use(
  (response) => {
    requestLog("response", {
      method: response.config.method?.toUpperCase() || "UNKNOWN",
      url: response.config.url || "unknown-url",
      status: response.status,
    });
    return response;
  },
  (error) => {
    if (error.response) {
      const { status, data, config } = error.response;
      const url = config?.url || "unknown-url";

      console.error("Response error:", {
        method: config?.method?.toUpperCase() || "UNKNOWN",
        url,
        status,
        data,
        hasAuthHeader: Boolean(config?.headers?.Authorization),
      });

      switch (status) {
        case 400:
          ElMessage.error(data?.message || "请求参数错误");
          break;
        case 401:
          if (url.includes("/api/auth/login")) {
            requestWarn("Login API returned 401.");
          } else {
            ElMessage.error("登录状态无效或已过期，请重新登录");
            localStorage.removeItem("homestay_token");
            localStorage.removeItem("homestay_user");
            localStorage.removeItem("token");
            localStorage.removeItem("user");
            localStorage.removeItem("userInfo");
            delete axios.defaults.headers.common["Authorization"];
            setTimeout(() => {
              if (router.currentRoute.value.path !== "/login") {
                router.push("/login");
              }
            }, 1500);
          }
          break;
        case 403:
          ElMessage.error("没有权限访问该资源，请重新登录");
          break;
        case 500:
          ElMessage.error(data?.message || "服务器错误，请稍后再试");
          break;
        default:
          ElMessage.error(data?.message || "未知错误");
      }
    } else if (error.request) {
      console.error("Network error:", error.request);
      ElMessage.error("网络错误，请检查您的网络连接");
    } else {
      console.error("Request config error:", error.message);
    }

    return Promise.reject(error);
  },
);

export default request;
