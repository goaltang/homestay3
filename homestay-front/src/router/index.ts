import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import { useUserStore } from "@/stores/user";
import DefaultLayout from "@/layouts/DefaultLayout.vue";
import BlankLayout from "@/layouts/BlankLayout.vue";

const routes: RouteRecordRaw[] = [
  {
    path: "/",
    component: DefaultLayout,
    children: [
      {
        path: "",
        name: "home",
        component: () => import("@/views/Home.vue"),
      },
      {
        path: "profile",
        name: "profile",
        component: () => import("@/views/user/Profile.vue"),
      },
    ],
  },
  {
    path: "/",
    component: BlankLayout,
    children: [
      {
        path: "login",
        name: "login",
        component: () => import("@/views/Login.vue"),
      },
      {
        path: "register",
        name: "register",
        component: () => import("@/views/Register.vue"),
      },
      {
        path: "forgot-password",
        name: "forgot-password",
        component: () => import("@/views/ForgotPassword.vue"),
      },
      {
        path: "reset-password",
        name: "reset-password",
        component: () => import("@/views/ResetPassword.vue"),
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

// 添加路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore();
  const isAuthenticated = userStore.isAuthenticated;
  const publicPages = [
    "/login",
    "/register",
    "/forgot-password",
    "/reset-password",
  ];
  const authRequired = !publicPages.includes(to.path);

  if (authRequired && !isAuthenticated) {
    next("/login");
  }
  // 已登录用户访问登录页，重定向到首页
  else if (
    isAuthenticated &&
    (to.path === "/login" || to.path === "/register")
  ) {
    next("/");
  } else {
    next();
  }
});

export default router;
