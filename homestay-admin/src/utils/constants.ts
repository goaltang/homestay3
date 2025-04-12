/**
 * 系统常量定义
 */

// 房源状态
export const HOMESTAY_STATUS = {
  // 前端状态码
  FRONTEND: {
    ACTIVE: "1",
    INACTIVE: "0",
  },
  // 后端状态码
  BACKEND: {
    ACTIVE: "ACTIVE",
    INACTIVE: "INACTIVE",
  },
  // 显示文本
  TEXT: {
    "1": "上架",
    "0": "下架",
  },
  // 显示类型
  TYPE: {
    "1": "success",
    "0": "info",
  },
};

// 订单状态
export const ORDER_STATUS = {
  // 前端状态码
  FRONTEND: {
    PENDING: "0",
    PAID: "1",
    CANCELLED: "2",
    COMPLETED: "3",
  },
  // 后端状态码
  BACKEND: {
    PENDING: "PENDING",
    PAID: "PAID",
    CANCELLED: "CANCELLED",
    COMPLETED: "COMPLETED",
  },
  // 显示文本
  TEXT: {
    "0": "待支付",
    "1": "已支付",
    "2": "已取消",
    "3": "已完成",
  },
  // 显示类型
  TYPE: {
    "0": "warning",
    "1": "success",
    "2": "info",
    "3": "success",
  },
};

// 用户状态
export const USER_STATUS = {
  // 前端状态码
  FRONTEND: {
    ACTIVE: "1",
    INACTIVE: "0",
  },
  // 显示文本
  TEXT: {
    "1": "启用",
    "0": "禁用",
  },
  // 显示类型
  TYPE: {
    "1": "success",
    "0": "info",
  },
};

// 分页默认参数
export const DEFAULT_PAGE_PARAMS = {
  page: 1,
  pageSize: 10,
};

// 表格操作类型
export const TABLE_ACTIONS = {
  EDIT: "edit",
  DELETE: "delete",
  VIEW: "view",
  STATUS: "status",
  RESET_PASSWORD: "resetPassword",
};

// 角色类型
export const ROLE_TYPES = {
  ADMIN: "ROLE_ADMIN",
  HOST: "ROLE_HOST",
  USER: "ROLE_USER",
};

// 表单操作模式
export const FORM_MODES = {
  CREATE: "create",
  EDIT: "edit",
  VIEW: "view",
};

// 响应状态码
export const RESPONSE_CODES = {
  SUCCESS: 200,
  CREATED: 201,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  VALIDATION_ERROR: 422,
  SERVER_ERROR: 500,
};

// 接口超时时间(毫秒)
export const API_TIMEOUT = 10000;

// 登录过期自动重定向
export const AUTO_REDIRECT_ON_EXPIRED = true;

// 导出默认文件名
export const DEFAULT_EXPORT_FILENAME = {
  ORDERS: "订单数据",
  USERS: "用户数据",
  HOMESTAYS: "房源数据",
};
