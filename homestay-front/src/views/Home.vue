<template>
  <div class="home-container">
    <!-- 搜索栏组件 -->
    <SearchBar :loading="searchStore.loading" @search="handleSearch" @reset="handleReset" />

    <!-- 筛选栏组件 - 只在搜索后显示 -->
    <FilterBar v-if="searchStore.isSearchMode" :property-types="homestayTypes" :grouped-amenities="groupedAmenities"
      :amenities-loading="amenitiesLoading" :selected-property-type="searchStore.searchParams.propertyType"
      @property-type-change="handlePropertyTypeChange" @filters-change="handleFiltersChange"
      @filters-reset="handleFiltersReset" />

    <!-- 搜索模式：显示搜索结果 -->
    <SearchResults v-if="searchStore.isSearchMode"
      :results="searchStore.searchResults"
      :homestay-types="homestayTypes"
      :loading="searchStore.loading"
      :total="searchStore.total"
      :current-page="searchStore.currentPage"
      :page-size="searchStore.pageSize"
      :sort-by="searchStore.searchParams.sortBy"
      :sort-direction="searchStore.searchParams.sortDirection"
      :error="searchStore.searchError"
      @homestay-click="handleHomestayClick"
      @clear-search="clearSearchAndBrowse"
      @page-change="handlePageChange"
      @size-change="handleSizeChange"
      @sort-change="handleSortChange"
      @retry="handleRetry"
    />

    <!-- 浏览模式：显示推荐和热门民宿，个性化推荐放在底部 -->
    <template v-else>
      <!-- 推荐民宿区域 -->
      <RecommendedHomestays @homestay-click="handleHomestayClick" @view-all="handleViewAll" />

      <!-- 热门民宿区域 -->
      <PopularHomestays @homestay-click="handleHomestayClick" @view-all="handleViewAll" />

      <!-- 个性化推荐区域 - 仅登录用户可见，放在底部 -->
      <PersonalizedRecommendations v-if="userStore.isAuthenticated" @homestay-click="handleHomestayClick"
        @view-all="handleViewAll" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { debounce } from '@/utils/debounce'
import {
  getHomestayTypes,
  getAvailableAmenitiesGrouped,
  type HomestayType,
  type AmenityCategoryOption
} from '@/api/homestay'
import { useUserStore } from '@/stores/user'
import { useSearchStore } from '@/stores/search'
import SearchBar from '@/components/SearchBar.vue'
import FilterBar from '@/components/FilterBar.vue'
import SearchResults from '@/components/homestay/SearchResults.vue'
import PersonalizedRecommendations from '@/components/homestay/PersonalizedRecommendations.vue'
import RecommendedHomestays from '@/components/homestay/RecommendedHomestays.vue'
import PopularHomestays from '@/components/homestay/PopularHomestays.vue'

const router = useRouter()
const userStore = useUserStore()
const searchStore = useSearchStore()

// 房源类型和设施数据（页面级别，不需要放入 store）
const homestayTypes = ref<HomestayType[]>([])
const groupedAmenities = ref<AmenityCategoryOption[]>([])
const amenitiesLoading = ref(false)

// 搜索请求取消控制
let searchAbortController: AbortController | null = null
let searchRequestVersion = 0

// 事件处理函数
const handleSearch = async (params: any) => {
  searchStore.setSearchState(params)
  searchStore.updatePagination(1)
  searchStore.syncToUrl()
  await searchHomestays()
}

const handleReset = () => {
  searchStore.resetSearch()
  router.push('/')
}

const handlePropertyTypeChange = (typeName: string | null) => {
  searchStore.updateParam('propertyType', typeName)
  searchStore.syncToUrl()
  debouncedSearchHomestays()
}

const handleFiltersChange = (filters: any) => {
  searchStore.updateParam('minPrice', filters.minPrice)
  searchStore.updateParam('maxPrice', filters.maxPrice)
  searchStore.updateParam('amenities', filters.amenities)
  searchStore.syncToUrl()
  debouncedSearchHomestays()
}

const handleFiltersReset = () => {
  searchStore.updateParam('minPrice', null)
  searchStore.updateParam('maxPrice', null)
  searchStore.updateParam('amenities', [])
  debouncedSearchHomestays()
}

const handleHomestayClick = (homestay: any) => {
  router.push(`/homestays/${homestay.id}`)
}

const handleViewAll = (route: string, query: Record<string, any>) => {
  router.push({ path: route, query })
}

// 分页处理
const handlePageChange = (page: number) => {
  searchStore.updatePagination(page)
  searchStore.syncToUrl({ includePage: true })
  searchHomestays()
}

const handleSizeChange = (size: number) => {
  searchStore.updatePagination(1, size)
  searchHomestays()
}

