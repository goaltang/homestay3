import { defineStore } from "pinia";
import { ref } from "vue";
import request from "@/utils/request";
import type {
  LoginRequest,
  RegisterRequest,
  UserInfo,
  AuthResponse,
} from "@/types/auth";

// 添加新的类型定义
interface ProfileUpdateRequest {
  username: string;
  email: string;
  phone?: string;
  realName?: string;
  idCard?: string;
}

interface PasswordChangeRequest {
  oldPassword: string;
  newPassword: string;
}

export const useUserStore = defineStore("user", () => {
  const token = ref(localStorage.getItem("token") || "");
  const userInfo = ref<UserInfo>(
    JSON.parse(localStorage.getItem("userInfo") || "{}")
  );
  const isAuthenticated = ref(!!token.value);

  const login = async (credentials: LoginRequest) => {
    try {
      const response = await request.post<AuthResponse>(
        "/auth/login",
        credentials
      );
      const { token: newToken, user } = response.data;

      // 确保存储的 token 是干净的
      token.value = newToken.trim();
      userInfo.value = user;
      isAuthenticated.value = true;

      if (credentials.remember) {
        localStorage.setItem("token", token.value);
        localStorage.setItem("userInfo", JSON.stringify(user));
      }

      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || "登录失败");
    }
  };

  const register = async (data: RegisterRequest) => {
    try {
      const response = await request.post<AuthResponse>("/auth/register", data);
      const { token: newToken, user } = response.data;

      // 确保 token 是干净的
      token.value = newToken.trim();
      userInfo.value = user;
      isAuthenticated.value = true;

      // 保存到 localStorage
      localStorage.setItem("token", token.value);
      localStorage.setItem("userInfo", JSON.stringify(user));

      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || "注册失败");
    }
  };

  const forgotPassword = async (email: string) => {
    try {
      await request.post("/auth/forgot-password", null, {
        params: { email },
      });
    } catch (error: any) {
      throw new Error(error.response?.data?.message || "发送重置邮件失败");
    }
  };

  const logout = () => {
    token.value = "";
    userInfo.value = {} as UserInfo;
    isAuthenticated.value = false;
    localStorage.removeItem("token");
    localStorage.removeItem("userInfo");
  };

  const updateProfile = async (data: ProfileUpdateRequest) => {
    try {
      const response = await request.put<AuthResponse>("/auth/profile", data);
      userInfo.value = response.data.user;
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || "更新个人信息失败");
    }
  };

  const changePassword = async (data: PasswordChangeRequest) => {
    try {
      await request.post("/auth/change-password", data);
    } catch (error: any) {
      throw new Error(error.response?.data?.message || "修改密码失败");
    }
  };

  return {
    token,
    userInfo,
    isAuthenticated,
    login,
    register,
    forgotPassword,
    logout,
    updateProfile,
    changePassword,
  };
});
