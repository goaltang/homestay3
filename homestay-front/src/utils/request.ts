import axios from "axios";
import type { AxiosInstance, AxiosResponse } from "axios";
import { useUserStore } from "@/stores/user";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";

const request: AxiosInstance = axios.create({
  baseURL: "http://localhost:8080/api",
  timeout: 5000,
});

request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore();
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token.trim()}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

request.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error) => {
    if (error.response?.status === 401) {
      const userStore = useUserStore();
      const router = useRouter();
      userStore.logout();
      router.push({ name: "Login" });
    }
    ElMessage.error(error.response?.data?.message || "请求失败");
    return Promise.reject(error);
  }
);

export default request;