const handleSortChange = (newSortBy: string, newSortDirection: string) => {
  searchStore.updateParam('sortBy', newSortBy)
  searchStore.updateParam('sortDirection', newSortDirection)
  searchStore.updatePagination(1)
  searchHomestays()
}

// 重试搜索
const handleRetry = () => {
  searchStore.clearError()
  searchHomestays()
}

// 业务逻辑函数
const searchHomestays = async () => {
  // 取消前一次未完成的搜索请求
  if (searchAbortController) {
    searchAbortController.abort()
  }
  searchAbortController = new AbortController()

  // 增加请求版本号，用于过滤过期响应
  const currentVersion = ++searchRequestVersion

  // 清除之前的错误
  searchStore.clearError()
  searchStore.setLoading(true)

  try {
    const sp = searchStore.searchParams
    // 构建搜索请求对象
    const searchRequest: any = {
      keyword: sp.keyword || null,
      propertyType: sp.propertyType || null,
      minPrice: sp.minPrice || null,
      maxPrice: sp.maxPrice || null,
      requiredAmenities: sp.amenities.length > 0 ? sp.amenities : null,
      page: searchStore.currentPage - 1,
      size: searchStore.pageSize,
      sortBy: sp.sortBy,
      sortDirection: sp.sortDirection
    }

    // 处理地区选择
    if (sp.selectedRegion.length > 0) {
      if (sp.selectedRegion.length >= 1) {
        searchRequest.provinceCode = sp.selectedRegion[0]
      }
      if (sp.selectedRegion.length >= 2) {
        searchRequest.cityCode = sp.selectedRegion[1]
      }
      if (sp.selectedRegion.length >= 3) {
        searchRequest.districtCode = sp.selectedRegion[2]
      }
    }

    // 处理日期
    if (sp.checkIn) {
      searchRequest.checkInDate = sp.checkIn
    }
    if (sp.checkOut) {
      searchRequest.checkOutDate = sp.checkOut
    }

    // 处理客人数量
    if (sp.guestCount > 1) {
      searchRequest.minGuests = sp.guestCount
    }

    console.log('发送搜索请求:', searchRequest, '版本:', currentVersion)

    // 创建带超时的请求
    const timeoutPromise = new Promise((_, reject) => {
      setTimeout(() => {
        reject(new Error('请求超时'))
      }, searchStore.SEARCH_TIMEOUT)
    })

    // 发起请求
    const requestPromise = request.post('/api/homestays/search', searchRequest, {
      signal: searchAbortController.signal
    })

    // 竞态处理：谁先完成用谁的结果
    const response = await Promise.race([requestPromise, timeoutPromise])

    // 检查响应是否过期
    if (currentVersion !== searchRequestVersion) {
      console.log('搜索响应已过期，忽略')
      return
    }

    // 处理分页响应
    let results: any[] = []
    let totalCount = 0
    if (response.data && typeof response.data === 'object') {
      if (Array.isArray(response.data)) {
        results = response.data
        totalCount = response.data.length
      } else if (response.data.data) {
        results = response.data.data
        totalCount = response.data.total || response.data.data.length
      }
    }
    searchStore.setResults(results, totalCount)

    console.log('搜索结果:', results, '总数:', totalCount)
  } catch (error: any) {
    // 检查是否过期请求
    if (currentVersion !== searchRequestVersion) {
      console.log('搜索请求已被取消或过期')
      return
    }

    // 解析错误并设置到 store
    const parsedError = searchStore.parseError(error)
    searchStore.setError(parsedError)
    searchStore.setResults([], 0)

    console.error('搜索民宿失败:', parsedError)
  } finally {
    if (currentVersion === searchRequestVersion) {
      searchStore.setLoading(false)
    }
  }
}

// 防抖搜索
const debouncedSearchHomestays = debounce(() => {
  searchHomestays()
}, 500)

const clearSearchAndBrowse = () => {
  searchStore.resetSearch()
  router.push('/')
}

// 初始化数据加载
const initializeData = async () => {
  try {
    const [typesResponse, amenitiesResponse] = await Promise.all([
      getHomestayTypes(),
      getAvailableAmenitiesGrouped()
    ])

    homestayTypes.value = typesResponse || []
    groupedAmenities.value = amenitiesResponse || []
  } catch (error) {
    console.error('初始化数据失败:', error)
  }
}

// 组件挂载时初始化
onMounted(async () => {
  amenitiesLoading.value = true
  // 从 URL 恢复搜索状态
  searchStore.initFromRoute()
  await initializeData()

  // 如果是搜索模式，执行搜索
  if (searchStore.isSearchMode) {
    await searchHomestays()
  }
})
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
</style>
