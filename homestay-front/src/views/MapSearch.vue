<template>
  <div class="map-search-page">
    <div class="left-panel">
      <div class="filter-section">
        <div class="header-row">
          <h2 class="section-title">地图找房</h2>
          <div class="mode-switches">
            <el-tooltip content="搜索当前位置附近的房源" placement="bottom">
              <el-button
                :type="searchMode === 'nearby' ? 'primary' : ''"
                size="small"
                :icon="Location"
                @click="handleSearchNearby"
              >
                附近
              </el-button>
            </el-tooltip>
            <el-tooltip content="使用聚合点模式查看房源分布" placement="bottom">
              <el-button
                :type="useClusterMode ? 'primary' : ''"
                size="small"
                :icon="Grid"
                @click="handleToggleCluster"
              >
                聚合
              </el-button>
            </el-tooltip>
          </div>
        </div>

        <div v-if="searchMode === 'nearby' || searchMode === 'landmark'" class="filter-item radius-selector">
          <label>搜索半径</label>
          <el-slider
            v-model="nearbyRadius"
            :min="1"
            :max="20"
            :step="1"
            show-input
            @change="handleRadiusChange"
          />
          <p v-if="searchMode === 'landmark'" class="hint-text">
            以地标为中心，搜索周边 {{ nearbyRadius }}km 内的房源
          </p>
          <p v-else class="hint-text">
            以地图中心或当前位置为圆心，搜索周边 {{ nearbyRadius }}km 内的房源
          </p>
        </div>

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

        <div class="filter-item">
          <label>地标找房</label>
          <div class="landmark-search">
            <el-autocomplete
              v-model="landmarkKeyword"
              class="landmark-autocomplete"
              clearable
              :fetch-suggestions="queryLandmarkSuggestions"
              :trigger-on-focus="false"
              :debounce="250"
              :teleported="false"
              value-key="name"
              placeholder="输入小区、商圈、地铁站、学校或景点"
              @select="handleSelectLandmarkSuggestion"
              @clear="handleClearLandmarkInput"
              @input="handleLandmarkInputChange"
              @keyup.enter="handleSearchLandmark"
            >
              <template #default="{ item }">
                <div class="landmark-option">
                  <div class="landmark-option__main">
                    <span class="landmark-option__name">{{ item.name }}</span>
                  </div>
                  <p v-if="item.secondaryText" class="landmark-option__meta">{{ item.secondaryText }}</p>
                </div>
              </template>
            </el-autocomplete>
            <el-button
              :type="searchMode === 'landmark' ? 'primary' : 'default'"
              :loading="isLandmarkSearching"
              @click="handleSearchLandmark"
            >
              搜索地标
            </el-button>
          </div>
          <div
            v-if="isLandmarkSuggestionLoading || landmarkStatusText || hasPendingLandmarkSelection"
            class="landmark-panel"
            :class="{ 'is-active': searchMode === 'landmark' || hasPendingLandmarkSelection }"
          >
            <div class="landmark-panel__header">
              <span class="landmark-panel__title">地标状态</span>
              <span v-if="isLandmarkSuggestionLoading" class="landmark-panel__status">正在获取候选</span>
            </div>
            <p v-if="landmarkStatusText" class="landmark-panel__text">{{ landmarkStatusText }}</p>
            <p
              v-if="hasPendingLandmarkSelection && landmarkSuggestions.length > 0"
              class="landmark-panel__text landmark-panel__text--warning"
            >
              当前输入还未生效，请从候选中选择地点，或按回车优先使用第一条候选
            </p>
          </div>
        </div>

        <div class="filter-item">
          <label>我的位置</label>
          <div class="location-actions">
            <el-button :loading="isLocating" @click="handleUseCurrentLocation">
              使用当前位置
            </el-button>
            <span v-if="userLocation" class="location-status">已获取当前位置</span>
          </div>
          <p v-if="locationError" class="inline-error">{{ locationError }}</p>
        </div>

        <div class="filter-actions">
          <el-button type="primary" :loading="isLoading" @click="handleSearch">
            搜索房源
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>

      <div ref="listRef" class="homestay-list">
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
          <p class="hint">{{ emptyStateHint }}</p>
        </div>

        <template v-else>
          <div class="list-header">
            <span class="count">共 {{ homestays.length }} 个房源</span>
            <span v-if="listAvailabilityHint" class="list-hint">{{ listAvailabilityHint }}</span>
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

    <div ref="mapContainerRef" class="right-map">
      <div v-if="showMapSearchContextCard" class="map-context-card">
        <div class="map-context-card__body">
          <span class="map-context-card__title">{{ mapSearchContextTitle }}</span>
          <span class="map-context-card__desc">{{ mapSearchContextDescription }}</span>
        </div>
        <el-button
          size="small"
          text
          data-testid="exit-special-search"
          @click="handleExitSpecialSearch"
        >
          鍥炲埌鏅€氭悳绱?
        </el-button>
      </div>

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
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Grid, Loading, Location, LocationInformation } from '@element-plus/icons-vue';
import { regionData as regionOptions } from 'element-china-area-data';
import { geocodeAddress, searchAmapPoiSuggestions, type AmapPoiSuggestion } from '@/utils/mapService';
import { useMapSearch, type MapHomestay } from '@/composables/useMapSearch';
import MapHomestayCard from '@/components/homestay/MapHomestayCard.vue';

