<template>
  <div class="map-search-page">
    <!-- 左侧面板 -->
    <div class="left-panel">
      <!-- 筛选区域 -->
      <div class="filter-section">
        <h2 class="section-title">地图找房</h2>

        <!-- 城市选择 -->
        <div class="filter-item">
          <label>目的地</label>
          <el-cascader
            v-model="selectedRegion"
            :options="regionOptions"
            placeholder="选择城市"
            size="large"
            clearable
            filterable
            @change="handleRegionChange"
          />
        </div>

        <!-- 价格区间 -->
        <div class="filter-item">
          <label>价格区间</label>
          <div class="price-range">
            <el-input-number
              v-model="minPrice"
              :min="0"
              :max="minPriceUpperBound"
              placeholder="最低价"
              size="default"
            />
            <span class="range-separator">-</span>
            <el-input-number
              v-model="maxPrice"
              :min="maxPriceLowerBound"
              :max="99999"
              placeholder="最高价"
              size="default"
            />
          </div>
        </div>

        <!-- 人数 -->
        <div class="filter-item">
          <label>入住人数</label>
          <el-input-number
            v-model="guestCount"
            :min="1"
            :max="20"
            placeholder="人数"
            size="default"
          />
        </div>

        <div class="filter-item">
          <label>Check-in</label>
          <el-date-picker
            v-model="checkInDate"
            type="date"
            placeholder="Select check-in date"
            value-format="YYYY-MM-DD"
            size="large"
            class="date-picker"
            :disabled-date="disabledCheckInDate"
            @change="handleCheckInChange"
          />
        </div>

        <div class="filter-item">
          <label>Check-out</label>
          <el-date-picker
            v-model="checkOutDate"
            type="date"
            placeholder="Select check-out date"
            value-format="YYYY-MM-DD"
            size="large"
            class="date-picker"
            :disabled-date="disabledCheckOutDate"
          />
        </div>

        <div class="filter-item filter-switch">
          <div class="switch-row">
            <span>仅看当前地图范围</span>
            <el-switch
              v-model="viewportSearchEnabled"
              @change="handleViewportModeChange"
            />
          </div>
          <p class="switch-hint">
            开启后，拖动或缩放地图会按当前视口范围刷新房源
          </p>
        </div>

        <!-- 搜索按钮 -->
        <div class="filter-actions">
          <el-button type="primary" @click="handleSearch" :loading="isLoading">
            搜索房源
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>

      <!-- 房源列表 -->
      <div class="homestay-list" ref="listRef">
        <div v-if="isLoading && homestays.length === 0" class="loading-state">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>

        <div v-else-if="searchError && homestays.length === 0" class="error-state">
          <el-icon :size="44" color="#f56c6c"><LocationInformation /></el-icon>
          <p>{{ searchError }}</p>
          <el-button type="primary" @click="handleRetrySearch">
            重试加载
          </el-button>
        </div>

        <div v-else-if="homestays.length === 0" class="empty-state">
          <el-icon :size="48" color="#ccc"><LocationInformation /></el-icon>
          <p>暂无符合条件的房源</p>
          <p class="hint">请尝试调整筛选条件</p>
        </div>

        <template v-else>
          <div class="list-header">
            <span class="count">共 {{ homestays.length }} 个房源</span>
          </div>
          <div class="list-content">
            <MapHomestayCard
              v-for="homestay in homestays"
              :key="homestay.id"
              :homestay="homestay"
              :is-active="selectedHomestayId === homestay.id"
              :is-hovered="hoveredHomestayId === homestay.id"
              @click="handleCardClick"
              @hover="handleCardHover"
              @view-detail="handleViewDetail"
            />
          </div>
        </template>
      </div>
    </div>

    <!-- 右侧地图 -->
    <div class="right-map" ref="mapContainerRef">
      <div v-if="mapError" class="map-loading map-error">
        <el-icon :size="48" color="#f56c6c"><LocationInformation /></el-icon>
        <span>{{ mapError }}</span>
        <el-button type="primary" @click="handleRetryMap">
          重新加载地图
        </el-button>
      </div>
      <div v-else-if="!isMapReady" class="map-loading">
        <el-icon class="is-loading" :size="48"><Loading /></el-icon>
        <span>地图加载中...</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Loading, LocationInformation } from '@element-plus/icons-vue';
