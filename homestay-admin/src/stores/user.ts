import { defineStore } from "pinia";

interface UserState {
  token: string;
  userInfo: {
    username: string;
    role: string;
  } | null;
}

export const useUserStore = defineStore("user", {
  state: (): UserState => ({
    token: localStorage.getItem("token") || "",
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
      localStorage.setItem("token", token);
    },

    setUserInfo(userInfo: UserState["userInfo"]) {
      this.userInfo = userInfo;
      if (userInfo) {
        localStorage.setItem("userInfo", JSON.stringify(userInfo));
      } else {
        localStorage.removeItem("userInfo");
      }
    },

    loadUserInfo() {
      const userInfoStr = localStorage.getItem("userInfo");
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
            localStorage.removeItem("userInfo");
            this.userInfo = null;
          }
        } catch (e) {
          console.error("解析用户信息失败:", e);
          localStorage.removeItem("userInfo");
          this.userInfo = null;
        }
      }
    },

    logout() {
      this.token = "";
      this.userInfo = null;
      localStorage.removeItem("token");
      localStorage.removeItem("userInfo");
    },
  },
});
