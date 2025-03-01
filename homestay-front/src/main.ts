import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import zhCn from "element-plus/es/locale/lang/zh-cn";
import * as ElementPlusIconsVue from "@element-plus/icons-vue";
import App from "./App.vue";
import router from "./router";
import "./assets/main.css";
import { useUserStore } from "./stores/user";

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);
app.use(ElementPlus, {
  locale: zhCn,
});

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

// 初始化用户信息
const initializeApp = async () => {
  const userStore = useUserStore();

  // 如果有token，尝试获取用户信息
  if (userStore.isLoggedIn) {
    try {
      console.log("应用启动时获取用户信息");
      await userStore.fetchUserInfo();
      console.log("用户信息获取成功:", userStore.user);
    } catch (error: any) {
      console.error("获取用户信息失败，可能需要重新登录:", error);
      // 如果获取用户信息失败，清除token
      if (error.response && error.response.status === 401) {
        userStore.logout();
        router.push("/login");
      }
    }
  }

  // 挂载应用
  app.mount("#app");
};

// 启动应用
initializeApp();