import { regionData as regionOptions } from 'element-china-area-data';
import { useMapSearch, type MapHomestay } from '@/composables/useMapSearch';
import MapHomestayCard from '@/components/homestay/MapHomestayCard.vue';

const router = useRouter();
const route = useRoute();

// Refs
const mapContainerRef = ref<HTMLElement | null>(null);
const listRef = ref<HTMLElement | null>(null);

// 使用地图搜索组合式函数
const {
  homestays,
  selectedHomestayId,
  hoveredHomestayId,
  viewportSearchEnabled,
  isLoading,
  isMapReady,
  mapError,
  searchError,
  mapView,
  initMap,
  applySearchState,
  retrySearch,
  selectHomestay,
  hoverHomestay,
  destroyMap,
} = useMapSearch();

// 筛选条件
const selectedRegion = ref<string[]>([]);
const minPrice = ref<number | undefined>(undefined);
const maxPrice = ref<number | undefined>(undefined);
const guestCount = ref<number | undefined>(undefined);
const checkInDate = ref<string | undefined>(undefined);
const checkOutDate = ref<string | undefined>(undefined);
const hasRestoredRouteState = ref(false);
const skipNextRouteReplay = ref(false);
const minPriceUpperBound = computed(() => (maxPrice.value ?? 100000) - 1);
const maxPriceLowerBound = computed(() => (minPrice.value ?? 0) + 1);

const getQueryString = (value: unknown): string | undefined => {
  return typeof value === 'string' && value.trim() ? value : undefined;
};

const getQueryNumber = (value: unknown): number | undefined => {
  const rawValue = getQueryString(value);
  if (!rawValue) return undefined;

  const parsed = Number(rawValue);
  return Number.isFinite(parsed) ? parsed : undefined;
};

const buildFiltersFromForm = () => {
  const filters: Record<string, number | string> = {};

  if (selectedRegion.value.length > 0) {
    const [provinceCode, cityCode, districtCode] = getBackendRegionCodes(selectedRegion.value);
    if (provinceCode) filters.provinceCode = provinceCode;
    if (cityCode) filters.cityCode = cityCode;
    if (districtCode) filters.districtCode = districtCode;
  }

  if (minPrice.value !== undefined) filters.minPrice = minPrice.value;
  if (maxPrice.value !== undefined) filters.maxPrice = maxPrice.value;
  if (guestCount.value !== undefined) filters.minGuests = guestCount.value;
  if (checkInDate.value) filters.checkInDate = checkInDate.value;
  if (checkOutDate.value) filters.checkOutDate = checkOutDate.value;

  return filters;
};

const getBackendRegionCode = (
  code: string | undefined,
  level: 'province' | 'city' | 'district'
) => {
  if (!code) return undefined;

  if (level === 'province' && code.length === 2) {
    return `${code}0000`;
  }

  if (level === 'city' && code.length === 4) {
    return `${code}00`;
  }

  return code;
};

const getBackendRegionCodes = (region: string[]) => {
  return [
    getBackendRegionCode(region[0], 'province'),
    getBackendRegionCode(region[1], 'city'),
    getBackendRegionCode(region[2], 'district'),
  ];
};

const getQuerySignature = (query: Record<string, string>) => {
  return JSON.stringify(
    Object.entries(query).sort(([keyA], [keyB]) => keyA.localeCompare(keyB))
  );
};

