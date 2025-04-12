import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";

const routes: RouteRecordRaw[] = [
  {
    path: "/",
    redirect: "/dashboard",
  },
  {
    path: "/",
    name: "Home",
    component: () =>
      import("@/views/layout/index.vue").catch((error) => {
        console.error("加载布局组件失败:", error);
        return import("@/views/error/404.vue"); // 确保创建这个错误页面
      }),
    children: [
      {
        path: "/dashboard",
        name: "dashboard",
        meta: {
          title: "系统首页",
          requiresAuth: true,
        },
        component: () =>
          import("@/views/dashboard/index.vue").catch((error) => {
            console.error("加载dashboard组件失败:", error);
            return import("@/views/error/404.vue");
          }),
      },
      {
        path: "/homestays",
        name: "homestays",
        meta: {
          title: "房源管理",
          requiresAuth: true,
        },
        component: () => import("@/views/homestay/list.vue"),
      },
      {
        path: "/orders",
        name: "orders",
        meta: {
          title: "订单管理",
          requiresAuth: true,
        },
        component: () => import("@/views/order/list.vue"),
      },
      {
        path: "/users",
        name: "users",
        meta: {
          title: "用户管理",
          requiresAuth: true,
        },
        component: () => import("@/views/user/list.vue"),
      },
      {
        path: "/reviews",
        name: "reviews",
        meta: {
          title: "评价管理",
          requiresAuth: true,
        },
        component: () => import("@/views/review/list.vue"),
      },
      {
        path: "/statistics",
        name: "statistics",
        meta: {
          title: "统计分析",
          requiresAuth: true,
        },
        component: () => import("@/views/statistics/index.vue"),
      },
    ],
  },
  {
    path: "/login",
    name: "Login",
    meta: {
      title: "登录",
    },
    component: () =>
      import("@/views/login/index.vue").catch((error) => {
        console.error("加载登录组件失败:", error);
        return import("@/views/error/404.vue");
      }),
  },
  {
    path: "/:pathMatch(.*)*",
    name: "NotFound",
    meta: {
      title: "页面不存在",
    },
    component: () =>
      import("@/views/error/404.vue").catch(() => {
        // 创建一个简单的组件作为后备
        return {
          template: `<div style="text-align:center;padding:50px;">
                    <h1>404</h1>
                    <p>页面不存在</p>
                    <a href="/">返回首页</a>
                   </div>`,
        };
      }),
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {
  try {
    const title = to.meta?.title as string | undefined;
    if (title) {
      document.title = title;
    }

    const token = localStorage.getItem("token");
    if (to.meta?.requiresAuth && !token) {
      next("/login");
    } else {
      next();
    }
  } catch (error) {
    console.error("路由导航错误:", error);
    next("/login"); // 出错时导航到登录页
  }
});

export default router;
