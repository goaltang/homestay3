import { defineStore } from "pinia";
import { ref, computed } from "vue";
import api from "@/api";
import { LoginRequest, RegisterRequest, AuthResponse } from "@/types/auth";

export interface UserInfo {
  id: number;
  username: string;
  email: string;
  phone?: string;
  realName?: string;
  idCard?: string;
  role: string;
  avatar?: string;
  verificationStatus?: string;
  authorities?: { authority: string }[];
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
  const userInfo = ref<UserInfo | null>(
    JSON.parse(localStorage.getItem("userInfo") || "null")
  );

  const isAuthenticated = computed(() => !!token.value);
  const isAdmin = computed(() => userInfo.value?.role === "ROLE_ADMIN");
  const isLandlord = computed(() => {
    if (!userInfo.value?.role) return false;

    // 支持多种可能的房东角色名称
    const role = userInfo.value.role.toUpperCase();
    const isLandlordResult =
      role === "ROLE_LANDLORD" ||
      role === "ROLE_HOST" ||
      role === "LANDLORD" ||
      role === "HOST";

    console.log("isLandlord计算:", {
      role: userInfo.value.role,
      normalized: role,
      result: isLandlordResult,
    });

    return isLandlordResult;
  });

  const setToken = (newToken: string | null) => {
    token.value = newToken;
    if (newToken) {
      localStorage.setItem("token", newToken);
    } else {
      localStorage.removeItem("token");
    }
  };

  const setUser = (user: UserInfo) => {
    console.log("设置用户信息:", user);
    userInfo.value = user;
    // 保存用户信息到 localStorage
    localStorage.setItem("userInfo", JSON.stringify(user));
    // 输出调试信息
    console.log("用户角色:", user.role);
    console.log("isLandlord计算值:", user.role === "ROLE_LANDLORD");
  };

  const login = async (username: string, password: string) => {
    try {
      // 调用后端登录API
      const response = await api.post("/api/auth/login", {
        username,
        password,
      });

      console.log("登录响应:", response.data);

      if (response.data && response.data.token) {
        // 设置token
        setToken(response.data.token);

        // 直接从响应中提取角色信息
        let role = response.data.role;
        console.log("从响应中直接获取的角色:", role);

        // 如果响应中有user对象并包含角色
        if (response.data.user && response.data.user.role) {
          role = response.data.user.role;
          console.log("从user对象获取的角色:", role);
        }

        // 如果role为空但authorities存在，从authorities中提取角色
        if (
          (!role || role === "") &&
          response.data.authorities &&
          response.data.authorities.length > 0
        ) {
          console.log(
            "从authorities中提取角色信息:",
            response.data.authorities
          );
          const authority = response.data.authorities.find((auth: any) =>
            auth.authority.startsWith("ROLE_")
          );
          if (authority) {
            role = authority.authority;
            console.log("已从authorities提取角色:", role);
          }
        }

        // 确保userData的role字段有值
        const userData = {
          id:
            response.data.id ||
            (response.data.user ? response.data.user.id : 0),
          username:
            response.data.username ||
            (response.data.user ? response.data.user.username : username),
          email:
            response.data.email ||
            (response.data.user ? response.data.user.email : ""),
          phone:
            response.data.phone ||
            (response.data.user ? response.data.user.phone : ""),
          realName:
            response.data.realName ||
            (response.data.user ? response.data.user.realName : ""),
          idCard:
            response.data.idCard ||
            (response.data.user ? response.data.user.idCard : ""),
          role: role || "ROLE_USER", // 确保始终有角色
          avatar:
            response.data.avatar ||
            (response.data.user ? response.data.user.avatar : ""),
          verificationStatus:
            response.data.verificationStatus ||
            (response.data.user ? response.data.user.verificationStatus : ""),
        };

        console.log("准备保存的用户数据:", userData);
        setUser(userData);

        // 确保localStorage保存了用户信息
        localStorage.setItem("userInfo", JSON.stringify(userData));
        console.log("用户信息已保存到localStorage", userData);
        console.log("检查isLandlord:", isLandlord.value);

        return true;
      }
      return false;
    } catch (error: any) {
      console.error("登录失败:", error);
      if (error.response) {
        console.error("错误响应:", error.response.data);
      }

      // 检查是否有开发模式模拟数据
      if (
        process.env.NODE_ENV === "development" &&
        import.meta.env.VITE_USE_MOCK === "true"
      ) {
        console.warn("使用模拟登录数据（仅用于开发测试）");
        // 仅在开发环境且配置了使用模拟数据时才使用模拟数据
        setToken("mock-token-" + Date.now());

        const mockUser = {
          id: 1,
          username: username,
          email: username + "@example.com",
          phone: "13800138000",
          role: username.includes("landlord") ? "ROLE_LANDLORD" : "ROLE_USER",
          avatar: "",
        };

        setUser(mockUser);
        console.log("模拟登录用户:", mockUser);
        return true;
      }

      return false;
    }
  };