const getTrackedRouteQuery = () => {
  const nextQuery: Record<string, string> = {};

  const region = getQueryString(route.query.region);
  const minPriceQuery = getQueryString(route.query.minPrice);
  const maxPriceQuery = getQueryString(route.query.maxPrice);
  const guestCountQuery = getQueryString(route.query.guestCount);
  const checkInQuery = getQueryString(route.query.checkIn);
  const checkOutQuery = getQueryString(route.query.checkOut);
  const viewportOnlyQuery = getQueryString(route.query.viewportOnly);
  const mapLatQuery = getQueryString(route.query.mapLat);
  const mapLngQuery = getQueryString(route.query.mapLng);
  const mapZoomQuery = getQueryString(route.query.mapZoom);

  if (region) {
    nextQuery.region = region;
  }
  if (minPriceQuery) {
    nextQuery.minPrice = minPriceQuery;
  }
  if (maxPriceQuery) {
    nextQuery.maxPrice = maxPriceQuery;
  }
  if (guestCountQuery) {
    nextQuery.guestCount = guestCountQuery;
  }
  if (checkInQuery) {
    nextQuery.checkIn = checkInQuery;
  }
  if (checkOutQuery) {
    nextQuery.checkOut = checkOutQuery;
  }
  if (viewportOnlyQuery === '0') {
    nextQuery.viewportOnly = viewportOnlyQuery;
  }
  if (mapLatQuery) {
    nextQuery.mapLat = mapLatQuery;
  }
  if (mapLngQuery) {
    nextQuery.mapLng = mapLngQuery;
  }
  if (mapZoomQuery) {
    nextQuery.mapZoom = mapZoomQuery;
  }

  return nextQuery;
};

const buildQueryFromForm = () => {
  const nextQuery: Record<string, string> = {};

  if (selectedRegion.value.length > 0) {
    nextQuery.region = selectedRegion.value.join(',');
  }
  if (minPrice.value !== undefined) {
    nextQuery.minPrice = String(minPrice.value);
  }
  if (maxPrice.value !== undefined) {
    nextQuery.maxPrice = String(maxPrice.value);
  }
  if (guestCount.value !== undefined) {
    nextQuery.guestCount = String(guestCount.value);
  }
  if (checkInDate.value) {
    nextQuery.checkIn = checkInDate.value;
  }
  if (checkOutDate.value) {
    nextQuery.checkOut = checkOutDate.value;
  }
  if (!viewportSearchEnabled.value) {
    nextQuery.viewportOnly = '0';
  }
  if (mapView.value.centerLat !== undefined) {
    nextQuery.mapLat = mapView.value.centerLat.toFixed(6);
  }
  if (mapView.value.centerLng !== undefined) {
    nextQuery.mapLng = mapView.value.centerLng.toFixed(6);
  }
  if (mapView.value.zoom !== undefined) {
    nextQuery.mapZoom = String(Math.round(mapView.value.zoom * 100) / 100);
  }

  return nextQuery;
};

const syncQueryFromForm = async (
  mode: 'push' | 'replace' = 'replace',
  options: { skipRouteReplay?: boolean } = {}
) => {
  const nextQuery = buildQueryFromForm();
  if (getQuerySignature(nextQuery) === getQuerySignature(getTrackedRouteQuery())) {
    return false;
  }

  if (options.skipRouteReplay) {
    skipNextRouteReplay.value = true;
  }

  await router[mode]({
    path: route.path,
    query: nextQuery,
  });

  return true;
};

const hydrateFiltersFromQuery = () => {
  const regionQuery = getQueryString(route.query.region);
  selectedRegion.value = regionQuery ? regionQuery.split(',').filter(Boolean) : [];
  minPrice.value = getQueryNumber(route.query.minPrice);
  maxPrice.value = getQueryNumber(route.query.maxPrice);
  guestCount.value = getQueryNumber(route.query.guestCount);
  checkInDate.value = getQueryString(route.query.checkIn);
  checkOutDate.value = getQueryString(route.query.checkOut);
  viewportSearchEnabled.value = getQueryString(route.query.viewportOnly) !== '0';
};

