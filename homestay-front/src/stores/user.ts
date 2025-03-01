import { defineStore } from "pinia";
import { ref, computed } from "vue";
import request from "@/utils/request";
import { LoginRequest, RegisterRequest, AuthResponse } from "@/types/auth";

export interface UserInfo {
  id: number;
  username: string;
  email: string;
  phone?: string;
  realName?: string;
  idCard?: string;
  role: string;
  verificationStatus?: string;
  avatar?: string;
}

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
  const user = ref<UserInfo | null>(null);

  const isLoggedIn = computed(() => token.value !== null);

  const setToken = (newToken: string | null) => {
    token.value = newToken;
    if (newToken) {
      localStorage.setItem("token", newToken);
    } else {
      localStorage.removeItem("token");
    }
  };

  const setUser = (userInfo: UserInfo) => {
    console.log("设置用户信息:", userInfo);
    user.value = userInfo;
  };

  const login = async (username: string, password: string) => {
    try {
      // 调用后端登录API
      const response = await request.post("/api/auth/login", {
        username,
        password,
      });

      console.log("登录响应:", response.data);

      if (response.data && response.data.token) {
        // 设置token
        setToken(response.data.token);

        // 如果登录响应中包含用户信息，直接使用
        if (response.data.id && response.data.username) {
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
          setUser(userData);
        } else {
          // 否则调用获取用户信息接口
          await fetchUserInfo();
        }

        return true;
      }
      return false;
    } catch (error: any) {
      console.error("登录失败:", error);
      if (error.response) {
        console.error("错误响应:", error.response.data);
      }

      // 仅在开发环境下使用模拟数据
      if (process.env.NODE_ENV === "development") {
        console.warn("使用模拟登录数据（仅用于开发测试）");
        setToken("mock-token-" + Date.now());
        user.value = {
          id: 1,
          username: username,
          email: username + "@example.com",
          phone: "13800138000",
          role: "user",
          avatar: "",
        };
        return true;
      }

      return false;
    }
  };

  const register = async (
    registerData: Omit<RegisterRequest, "confirmPassword">
  ) => {
    try {
      console.log("模拟注册成功:", registerData);
      return true;
    } catch (error) {
      console.error("注册失败:", error);
      throw error;
    }
  };

  const logout = () => {
    setToken(null);
    user.value = null;
  };

  const updateProfile = async (data: ProfileUpdateRequest) => {
    try {
      const response = await request.put<UserInfo>("/api/users/profile", data);
      user.value = response.data;
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || "更新个人信息失败");
    }
  };

  const changePassword = async (data: PasswordChangeRequest) => {
    try {
      await request.post("/api/users/change-password", data);
    } catch (error: any) {
      throw new Error(error.response?.data?.message || "修改密码失败");
    }
  };

  const fetchUserInfo = async () => {
    if (!token.value) return null;

    try {
      console.log("开始获取用户信息");

      // 检查当前API路径是否正确
      const apiPath = "/api/auth/info";
      console.log("尝试从以下路径获取用户信息:", apiPath);

      // 从后端获取真实用户数据
      const response = await request.get<UserInfo>(apiPath);
      console.log("获取用户信息成功:", response.data);

      if (response.data) {
        user.value = response.data;
        return response.data;
      }
      throw new Error("获取用户信息失败: 响应数据为空");
    } catch (error: any) {
      console.error("获取用户信息失败:", error);

      // 只有在开发环境下且请求返回404时才使用模拟数据
      if (
        import.meta.env.DEV &&
        error.response &&
        (error.response.status === 404 || error.response.status === 500) &&
        !user.value
      ) {
        console.warn("使用模拟用户数据用于开发测试");
        user.value = {
          id: 1,
          username: "开发测试用户",
          email: "dev@example.com",
          phone: "13800138000",
          role: "user",
          avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=dev${Date.now()}`,
        };
        return user.value;
      }

      // 如果是401错误（未授权），清除token并返回null
      if (error.response && error.response.status === 401) {
        console.warn("用户未授权，清除token");
        setToken(null);
        user.value = null;
        return null;
      }

      throw error;
    }
  };

  /**
   * 上传用户头像
   */
  const uploadAvatar = async (file: File) => {
    try {
      // 基本验证
      if (!file) {
        throw new Error("请选择要上传的头像文件");
      }

      // 验证文件类型
      if (!file.type.startsWith("image/")) {
        throw new Error("只能上传图片文件");
      }

      // 验证文件大小 (10MB)
      const maxSize = 10 * 1024 * 1024;
      if (file.size > maxSize) {
        throw new Error("头像文件大小不能超过10MB");
      }

      // 记录文件信息
      const fileSizeKB = (file.size / 1024).toFixed(2);
      console.log("准备上传头像:", {
        文件名: file.name,
        类型: file.type,
        大小: `${fileSizeKB}KB`,
      });

      // 创建FormData对象
      const formData = new FormData();
      formData.append("avatar", file);

      // 发送请求，确保不设置Content-Type，让浏览器自动设置正确的boundary
      const response = await request.post("/api/auth/avatar", formData, {
        headers: {
          // 不要手动设置Content-Type，让axios自动处理
          // 'Content-Type': 'multipart/form-data'
        },
      });

      // 处理响应
      if (response.data) {
        // 保存新的头像路径
        const avatarPath = response.data;
        console.log("头像上传成功:", avatarPath);

        // 更新用户头像
        if (user.value) {
          const oldAvatar = user.value.avatar;
          user.value.avatar = avatarPath;
          console.log("用户头像已更新:", avatarPath);

          // 重要：保存当前用户状态到localStorage，防止状态丢失
          saveUserToLocalStorage();

          // 不再尝试刷新用户信息，避免可能的登录状态丢失
          console.log("头像更新完成，无需刷新用户信息");
        }

        return avatarPath;
      } else {
        throw new Error("上传头像失败：服务器未返回有效数据");
      }
    } catch (error: any) {
      console.error("上传头像失败:", error);
      // 不要在这里尝试刷新用户信息或重新登录
      // 只是抛出错误，让调用者处理
      throw error;
    }
  };

  /**
   * 保存用户信息到本地存储
   */
  const saveUserToLocalStorage = () => {
    if (user.value) {
      localStorage.setItem("user", JSON.stringify(user.value));
      console.log("用户信息已保存到本地存储");
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
      console.log("模拟发送重置密码邮件到:", email);
      return true;
    } catch (error) {
      console.error("发送重置密码邮件失败:", error);
      throw error;
    }
  };

  return {
    token,
    user,
    isLoggedIn,
    setToken,
    login,
    logout,
    register,
    updateProfile,
    changePassword,
    fetchUserInfo,
    uploadAvatar,
    resetPassword,
    forgotPassword,
  };
});