interface LandmarkSuggestionOption extends AmapPoiSuggestion {
  secondaryText: string;
}

const router = useRouter();
const route = useRoute();

const mapContainerRef = ref<HTMLElement | null>(null);
const listRef = ref<HTMLElement | null>(null);

const {
  homestays,
  selectedHomestayId,
  hoveredHomestayId,
  viewportSearchEnabled,
  useClusterMode,
  searchMode,
  nearbyRadius,
  userLocation,
  activeLandmark,
  isLocating,
  locationError,
  isLoading,
  isMapReady,
  mapError,
  searchError,
  mapView,
  currentSearchContext,
  initMap,
  applySearchState,
  retrySearch,
  setMapView,
  selectHomestay,
  hoverHomestay,
  destroyMap,
  loadClusters,
  loadHomestays,
  locateUser,
  searchNearby,
  searchByLandmark,
  resetSearchMode,
} = useMapSearch();

const selectedRegion = ref<string[]>([]);
const minPrice = ref<number | undefined>(undefined);
const maxPrice = ref<number | undefined>(undefined);
const guestCount = ref<number | undefined>(undefined);
const checkInDate = ref<string | undefined>(undefined);
const checkOutDate = ref<string | undefined>(undefined);
const landmarkKeyword = ref('');
const landmarkLat = ref<number | undefined>(undefined);
const landmarkLng = ref<number | undefined>(undefined);
const resolvedLandmark = ref<LandmarkSuggestionOption | null>(null);
const landmarkSuggestions = ref<LandmarkSuggestionOption[]>([]);
const isLandmarkDirty = ref(false);
const isLandmarkSuggestionLoading = ref(false);
const isLandmarkSearching = ref(false);
const lastLandmarkSuggestionKeyword = ref('');
const hasRestoredRouteState = ref(false);
const skipNextRouteReplay = ref(false);

let landmarkSuggestionRequestId = 0;

const minPriceUpperBound = computed(() => (maxPrice.value ?? 100000) - 1);
const maxPriceLowerBound = computed(() => (minPrice.value ?? 0) + 1);

const resolvedLandmarkName = computed(() => resolvedLandmark.value?.name?.trim() ?? '');
const currentAppliedLandmarkName = computed(() => activeLandmark.value?.name?.trim() || resolvedLandmarkName.value);
const hasResolvedLandmark = computed(
  () =>
    landmarkLat.value !== undefined
    && landmarkLng.value !== undefined
    && Boolean(resolvedLandmarkName.value)
);
const hasPendingLandmarkSelection = computed(() => {
  const keyword = landmarkKeyword.value.trim();
  if (!keyword) {
    return false;
  }

  return !hasResolvedLandmark.value || isLandmarkDirty.value || keyword !== resolvedLandmarkName.value;
});