const getMapViewFromQuery = () => {
  const centerLat = getQueryNumber(route.query.mapLat);
  const centerLng = getQueryNumber(route.query.mapLng);
  const zoom = getQueryNumber(route.query.mapZoom);

  if (centerLat === undefined && centerLng === undefined && zoom === undefined) {
    return undefined;
  }

  return {
    centerLat,
    centerLng,
    zoom,
  };
};

const applyRouteStateFromQuery = async () => {
  if (!isMapReady.value) {
    return;
  }

  hydrateFiltersFromQuery();
  const restoredMapView = getMapViewFromQuery();
  await applySearchState(buildFiltersFromForm(), {
    viewportOnly: viewportSearchEnabled.value,
    fitView: !restoredMapView && !viewportSearchEnabled.value,
    skipCityCenter: Boolean(restoredMapView),
    mapView: restoredMapView,
  });
};

const disabledCheckInDate = (time: Date) => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return time.getTime() < today.getTime();
};

const disabledCheckOutDate = (time: Date) => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  if (!checkInDate.value) {
    return time.getTime() < today.getTime();
  }

  const checkIn = new Date(checkInDate.value);
  checkIn.setHours(0, 0, 0, 0);
  return time.getTime() <= checkIn.getTime();
};

const handleCheckInChange = (value: string | null) => {
  if (!value || !checkOutDate.value) {
    return;
  }

  const nextCheckIn = new Date(value);
  const currentCheckOut = new Date(checkOutDate.value);
  if (currentCheckOut > nextCheckIn) {
    return;
  }

  const nextDay = new Date(nextCheckIn);
  nextDay.setDate(nextDay.getDate() + 1);
  checkOutDate.value = nextDay.toISOString().split('T')[0];
};

// 地区选择变化
const handleRegionChange = () => {
  // 自动触发搜索
  handleSearch();
};

const handleViewportModeChange = async (enabled: boolean) => {
  viewportSearchEnabled.value = enabled;
  const changed = await syncQueryFromForm('push');
  if (!changed) {
    await applyRouteStateFromQuery();
  }
};

// 搜索
const handleSearch = async () => {
  const changed = await syncQueryFromForm('push');
  if (!changed) {
    await applyRouteStateFromQuery();
  }
};

// 重置筛选条件
const handleReset = async () => {
  selectedRegion.value = [];
  minPrice.value = undefined;
  maxPrice.value = undefined;
  guestCount.value = undefined;
  checkInDate.value = undefined;
  checkOutDate.value = undefined;
  const changed = await syncQueryFromForm('push');
  if (!changed) {
    await applyRouteStateFromQuery();
  }
};

const handleRetryMap = async () => {
  if (!mapContainerRef.value) return;
  await initMap(mapContainerRef.value);
  await retrySearch();
};

const handleRetrySearch = async () => {
  await retrySearch();
};

// 卡片点击
const handleCardClick = (homestay: MapHomestay) => {
  selectHomestay(homestay.id);
};

const handleViewDetail = (id: number) => {
  router.push(`/homestays/${id}`);
};

// 卡片悬停
const handleCardHover = (id: number | null) => {
  hoverHomestay(id);
};

// 滚动到选中的卡片
const scrollToSelectedCard = () => {
  if (!listRef.value || !selectedHomestayId.value) return;

  const card = listRef.value.querySelector(`[data-id="${selectedHomestayId.value}"]`);
  if (card) {
    card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }
};

// 监听选中房源变化，滚动到对应卡片
watch(selectedHomestayId, (newId) => {
  if (newId) {
    scrollToSelectedCard();
  }
});

watch(
  () => route.query,
  async () => {
    if (!hasRestoredRouteState.value || !isMapReady.value) {
      return;
    }

    if (skipNextRouteReplay.value) {
      skipNextRouteReplay.value = false;
      return;
    }

    await applyRouteStateFromQuery();
  },
  { deep: true }
);

