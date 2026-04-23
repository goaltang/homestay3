<template>
  <div class="home-container">
    <!-- 搜索栏组件 -->
    <SearchBar :loading="searchStore.loading" @search="handleSearch" @reset="handleReset" />

    <!-- 地图找房入口 -->
    <button type="button" class="map-search-entry" @click="goToMapSearch">
      <div class="entry-icon">
        <el-icon :size="32"><Location /></el-icon>
      </div>
      <div class="entry-content">
        <h3>地图找房</h3>
        <p>在地图上探索房源，位置一目了然</p>
      </div>
      <div class="entry-arrow">
        <el-icon :size="24"><ArrowRight /></el-icon>
      </div>
    </button>

    <!-- 浏览模式：显示推荐和热门民宿，个性化推荐放在底部 -->
    <!-- 推荐民宿区域 -->
    <RecommendedHomestays @homestay-click="handleHomestayClick" @view-all="handleViewAll" />

    <!-- 热门民宿区域 -->
    <PopularHomestays @homestay-click="handleHomestayClick" @view-all="handleViewAll" />

    <!-- 个性化推荐区域 - 仅登录用户可见，放在底部 -->
    <PersonalizedRecommendations v-if="userStore.isAuthenticated" @homestay-click="handleHomestayClick"
      @view-all="handleViewAll" />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Location, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useSearchStore } from '@/stores/search'
import SearchBar from '@/components/SearchBar.vue'
import PersonalizedRecommendations from '@/components/homestay/PersonalizedRecommendations.vue'
import RecommendedHomestays from '@/components/homestay/RecommendedHomestays.vue'
import PopularHomestays from '@/components/homestay/PopularHomestays.vue'

const router = useRouter()
const userStore = useUserStore()
const searchStore = useSearchStore()

// 事件处理函数
const handleSearch = (params: any) => {
  searchStore.setSearchState(params)
  searchStore.updatePagination(1)
  searchStore.syncToUrl()
}

const handleReset = () => {
  searchStore.resetSearch()
  router.push('/')
}

const handleHomestayClick = (homestay: any) => {
  router.push(`/homestays/${homestay.id}`)
}

const handleViewAll = (route: string, query: Record<string, any>) => {
  router.push({ path: route, query })
}

const goToMapSearch = () => {
  router.push('/map-search')
}
</script>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 40px 0 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.section-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #222;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.loading-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  margin: 20px 0;
}

.empty-result {
  text-align: center;
  padding: 40px 20px;
}

.homestay-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  margin: 20px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .home-container {
    padding: 16px;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .homestay-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}

/* 地图找房入口 */
.map-search-entry {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 20px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: 0;
  border-radius: 16px;
  margin-bottom: 24px;
  cursor: pointer;
  font: inherit;
  text-align: left;
  transition: all 0.3s ease;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
}

.map-search-entry:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
}

.entry-icon {
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.entry-content {
  flex: 1;
}

.entry-content h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}

.entry-content p {
  margin: 4px 0 0 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
}

.entry-arrow {
  color: #fff;
  opacity: 0.8;
}

@media (max-width: 768px) {
  .map-search-entry {
    padding: 16px;
    gap: 12px;
  }

  .entry-icon {
    width: 48px;
    height: 48px;
  }

  .entry-content h3 {
    font-size: 16px;
  }

  .entry-content p {
    font-size: 13px;
  }
}
</style>
