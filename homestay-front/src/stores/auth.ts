import { defineStore } from "pinia";
import { ref, computed } from "vue";
import api from "@/api";
import router from "@/router";

interface User {
  id: number;
  username: string;
  email: string;
  role: string;
  authorities?: { authority: string }[];
  avatar?: string;
  phone?: string;
  fullName?: string;
  realName?: string;
  idCard?: string;
  verificationStatus?: string;
}

interface AuthResponse {
  token: string;
  user: User;
}

interface LoginCredentials {
  username: string;
  password: string;
}

interface RegisterData {
  username: string;
  email: string;
  password: string;
  phone?: string;
  role?: string;
}

interface ProfileData {
  email?: string;
  phone?: string;
  fullName?: string;
  realName?: string;
  idCard?: string;
}

interface ResetPasswordData {
  token: string;
  newPassword: string;
}

export const useAuthStore = defineStore("auth", () => {
  const token = ref<string>(localStorage.getItem("token") || "");
  const user = ref<User | null>(
    JSON.parse(localStorage.getItem("user") || "null")
  );

  // 如果从localStorage加载的用户对象存在但role为空，尝试从authorities中提取角色
  if (
    user.value &&
    (!user.value.role || user.value.role === "") &&
    user.value.authorities &&
    user.value.authorities.length > 0
  ) {
    console.log("从authorities中提取角色信息:", user.value.authorities);
    const authority = user.value.authorities.find((auth) =>
      auth.authority.startsWith("ROLE_")
    );
    if (authority) {
      user.value.role = authority.authority;
      console.log("已提取角色:", user.value.role);
      localStorage.setItem("user", JSON.stringify(user.value));
    }
  }

  const isAuthenticated = computed(() => !!token.value);
  const userRole = computed(() => {
    console.log("获取用户角色:", user.value?.role);
    return user.value?.role || "";
  });
  const isLandlord = computed(() => {
    const isLandlordValue = userRole.value === "ROLE_HOST";
    console.log("检查是否是房东:", {
      role: userRole.value,
      isLandlord: isLandlordValue,
    });
    return isLandlordValue;
  });

  async function login(credentials: LoginCredentials) {
    try {
      const response = await api.post<AuthResponse>(
        "/api/auth/login",
        credentials
      );
      setAuth(response.data);
      return response;
    } catch (error) {
      console.error("Login error:", error);
      throw error;
    }
  }

  async function register(userData: RegisterData) {
    try {
      const response = await api.post<AuthResponse>(
        "/api/auth/register",
        userData
      );
      setAuth(response.data);
      return response;
    } catch (error) {
      console.error("Register error:", error);
      throw error;
    }
  }

  function setAuth(data: AuthResponse) {
    console.log("设置认证信息:", data);
    token.value = data.token;

    // 确保用户对象的角色信息正确
    if (data.user) {
      // 如果role不存在或为空，但authorities存在，从authorities提取角色
      if (
        (!data.user.role || data.user.role === "") &&
        data.user.authorities &&
        data.user.authorities.length > 0
      ) {
        console.log("从authorities中提取角色信息:", data.user.authorities);
        const authority = data.user.authorities.find((auth) =>
          auth.authority.startsWith("ROLE_")
        );
        if (authority) {
          data.user.role = authority.authority;
          console.log("已提取角色:", data.user.role);
        }
      }
    }

    user.value = data.user;
    localStorage.setItem("token", data.token);
    localStorage.setItem("user", JSON.stringify(data.user));
    // 同时保存为userInfo，确保两个store同步
    localStorage.setItem("userInfo", JSON.stringify(data.user));
    api.defaults.headers.common["Authorization"] = `Bearer ${data.token}`;
    console.log(
      "用户认证完成，角色:",
      data.user.role,
      "是否房东:",
      data.user.role === "ROLE_HOST"
    );
  }

  function logout() {
    token.value = "";
    user.value = null;

    // 清除所有相关存储
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    localStorage.removeItem("userInfo");

    delete api.defaults.headers.common["Authorization"];

    // 使用window.location导航以确保页面刷新
    window.location.href = "/login";
  }

  async function updateProfile(profileData: ProfileData) {
    try {
      const response = await api.put<User>("/api/users/profile", profileData);
      user.value = response.data;
      localStorage.setItem("user", JSON.stringify(response.data));
      return response;
    } catch (error) {
      console.error("Update profile error:", error);
      throw error;
    }
  }

  async function uploadAvatar(formData: FormData) {
    try {
      const response = await api.post<{ url: string }>(
        "/api/auth/upload-avatar",
        formData,
        {
          headers: {
            "Content-Type": "multipart/form-data",
          },
        }
      );

      if (response.data && response.data.url && user.value) {
        user.value = {
          ...user.value,
          avatar: response.data.url,
        };
        localStorage.setItem("user", JSON.stringify(user.value));
      }

      return response;
    } catch (error) {
      console.error("Upload avatar error:", error);
      throw error;
    }
  }

  async function forgotPassword(email: string) {
    try {
      const response = await api.post("/api/auth/forgot-password", null, {
        params: { email },
      });
      return response;
    } catch (error) {
      console.error("Forgot password error:", error);
      throw error;
    }
  }

  async function resetPassword(resetData: ResetPasswordData) {
    try {
      const response = await api.post("/api/auth/reset-password", resetData);
      return response;
    } catch (error) {
      console.error("Reset password error:", error);
      throw error;
    }
  }

  // 检查并修复用户角色数据
  function syncUserRole() {
    if (
      user.value &&
      (!user.value.role || user.value.role === "") &&
      user.value.authorities &&
      user.value.authorities.length > 0
    ) {
      const authority = user.value.authorities.find((auth) =>
        auth.authority.startsWith("ROLE_")
      );
      if (authority) {
        user.value.role = authority.authority;
        localStorage.setItem("user", JSON.stringify(user.value));
        console.log("用户角色已同步:", user.value.role);
        return true;
      }
    }
    return false;
  }

  // 初始化时设置 API 请求头
  if (token.value) {
    api.defaults.headers.common["Authorization"] = `Bearer ${token.value}`;
  }

  return {
    token,
    user,
    isAuthenticated,
    userRole,
    isLandlord,
    login,
    register,
    logout,
    updateProfile,
    uploadAvatar,
    forgotPassword,
    resetPassword,
    syncUserRole,
  };
});
