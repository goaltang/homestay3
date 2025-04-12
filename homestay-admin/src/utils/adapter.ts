/**
 * 数据适配器 - 处理前后端数据格式转换
 */
import { HOMESTAY_STATUS, ORDER_STATUS, USER_STATUS } from "./constants";

// 分页参数转换 - 前端从1开始，后端从0开始
export function adaptPageParams(params: any) {
  const adaptedParams = { ...params };

  // 调整页码
  if ("page" in adaptedParams) {
    adaptedParams.page = (adaptedParams.page || 1) - 1;
  }

  // 重命名pageSize为size
  if ("pageSize" in adaptedParams) {
    adaptedParams.size = adaptedParams.pageSize;
    delete adaptedParams.pageSize;
  }

  return adaptedParams;
}

// 分页响应转换 - 后端返回{content,totalElements} 转为前端需要的 {list,total}
export function adaptPageResponse<T>(
  response: any,
  itemAdapter?: (item: any) => T
) {
  if (!response) return { list: [], total: 0 };

  let list = response.content || [];

  // 如果提供了项目适配器，则应用到每个项目
  if (itemAdapter && Array.isArray(list)) {
    list = list.map(itemAdapter);
  }

  return {
    list,
    total: response.totalElements || 0,
  };
}

// 状态值转换 - 用于房源状态
export function adaptHomestayStatus(
  status: string | undefined,
  toFrontend = true
) {
  if (toFrontend) {
    // 后端 -> 前端
    switch (status) {
      case HOMESTAY_STATUS.BACKEND.ACTIVE:
        return HOMESTAY_STATUS.FRONTEND.ACTIVE;
      case HOMESTAY_STATUS.BACKEND.INACTIVE:
        return HOMESTAY_STATUS.FRONTEND.INACTIVE;
      default:
        return status || "";
    }
  } else {
    // 前端 -> 后端
    switch (status) {
      case HOMESTAY_STATUS.FRONTEND.ACTIVE:
        return HOMESTAY_STATUS.BACKEND.ACTIVE;
      case HOMESTAY_STATUS.FRONTEND.INACTIVE:
        return HOMESTAY_STATUS.BACKEND.INACTIVE;
      default:
        return status || "";
    }
  }
}

// 订单状态转换
export function adaptOrderStatus(
  status: string | undefined,
  toFrontend = true
) {
  if (toFrontend) {
    // 后端 -> 前端
    switch (status) {
      case ORDER_STATUS.BACKEND.PENDING:
        return ORDER_STATUS.FRONTEND.PENDING;
      case ORDER_STATUS.BACKEND.PAID:
        return ORDER_STATUS.FRONTEND.PAID;
      case ORDER_STATUS.BACKEND.CANCELLED:
        return ORDER_STATUS.FRONTEND.CANCELLED;
      case ORDER_STATUS.BACKEND.COMPLETED:
        return ORDER_STATUS.FRONTEND.COMPLETED;
      default:
        return ORDER_STATUS.FRONTEND.PENDING;
    }
  } else {
    // 前端 -> 后端
    switch (status) {
      case ORDER_STATUS.FRONTEND.PENDING:
        return ORDER_STATUS.BACKEND.PENDING;
      case ORDER_STATUS.FRONTEND.PAID:
        return ORDER_STATUS.BACKEND.PAID;
      case ORDER_STATUS.FRONTEND.CANCELLED:
        return ORDER_STATUS.BACKEND.CANCELLED;
      case ORDER_STATUS.FRONTEND.COMPLETED:
        return ORDER_STATUS.BACKEND.COMPLETED;
      default:
        return status || "";
    }
  }
}

// 用户状态转换
export function adaptUserStatus(
  status: string | boolean | undefined,
  toFrontend = true
) {
  if (toFrontend) {
    // 后端 -> 前端
    return status === true || status === "true" || status === "ACTIVE"
      ? USER_STATUS.FRONTEND.ACTIVE
      : USER_STATUS.FRONTEND.INACTIVE;
  } else {
    // 前端 -> 后端
    return (
      status === USER_STATUS.FRONTEND.ACTIVE ||
      status === true ||
      status === "true"
    );
  }
}

// 房源数据适配器
export function adaptHomestayItem(item: any) {
  if (!item) return null;

  return {
    id: item.id,
    name: item.title || "",
    price: item.price,
    address:
      item.province && item.city
        ? `${item.province}${item.city}${item.district || ""}`
        : item.address || "",
    status: adaptHomestayStatus(item.status, true),
    createTime: item.createdAt || "",
  };
}

// 订单数据适配器
export function adaptOrderItem(item: any) {
  if (!item) return null;

  return {
    id: item.id,
    orderNo: item.orderNumber || "",
    homestayName: item.homestay?.title || "",
    userName: item.guest?.username || "",
    amount: item.totalAmount || 0,
    status: adaptOrderStatus(item.status, true),
    createTime: item.createdAt || "",
  };
}

// 用户数据适配器
export function adaptUserItem(item: any) {
  if (!item) return null;

  return {
    id: item.id,
    username: item.username || "",
    nickname: item.nickname || "",
    phone: item.phoneNumber || "",
    email: item.email || "",
    status: adaptUserStatus(item.enabled, true),
    createTime: item.createdAt || "",
  };
}
