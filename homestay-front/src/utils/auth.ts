import axios from "axios";
import request from "./request";
import { ElMessage } from "element-plus";

/**
 * 检查当前的认证状态并进行测试请求
 * 这个函数用于调试认证问题，可以在前端页面中添加一个按钮来调用它
 */
export const detailedAuthCheck = async () => {
  console.log("====== 开始检查认证状态 ======");

  // 1. 检查本地存储的token
  const localToken = localStorage.getItem("token");
  console.log(
    "localStorage中的token: ",
    localToken ? `${localToken.substring(0, 15)}...` : "不存在"
  );

  const sessionToken = sessionStorage.getItem("token");
  console.log(
    "sessionStorage中的token: ",
    sessionToken ? `${sessionToken.substring(0, 15)}...` : "不存在"
  );

  // 2. 检查用户信息
  let userInfo = null;
  try {
    const storedUserInfo = localStorage.getItem("userInfo");
    if (storedUserInfo) {
      userInfo = JSON.parse(storedUserInfo);
      console.log("localStorage中的用户信息: ", {
        username: userInfo.username || "未知",
        role: userInfo.role || "未知",
        tokenExists: !!userInfo.token,
      });
    } else {
      console.log("localStorage中没有用户信息");
    }
  } catch (e) {
    console.error("解析用户信息失败: ", e);
  }

  // 3. 发送测试请求到/api/auth/current
  console.log("发送测试请求到 /api/auth/current...");
  try {
    const currentUserResponse = await request({
      url: "/api/auth/current",
      method: "get",
    });
    console.log("获取当前用户成功: ", currentUserResponse.data);
  } catch (error) {
    console.error("获取当前用户失败: ", error);
  }

  // 4. 使用原生axios发送测试请求，确保请求拦截器正常工作
  console.log("使用原生axios发送测试请求，检查请求拦截器...");

  const headers: Record<string, string> = {};
  if (localToken) {
    headers["Authorization"] = `Bearer ${localToken}`;
  }

  try {
    const response = await axios.get("/api/auth/current", { headers });
    console.log("原生请求成功: ", response.data);
  } catch (error) {
    console.error("原生请求失败: ", error);
  }

  // 5. 测试请求到房源API
  console.log("测试请求到房源API...");
  try {
    const homestayResponse = await request({
      url: "/api/homestays/owner",
      method: "get",
    });
    console.log("获取房东房源成功: ", homestayResponse.data);
  } catch (error) {
    console.error("获取房东房源失败: ", error);
  }

  console.log("====== 认证状态检查完成 ======");

  return {
    localTokenExists: !!localToken,
    sessionTokenExists: !!sessionToken,
    userInfoExists: !!userInfo,
  };
};

/**
 * 使用指定的token发送测试请求
 * @param url 请求的URL
 * @param token 要使用的token
 */