watch(
  mapView,
  async (nextMapView) => {
    if (!hasRestoredRouteState.value || !isMapReady.value) {
      return;
    }

    if (
      nextMapView.centerLat === undefined ||
      nextMapView.centerLng === undefined ||
      nextMapView.zoom === undefined
    ) {
      return;
    }

    await syncQueryFromForm('replace', { skipRouteReplay: true });
  },
  { deep: true }
);

// 挂载
onMounted(async () => {
  // 初始化地图
  if (mapContainerRef.value) {
    await initMap(mapContainerRef.value);
  }

  await applyRouteStateFromQuery();
  hasRestoredRouteState.value = true;
});

// 卸载
onUnmounted(() => {
  destroyMap();
});
</script>

<style scoped>
.map-search-page {
  display: flex;
  height: calc(100vh - 64px); /* 减去 header 高度 */
  overflow: hidden;
}

/* 左侧面板 */
.left-panel {
  width: 400px;
  min-width: 380px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.filter-section {
  padding: 20px;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.section-title {
  margin: 0 0 16px 0;
  font-size: 20px;
  font-weight: 600;
  color: #222;
}

.filter-item {
  margin-bottom: 16px;
}

.filter-switch {
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fafafa;
}

.filter-item label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 8px;
}

.filter-item :deep(.el-cascader) {
  width: 100%;
}

.filter-item :deep(.date-picker) {
  width: 100%;
}

.price-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-range :deep(.el-input-number) {
  flex: 1;
}

.range-separator {
  color: #909399;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.switch-hint {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: #909399;
}

.filter-actions {
  display: flex;
  gap: 8px;
  margin-top: 20px;
}

.filter-actions :deep(.el-button) {
  flex: 1;
}

/* 房源列表 */
.homestay-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.loading-state,
.error-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #909399;
  gap: 12px;
}

.empty-state .hint {
  font-size: 12px;
  color: #c0c4cc;
}

.error-state p {
  margin: 0;
  color: #606266;
}

.list-header {
  margin-bottom: 12px;
}

.list-header .count {
  font-size: 14px;
  color: #606266;
}

.list-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 右侧地图 */
.right-map {
  flex: 1;
  position: relative;
  background: #f5f5f5;
}

.map-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 24px;
  gap: 12px;
  background: rgba(245, 245, 245, 0.92);
  color: #909399;
}

.map-error {
  gap: 16px;
}

/* 响应式 */
@media (max-width: 768px) {
  .map-search-page {
    flex-direction: column;
  }

  .left-panel {
    width: 100%;
    min-width: unset;
    height: 50vh;
  }

  .right-map {
    height: 50vh;
  }
}
</style>

<style>
/* 全局样式 - 价格气泡标记 */
.map-price-marker {
  background: #fff;
  padding: 6px 12px;
  border-radius: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  font-size: 13px;
  font-weight: 600;
  color: #222;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  border: 2px solid transparent;
}

.map-price-marker:hover,
.map-price-marker.active {
  transform: scale(1.1);
  background: #ff385c;
  color: #fff;
  box-shadow: 0 4px 12px rgba(255, 56, 92, 0.4);
}

.map-price-marker.selected {
  background: #ff385c;
  color: #fff;
  border-color: #ff385c;
  transform: scale(1.15);
}

/* 信息窗体样式 */
.map-info-window {
  width: 240px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.map-info-window .info-image {
  width: 100%;
  height: 140px;
  background-size: cover;
  background-position: center;
  border-radius: 8px 8px 0 0;
}

.map-info-window .info-content {
  padding: 12px;
}

.map-info-window .info-content h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #222;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.map-info-window .info-price {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
  color: #ff385c;
}

.map-info-window .info-rating {
  margin: 0;
  font-size: 12px;
  color: #717171;
}
</style>
