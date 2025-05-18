import { Menus } from "@/types/menu";

export const menuData: Menus[] = [
  {
    id: "0",
    title: "系统首页",
    index: "/dashboard",
    icon: "Odometer",
  },
  {
    id: "1",
    title: "房源管理",
    index: "/homestays",
    icon: "House",
  },
  {
    id: "2",
    title: "订单管理",
    index: "/orders",
    icon: "ShoppingCart",
  },
  {
    id: "3",
    title: "用户管理",
    index: "/users",
    icon: "User",
  },
  {
    id: "4",
    title: "身份认证审核",
    index: "/verifications",
    icon: "Document",
  },
  {
    id: "5",
    title: "评价管理",
    index: "/reviews",
    icon: "ChatDotSquare",
  },
  {
    id: "6",
    title: "统计分析",
    index: "/statistics",
    icon: "DataAnalysis",
  },
  {
    id: "7",
    title: "房源类型管理",
    index: "/types",
    icon: "Collection",
  },
  {
    id: "8",
    title: "设施管理",
    index: "/amenities",
    icon: "SetUp",
    children: [
      {
        id: "8-0",
        pid: "8",
        title: "设施列表",
        index: "/amenities/manage",
      },
      {
        id: "8-1",
        pid: "8",
        title: "设施分类",
        index: "/amenities/categories",
      },
    ],
  },
];
