import { Menus } from "@/types/menu";

export const menuData: Menus[] = [
  {
    id: "0",
    title: "系统首页",
    index: "/dashboard",
    icon: "Odometer",
  },
  {
    id: "audit",
    title: "审核工作台",
    index: "/audit/workbench",
    icon: "Document",
  },
  {
    id: "notifications",
    title: "通知中心",
    index: "/notifications",
    icon: "Bell",
  },
  {
    id: "homestay",
    title: "房源管理",
    index: "/homestays",
    icon: "House",
    children: [
      {
        id: "homestay-list",
        pid: "homestay",
        title: "房源列表",
        index: "/homestays",
      },
      {
        id: "homestay-types",
        pid: "homestay",
        title: "房源类型管理",
        index: "/types",
      },
      {
        id: "amenities",
        pid: "homestay",
        title: "设施管理",
        index: "/amenities",
        children: [
          {
            id: "amenities-list",
            pid: "amenities",
            title: "设施列表",
            index: "/amenities/manage",
          },
          {
            id: "amenities-categories",
            pid: "amenities",
            title: "设施分类",
            index: "/amenities/categories",
          },
        ],
      },
    ],
  },
  {
    id: "orders",
    title: "订单管理",
    index: "/orders",
    icon: "List",
  },
  {
    id: "users",
    title: "用户管理",
    index: "/users",
    icon: "User",
    children: [
      {
        id: "users-list",
        pid: "users",
        title: "用户列表",
        index: "/users",
      },
      {
        id: "verifications",
        pid: "users",
        title: "身份认证审核",
        index: "/verifications",
      },
    ],
  },
  {
    id: "reviews",
    title: "评价管理",
    index: "/reviews",
    icon: "ChatDotSquare",
  },
  {
    id: "system",
    title: "系统管理",
    index: "/system",
    icon: "Setting",
    children: [
      {
        id: "system-config",
        pid: "system",
        title: "系统配置",
        index: "/system/config",
      },
      {
        id: "system-logs",
        pid: "system",
        title: "操作日志",
        index: "/system/logs",
      },
      {
        id: "login-logs",
        pid: "system",
        title: "登录日志",
        index: "/system/login-logs",
      },
      {
        id: "announcements",
        pid: "system",
        title: "公告管理",
        index: "/system/announcements",
      },
      {
        id: "dispute-manage",
        pid: "system",
        title: "争议管理",
        index: "/system/disputes",
      },
    ],
  },
];