const landmarkStatusText = computed(() => {
  const keyword = landmarkKeyword.value.trim();
  const appliedName = currentAppliedLandmarkName.value;

  if (searchMode.value === 'landmark' && appliedName && isLandmarkDirty.value && keyword) {
    return `当前结果仍来自“${appliedName}”，输入框已改为“${keyword}”。`;
  }

  if (searchMode.value === 'landmark' && appliedName) {
    return `当前地标：${appliedName}，周边 ${nearbyRadius.value}km。`;
  }

  if (hasPendingLandmarkSelection.value && landmarkSuggestions.value.length > 0) {
    return `已找到 ${landmarkSuggestions.value.length} 条候选，选择后会更新地图搜索。`;
  }

  if (
    keyword
    && !isLandmarkSuggestionLoading.value
    && lastLandmarkSuggestionKeyword.value === keyword
    && landmarkSuggestions.value.length === 0
  ) {
    return '未找到匹配候选，可继续修改关键词后重试。';
  }

  if (keyword) {
    return '输入地标后可直接选择候选，回车会优先使用第一条候选。';
  }

  return '';
});

const listAvailabilityHint = computed(() => {
  if (useClusterMode.value) {
    return '当前为聚合模式，放大地图或关闭聚合后可查看房源卡片。';
  }

  if (hasPendingLandmarkSelection.value && currentAppliedLandmarkName.value) {
    return `输入中的地标尚未生效，当前列表仍显示“${currentAppliedLandmarkName.value}”附近结果。`;
  }

  if (searchMode.value === 'landmark' && currentAppliedLandmarkName.value) {
    return `当前按“${currentAppliedLandmarkName.value}”周边 ${nearbyRadius.value}km 展示。`;
  }

  if (selectedHomestayId.value) {
    return '地图标记与卡片已联动，可继续切换查看。';
  }

  return '悬停卡片会高亮对应地图标记。';
});

const showMapSearchContextCard = computed(() => {
  return currentSearchContext.value.mode === 'nearby' || currentSearchContext.value.mode === 'landmark';
});

const mapSearchContextTitle = computed(() => {
  if (currentSearchContext.value.mode === 'landmark') {
    return currentAppliedLandmarkName.value
      ? `鍦版爣锛?${currentAppliedLandmarkName.value}`
      : '鍦版爣鍛ㄨ竟鎼滅储';
  }

  if (currentSearchContext.value.mode === 'nearby') {
    return currentSearchContext.value.nearbyOrigin?.source === 'user'
      ? '褰撳墠浣嶇疆闄勮繎'
      : '鍦板浘涓績闄勮繎';
  }

  return '';
});

const mapSearchContextDescription = computed(() => {
  if (!showMapSearchContextCard.value) {
    return '';
  }

  const resultCount = homestays.value.length;
  const resultText = resultCount > 0 ? `褰撳墠鍏?${resultCount} 涓埧婧?` : '褰撳墠鏆傛棤鎴挎簮';
  return `鍦板浘宸叉寜 ${nearbyRadius.value}km 鑼冨洿灞曠ず锛?${resultText}`;
});

const emptyStateHint = computed(() => {
  if (useClusterMode.value) {
    return '当前处于聚合模式，放大地图或关闭聚合后再查看具体房源。';
  }

  if (hasPendingLandmarkSelection.value) {
    return '先从候选中选择地标，再查看该地标周边房源。';
  }

  if (searchMode.value === 'landmark' && currentAppliedLandmarkName.value) {
    return `可以尝试扩大半径，或更换地标“${currentAppliedLandmarkName.value}”。`;
  }

  return '请尝试调整筛选条件。';
});

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

const getLandmarkSuggestionCity = () => {
  const [provinceCode, cityCode, districtCode] = getBackendRegionCodes(selectedRegion.value);
  return districtCode || cityCode || provinceCode;
};

const formatLandmarkSecondaryText = (suggestion: AmapPoiSuggestion) => {
  const parts = [
    suggestion.address,
    [suggestion.district, suggestion.cityName, suggestion.provinceName]
      .filter((value): value is string => Boolean(value && value.trim()))
      .join(' · '),
  ].filter((value): value is string => Boolean(value && value.trim()));

  return parts.join(' · ');
};

