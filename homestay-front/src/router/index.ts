import { createRouter, createWebHistory } from "vue-router";
import { useUserStore } from "@/stores/user";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      name: "home",
      component: () => import("../views/Home.vue"),
    },
    {
      path: "/homestay/:id",
      name: "homestayDetail",
      component: () => import("../views/HomestayDetail.vue"),
    },
    {
      path: "/login",
      name: "login",
      component: () => import("../views/Login.vue"),
    },
    {
      path: "/register",
      name: "register",
      component: () => import("../views/Register.vue"),
    },
    {
      path: "/forgot-password",
      name: "forgotPassword",
      component: () => import("../views/ForgotPassword.vue"),
    },
    {
      path: "/reset-password",
      name: "reset-password",
      component: () => import("@/views/ResetPassword.vue"),
    },
    {
      path: "/user/profile",
      name: "userProfile",
      component: () => import("../views/user/Profile.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/user/favorites",
      name: "userFavorites",
      component: () => import("../views/user/Favorites.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/user/bookings",
      name: "userBookings",
      component: () => import("../views/user/Bookings.vue"),
      meta: { requiresAuth: true },
    },
  ],
});

// 全局前置守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore();

  // 检查路由是否需要认证
  if (to.matched.some((record) => record.meta.requiresAuth)) {
    // 如果需要认证但用户未登录，重定向到登录页
    if (!userStore.isLoggedIn) {
      next({
        path: "/login",
        query: { redirect: to.fullPath },
      });
    } else {
      next();
    }
  } else {
    next();
  }
});

export default router;
