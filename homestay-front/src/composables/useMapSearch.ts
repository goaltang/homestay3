/**
 * 地图找房核心组合式函数
 * 封装：
 * - 高德地图实例的初始化和生命周期管理
 * - 房源数据加载（调用现有搜索 API）
 * - 批量地理编码逻辑（地址 → 坐标，带缓存）
 * - 地图标记（Marker）的创建和管理
 * - 地图视口变化时的搜索触发（防抖）
 * - 卡片与地图互相联动的状态管理
 */

import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import AMapLoader from '@amap/amap-jsapi-loader';
import { codeToText } from 'element-china-area-data';
import { geocodeAddress } from '@/utils/mapService';
import { searchHomestays } from '@/api/homestay/search';

// 高德地图配置
const AMAP_CONFIG = {
  apiKey: import.meta.env.VITE_AMAP_API_KEY || '13725cc6ef2c302a407b3a2d12247ac5',
  version: '2.0',
};

// 默认城市中心坐标（用于失败降级）
const DEFAULT_CITY_CENTERS: Record<string, { lat: number; lng: number }> = {
  '1101': { lat: 39.9042, lng: 116.4074 }, // 北京
  '3101': { lat: 31.2304, lng: 121.4737 }, // 上海
  '4403': { lat: 22.5431, lng: 114.0579 }, // 深圳
  '4401': { lat: 23.1291, lng: 113.2644 }, // 广州
  '4602': { lat: 20.0444, lng: 110.1989 }, // 三亚
  '5101': { lat: 30.5728, lng: 104.0668 }, // 成都
  '3301': { lat: 30.2741, lng: 120.1551 }, // 杭州
  '3201': { lat: 32.0603, lng: 118.7969 }, // 南京
  '5001': { lat: 29.5630, lng: 106.5516 }, // 重庆
  '4406': { lat: 22.5311, lng: 113.1248 }, // 珠海
};

export interface MapHomestay {
  id: number;
  title: string;
  price: number;
  coverImage: string;
  rating?: number;
  provinceCode?: string;
  cityCode?: string;
  districtCode?: string;
  addressDetail?: string;
  maxGuests?: number;
  propertyType?: string;
  autoConfirm?: boolean;
  // 地理编码后的坐标
  lat?: number;
  lng?: number;
  // 标记对象
  marker?: any;
  // 唯一标识（用于缓存）
  geoKey?: string;
}

export interface SearchFilters {
  provinceCode?: string;
  cityCode?: string;
  districtCode?: string;
  minPrice?: number;
  maxPrice?: number;
  maxGuests?: number;
  keyword?: string;
}