export const testRequestWithToken = async (url: string, token: string) => {
  console.log(`测试请求: ${url} (使用提供的token)`);

  try {
    const response = await axios.get(url, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    console.log("测试请求成功: ", response.data);
    return {
      success: true,
      data: response.data,
    };
  } catch (error) {
    console.error("测试请求失败: ", error);
    return {
      success: false,
      error,
    };
  }
};

// 获取当前token
export function getToken(): string | null {
  return localStorage.getItem("token");
}

// 设置token到localStorage
export function setToken(token: string): void {
  localStorage.setItem("token", token);
}

// 移除token
export function removeToken(): void {
  localStorage.removeItem("token");
}

// 获取当前用户信息
export function getCurrentUser(): any {
  const userString = localStorage.getItem("user");
  if (!userString) return null;

  try {
    return JSON.parse(userString);
  } catch (e) {
    console.error("解析用户信息失败", e);
    return null;
  }
}

// 检查是否登录
export function isLoggedIn(): boolean {
  return !!getToken() && !!getCurrentUser();
}

// 检查认证状态
export function checkAuthentication(): Promise<boolean> {
  const token = getToken();
  const user = getCurrentUser();

  console.log("检查认证状态:");
  console.log(`- Token存在: ${!!token}`);
  if (token) {
    console.log(`- Token预览: ${token.substring(0, 15)}...`);
  }
  console.log(`- 用户信息存在: ${!!user}`);
  if (user) {
    console.log(`- 用户信息:`, user);
  }

  // 如果没有token，直接返回未认证
  if (!token) {
    console.error("认证检查失败: 没有找到token");
    ElMessage.error("认证检查失败: 没有找到token，请先登录");
    return Promise.resolve(false);
  }

  // 发送测试请求到用户API
  return request({
    url: "/api/auth/current",
    method: "get",
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })
    .then((response) => {
      console.log("认证检查成功:", response.data);
      ElMessage.success("认证状态正常，您已成功登录");
      return true;
    })
    .catch((error) => {
      console.error("认证检查失败:", error);

      if (error.response) {
        console.error(`状态码: ${error.response.status}`);
        console.error(`响应数据:`, error.response.data);

        if (error.response.status === 401 || error.response.status === 403) {
          ElMessage.error("认证已过期或无效，请重新登录");
          // 清除无效token
          removeToken();
        } else {
          ElMessage.error(`认证检查失败: ${error.message || "未知错误"}`);
        }
      } else {
        ElMessage.error(`认证检查失败: ${error.message || "网络错误"}`);
      }

      return false;
    });
}

// 显示登录提示并重定向
function alertLogin(redirectPath: string = "/login"): void {
  console.error("未找到登录token，将重定向到登录页");
  ElMessage.error("请先登录后再进行此操作");

  // 保存当前URL，以便登录后返回
  const currentPath = window.location.pathname;
  sessionStorage.setItem("redirectAfterLogin", currentPath);

  // 延迟重定向，让用户看到提示信息
  setTimeout(() => {
    window.location.href = redirectPath;
  }, 1500);
}

/**
 * 检查用户是否已登录，并且具有所需的角色权限
 * 如果用户未登录，会自动跳转到登录页面，并在登录成功后跳转回当前页面
 * 如果用户已登录但缺少权限，会显示错误消息
 * @param requiredRoles 所需角色数组，可选
 * @param redirectPath 重定向路径，默认为登录页面
 * @returns Promise<boolean> 如果用户已登录并具有所需权限，则为true
 */
export async function ensureUserLoggedIn(
  requiredRoles?: string[],
  redirectPath: string = "/login"
): Promise<boolean> {
  console.log("执行登录检查，要求角色:", requiredRoles);

  // 获取令牌
  const token = getToken();

  if (!token) {
    console.log("未找到有效令牌，需要登录");
    alertLogin(redirectPath);
    return false;
  }

  // 获取本地角色信息
  let localUserRole = "";
  try {
    const userInfoStr = localStorage.getItem("userInfo");
    if (userInfoStr) {
      const userInfo = JSON.parse(userInfoStr);
      if (userInfo.role) {
        localUserRole = userInfo.role;
        console.log("从userInfo中获取到角色:", localUserRole);
      }
    }

    if (!localUserRole) {
      const userStr = localStorage.getItem("user");
      if (!userStr) {
        console.warn("localStorage中没有找到user信息");
      } else {
        const user = JSON.parse(userStr);
        if (user.role) {
          localUserRole = user.role;
          console.log("从user中获取到角色:", localUserRole);
        } else if (user.authorities && user.authorities.length > 0) {
          localUserRole =
            typeof user.authorities[0] === "string"
              ? user.authorities[0]
              : user.authorities[0].authority;
          console.log("从user.authorities中获取到角色:", localUserRole);
        }
      }
    }
  } catch (e) {
    console.error("解析本地用户信息失败:", e);
  }

  // 特殊处理：检查是否在编辑房源页面，如果是，自动授予ROLE_HOST权限
  const currentPath = window.location.pathname;
  const isHomestayEditPage =
    currentPath.includes("/host/homestay/edit/") ||
    currentPath.includes("/host/homestay/create");

  if (isHomestayEditPage && token) {
    console.log("检测到用户在房源编辑页面，临时授予ROLE_HOST权限");

    // 只在开发环境强制授予权限
    if (import.meta.env.DEV) {
      // 在localStorage中更新角色信息
      try {
        // 更新userInfo
        const userInfoStr = localStorage.getItem("userInfo");
        if (userInfoStr) {
          const userInfo = JSON.parse(userInfoStr);
          if (!userInfo.role || !userInfo.role.includes("HOST")) {
            userInfo.role = userInfo.role
              ? `${userInfo.role},ROLE_HOST`
              : "ROLE_HOST";
            localStorage.setItem("userInfo", JSON.stringify(userInfo));
            console.log("已更新userInfo中的角色:", userInfo.role);
          }
        }

        // 更新user
        const userStr = localStorage.getItem("user");
        if (userStr) {
          const user = JSON.parse(userStr);
          // 检查authorities数组
          if (
            !user.authorities ||
            !user.authorities.some(
              (a: { authority?: string } | string) =>
                (typeof a === "string" && a.includes("HOST")) ||
                (a &&
                  typeof a === "object" &&
                  a.authority &&
                  a.authority.includes("HOST"))
            )
          ) {
            if (!user.authorities) {
              user.authorities = [];
            }
            user.authorities.push({ authority: "ROLE_HOST" });
            localStorage.setItem("user", JSON.stringify(user));
            console.log("已更新user.authorities:", user.authorities);
          }
        }

        // 设置本地角色变量，以便后续检查
        localUserRole = "ROLE_HOST";
      } catch (e) {
        console.error("更新本地用户角色失败:", e);
      }
    }
  }

  // 如果没有要求特定角色，则认为只要有token就可以
  if (!requiredRoles || requiredRoles.length === 0) {
    console.log("没有指定角色要求，认证通过");
    return true;
  }

  // 发送验证请求到后端
  return request({
    url: "/api/auth/current",
    method: "get",
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })
    .then((response) => {
      if (!response || !response.data) {
        console.error("验证响应为空");
        alertLogin(redirectPath);
        return false;
      }

      const userData = response.data;
      let userRole = "";

      // 尝试从响应中获取角色信息
      if (userData.role) {
        userRole = userData.role;
      } else if (userData.roles) {
        if (Array.isArray(userData.roles)) {
          userRole = userData.roles.join(",");
        } else {
          userRole = String(userData.roles);
        }
      } else if (userData.authorities) {
        if (Array.isArray(userData.authorities)) {
          userRole = userData.authorities
            .map((auth: any) =>
              typeof auth === "string" ? auth : auth.authority || ""
            )
            .filter(Boolean)
            .join(",");
        } else {
          userRole = String(userData.authorities);
        }
      }

      // 如果服务器未返回角色，使用本地角色
      if ((!userRole || userRole === "") && localUserRole) {
        userRole = localUserRole;
        console.log("服务器未返回角色，使用本地角色信息:", userRole);
      }

      console.log(
        `用户角色: ${userRole || "未设置"}, 需要的角色: ${
          requiredRoles ? requiredRoles.join(",") : "any"
        }`
      );

      // 特殊处理：房源编辑页面总是允许通过
      if (isHomestayEditPage && token && import.meta.env.DEV) {
        console.log("房源编辑页面特殊处理：强制授予访问权限");
        return true;
      }

      // 检查用户角色是否符合要求
      if (!userRole || userRole === "") {
        // 角色为空，显示错误信息
        console.error(
          "当前用户没有所需角色权限。用户角色:",
          userRole,
          "所需角色:",
          requiredRoles.join(",")
        );
        ElMessage.error("您没有所需的角色权限，请联系管理员");
        return false;
      }

      // 将用户角色和所需角色标准化（去掉ROLE_前缀）
      const userRoles = userRole.split(",").map((r) => {
        const role = r.trim();
        return role.startsWith("ROLE_") ? role.substring(5) : role;
      });

      const requiredRolesNormalized = requiredRoles.map((r) => {
        const role = r.trim();
        return role.startsWith("ROLE_") ? role.substring(5) : role;
      });

      // 检查用户是否拥有所需角色
      const hasRequiredRole = userRoles.some((ur) =>
        requiredRolesNormalized.some(
          (rr) => ur.toLowerCase() === rr.toLowerCase()
        )
      );

      if (!hasRequiredRole) {
        console.error(
          "当前用户没有所需角色权限。用户角色:",
          userRoles.join(","),
          "所需角色:",
          requiredRolesNormalized.join(",")
        );
        ElMessage.error(
          `您没有所需的角色权限 (${requiredRolesNormalized.join(",")})`
        );
        return false;
      }

      console.log("用户角色验证通过");
      return true;
    })
    .catch((error) => {
      console.error("认证检查失败:", error);

      // 特殊处理：房源编辑页面在开发环境中总是允许通过
      if (isHomestayEditPage && token && import.meta.env.DEV) {
        console.log("房源编辑页面特殊处理：尽管API失败，仍强制授予访问权限");
        return true;
      }

      // 如果有本地角色且符合要求，允许通过
      if (localUserRole && requiredRoles) {
        const normalizedLocalRole = localUserRole.replace("ROLE_", "");
        const hasLocalRequiredRole = requiredRoles.some(
          (role) =>
            normalizedLocalRole === role ||
            normalizedLocalRole === role.toUpperCase() ||
            localUserRole === role ||
            localUserRole === `ROLE_${role}` ||
            localUserRole === `ROLE_${role.toUpperCase()}`
        );

        if (hasLocalRequiredRole) {
          console.log(`虽然API请求失败，但本地角色检查通过: ${localUserRole}`);
          return true;
        }
      }

      // 获取失败，可能是令牌过期
      console.error("验证请求失败，可能是令牌已过期");
      alertLogin(redirectPath);
      return false;
    });
}

export default {
  checkAuthentication,
  testRequestWithToken,
  ensureUserLoggedIn,
};
