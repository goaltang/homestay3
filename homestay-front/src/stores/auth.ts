import { defineStore } from "pinia";
import { ref, computed } from "vue";
import api from "@/api";

const TOKEN_KEY = "homestay_token";
const USER_KEY = "homestay_user";

function migrateOldKeys() {
  const oldToken = localStorage.getItem("token");
  const oldUser = localStorage.getItem("user");
  if (oldToken && !localStorage.getItem(TOKEN_KEY)) {
    localStorage.setItem(TOKEN_KEY, oldToken);
  }
  if (oldUser && !localStorage.getItem(USER_KEY)) {
    localStorage.setItem(USER_KEY, oldUser);
  }
}

migrateOldKeys();

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
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || "");
  const user = ref<User | null>(
    JSON.parse(localStorage.getItem(USER_KEY) || "null")
  );

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
      localStorage.setItem(USER_KEY, JSON.stringify(user.value));
    }
  }

  const isAuthenticated = computed(() => !!token.value);
  const userRole = computed(() => user.value?.role || "");
  const isLandlord = computed(() => {
    const role = userRole.value;
    return role === "ROLE_HOST" || role === "ROLE_LANDLORD";
  });

  async function login(credentials: LoginCredentials) {
    const response = await api.post<AuthResponse>(
      "/api/auth/login",
      credentials
    );
    setAuth(response.data);
    return response;
  }

  async function register(userData: RegisterData) {
    const response = await api.post<AuthResponse>(
      "/api/auth/register",
      userData
    );
    setAuth(response.data);
    return response;
  }

  function setAuth(data: AuthResponse) {
    token.value = data.token;

    if (data.user) {
      if (
        (!data.user.role || data.user.role === "") &&
        data.user.authorities &&
        data.user.authorities.length > 0
      ) {
        const authority = data.user.authorities.find((auth) =>
          auth.authority.startsWith("ROLE_")
        );
        if (authority) {
          data.user.role = authority.authority;
        }
      }
    }

    user.value = data.user;
    localStorage.setItem(TOKEN_KEY, data.token);
    localStorage.setItem(USER_KEY, JSON.stringify(data.user));
    api.defaults.headers.common["Authorization"] = `Bearer ${data.token}`;

    syncFavoritesAfterLogin();
  }

  async function syncFavoritesAfterLogin() {
    try {
      const { useFavoritesStore } = await import("./favorites");
      const favoritesStore = useFavoritesStore();
      await favoritesStore.syncFavorites();
    } catch (error) {
      console.error("登录后同步收藏数据失败:", error);
    }
  }

  function logout() {
    token.value = "";
    user.value = null;

    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    localStorage.removeItem("userInfo");

    delete api.defaults.headers.common["Authorization"];

    window.location.href = "/login";
  }

  async function updateProfile(profileData: ProfileData) {
    const response = await api.put<User>("/api/users/profile", profileData);
    user.value = response.data;
    localStorage.setItem(USER_KEY, JSON.stringify(response.data));
    return response;
  }

  async function uploadAvatar(formData: FormData) {
    const response = await api.post<{ url: string }>(
      "/api/auth/upload-avatar",
      formData,
      {
        headers: { "Content-Type": "multipart/form-data" },
      }
    );

    if (response.data && response.data.url && user.value) {
      user.value = { ...user.value, avatar: response.data.url };
      localStorage.setItem(USER_KEY, JSON.stringify(user.value));
    }

    return response;
  }

  async function forgotPassword(email: string) {
    const response = await api.post("/api/auth/forgot-password", null, {
      params: { email },
    });
    return response;
  }

  async function resetPassword(resetData: ResetPasswordData) {
    const response = await api.post("/api/auth/reset-password", resetData);
    return response;
  }

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
        localStorage.setItem(USER_KEY, JSON.stringify(user.value));
        return true;
      }
    }
    return false;
  }

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
