// 高德地图服务工具类
// 需要先申请高德地图API Key: https://console.amap.com/

// 导入地区数据转换工具
import { codeToText } from "element-china-area-data";

// 高德地图API配置
const AMAP_CONFIG = {
  // 请替换为你申请的API Key
  apiKey: "13725cc6ef2c302a407b3a2d12247ac5", // 🔑 请在这里填入你申请到的API Key
  version: "2.0",
  plugins: ["AMap.Geocoder", "AMap.PlaceSearch"],
};

// 如果没有API Key，使用模拟模式
const USE_MOCK_DATA = false; // 强制使用真实API，已有有效的API Key

// 地理编码接口
interface GeocodeResult {
  lat: number;
  lng: number;
  address: string;
  formattedAddress: string;
}

// 周边设施类型
interface NearbyPlace {
  name: string;
  type: string;
  distance: number;
  address: string;
}

/**
 * 根据省市区代码和详细地址获取经纬度
 */
export const geocodeAddress = async (
  provinceCode: string,
  cityCode: string,
  districtCode: string,
  addressDetail?: string
): Promise<GeocodeResult | null> => {
  // 模拟模式：返回一些预设坐标
  if (USE_MOCK_DATA) {
    console.log("使用模拟地理编码数据");

    // 根据城市代码返回大概的坐标
    const mockLocations: Record<
      string,
      { lat: number; lng: number; name: string }
    > = {
      "1101": { lat: 39.9042, lng: 116.4074, name: "北京市" },
      "3101": { lat: 31.2304, lng: 121.4737, name: "上海市" },
      "4403": { lat: 22.5431, lng: 114.0579, name: "深圳市" },
      "4401": { lat: 23.1291, lng: 113.2644, name: "广州市" },
      "4602": { lat: 20.0444, lng: 110.1989, name: "三亚市" },
      "5101": { lat: 30.5728, lng: 104.0668, name: "成都市" },
      "3301": { lat: 30.2741, lng: 120.1551, name: "杭州市" },
    };

    const mockLocation = mockLocations[cityCode] || mockLocations["1101"]; // 默认北京

    return {
      lat: mockLocation.lat + (Math.random() - 0.5) * 0.1, // 添加随机偏移
      lng: mockLocation.lng + (Math.random() - 0.5) * 0.1,
      address: `${mockLocation.name}${addressDetail || "某区域"}`,
      formattedAddress: `${mockLocation.name}${addressDetail || "某区域"}`,
    };
  }

  try {
    // 构建完整地址字符串
    const addressParts: string[] = [];

    console.log("=== 开始构建地址 ===");
    console.log("provinceCode:", provinceCode);
    console.log("cityCode:", cityCode);
    console.log("districtCode:", districtCode);
    console.log("addressDetail:", addressDetail);

    // 使用 codeToText 将代码转换为中文地名
    if (provinceCode && codeToText[provinceCode]) {
      addressParts.push(codeToText[provinceCode]);
      console.log("省份:", codeToText[provinceCode]);
    }

    if (cityCode && codeToText[cityCode]) {
      // 避免直辖市重复（如北京市、上海市）
      const cityName = codeToText[cityCode];
      if (!addressParts.includes(cityName)) {
        addressParts.push(cityName);
        console.log("城市:", cityName);
      }
    }

    if (districtCode && codeToText[districtCode]) {
      addressParts.push(codeToText[districtCode]);
      console.log("区县:", codeToText[districtCode]);
    }

    // 添加详细地址
    if (addressDetail && addressDetail.trim()) {
      addressParts.push(addressDetail.trim());
      console.log("详细地址:", addressDetail.trim());
    }

    const fullAddress = addressParts.join("");
    console.log("完整地址:", fullAddress);

    if (!fullAddress) {
      console.warn("地址为空，无法进行地理编码");
      return null;
    }

    // 使用高德地图Web API
    const apiUrl = `https://restapi.amap.com/v3/geocode/geo?address=${encodeURIComponent(
      fullAddress
    )}&key=${AMAP_CONFIG.apiKey}&output=json`;
    console.log("API请求URL:", apiUrl);

    const response = await fetch(apiUrl);
    const data = await response.json();

    console.log("高德地图API响应:", data);

    if (data.status === "1" && data.geocodes && data.geocodes.length > 0) {
      const geocode = data.geocodes[0];
      const location = geocode.location.split(",");

      const result = {
        lat: parseFloat(location[1]),
        lng: parseFloat(location[0]),
        address: geocode.formatted_address,
        formattedAddress: geocode.formatted_address,
      };

      console.log("地理编码成功:", result);
      return result;
    } else {
      console.warn("地理编码API未返回有效结果:", data);

      // 如果精确地址找不到，尝试只用省市查询
      if (addressParts.length > 1) {
        const cityOnlyAddress = addressParts.slice(0, 2).join("");
        console.log("尝试使用省市地址:", cityOnlyAddress);

        const fallbackUrl = `https://restapi.amap.com/v3/geocode/geo?address=${encodeURIComponent(
          cityOnlyAddress
        )}&key=${AMAP_CONFIG.apiKey}&output=json`;
        const fallbackResponse = await fetch(fallbackUrl);
        const fallbackData = await fallbackResponse.json();

        if (
          fallbackData.status === "1" &&
          fallbackData.geocodes &&
          fallbackData.geocodes.length > 0
        ) {
          const geocode = fallbackData.geocodes[0];
          const location = geocode.location.split(",");

          const result = {
            lat: parseFloat(location[1]),
            lng: parseFloat(location[0]),
            address: fullAddress, // 保持原始完整地址
            formattedAddress: fullAddress,
          };

          console.log("省市级地理编码成功:", result);
          return result;
        }
      }

      return null;
    }
  } catch (error) {
    console.error("地理编码失败:", error);
    return null;
  }
};

