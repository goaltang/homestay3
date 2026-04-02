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
              :max="maxPrice - 1"
              placeholder="最低价"
              size="default"
            />
            <span class="range-separator">-</span>
            <el-input-number
              v-model="maxPrice"
              :min="minPrice + 1"
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
            />
          </div>
        </template>
      </div>
    </div>

    <!-- 右侧地图 -->
    <div class="right-map" ref="mapContainerRef">
      <div v-if="!isMapReady" class="map-loading">
        <el-icon class="is-loading" :size="48"><Loading /></el-icon>
        <span>地图加载中...</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Loading, LocationInformation } from '@element-plus/icons-vue';
import { regionOptions } from 'element-china-area-data';
import { useMapSearch, type MapHomestay } from '@/composables/useMapSearch';
import MapHomestayCard from '@/components/homestay/MapHomestayCard.vue';

const router = useRouter();

// Refs
const mapContainerRef = ref<HTMLElement | null>(null);
const listRef = ref<HTMLElement | null>(null);

// 使用地图搜索组合式函数
const {
  homestays,
  selectedHomestayId,
  hoveredHomestayId,
  isLoading,
  isMapReady,
  initMap,
  updateFilters,
  selectHomestay,
  hoverHomestay,
  destroyMap,
} = useMapSearch();

// 筛选条件
const selectedRegion = ref<string[]>([]);
const minPrice = ref<number | undefined>(undefined);
const maxPrice = ref<number | undefined>(undefined);
const guestCount = ref<number | undefined>(undefined);

// 地区选择变化
const handleRegionChange = (value: string[]) => {
  // 自动触发搜索
  handleSearch();
};

// 搜索
const handleSearch = async () => {
  const filters: any = {};

  if (selectedRegion.value && selectedRegion.value.length > 0) {
    if (selectedRegion.value[0]) filters.provinceCode = selectedRegion.value[0];
    if (selectedRegion.value[1]) filters.cityCode = selectedRegion.value[1];
    if (selectedRegion.value[2]) filters.districtCode = selectedRegion.value[2];
  }

  if (minPrice.value !== undefined) filters.minPrice = minPrice.value;
  if (maxPrice.value !== undefined) filters.maxPrice = maxPrice.value;
  if (guestCount.value !== undefined) filters.maxGuests = guestCount.value;

  await updateFilters(filters);
};

// 重置筛选条件
const handleReset = () => {
  selectedRegion.value = [];
  minPrice.value = undefined;
  maxPrice.value = undefined;
  guestCount.value = undefined;
  updateFilters({});
};

// 卡片点击
const handleCardClick = (homestay: MapHomestay) => {
  selectHomestay(homestay.id);
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

// 挂载
onMounted(async () => {
  // 初始化地图
  if (mapContainerRef.value) {
    await initMap(mapContainerRef.value);
  }

  // 默认加载数据
  await updateFilters({});
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
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #909399;
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