const normalizeLandmarkSuggestion = (suggestion: AmapPoiSuggestion): LandmarkSuggestionOption => {
  return {
    ...suggestion,
    secondaryText: formatLandmarkSecondaryText(suggestion),
  };
};

const clearResolvedLandmark = () => {
  resolvedLandmark.value = null;
  landmarkLat.value = undefined;
  landmarkLng.value = undefined;
};

const setResolvedLandmark = (suggestion: LandmarkSuggestionOption) => {
  resolvedLandmark.value = suggestion;
  landmarkKeyword.value = suggestion.name;
  landmarkLat.value = suggestion.latitude;
  landmarkLng.value = suggestion.longitude;
  isLandmarkDirty.value = false;
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
  const modeQuery = getQueryString(route.query.mode);
  const radiusQuery = getQueryString(route.query.radiusKm);
  const landmarkQuery = getQueryString(route.query.landmark);
  const landmarkLatQuery = getQueryString(route.query.landmarkLat);
  const landmarkLngQuery = getQueryString(route.query.landmarkLng);
  const viewportOnlyQuery = getQueryString(route.query.viewportOnly);
  const mapLatQuery = getQueryString(route.query.mapLat);
  const mapLngQuery = getQueryString(route.query.mapLng);
  const mapZoomQuery = getQueryString(route.query.mapZoom);

  if (region) nextQuery.region = region;
  if (minPriceQuery) nextQuery.minPrice = minPriceQuery;
  if (maxPriceQuery) nextQuery.maxPrice = maxPriceQuery;
  if (guestCountQuery) nextQuery.guestCount = guestCountQuery;
  if (checkInQuery) nextQuery.checkIn = checkInQuery;
  if (checkOutQuery) nextQuery.checkOut = checkOutQuery;
  if (modeQuery) nextQuery.mode = modeQuery;
  if (radiusQuery) nextQuery.radiusKm = radiusQuery;
  if (landmarkQuery) nextQuery.landmark = landmarkQuery;
  if (landmarkLatQuery) nextQuery.landmarkLat = landmarkLatQuery;
  if (landmarkLngQuery) nextQuery.landmarkLng = landmarkLngQuery;
  if (viewportOnlyQuery === '0') nextQuery.viewportOnly = viewportOnlyQuery;
  if (mapLatQuery) nextQuery.mapLat = mapLatQuery;
  if (mapLngQuery) nextQuery.mapLng = mapLngQuery;
  if (mapZoomQuery) nextQuery.mapZoom = mapZoomQuery;

  return nextQuery;
};

