import { codeToText } from "element-china-area-data";
import type { HomestayDetail } from "@/types/homestay";

// 日期格式化工具
export const formatDateString = (date: Date | string | null): string => {
  if (!date) return "";
  try {
    const d = typeof date === "string" ? new Date(date) : date;
    if (!(d instanceof Date) || isNaN(d.getTime())) {
      console.error("Invalid date object for formatting:", date);
      return "";
    }
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  } catch (e) {
    console.error("Error formatting date:", date, e);
    return "";
  }
};

export const formatDisplayDate = (date: Date | string | null): string => {
  if (!date) return "日期无效";
  try {
    const d = typeof date === "string" ? new Date(date) : date;
    if (!(d instanceof Date) || isNaN(d.getTime())) return "日期无效";
    const year = d.getFullYear();
    const month = d.getMonth() + 1;
    const day = d.getDate();
    return `${year}年${month}月${day}日`;
  } catch (e) {
    return "日期格式错误";
  }
};

// 位置格式化工具
export const formatLocation = (homestay: HomestayDetail | null): string => {
  if (!homestay) return "位置待更新";

  const parts = [];

  if (homestay.provinceCode && codeToText[homestay.provinceCode]) {
    parts.push(codeToText[homestay.provinceCode]);
  }
  if (homestay.cityCode && codeToText[homestay.cityCode]) {
    if (!parts.includes(codeToText[homestay.cityCode])) {
      parts.push(codeToText[homestay.cityCode]);
    }
  }
  if (homestay.districtCode && codeToText[homestay.districtCode]) {
    parts.push(codeToText[homestay.districtCode]);
  }

  if (parts.length > 0) {
    return parts.join(" · ");
  }

  return homestay.addressDetail || "位置待更新";
};

// 价格计算工具
export const parsePrice = (homestay: HomestayDetail | null): number => {
  if (homestay) {
    let priceValue: number | undefined;

    if (typeof homestay.price !== "undefined" && homestay.price !== null) {
      priceValue = Number(homestay.price);
    } else if (
      typeof (homestay as any).pricePerNight !== "undefined" &&
      (homestay as any).pricePerNight !== null
    ) {
      priceValue = Number((homestay as any).pricePerNight);
    }

    if (priceValue !== undefined && !isNaN(priceValue)) {
      return priceValue;
    }
  }
  return 0;
};

export const calculateNights = (
  checkIn: Date | null,
  checkOut: Date | null
): number => {
  if (checkIn && checkOut) {
    const checkInTime = checkIn.getTime();
    const checkOutTime = checkOut.getTime();
    if (checkOutTime > checkInTime) {
      const diffTime = checkOutTime - checkInTime;
      return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    }
  }
  return 0;
};

export const calculateFees = (pricePerNight: number, nights: number) => {
  const basePrice = pricePerNight * nights;
  const cleaningFee =
    nights > 0 && pricePerNight > 0 ? Math.round(pricePerNight * 0.1) : 0;
  const serviceFee = basePrice > 0 ? Math.round(basePrice * 0.05) : 0;
  const totalPrice = basePrice + cleaningFee + serviceFee;

  return {
    basePrice,
    cleaningFee,
    serviceFee,
    totalPrice,
  };
};

// 图片处理工具
export const processImages = (homestay: HomestayDetail | null): string[] => {
  if (!homestay) return [];

  const rawImageUrls =
    homestay.images && Array.isArray(homestay.images)
      ? (homestay.images
          .map((img: string | { url: string }) =>
            typeof img === "string" ? img : img?.url
          )
          .filter(Boolean) as string[])
      : [];

  const coverImageUrl = homestay.coverImage;
  let finalImages: string[] = [];

  if (coverImageUrl) {
    finalImages.push(coverImageUrl);
    rawImageUrls.forEach((imgUrl) => {
      if (imgUrl !== coverImageUrl) {
        finalImages.push(imgUrl);
      }
    });
  } else {
    finalImages = rawImageUrls;
  }

  return finalImages;
};

// 房源类型格式化
export const formatPropertyType = (type?: string): string => {
  const typeMap: Record<string, string> = {
    TRADITIONAL: "传统民居",
    APARTMENT: "公寓",
    UNIQUE_STAY: "特色房源",
    BOUTIQUE_HOTEL: "精品酒店",
    VILLA: "别墅",
    CABIN: "小木屋",
  };

  if (type && typeMap[type]) {
    return typeMap[type];
  }
  return type || "特色住宿";
};
