import { defineStore } from "pinia";

const TOKEN_KEY = "homestay_admin_token";
const USER_KEY = "homestay_admin_user";
const USERNAME_KEY = "homestay_admin_username";
const USERID_KEY = "homestay_admin_userId";

function migrateOldKeys() {
  const oldToken = localStorage.getItem("token");
  const oldUserInfo = localStorage.getItem("userInfo");
  if (oldToken && !localStorage.getItem(TOKEN_KEY)) {
    localStorage.setItem(TOKEN_KEY, oldToken);
  }
  if (oldUserInfo && !localStorage.getItem(USER_KEY)) {
    localStorage.setItem(USER_KEY, oldUserInfo);
    try {
      const parsed = JSON.parse(oldUserInfo);
      if (parsed.username && !localStorage.getItem(USERNAME_KEY)) {
        localStorage.setItem(USERNAME_KEY, parsed.username);
      }
      if (parsed.id && !localStorage.getItem(USERID_KEY)) {
        localStorage.setItem(USERID_KEY, String(parsed.id));
      }
    } catch (_) {}
  }
}

migrateOldKeys();

interface UserState {
  token: string;
  userInfo: {
    username: string;
    role: string;
  } | null;
}

export const useUserStore = defineStore("user", {
  state: (): UserState => ({
    token: localStorage.getItem(TOKEN_KEY) || "",
    userInfo: null,
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    username: (state) => state.userInfo?.username,
    role: (state) => state.userInfo?.role,
  },

  actions: {
    setToken(token: string) {
      this.token = token;
      localStorage.setItem(TOKEN_KEY, token);
    },

    setUserInfo(userInfo: UserState["userInfo"]) {
      this.userInfo = userInfo;
      if (userInfo) {
        localStorage.setItem(USER_KEY, JSON.stringify(userInfo));
        if (userInfo.username) {
          localStorage.setItem(USERNAME_KEY, userInfo.username);
        }
      } else {
        localStorage.removeItem(USER_KEY);
        localStorage.removeItem(USERNAME_KEY);
        localStorage.removeItem(USERID_KEY);
      }
    },

    loadUserInfo() {
      const userInfoStr = localStorage.getItem(USER_KEY);
      if (userInfoStr) {
        try {
          const parsedUserInfo = JSON.parse(userInfoStr);
          if (
            parsedUserInfo &&
            typeof parsedUserInfo === "object" &&
            "username" in parsedUserInfo &&
            "role" in parsedUserInfo
          ) {
            this.userInfo = parsedUserInfo;
          } else {
            console.warn("保存的用户信息格式不正确，已重置");
            localStorage.removeItem(USER_KEY);
            this.userInfo = null;
          }
        } catch (e) {
          console.error("解析用户信息失败:", e);
          localStorage.removeItem(USER_KEY);
          this.userInfo = null;
        }
      }
    },

    logout() {
      this.token = "";
      this.userInfo = null;
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      localStorage.removeItem(USERNAME_KEY);
      localStorage.removeItem(USERID_KEY);
      localStorage.removeItem("token");
      localStorage.removeItem("userInfo");
    },
  },
});