  const register = async (
    registerData: Omit<RegisterRequest, "confirmPassword">
  ) => {
    try {
      // 确保角色信息是大写且格式正确
      if (registerData.role && !registerData.role.startsWith("ROLE_")) {
        registerData.role = `ROLE_${registerData.role.toUpperCase()}`;
      }

      console.log("尝试注册新用户，发送数据:", JSON.stringify(registerData));
      // 添加后端API调用
      const response = await api.post("/api/auth/register", registerData);

      console.log("注册响应:", response.data);

      if (response.data && response.data.token) {
        // 设置token
        setToken(response.data.token);

        // 确保角色信息正确
        let role = response.data.role || registerData.role;

        // 如果role为空但authorities存在，从authorities中提取角色
        if (
          (!role || role === "") &&
          response.data.authorities &&
          response.data.authorities.length > 0
        ) {
          console.log(
            "从authorities中提取角色信息:",
            response.data.authorities
          );
          const authority = response.data.authorities.find((auth: any) =>
            auth.authority.startsWith("ROLE_")
          );
          if (authority) {
            role = authority.authority;
            console.log("已提取角色:", role);
          }
        }

        // 设置用户信息
        const userData = {
          id: response.data.id,
          username: response.data.username,
          email: response.data.email,
          phone: response.data.phone || registerData.phone,
          role: role,
          avatar: response.data.avatar,
          verificationStatus: response.data.verificationStatus,
          authorities: response.data.authorities, // 确保authorities也被保存
        };

        setUser(userData);

        // 确保localStorage保存了用户信息
        localStorage.setItem("userInfo", JSON.stringify(userData));
        console.log("用户信息已保存到localStorage", userData);

        // 不再强制刷新页面，让调用方处理导航
        // window.location.reload();

        return true;
      }

      // 如果后端API调用失败，回退到模拟注册
      if (import.meta.env.DEV) {
        console.warn("使用模拟注册数据（仅用于开发测试）");
        setToken("mock-token-" + Date.now());

        const mockUserData = {
          id: Date.now(),
          username: registerData.username,
          email: registerData.email,
          phone: registerData.phone || "",
          role: registerData.role || "ROLE_USER",
          avatar: "",
        };

        setUser(mockUserData);
        console.log("模拟注册成功:", mockUserData);
        return true;
      }

      return false;
    } catch (error: any) {
      console.error("注册失败:", error);

      // 只有在开发环境才使用模拟数据
      if (import.meta.env.DEV && !error.response) {
        console.warn("API调用失败，使用模拟注册数据（仅用于开发测试）");
        setToken("mock-token-" + Date.now());

        const mockUserData = {
          id: Date.now(),
          username: registerData.username,
          email: registerData.email,
          phone: registerData.phone || "",
          role: registerData.role || "ROLE_USER",
          avatar: "",
        };

        setUser(mockUserData);
        console.log("模拟注册成功:", mockUserData);
        return true;
      }

      // 确保错误中包含后端返回的消息
      if (error.response && error.response.data) {
        if (typeof error.response.data === "string") {
          error.message = error.response.data;
        } else if (error.response.data.message) {
          error.message = error.response.data.message;
        }
      }

      // 抛出错误，让调用者处理
      throw error;
    }
  };

