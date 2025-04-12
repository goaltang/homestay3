import { createRouter, createWebHistory } from "vue-router";
import Home from "../views/Home.vue";
import { useAuthStore } from "../stores/auth";
import { useUserStore } from "../stores/user";
import OrderSubmitSuccess from "../views/order/OrderSubmitSuccess.vue";
import MyOrders from "../views/order/MyOrders.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      name: "home",
      component: Home,
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
      path: "/homestays",
      name: "homestays",
      component: () => import("../views/HomestayListView.vue"),
    },
    {
      path: "/homestays/:id",
      name: "homestay-detail",
      component: () => import("../views/HomestayDetail.vue"),
    },
    {
      path: "/profile",
      name: "profile",
      component: () => import("../views/user/Profile.vue"),
      meta: { requiresAuth: true },
    },

    {
      path: "/reset-password",
      name: "reset-password",
      component: () => import("../views/ResetPassword.vue"),
    },
    {
      path: "/forgot-password",
      name: "forgot-password",
      component: () => import("../views/ForgotPassword.vue"),
    },

    // // 房源管理路由
    // {
    //   path: "/homestay",
    //   component: () => import("@/layouts/DefaultLayout.vue"),
    //   children: [
    //     {
    //       path: "list",
    //       name: "HomestayList",
    //       component: () => import("@/views/homestay/HomestayList.vue"),
    //       meta: {
    //         title: "房源管理",
    //         requiresAuth: true,
    //       },
    //     },
    //     {
    //       path: "create",
    //       name: "HomestayCreate",
    //       component: () => import("@/views/homestay/HomestayForm.vue"),
    //       meta: {
    //         title: "创建房源",
    //         requiresAuth: true,
    //       },
    //     },
    //     {
    //       path: "edit/:id",
    //       name: "HomestayEdit",
    //       component: () => import("@/views/homestay/HomestayForm.vue"),
    //       meta: {
    //         title: "编辑房源",
    //         requiresAuth: true,
    //       },
    //     },
    //   ],
    // },

    // 房东中心路由
    {
      path: "/host",
      component: () => import("../views/host/HostLayout.vue"),
      meta: {
        requiresAuth: true,
        title: "房东中心",
        roles: ["ROLE_LANDLORD"],
      },
      children: [
        {
          path: "",
          name: "HostDashboard",
          component: () => import("../views/host/Dashboard.vue"),
          meta: {
            title: "房东控制台",
            icon: "dashboard",
          },
        },
        {
          path: "homestay",
          name: "HostHomestay",
          component: () => import("../views/host/HomestayManage.vue"),
          meta: {
            title: "房源管理",
            icon: "home",
          },
        },
        {
          path: "homestay/create",
          name: "HostHomestayCreate",
          component: () => import("../views/host/HomestayForm.vue"),
          meta: {
            title: "添加新房源",
            icon: "plus",
            activeMenu: "/host/homestay",
          },
        },
        {
          path: "homestay/edit/:id",
          name: "HostHomestayEdit",
          component: () => import("../views/host/HomestayForm.vue"),
          meta: {
            title: "编辑房源",
            activeMenu: "/host/homestay",
          },
        },
        {
          path: "orders",
          name: "HostOrders",
          component: () => import("../views/host/OrderManage.vue"),
          meta: {
            title: "订单管理",
            icon: "list",
          },
        },
        {
          path: "earnings",
          name: "HostEarnings",
          component: () => import("../views/host/EarningManage.vue"),
          meta: {
            title: "收益管理",
            icon: "money",
          },
        },
        {
          path: "withdrawal",
          name: "HostWithdrawal",
          component: () => import("../views/host/Withdrawal.vue"),
          meta: {
            title: "提现管理",
            icon: "wallet",
            activeMenu: "/host/earnings",
          },
        },
        {
          path: "reviews",
          name: "HostReviews",
          component: () => import("../views/host/ReviewManage.vue"),
          meta: {
            title: "评价管理",
            icon: "star",
          },
        },
        {
          path: "profile",
          name: "HostProfile",
          component: () => import("../views/host/ProfileManage.vue"),
          meta: {
            title: "个人资料",
            icon: "user",
          },
        },
      ],
    },
    {
      path: "/homestay/:id",
      name: "HomestayDetail",
      component: () => import("../views/HomestayDetail.vue"),
      meta: { title: "房源详情" },
    },
    {
      path: "/order/confirm",
      name: "OrderConfirm",
      component: () => import("../views/order/OrderConfirm.vue"),
      meta: { title: "确认订单", requiresAuth: true },
    },
    {
      path: "/orders",
      name: "MyOrders",
      component: MyOrders,
      meta: { requiresAuth: true },
    },
    {
      path: "/orders/:id",
      name: "OrderDetail",
      component: () => import("../views/order/OrderDetail.vue"),
      meta: { title: "订单详情", requiresAuth: true },
    },
    {
      path: "/orders/:id/pay",
      name: "OrderPay",
      component: () => import("../views/order/OrderPay.vue"),
      meta: { title: "订单支付", requiresAuth: true },
    },
    {
      path: "/orders/submit-success/:id",
      name: "OrderSubmitSuccess",
      component: OrderSubmitSuccess,
      meta: { requiresAuth: true },
    },

    // 用户中心路由
    {
      path: "/user",
      component: () => import("../layouts/DefaultLayout.vue"),
      meta: { requiresAuth: true },
      children: [
        {
          path: "profile",
          name: "UserProfile",
          component: () => import("../views/user/Profile.vue"),
          meta: { title: "个人中心" },
        },
        {
          path: "favorites",
          name: "UserFavorites",
          component: () => import("../views/user/Favorites.vue"),
          meta: { title: "我的收藏" },
        },
        {
          path: "bookings",
          name: "UserBookings",
          component: () => import("../views/user/Bookings.vue"),
          meta: { title: "我的预订" },
        },
      ],
    },
  ],
});

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();
  const userStore = useUserStore();

  // 获取 token 和用户信息
  const storedToken = localStorage.getItem("token");
  const userInfo = JSON.parse(localStorage.getItem("userInfo") || "null");
  const userFromAuth = JSON.parse(localStorage.getItem("user") || "null");

  // 确保 authStore 和 userStore 的状态与 localStorage 同步
  if (
    storedToken &&
    (!authStore.isAuthenticated || !userStore.isAuthenticated)
  ) {
    console.log("检测到 token 存在但状态未同步，正在同步...");
    if (!authStore.isAuthenticated && storedToken) {
      authStore.$patch({
        token: storedToken,
        user: userFromAuth || userInfo,
      });
    }

    // 直接使用localStorage更新userStore状态
    if (userInfo) {
      console.log("从localStorage同步用户信息到userStore");
      // 注意：这里没有调用userStore的方法，而是更新状态
    }
  }

  // 重新获取计算状态
  const isAuthenticated = !!storedToken;
  const userRole = userInfo?.role || userFromAuth?.role || "";

  console.log("路由导航:", { from: from.path, to: to.path, meta: to.meta });
  console.log("用户认证状态:", {
    tokenExists: !!storedToken,
    isAuthenticated,
    userRole,
    storedUserInfo: !!userInfo,
    storedAuthUser: !!userFromAuth,
  });

  // 根据实际 token 和用户信息判断权限，而不是只依赖 store
  if (to.meta.requiresAuth && !isAuthenticated) {
    console.warn("需要登录权限，重定向到登录页");
    next({ name: "login", query: { redirect: to.fullPath } });
  } else if (
    to.meta.roles &&
    Array.isArray(to.meta.roles) &&
    userRole && // 确保有角色信息
    !to.meta.roles.some(
      (role) =>
        userRole.toUpperCase() === role.toUpperCase() ||
        userRole.toUpperCase() === role.replace("ROLE_", "").toUpperCase() ||
        `ROLE_${userRole.toUpperCase()}` === role.toUpperCase()
    )
  ) {
    console.warn("用户角色不符合要求:", {
      userRole: userRole,
      requiredRoles: to.meta.roles,
    });
    next({ name: "home" });
  } else {
    console.log("导航允许通过");
    next();
  }
});

export default router;
