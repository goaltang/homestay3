<template>
  <div class="home-container">
    <!-- 搜索栏组件 -->
    <SearchBar :loading="loading" @search="handleSearch" @reset="handleReset" />

    <!-- 筛选栏组件 - 只在搜索后显示 -->
    <FilterBar v-if="isSearchMode" :property-types="homestayTypes" :grouped-amenities="groupedAmenities"
      :amenities-loading="amenitiesLoading" :selected-property-type="searchParams.propertyType"
      @property-type-change="handlePropertyTypeChange" @filters-change="handleFiltersChange"
      @filters-reset="handleFiltersReset" />

    <!-- 搜索模式：显示搜索结果 -->
    <SearchResults v-if="isSearchMode" :results="searchResults" :homestay-types="homestayTypes" :loading="loading"
      @homestay-click="handleHomestayClick" @clear-search="clearSearchAndBrowse" />

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
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import {
  getHomestayTypes,
  getAvailableAmenitiesGrouped,
  type HomestayType,
  type AmenityCategoryOption
} from '@/api/homestay'
import { useUserStore } from '@/stores/user'
import SearchBar from '@/components/SearchBar.vue'
import FilterBar from '@/components/FilterBar.vue'
import HomestayCard from '@/components/homestay/HomestayCard.vue'
import SearchResults from '@/components/homestay/SearchResults.vue'
import PersonalizedRecommendations from '@/components/homestay/PersonalizedRecommendations.vue'
import RecommendedHomestays from '@/components/homestay/RecommendedHomestays.vue'
import PopularHomestays from '@/components/homestay/PopularHomestays.vue'
import axios from 'axios'

// 定义房源数据类型
interface Homestay {
  id: number
  title: string
  description: string
  pricePerNight: number
  maxGuests: number
  amenities: string[]
  images: string[]
  rating?: number
  reviewCount?: number
  type: string
  status: string
  featured: boolean
  coverImage?: string
  provinceCode?: string
  cityCode?: string
  districtCode?: string
  addressDetail?: string
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 响应式状态
const loading = ref(false)
const searchResults = ref<Homestay[]>([])
const homestayTypes = ref<HomestayType[]>([])
const groupedAmenities = ref<AmenityCategoryOption[]>([])
const amenitiesLoading = ref(false)

// 搜索参数
const searchParams = reactive({
  selectedRegion: [] as string[],
  keyword: '',
  checkIn: null as string | null,
  checkOut: null as string | null,
  guestCount: 1,
  minPrice: null as number | null,
  maxPrice: null as number | null,
  propertyType: null as string | null,
  amenities: [] as string[],
  minRating: null
})

// 计算属性：是否为搜索模式
const isSearchMode = computed(() => {
  return searchParams.selectedRegion.length > 0 ||
    searchParams.keyword.trim().length > 0 ||
    searchParams.propertyType ||
    searchParams.checkIn ||
    searchParams.checkOut ||
    searchParams.minPrice !== null ||
    searchParams.maxPrice !== null ||
    searchParams.amenities.length > 0
})

// 事件处理函数
const handleSearch = async (params: any) => {
  Object.assign(searchParams, params)
  await searchHomestays()
}

const handleReset = () => {
  resetAndRefresh()
}

const handlePropertyTypeChange = (typeName: string | null) => {
  searchParams.propertyType = typeName
  searchHomestays()
}

const handleFiltersChange = (filters: any) => {
  searchParams.minPrice = filters.minPrice
  searchParams.maxPrice = filters.maxPrice
  searchParams.amenities = filters.amenities
  searchHomestays()
}

const handleFiltersReset = () => {
  searchParams.minPrice = null
  searchParams.maxPrice = null
  searchParams.amenities = []
  searchHomestays()
}

const handleHomestayClick = (homestay: any) => {
  viewHomestayDetails(homestay.id)
}

const handleViewAll = (route: string, query: Record<string, any>) => {
  router.push({ path: route, query })
}

// 业务逻辑函数
const searchHomestays = async () => {
  loading.value = true
  try {
    // 构建搜索请求对象，匹配后端的HomestaySearchRequest结构
    const searchRequest: any = {
      keyword: searchParams.keyword || null,
      propertyType: searchParams.propertyType || null,
      minPrice: searchParams.minPrice || null,
      maxPrice: searchParams.maxPrice || null,
      requiredAmenities: searchParams.amenities.length > 0 ? searchParams.amenities : null,
      page: 0,
      size: 50,
      sortBy: 'id',
      sortDirection: 'desc'
    }

    // 处理地区选择 - 将地区代码映射到相应字段
    if (searchParams.selectedRegion.length > 0) {
      const regionCodes = searchParams.selectedRegion
      if (regionCodes.length >= 1) {
        searchRequest.provinceCode = regionCodes[0]
      }
      if (regionCodes.length >= 2) {
        searchRequest.cityCode = regionCodes[1]
      }
      if (regionCodes.length >= 3) {
        searchRequest.districtCode = regionCodes[2]
      }
    }

    // 处理入住和退房日期
    if (searchParams.checkIn) {
      searchRequest.checkInDate = searchParams.checkIn
    }
    if (searchParams.checkOut) {
      searchRequest.checkOutDate = searchParams.checkOut
    }

    // 处理客人数量
    if (searchParams.guestCount > 1) {
      searchRequest.minGuests = searchParams.guestCount
    }

    console.log('发送搜索请求:', searchRequest)

    // 发送POST请求到搜索接口
    const response = await request.post('/api/homestays/search', searchRequest)
    searchResults.value = response.data || []

    console.log('搜索结果:', searchResults.value)
  } catch (error) {
    console.error('搜索民宿失败:', error)
    ElMessage.error('搜索失败，请稍后重试')
    searchResults.value = []
  } finally {
    loading.value = false
  }
}

const resetAndRefresh = () => {
  // 重置搜索参数
  Object.assign(searchParams, {
    selectedRegion: [],
    keyword: '',
    checkIn: null,
    checkOut: null,
    guestCount: 1,
    minPrice: null,
    maxPrice: null,
    propertyType: null,
    amenities: [],
    minRating: null
  })

  // 清空搜索结果
  searchResults.value = []

  // 导航到首页浏览模式
  router.push('/')
}

const clearSearchAndBrowse = () => {
  resetAndRefresh()
}

const viewHomestayDetails = (id: number) => {
  router.push(`/homestays/${id}`)
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
onMounted(() => {
  initializeData()
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