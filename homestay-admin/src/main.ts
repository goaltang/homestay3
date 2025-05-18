import { createApp } from "vue";
import ElementPlus from "element-plus";
import * as ElementPlusIconsVue from "@element-plus/icons-vue";
import "element-plus/dist/index.css";
import App from "./App.vue";
import router from "./router";
import { createPinia } from "pinia";
import "./style.css";

const app = createApp(App);
const pinia = createPinia();

// 注册权限指令
app.directive("permiss", {
  mounted(el, binding) {
    // 这里简单实现，默认所有菜单项都显示
    // 实际应用中应从用户权限列表中检查
    el.style.display = "block";
  },
});

// 全局错误处理
app.config.errorHandler = (err, instance, info) => {
  console.error("全局错误:", err);
  console.error("错误来源:", instance);
  console.error("错误信息:", info);
};

// 捕获未处理的Promise异常
window.addEventListener("unhandledrejection", (event) => {
  console.error("未处理的Promise拒绝:", event.reason);
});

// 注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

app.use(ElementPlus);
app.use(router);
app.use(pinia);

app.mount("#app");
