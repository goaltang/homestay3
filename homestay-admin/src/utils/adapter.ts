/**
 * 数据适配器 - 处理前后端数据格式转换
 */
import { HOMESTAY_STATUS, ORDER_STATUS, USER_STATUS } from "./constants";
import type { Homestay } from "@/types";

// 分页参数转换 - 前端从1开始，后端也从1开始，后端控制器会自行处理
export function adaptPageParams(params: any) {
  const adaptedParams = { ...params };

  // 确保页码始终为正整数
  if ("page" in adaptedParams) {
    adaptedParams.page = Math.max(1, adaptedParams.page || 1);
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
/*
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
*/

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

// Comment out adaptUserStatus as we'll use boolean directly
/*
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
*/

// 房源数据适配器
export function adaptHomestayItem(item: any): Homestay {
  if (!item) {
    console.warn(
      "adaptHomestayItem received invalid item, returning default object."
    );
    return {
      id: 0,
      title: "无效房源",
      status: "UNKNOWN",
    } as Homestay;
  }

  const originalImages = Array.isArray(item.images)
    ? item.images.filter(
        (img: any) =>
          typeof img === "string" &&
          img.trim() !== "" &&
          img !== item.coverImage
      )
    : [];

  return {
    id: item.id,
    title: item.title || "",
    price: item.price,
    pricePerNight: item.pricePerNight,
    status: item.status,
    createTime: item.createdAt || item.createTime || "",
    type: item.type,
    maxGuests: item.maxGuests,
    bedrooms: item.bedrooms,
    beds: item.beds,
    bathrooms: item.bathrooms,
    amenities: Array.isArray(item.amenities) ? item.amenities : [],
    images: originalImages,
    coverImage: item.coverImage || "",
    description: item.description,
    featured: !!item.featured,
    rating: item.rating,
    reviewCount: item.reviewCount,
    hostId: item.hostId || item.owner?.id,
    hostName: item.hostName || item.owner?.username,
    updatedAt: item.updatedAt || "",
    minNights: item.minNights,
    ownerName: item.ownerName,
    ownerUsername: item.ownerUsername,
    provinceCode: item.provinceCode,
    cityCode: item.cityCode,
    districtCode: item.districtCode,
    addressDetail: item.addressDetail,
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

  // Directly use the boolean `enabled` status from the backend item
  const backendEnabled = item.enabled;
  console.log(`Adapting user ${item.id}, backend enabled: ${backendEnabled}`);

  return {
    id: item.id,
    username: item.username || "",
    nickname: item.nickname || "",
    phone: item.phoneNumber || "",
    email: item.email || "",
    // Use the boolean value directly, potentially rename for clarity
    enabled: backendEnabled,
    createTime: item.createdAt || "",
    // Include other fields if needed
    role: item.role,
    avatar: item.avatar,
  };
}