export function useMapSearch() {
  // 状态
  const mapInstance = ref<any>(null);
  const markers = ref<any[]>([]);
  const homestays = ref<MapHomestay[]>([]);
  const selectedHomestayId = ref<number | null>(null);
  const hoveredHomestayId = ref<number | null>(null);
  const isLoading = ref(false);
  const isMapReady = ref(false);
  const filters = ref<SearchFilters>({});
  const infoWindow = ref<any>(null);
  const currentCityCode = ref<string>('');

  // 地理编码缓存（sessionStorage）
  const geoCache = new Map<string, { lat: number; lng: number }>();

  // 防抖定时器
  let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null;
  // 节流控制
  let geoThrottleCount = 0;
  const GEO_THROTTLE_LIMIT = 500; // 高德免费API日限，每批次并发控制
  const GEO_THROTTLE_DELAY = 50; // ms

  /**
   * 加载地理编码缓存
   */
  const loadGeoCache = () => {
    try {
      const cached = sessionStorage.getItem('map_geo_cache');
      if (cached) {
        const parsed = JSON.parse(cached);
        Object.entries(parsed).forEach(([key, value]) => {
          geoCache.set(key, value as { lat: number; lng: number });
        });
      }
    } catch (e) {
      console.warn('[MapSearch] 加载地理编码缓存失败:', e);
    }
  };

  /**
   * 保存地理编码缓存
   */
  const saveGeoCache = () => {
    try {
      const obj = Object.fromEntries(geoCache);
      sessionStorage.setItem('map_geo_cache', JSON.stringify(obj));
    } catch (e) {
      console.warn('[MapSearch] 保存地理编码缓存失败:', e);
    }
  };

  /**
   * 获取缓存的坐标
   */
  const getCachedGeo = (key: string): { lat: number; lng: number } | null => {
    return geoCache.get(key) || null;
  };

  /**
   * 设置坐标缓存
   */
  const setCachedGeo = (key: string, geo: { lat: number; lng: number }) => {
    geoCache.set(key, geo);
    // 每10个新缓存就保存一次
    if (geoCache.size % 10 === 0) {
      saveGeoCache();
    }
  };

  /**
   * 生成地理编码的缓存key
   */
  const generateGeoKey = (homestay: MapHomestay): string => {
    return `${homestay.provinceCode || ''}_${homestay.cityCode || ''}_${homestay.districtCode || ''}_${homestay.addressDetail || ''}`;
  };

  /**
   * 获取降级坐标（城市中心 + 随机偏移）
   */
  const getFallbackGeo = (cityCode: string): { lat: number; lng: number } => {
    const center = DEFAULT_CITY_CENTERS[cityCode] || DEFAULT_CITY_CENTERS['1101'];
    // 添加随机偏移 0.01 ≈ 1km
    const offset = {
      lat: center.lat + (Math.random() - 0.5) * 0.02,
      lng: center.lng + (Math.random() - 0.5) * 0.02,
    };
    return offset;
  };

  /**
   * 地理编码单个房源（带缓存和节流）
   */
  const geocodeHomestay = async (homestay: MapHomestay): Promise<{ lat: number; lng: number } | null> => {
    const geoKey = generateGeoKey(homestay);

    // 1. 检查缓存
    const cached = getCachedGeo(geoKey);
    if (cached) {
      return cached;
    }

    // 2. 检查节流
    if (geoThrottleCount >= GEO_THROTTLE_LIMIT) {
      console.warn('[MapSearch] 地理编码节流，使用降级策略');
      return getFallbackGeo(homestay.cityCode || currentCityCode.value || '1101');
    }

    // 3. 调用地理编码API
    geoThrottleCount++;

    try {
      const result = await geocodeAddress(
        homestay.provinceCode || '',
        homestay.cityCode || '',
        homestay.districtCode || '',
        homestay.addressDetail
      );

      if (result) {
        const geo = { lat: result.lat, lng: result.lng };
        setCachedGeo(geoKey, geo);
        return geo;
      }
    } catch (e) {
      console.error('[MapSearch] 地理编码失败:', e);
    }

    // 4. 失败降级
    return getFallbackGeo(homestay.cityCode || currentCityCode.value || '1101');
  };

  /**
   * 批量地理编码
   */
  const batchGeocode = async (homestayList: MapHomestay[]): Promise<void> => {
    const promises = homestayList.map(async (homestay) => {
      const geo = await geocodeHomestay(homestay);
      if (geo) {
        homestay.lat = geo.lat;
        homestay.lng = geo.lng;
      }
    });

    // 并发控制，每次最多处理10个
    const batchSize = 10;
    for (let i = 0; i < promises.length; i += batchSize) {
      const batch = promises.slice(i, i + batchSize);
      await Promise.all(batch);
      // 每个批次之间稍作延迟，避免请求过于密集
      if (i + batchSize < promises.length) {
        await new Promise(resolve => setTimeout(resolve, GEO_THROTTLE_DELAY));
      }
    }
  };

  /**
   * 初始化高德地图
   */
  const initMap = async (container: HTMLElement) => {
    if (isMapReady.value) return;

    try {
      const AMap = await AMapLoader.load({
        key: AMAP_CONFIG.apiKey,
        version: AMAP_CONFIG.version,
        plugins: ['AMap.Geocoder'],
      });

      // 创建地图实例
      const map = new AMap.Map(container, {
        zoom: 12,
        center: [116.397428, 39.90923], // 默认北京
        resizeEnable: true,
      });

      mapInstance.value = map;

      // 创建信息窗体
      infoWindow.value = new AMap.InfoWindow({
        offset: new AMap.Pixel(0, -30),
        closeWhenClickMap: true,
      });

      // 监听地图移动结束事件
      map.on('moveend', handleMapMoveEnd);
      // 监听缩放结束事件
      map.on('zoomend', handleMapMoveEnd);

      isMapReady.value = true;
    } catch (e) {
      console.error('[MapSearch] 地图初始化失败:', e);
    }
  };

  /**
   * 地图移动结束处理
   */
  const handleMapMoveEnd = () => {
    // 防抖处理
    if (searchDebounceTimer) {
      clearTimeout(searchDebounceTimer);
    }
    searchDebounceTimer = setTimeout(() => {
      searchVisibleHomestays();
    }, 500);
  };

  /**
   * 搜索可视区域的房源
   */
  const searchVisibleHomestays = async () => {
    if (!mapInstance.value || isLoading.value) return;

    const bounds = mapInstance.value.getBounds();
    if (!bounds) return;

    // 获取当前地图中心
    const center = mapInstance.value.getCenter();
    currentCityCode.value = filters.value.cityCode || '';

    // 更新当前可视区域内的房源
    // 由于后端没有空间查询能力，我们仍然调用现有的搜索API
    // 但可以传入城市/区域作为筛选条件
    await loadHomestays();
  };

  /**
   * 加载房源数据
   */
  const loadHomestays = async () => {
    isLoading.value = true;
    try {
      const searchRequest: any = {};

      if (filters.value.keyword) searchRequest.keyword = filters.value.keyword;
      if (filters.value.provinceCode) searchRequest.provinceCode = filters.value.provinceCode;
      if (filters.value.cityCode) searchRequest.cityCode = filters.value.cityCode;
      if (filters.value.districtCode) searchRequest.districtCode = filters.value.districtCode;
      if (filters.value.minPrice) searchRequest.minPrice = filters.value.minPrice;
      if (filters.value.maxPrice) searchRequest.maxPrice = filters.value.maxPrice;
      if (filters.value.maxGuests) searchRequest.maxGuests = filters.value.maxGuests;

      // 分页加载所有可见区域的房源
      searchRequest.page = 0;
      searchRequest.size = 100;

      const response = await searchHomestays(searchRequest);
      const data = response.data || response || [];

      // 转换为 MapHomestay 格式
      const list: MapHomestay[] = data.map((item: any) => ({
        id: item.id,
        title: item.title,
        price: parseFloat(item.price) || 0,
        coverImage: item.coverImage,
        rating: item.rating,
        provinceCode: item.provinceCode,
        cityCode: item.cityCode,
        districtCode: item.districtCode,
        addressDetail: item.addressDetail,
        maxGuests: item.maxGuests,
        propertyType: item.propertyType || item.type,
        autoConfirm: item.autoConfirm,
      }));

      homestays.value = list;

      // 批量地理编码
      await batchGeocode(list);

      // 更新地图标记
      updateMarkers();

      // 更新城市中心
      if (filters.value.cityCode && mapInstance.value) {
        const center = DEFAULT_CITY_CENTERS[filters.value.cityCode];
        if (center) {
          mapInstance.value.setCenter([center.lng, center.lat]);
        }
      }
    } catch (e) {
      console.error('[MapSearch] 加载房源失败:', e);
      homestays.value = [];
    } finally {
      isLoading.value = false;
    }
  };

  /**
   * 更新地图标记
   */
  const updateMarkers = () => {
    if (!mapInstance.value) return;

    // 清除旧标记
    clearMarkers();

    // 创建新标记
    homestays.value.forEach((homestay) => {
      if (homestay.lat && homestay.lng) {
        createMarker(homestay);
      }
    });

    // 自动调整视野显示所有标记
    if (markers.value.length > 0 && mapInstance.value) {
      mapInstance.value.setFitView();
    }
  };

  /**
   * HTML转义防止XSS
   */
  const escapeHtml = (str: string): string => {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  };

  /**
   * 验证图片URL是否安全
   */
  const isValidImageUrl = (url: string): boolean => {
    if (!url) return false;
    // 只允许 http/https 协议，且必须是图片域名或localhost
    try {
      const parsed = new URL(url);
      if (!['http:', 'https:'].includes(parsed.protocol)) return false;
      // 如果是完整URL，必须是可信域名或localhost
      if (parsed.hostname !== 'localhost' && !url.startsWith('https://picsum.photos')) {
        return false;
      }
      return true;
    } catch {
      // 相对路径认为是安全的（会被加上base URL）
      return true;
    }
  };

  /**
   * 创建单个标记
   */
  const createMarker = (homestay: MapHomestay) => {
    if (!mapInstance.value) return;

    const AMap = (window as any).AMap;
    if (!AMap) return;

    // 使用textContent创建DOM元素，避免XSS
    const markerEl = document.createElement('div');
    markerEl.className = `map-price-marker ${hoveredHomestayId.value === homestay.id ? 'active' : ''} ${selectedHomestayId.value === homestay.id ? 'selected' : ''}`;
    markerEl.dataset['id'] = String(homestay.id);

    const priceEl = document.createElement('span');
    priceEl.className = 'price';
    priceEl.textContent = `¥${homestay.price}`;
    markerEl.appendChild(priceEl);

    const marker = new AMap.Marker({
      position: [homestay.lng, homestay.lat],
      content: markerEl,
      offset: new AMap.Pixel(-40, -40),
      extData: homestay,
    });

    // 点击事件
    marker.on('click', () => {
      selectHomestay(homestay.id);
    });

    // 悬停事件
    marker.on('mouseover', () => {
      hoveredHomestayId.value = homestay.id;
      updateMarkerStyle(homestay.id, 'active', true);
    });

    marker.on('mouseout', () => {
      hoveredHomestayId.value = null;
      updateMarkerStyle(homestay.id, 'active', false);
    });

    mapInstance.value.add(marker);
    markers.value.push(marker);
    homestay.marker = marker;
  };

  /**
   * 更新标记样式
   */
  const updateMarkerStyle = (id: number, className: string, add: boolean) => {
    const marker = markers.value.find(m => m.getExtData()?.id === id);
    if (!marker) return;

    const content = marker.getContent();
    // content可能是DOM元素或字符串
    if (content instanceof HTMLElement) {
      if (add) {
        content.classList.add(className);
      } else {
        content.classList.remove(className);
      }
    }
  };

  /**
   * 清除所有标记
   */
  const clearMarkers = () => {
    if (!mapInstance.value) return;

    markers.value.forEach((marker) => {
      mapInstance.value.remove(marker);
    });
    markers.value = [];
  };

  /**
   * 选择房源
   */
  const selectHomestay = (id: number) => {
    selectedHomestayId.value = id;
    const homestay = homestays.value.find(h => h.id === id);
    if (!homestay || !infoWindow.value || !mapInstance.value) return;

    // 创建DOM元素，避免XSS
    const container = document.createElement('div');
    container.className = 'map-info-window';

    const imageDiv = document.createElement('div');
    imageDiv.className = 'info-image';
    const safeImageUrl = isValidImageUrl(homestay.coverImage)
      ? getImageUrl(homestay.coverImage)
      : 'https://picsum.photos/300/200';
    imageDiv.style.backgroundImage = `url('${safeImageUrl}')`;

    const contentDiv = document.createElement('div');
    contentDiv.className = 'info-content';

    const titleEl = document.createElement('h4');
    titleEl.textContent = homestay.title || '';
    contentDiv.appendChild(titleEl);

    const priceEl = document.createElement('p');
    priceEl.className = 'info-price';
    priceEl.textContent = `¥${homestay.price}/晚`;
    contentDiv.appendChild(priceEl);

    if (homestay.rating) {
      const ratingEl = document.createElement('p');
      ratingEl.className = 'info-rating';
      ratingEl.textContent = `⭐ ${homestay.rating}`;
      contentDiv.appendChild(ratingEl);
    }

    container.appendChild(imageDiv);
    container.appendChild(contentDiv);

    infoWindow.value.setContent(container);
    infoWindow.value.open(mapInstance.value, [homestay.lng!, homestay.lat!]);

    // 移动地图中心
    mapInstance.value.setCenter([homestay.lng!, homestay.lat!]);
  };

  /**
   * 获取图片URL
   */
  const getImageUrl = (imageUrl: string): string => {
    if (!imageUrl) return 'https://picsum.photos/300/200';
    if (imageUrl.startsWith('http')) return imageUrl;
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
    return `${baseUrl}${imageUrl.startsWith('/') ? '' : '/'}${imageUrl}`;
  };

  /**
   * 更新筛选条件并重新搜索
   */
  const updateFilters = async (newFilters: SearchFilters) => {
    filters.value = { ...newFilters };

    // 设置地图中心
    if (newFilters.cityCode && mapInstance.value) {
      const center = DEFAULT_CITY_CENTERS[newFilters.cityCode];
      if (center) {
        mapInstance.value.setCenter([center.lng, center.lat]);
      }
    }

    await loadHomestays();
  };

  /**
   * 悬停房源卡片
   */
  const hoverHomestay = (id: number | null) => {
    hoveredHomestayId.value = id;
    if (id !== null) {
      updateMarkerStyle(id, 'active', true);
    }
  };

  /**
   * 销毁地图
   */
  const destroyMap = () => {
    if (searchDebounceTimer) {
      clearTimeout(searchDebounceTimer);
    }
    if (infoWindow.value) {
      infoWindow.value.close();
    }
    clearMarkers();
    if (mapInstance.value) {
      mapInstance.value.destroy();
      mapInstance.value = null;
    }
    isMapReady.value = false;
    saveGeoCache();
  };

  // 组件挂载时加载缓存
  onMounted(() => {
    loadGeoCache();
  });

  // 组件卸载时保存缓存
  onUnmounted(() => {
    saveGeoCache();
  });

  return {
    // 状态
    homestays,
    selectedHomestayId,
    hoveredHomestayId,
    isLoading,
    isMapReady,
    filters,

    // 方法
    initMap,
    loadHomestays,
    updateFilters,
    selectHomestay,
    hoverHomestay,
    destroyMap,
    getImageUrl,
  };
}