const buildQueryFromForm = () => {
  const nextQuery: Record<string, string> = {};
  const landmarkQueryReady = searchMode.value === 'landmark' && hasResolvedLandmark.value;

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

  if (searchMode.value === 'nearby' || landmarkQueryReady) {
    nextQuery.mode = searchMode.value;
    nextQuery.radiusKm = String(nearbyRadius.value);
  }

  if (landmarkQueryReady) {
    nextQuery.landmark = resolvedLandmarkName.value;
    nextQuery.landmarkLat = landmarkLat.value!.toFixed(6);
    nextQuery.landmarkLng = landmarkLng.value!.toFixed(6);
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

  const modeQuery = getQueryString(route.query.mode);
  searchMode.value = modeQuery === 'nearby' || modeQuery === 'landmark' ? modeQuery : 'normal';
  nearbyRadius.value = getQueryNumber(route.query.radiusKm) ?? 5;

  const queryLandmarkName = getQueryString(route.query.landmark) ?? '';
  const queryLandmarkLat = getQueryNumber(route.query.landmarkLat);
  const queryLandmarkLng = getQueryNumber(route.query.landmarkLng);

  landmarkKeyword.value = queryLandmarkName;
  landmarkLat.value = queryLandmarkLat;
  landmarkLng.value = queryLandmarkLng;
  landmarkSuggestions.value = [];
  lastLandmarkSuggestionKeyword.value = '';
  isLandmarkDirty.value = false;
  viewportSearchEnabled.value = getQueryString(route.query.viewportOnly) !== '0';

  if (queryLandmarkName && queryLandmarkLat !== undefined && queryLandmarkLng !== undefined) {
    resolvedLandmark.value = normalizeLandmarkSuggestion({
      id: `query-${queryLandmarkName}-${queryLandmarkLat}-${queryLandmarkLng}`,
      name: queryLandmarkName,
      address: '',
      latitude: queryLandmarkLat,
      longitude: queryLandmarkLng,
    });
    return;
  }

  clearResolvedLandmark();
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
  const nextFilters = buildFiltersFromForm();
  const fitView = !restoredMapView && !viewportSearchEnabled.value;

  if (restoredMapView) {
    setMapView(restoredMapView);
  }

  if (searchMode.value === 'landmark' && hasResolvedLandmark.value) {
    await searchByLandmark(
      {
        latitude: landmarkLat.value!,
        longitude: landmarkLng.value!,
        name: resolvedLandmarkName.value || undefined,
      },
      {
        radius: nearbyRadius.value,
        filters: nextFilters,
        fitView: restoredMapView ? false : fitView,
      }
    );
    return;
  }

  if (searchMode.value === 'nearby') {
    await searchNearby({
      radius: nearbyRadius.value,
      filters: nextFilters,
      fitView: restoredMapView ? false : fitView,
    });
    return;
  }

  await resetSearchMode({ filters: nextFilters });
  await applySearchState(nextFilters, {
    viewportOnly: viewportSearchEnabled.value,
    fitView,
    skipCityCenter: Boolean(restoredMapView),
    mapView: restoredMapView,
  });
};

const resolveLandmarkSuggestion = async (
  suggestion: LandmarkSuggestionOption
): Promise<LandmarkSuggestionOption | null> => {
  if (suggestion.latitude !== undefined && suggestion.longitude !== undefined) {
    return suggestion;
  }

  const [provinceCode, cityCode, districtCode] = getBackendRegionCodes(selectedRegion.value);
  const geocode = await geocodeAddress(
    provinceCode || '',
    cityCode || '',
    districtCode || '',
    suggestion.name
  );

  if (!geocode) {
    return null;
  }

  return {
    ...suggestion,
    address: suggestion.address || geocode.formattedAddress,
    secondaryText: suggestion.secondaryText || geocode.formattedAddress,
    latitude: geocode.lat,
    longitude: geocode.lng,
  };
};

const runLandmarkSearch = async (suggestion: LandmarkSuggestionOption) => {
  isLandmarkSearching.value = true;

  try {
    const resolvedSuggestion = await resolveLandmarkSuggestion(suggestion);
    if (!resolvedSuggestion || resolvedSuggestion.latitude === undefined || resolvedSuggestion.longitude === undefined) {
      ElMessage.error('未找到对应地标，请尝试更换关键词');
      return;
    }

    setResolvedLandmark(resolvedSuggestion);
    searchMode.value = 'landmark';
    useClusterMode.value = false;

    const changed = await syncQueryFromForm('push');
    if (!changed) {
      await applyRouteStateFromQuery();
    }
  } finally {
    isLandmarkSearching.value = false;
  }
};

const queryLandmarkSuggestions = async (
  queryString: string,
  callback: (items: LandmarkSuggestionOption[]) => void
) => {
  const keyword = queryString.trim();
  const suggestionCity = getLandmarkSuggestionCity();
  lastLandmarkSuggestionKeyword.value = keyword;

  if (!keyword) {
    landmarkSuggestions.value = [];
    callback([]);
    return;
  }

  const requestId = ++landmarkSuggestionRequestId;
  isLandmarkSuggestionLoading.value = true;

  try {
    const suggestions = await searchAmapPoiSuggestions(keyword, {
      city: suggestionCity,
      cityLimit: Boolean(suggestionCity),
      limit: 8,
    });

    if (requestId !== landmarkSuggestionRequestId) {
      return;
    }

    const options = suggestions.map(normalizeLandmarkSuggestion);
    landmarkSuggestions.value = options;
    callback(options);
  } catch (error) {
    if (requestId !== landmarkSuggestionRequestId) {
      return;
    }

    console.error('Failed to query landmark suggestions:', error);
    landmarkSuggestions.value = [];
    callback([]);
  } finally {
    if (requestId === landmarkSuggestionRequestId) {
      isLandmarkSuggestionLoading.value = false;
    }
  }
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

const handleRegionChange = () => {
  void handleSearch();
};

const handleViewportModeChange = async (enabled: boolean) => {
  viewportSearchEnabled.value = enabled;
  const changed = await syncQueryFromForm('push');
  if (!changed) {
    await applyRouteStateFromQuery();
  }
};

const handleLandmarkInputChange = (value: string) => {
  const keyword = value.trim();

  if (!keyword) {
    landmarkSuggestions.value = [];
    lastLandmarkSuggestionKeyword.value = '';
    isLandmarkDirty.value = searchMode.value === 'landmark' && hasResolvedLandmark.value;
    return;
  }

  isLandmarkDirty.value = keyword !== resolvedLandmarkName.value;
};

const handleClearLandmarkInput = () => {
  landmarkKeyword.value = '';
  landmarkSuggestions.value = [];
  lastLandmarkSuggestionKeyword.value = '';

  if (searchMode.value === 'landmark' && hasResolvedLandmark.value) {
    isLandmarkDirty.value = true;
    return;
  }

  isLandmarkDirty.value = false;
  clearResolvedLandmark();
};

const handleSearch = async () => {
  await resetSearchMode({ filters: buildFiltersFromForm() });
  const changed = await syncQueryFromForm('push');
  if (!changed) {
    await applyRouteStateFromQuery();
  }
};

const handleReset = async () => {
  selectedRegion.value = [];
  minPrice.value = undefined;
  maxPrice.value = undefined;
  guestCount.value = undefined;
  checkInDate.value = undefined;
  checkOutDate.value = undefined;
  landmarkKeyword.value = '';
  landmarkSuggestions.value = [];
  lastLandmarkSuggestionKeyword.value = '';
  isLandmarkDirty.value = false;
  clearResolvedLandmark();
  nearbyRadius.value = 5;

  await resetSearchMode({ filters: buildFiltersFromForm() });
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

const handleCardClick = (homestay: MapHomestay) => {
  selectHomestay(homestay.id);
};

const handleViewDetail = (id: number) => {
  void router.push(`/homestays/${id}`);
};

const handleCardHover = (id: number | null) => {
  hoverHomestay(id);
};

const handleSearchNearby = async () => {
  searchMode.value = 'nearby';
  useClusterMode.value = false;
  const changed = await syncQueryFromForm('push');
  if (!changed) {
    await applyRouteStateFromQuery();
  }
};

const handleRadiusChange = async () => {
  if (searchMode.value === 'normal') {
    return;
  }

  const changed = await syncQueryFromForm('push');
  if (!changed) {
    await applyRouteStateFromQuery();
  }
};

const handleUseCurrentLocation = async () => {
  const location = await locateUser({ recenter: true });
  if (!location) {
    ElMessage.error(locationError.value || '定位失败，请检查浏览器权限设置');
    return;
  }

  searchMode.value = 'nearby';
  useClusterMode.value = false;
  const changed = await syncQueryFromForm('push');
  if (!changed) {
    await applyRouteStateFromQuery();
  }
};

const handleSelectLandmarkSuggestion = (suggestion: LandmarkSuggestionOption) => {
  void runLandmarkSearch(suggestion);
};

const handleSearchLandmark = async () => {
  const keyword = landmarkKeyword.value.trim();
  if (!keyword) {
    ElMessage.warning('请输入地标关键词');
    return;
  }

  if (!isLandmarkDirty.value && hasResolvedLandmark.value && keyword === resolvedLandmarkName.value) {
    searchMode.value = 'landmark';
    useClusterMode.value = false;
    const changed = await syncQueryFromForm('push');
    if (!changed) {
      await applyRouteStateFromQuery();
    }
    return;
  }

  const canReuseSuggestions = lastLandmarkSuggestionKeyword.value === keyword;
  const matchedSuggestion = canReuseSuggestions
    ? landmarkSuggestions.value.find((item) => item.name === keyword)
    : undefined;
  const firstSuggestion = canReuseSuggestions ? (matchedSuggestion ?? landmarkSuggestions.value[0]) : undefined;

  if (firstSuggestion) {
    await runLandmarkSearch(firstSuggestion);
    return;
  }

  await runLandmarkSearch(
    normalizeLandmarkSuggestion({
      id: `manual-${keyword}`,
      name: keyword,
      address: '',
    })
  );
};

const handleToggleCluster = async () => {
  useClusterMode.value = !useClusterMode.value;

  if (useClusterMode.value) {
    await resetSearchMode({ filters: buildFiltersFromForm() });
    await loadClusters();
    return;
  }

  await loadHomestays({ fitView: true });
};

const handleExitSpecialSearch = async () => {
  await resetSearchMode({ filters: buildFiltersFromForm() });
  const changed = await syncQueryFromForm('push');
  if (!changed) {
    await applyRouteStateFromQuery();
  }
};

const scrollToSelectedCard = () => {
  if (!listRef.value || !selectedHomestayId.value) return;

  const card = listRef.value.querySelector(`[data-id="${selectedHomestayId.value}"]`);
  if (card) {
    card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }
};

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
      nextMapView.centerLat === undefined
      || nextMapView.centerLng === undefined
      || nextMapView.zoom === undefined
    ) {
      return;
    }

    await syncQueryFromForm('replace', { skipRouteReplay: true });
  },
  { deep: true }
);