/**
 * 生成静态地图图片URL
 */
export const generateStaticMapUrl = (
  lat: number,
  lng: number,
  width: number = 800,
  height: number = 400,
  zoom: number = 15
): string => {
  // 模拟模式：返回占位图
  if (USE_MOCK_DATA) {
    console.log("使用模拟静态地图");
    return `https://picsum.photos/${width}/${height}?random=map-${Math.floor(
      Math.random() * 1000
    )}`;
  }

  // 高德地图静态图API
  // 文档: https://lbs.amap.com/api/webservice/guide/api/staticmaps

  const markers = `${lng},${lat}`; // 经度,纬度

  const url =
    `https://restapi.amap.com/v3/staticmap?` +
    `location=${lng},${lat}&` +
    `zoom=${zoom}&` +
    `size=${width}*${height}&` +
    `markers=mid,0xFF0000,A:${markers}&` +
    `key=${AMAP_CONFIG.apiKey}`;

  console.log("生成的静态地图URL:", url);
  return url;
};

/**
 * 为保护隐私，给坐标添加随机偏移
 */
export const addPrivacyOffset = (
  lat: number,
  lng: number
): { lat: number; lng: number } => {
  // 添加小范围随机偏移（约100-500米）
  const offsetRange = 0.005; // 大约500米的偏移范围
  const latOffset = (Math.random() - 0.5) * offsetRange;
  const lngOffset = (Math.random() - 0.5) * offsetRange;

  return {
    lat: lat + latOffset,
    lng: lng + lngOffset,
  };
};

/**
 * 搜索周边设施
 */
export const searchNearbyPlaces = async (
  lat: number,
  lng: number,
  types: string[] = ["地铁站", "商场", "医院", "学校"]
): Promise<NearbyPlace[]> => {
  // 模拟模式：返回模拟数据
  if (USE_MOCK_DATA) {
    console.log("使用模拟周边设施数据");

    const mockPlaces: NearbyPlace[] = [
      {
        name: "地铁1号线某某站",
        type: "地铁站",
        distance: 450,
        address: "步行约5分钟",
      },
      {
        name: "某某购物中心",
        type: "商场",
        distance: 750,
        address: "步行约8分钟",
      },
      {
        name: "某某三甲医院",
        type: "医院",
        distance: 1200,
        address: "步行约12分钟",
      },
      { name: "某某小学", type: "学校", distance: 600, address: "步行约6分钟" },
    ];

    // 根据传入的types过滤
    return mockPlaces.filter((place) => types.includes(place.type)).slice(0, 3);
  }

  try {
    const places: NearbyPlace[] = [];

    for (const type of types) {
      const response = await fetch(
        `https://restapi.amap.com/v3/place/around?` +
          `key=${AMAP_CONFIG.apiKey}&` +
          `location=${lng},${lat}&` +
          `keywords=${encodeURIComponent(type)}&` +
          `radius=2000&` +
          `types=&` +
          `sortrule=distance&` +
          `output=JSON`
      );

      const data = await response.json();

      if (data.status === "1" && data.pois && data.pois.length > 0) {
        // 取最近的一个
        const poi = data.pois[0];
        places.push({
          name: poi.name,
          type: type,
          distance: parseInt(poi.distance),
          address: poi.address,
        });
      }
    }

    return places;
  } catch (error) {
    console.error("搜索周边设施失败:", error);
    // 返回备用模拟数据
    return [
      {
        name: "附近地铁站",
        type: "地铁站",
        distance: 500,
        address: "步行约5分钟",
      },
      { name: "附近商场", type: "商场", distance: 800, address: "步行约8分钟" },
      {
        name: "附近医院",
        type: "医院",
        distance: 1200,
        address: "步行约12分钟",
      },
    ];
  }
};

/**
 * 计算距离（米）
 */
export const calculateDistance = (
  lat1: number,
  lng1: number,
  lat2: number,
  lng2: number
): number => {
  const R = 6371000; // 地球半径（米）
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
};

/**
 * 格式化距离显示
 */
export const formatDistance = (distance: number): string => {
  if (distance < 1000) {
    return `${Math.round(distance)}米`;
  } else {
    return `${(distance / 1000).toFixed(1)}公里`;
  }
};

// 预设的周边设施类型
export const FACILITY_TYPES = {
  TRANSPORT: { key: "地铁站|公交站", name: "交通" },
  SHOPPING: { key: "商场|超市", name: "购物" },
  MEDICAL: { key: "医院|药店", name: "医疗" },
  EDUCATION: { key: "学校|幼儿园", name: "教育" },
  FOOD: { key: "餐厅|美食", name: "餐饮" },
  ENTERTAINMENT: { key: "电影院|KTV", name: "娱乐" },
};
