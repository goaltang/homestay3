import { defineStore } from "pinia";
import { ref } from "vue";
import request from "@/utils/request";
import type {
  LoginRequest,
  RegisterRequest,
  UserInfo,
  AuthResponse,
  User,
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
  const token = ref<string | null>(localStorage.getItem("token"));
  const userInfo = ref<UserInfo | null>(null);
  const isAuthenticated = ref(!!token.value);

  // 设置认证信息
  const setAuth = (newToken: string, user: any) => {
    console.log("设置认证信息:", { token: newToken, user });
    token.value = newToken.trim();
    localStorage.setItem("token", newToken.trim());
    isAuthenticated.value = true;
    userInfo.value = user;
  };

  const login = async (username: string, password: string) => {
    try {
      const response = await request.post("/api/auth/login", {
        username,
        password,
      });

      console.log("登录响应:", response.data);

      if (response.data && response.data.token) {
        // 直接使用登录响应中的用户信息
        const userData = {
          id: response.data.id,
          username: response.data.username,
          email: response.data.email,
          phone: response.data.phone,
          realName: response.data.realName,
          idCard: response.data.idCard,
          role: response.data.role,
          avatar: response.data.avatar,
          verificationStatus: response.data.verificationStatus,
        };

        // 设置认证信息
        setAuth(response.data.token, userData);

        // 不再调用 getUserInfo
        return true;
      }
      return false;
    } catch (error: any) {
      console.error("登录失败:", error);
      if (error.response) {
        console.error("错误响应:", error.response.data);
      }
      return false;
    }
  };

  const register = async (data: RegisterRequest) => {
    try {
      const response = await request.post<AuthResponse>(
        "/api/auth/register",
        data
      );
      console.log("注册响应:", response.data);

      // 注册成功后不自动登录，而是返回成功响应
      return response.data;
    } catch (error: any) {
      console.error("注册失败:", error);
      // 抛出错误，让组件处理错误显示
      throw error;
    }
  };

  const logout = () => {
    console.log("执行登出操作");
    token.value = null;
    userInfo.value = null;
    isAuthenticated.value = false;
    localStorage.removeItem("token");
  };

  const updateProfile = async (data: ProfileUpdateRequest) => {
    try {
      const response = await request.put<UserInfo>("/api/auth/profile", data);
      userInfo.value = response.data;
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || "更新个人信息失败");
    }
  };

  const changePassword = async (data: PasswordChangeRequest) => {
    try {
      await request.post("/api/auth/change-password", data);
    } catch (error: any) {
      throw new Error(error.response?.data?.message || "修改密码失败");
    }
  };

  const initialize = async () => {
    const savedToken = localStorage.getItem("token");
    console.log("初始化用户信息, 保存的token:", savedToken);

    if (savedToken) {
      token.value = savedToken.trim();
      isAuthenticated.value = true;
      try {
        await getUserInfo();
      } catch (error) {
        console.error("初始化获取用户信息失败:", error);
        logout();
      }
    }
  };

  const getUserInfo = async () => {
    try {
      console.log("开始获取用户信息");
      const response = await request.get<UserInfo>("/api/auth/info");
      console.log("获取用户信息成功:", response.data);

      if (response.data) {
        userInfo.value = response.data;
        return response.data;
      }
      throw new Error("获取用户信息失败: 响应数据为空");
    } catch (error) {
      console.error("获取用户信息失败:", error);
      throw error;
    }
  };

  const uploadAvatar = async (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    try {
      const response = await request.post("/api/auth/avatar", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      });
      if (userInfo.value && response.data.avatar) {
        userInfo.value.avatar = response.data.avatar;
      }
      return response.data;
    } catch (error) {
      console.error("上传头像失败:", error);
      throw error;
    }
  };

  const resetPassword = async (token: string, newPassword: string) => {
    try {
      console.log("开始重置密码");
      const response = await request.post("/api/auth/reset-password", {
        token,
        newPassword,
      });
      console.log("密码重置成功:", response.data);
      return response.data;
    } catch (error) {
      console.error("密码重置失败:", error);
      throw error;
    }
  };

  const forgotPassword = async (email: string) => {
    try {
      console.log("开始发送忘记密码请求，邮箱:", email);
      const response = await request.post("/api/auth/forgot-password", {
        email,
      });
      console.log("忘记密码请求发送成功:", response.data);
      return response.data;
    } catch (error: any) {
      console.error("忘记密码请求失败:", error);
      const errorMessage =
        error.response?.data?.message || "发送重置密码邮件失败，请重试";
      throw new Error(errorMessage);
    }
  };

  return {
    token,
    userInfo,
    isAuthenticated,
    login,
    register,
    logout,
    updateProfile,
    changePassword,
    getUserInfo,
    initialize,
    uploadAvatar,
    resetPassword,
    forgotPassword,
  };
});