onMounted(async () => {
  if (mapContainerRef.value) {
    await initMap(mapContainerRef.value);
  }

  await applyRouteStateFromQuery();
  hasRestoredRouteState.value = true;
});

onUnmounted(() => {
  destroyMap();
});
</script>

<style scoped>
.map-search-page {
  display: flex;
  height: calc(100vh - 64px);
  overflow: hidden;
}

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

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #222;
}

.mode-switches {
  display: flex;
  gap: 8px;
}

.radius-selector {
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #f0f9ff;
}

.radius-selector .hint-text {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: #3b82f6;
}

.filter-item {
  margin-bottom: 16px;
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

.landmark-search {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.landmark-autocomplete {
  flex: 1;
}

.landmark-search :deep(.el-input),
.landmark-search :deep(.el-autocomplete) {
  width: 100%;
}

.landmark-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 2px 0;
}

.landmark-option__main {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.landmark-option__name {
  color: #303133;
  font-size: 14px;
  font-weight: 500;
}

.landmark-option__meta {
  margin: 0;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}

.landmark-panel {
  margin-top: 10px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #f8fafc;
}

.landmark-panel.is-active {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.landmark-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.landmark-panel__title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
}

.landmark-panel__status {
  font-size: 12px;
  color: #3b82f6;
}

.landmark-panel__text {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: #475569;
}

.landmark-panel__text--warning {
  color: #c2410c;
}

.location-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.location-status {
  font-size: 12px;
  color: #16a34a;
}

.inline-error {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: #dc2626;
}

.filter-switch {
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fafafa;
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
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.list-header .count {
  font-size: 14px;
  color: #606266;
}

.list-hint {
  font-size: 12px;
  line-height: 1.5;
  color: #909399;
}

.list-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.right-map {
  flex: 1;
  position: relative;
  background: #f5f5f5;
}

.map-context-card {
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  max-width: min(440px, calc(100% - 32px));
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.14);
  border: 1px solid rgba(59, 130, 246, 0.14);
  backdrop-filter: blur(10px);
}

.map-context-card__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.map-context-card__title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.map-context-card__desc {
  font-size: 12px;
  line-height: 1.5;
  color: #4b5563;
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

  .map-context-card {
    top: 12px;
    left: 12px;
    right: 12px;
    max-width: none;
    align-items: flex-start;
  }
}
</style>

<style>
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