  const logout = () => {
    // 清除token和用户信息
    setToken(null);
    userInfo.value = null;

    // 清除localStorage中的所有用户相关数据
    localStorage.removeItem("token");
    localStorage.removeItem("userInfo");
    localStorage.removeItem("user");

    // 清除API请求头中的Authorization
    delete api.defaults.headers.common["Authorization"];

    // 导航到登录页
    window.location.href = "/login";
  };

  const updateProfile = async (data: ProfileUpdateRequest) => {
    try {
      const response = await api.put<UserInfo>("/api/users/profile", data);
      userInfo.value = response.data;
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.message || "更新个人信息失败");
    }
  };

  const changePassword = async (data: PasswordChangeRequest) => {
    try {
      await api.post("/api/users/change-password", data);
    } catch (error: any) {
      throw new Error(error.response?.data?.message || "修改密码失败");
    }
  };

  const fetchUserInfo = async () => {
    try {
      console.log("开始获取用户信息");

      // 如果没有token，不执行请求
      if (!token.value) {
        console.warn("没有token，无法获取用户信息");
        return null;
      }

      // 尝试从API获取用户信息
      const response = await api.get("/api/auth/current");
      console.log("获取用户信息响应:", response.data);

      // 处理可能的不同响应格式
      let userData: UserInfo | null = null;

      if (response.data) {
        // 处理响应中直接包含用户数据的情况
        if (response.data.username || response.data.id) {
          console.log("用户信息直接在响应中");
          userData = {
            id: response.data.id || 0,
            username: response.data.username || "",
            email: response.data.email || "",
            phone: response.data.phone || "",
            realName: response.data.realName || "",
            idCard: response.data.idCard || "",
            role:
              response.data.role ||
              (response.data.authorities && response.data.authorities.length > 0
                ? response.data.authorities[0].authority
                : "ROLE_USER"),
            avatar: response.data.avatar || "",
            verificationStatus: response.data.verificationStatus || "",
          };
        }
        // 处理响应中嵌套在data或user中的情况
        else if (response.data.data || response.data.user) {
          const userDataObj = response.data.data || response.data.user;
          console.log("用户信息嵌套在data或user字段中:", userDataObj);

          if (userDataObj) {
            userData = {
              id: userDataObj.id || 0,
              username: userDataObj.username || "",
              email: userDataObj.email || "",
              phone: userDataObj.phone || "",
              realName: userDataObj.realName || "",
              idCard: userDataObj.idCard || "",
              role:
                userDataObj.role ||
                (userDataObj.authorities && userDataObj.authorities.length > 0
                  ? userDataObj.authorities[0].authority
                  : "ROLE_USER"),
              avatar: userDataObj.avatar || "",
              verificationStatus: userDataObj.verificationStatus || "",
            };
          }
        }
      }

      if (userData) {
        console.log("解析到的用户数据:", userData);
        setUser(userData);
        return userData;
      } else {
        console.warn("未能从响应中解析出用户数据");
        return null;
      }
    } catch (error: any) {
      console.error("获取用户信息失败:", error);

      // 输出详细的API错误信息
      if (error.response) {
        console.error("API响应错误:", {
          status: error.response.status,
          url: error.config.url,
          method: error.config.method,
          data: error.response.data,
        });

        // 如果是401错误，可能是token过期
        if (error.response.status === 401) {
          console.warn("用户认证失败，可能是token已过期");
          logout(); // 清除token和用户信息
        }
      }

      // 尝试备用API端点
      try {
        console.log("尝试备用API获取用户信息");
        const backupResponse = await api.get("/api/users/current");
        console.log("备用API响应:", backupResponse.data);

        if (
          backupResponse.data &&
          (backupResponse.data.username || backupResponse.data.id)
        ) {
          const userData = {
            id: backupResponse.data.id || 0,
            username: backupResponse.data.username || "",
            email: backupResponse.data.email || "",
            phone: backupResponse.data.phone || "",
            realName: backupResponse.data.realName || "",
            idCard: backupResponse.data.idCard || "",
            role: backupResponse.data.role || "ROLE_USER",
            avatar: backupResponse.data.avatar || "",
            verificationStatus: backupResponse.data.verificationStatus || "",
          };

          setUser(userData);
          return userData;
        }
      } catch (backupError) {
        console.error("备用API也失败:", backupError);
      }

      return null;
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
      formData.append("file", file);
      formData.append("type", "avatar");

      // 发送请求，确保不设置Content-Type，让浏览器自动设置正确的boundary
      const response = await api.post("/api/files/upload", formData, {
        headers: {
          // 不要手动设置Content-Type，让axios自动处理
          // 'Content-Type': 'multipart/form-data'
        },
      });

      // 处理响应
      let avatarPath = "";

      if (response.data) {
        // 处理标准的成功响应格式
        if (
          response.data.success &&
          response.data.data &&
          response.data.data.url
        ) {
          avatarPath = response.data.data.url;
          console.log("从标准响应中解析头像路径:", avatarPath);
        }
        // 处理直接返回字符串的情况
        else if (typeof response.data === "string") {
          avatarPath = response.data;
          console.log("从字符串响应中解析头像路径:", avatarPath);
        }
        // 处理其他可能的格式
        else if (response.data.path) {
          avatarPath = response.data.path;
        } else if (
          response.data.data &&
          typeof response.data.data === "string"
        ) {
          avatarPath = response.data.data;
        }
      }

      if (!avatarPath) {
        console.error("无法从响应中解析头像路径:", response.data);
        throw new Error("上传头像失败：无法解析服务器返回的头像路径");
      }

      // 确保avatarPath格式正确
      // 如果已经包含/api/前缀，则不需要进一步处理
      if (!avatarPath.startsWith("/api/") && !avatarPath.startsWith("http")) {
        // 如果以/开头但不是/api/开头，添加/api前缀
        if (avatarPath.startsWith("/")) {
          avatarPath = `/api${avatarPath}`;
        } else {
          // 没有前导斜杠，添加完整路径
          avatarPath = `/api/files/avatar/${avatarPath}`;
        }
      }

      console.log("头像上传成功，标准化后的路径:", avatarPath);

      // 更新用户头像
      if (userInfo.value) {
        const oldAvatar = userInfo.value.avatar;
        userInfo.value.avatar = avatarPath;
        console.log("用户头像已更新:", avatarPath);

        // 保存当前用户状态到localStorage
        localStorage.setItem("userInfo", JSON.stringify(userInfo.value));
        console.log("用户信息已保存到本地存储");
      }

      return avatarPath;
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
    if (userInfo.value) {
      localStorage.setItem("userInfo", JSON.stringify(userInfo.value));
      console.log("用户信息已保存到本地存储");
    }
  };

  const resetPassword = async (token: string, newPassword: string) => {
    try {
      console.log("开始重置密码");
      const response = await api.post("/api/auth/reset-password", {
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

  /**
   * 同步用户角色信息
   */
  const syncUserRole = () => {
    if (!userInfo.value) return false;

    // 如果role已存在，不需要同步
    if (userInfo.value.role && userInfo.value.role !== "") return true;

    // 如果authorities存在，从中提取角色
    if (userInfo.value.authorities && userInfo.value.authorities.length > 0) {
      console.log("尝试从authorities同步用户角色:", userInfo.value.authorities);
      const authority = userInfo.value.authorities.find((auth) =>
        auth.authority.startsWith("ROLE_")
      );
      if (authority) {
        userInfo.value.role = authority.authority;
        localStorage.setItem("userInfo", JSON.stringify(userInfo.value));
        console.log("用户角色已同步:", userInfo.value.role);
        return true;
      }
    }

    return false;
  };

  // 初始化时检查并同步用户角色
  if (userInfo.value) {
    syncUserRole();
  }

  return {
    token,
    userInfo,
    isAuthenticated,
    isAdmin,
    isLandlord,
    login,
    register,
    logout,
    updateProfile,
    changePassword,
    fetchUserInfo,
    uploadAvatar,
    resetPassword,
    forgotPassword,
    syncUserRole,
  };
});
