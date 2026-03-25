import { defineStore } from 'pinia'
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

// 搜索错误类型
export interface SearchError {
  type: 'network' | 'timeout' | 'server' | 'unknown'
  message: string
  code?: string | number
}

// 搜索参数接口
export interface SearchState {
  keyword: string
  selectedRegion: string[]
  checkIn: string | null
  checkOut: string | null
  guestCount: number
  propertyType: string | null
  minPrice: number | null
  maxPrice: number | null
  amenities: string[]
  minRating: number | null
  sortBy: string
  sortDirection: string
}

// 默认搜索参数
const defaultSearchState: SearchState = {
  keyword: '',
  selectedRegion: [],
  checkIn: null,
  checkOut: null,
  guestCount: 1,
  propertyType: null,
  minPrice: null,
  maxPrice: null,
  amenities: [],
  minRating: null,
  sortBy: 'id',
  sortDirection: 'desc'
}

// 搜索超时时间（毫秒）
const SEARCH_TIMEOUT = 15000

export const useSearchStore = defineStore('search', () => {
  const router = useRouter()
  const route = useRoute()

  // 搜索参数状态
  const searchParams = reactive<SearchState>({ ...defaultSearchState })

  // 分页状态
  const currentPage = ref(1)
  const pageSize = ref(12)
  const total = ref(0)

  // 搜索结果
  const searchResults = ref<any[]>([])
  const loading = ref(false)

  // 错误状态
  const searchError = ref<SearchError | null>(null)
  const hasError = computed(() => searchError.value !== null)

  // 计算属性：是否为搜索模式
  const isSearchMode = computed(() => {
    return (
      searchParams.keyword.trim() !== '' ||
      searchParams.selectedRegion.length > 0 ||
      searchParams.checkIn !== null ||
      searchParams.checkOut !== null ||
      searchParams.propertyType !== null ||
      searchParams.minPrice !== null ||
      searchParams.maxPrice !== null ||
      searchParams.amenities.length > 0
    )
  })

  // 计算属性：是否有活跃搜索
  const hasActiveSearch = computed(() => isSearchMode.value)

  // 清除错误
  const clearError = () => {
    searchError.value = null
  }

  // 设置错误
  const setError = (error: SearchError | null) => {
    searchError.value = error
  }

  // 解析错误类型
  const parseError = (error: any): SearchError => {
    // 网络错误（请求未发出或连接失败）
    if (!error.response) {
      if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
        return {
          type: 'timeout',
          message: '搜索请求超时，请稍后重试'
        }
      }
      return {
        type: 'network',
        message: '网络连接失败，请检查网络后重试'
      }
    }

    // 服务器错误（返回了错误状态码）
    const status = error.response?.status
    if (status >= 500) {
      return {
        type: 'server',
        message: '服务器繁忙，请稍后重试',
        code: status
      }
    }

    if (status === 429) {
      return {
        type: 'server',
        message: '请求过于频繁，请稍后再试',
        code: status
      }
    }

    // 客户端错误
    if (status >= 400) {
      const errorMsg = error.response?.data?.message || '搜索失败'
      return {
        type: 'unknown',
        message: errorMsg,
        code: status
      }
    }

    // 未知错误
    return {
      type: 'unknown',
      message: error.message || '搜索失败，请稍后重试'
    }
  }

  // 从 URL 同步参数
  const syncFromUrl = () => {
    if (!route.query) return

    if (route.query.keyword) {
      searchParams.keyword = route.query.keyword as string
    } else {
      searchParams.keyword = ''
    }

    if (route.query.region) {
      searchParams.selectedRegion = (route.query.region as string).split(',')
    } else {
      searchParams.selectedRegion = []
    }

    searchParams.checkIn = (route.query.checkIn as string) || null
    searchParams.checkOut = (route.query.checkOut as string) || null

    if (route.query.guestCount) {
      searchParams.guestCount = Number(route.query.guestCount)
    } else {
      searchParams.guestCount = 1
    }

    if (route.query.type) {
      searchParams.propertyType = route.query.type as string
    } else {
      searchParams.propertyType = null
    }

    if (route.query.minPrice) {
      searchParams.minPrice = Number(route.query.minPrice)
    } else {
      searchParams.minPrice = null
    }

    if (route.query.maxPrice) {
      searchParams.maxPrice = Number(route.query.maxPrice)
    } else {
      searchParams.maxPrice = null
    }

    if (route.query.amenities) {
      searchParams.amenities = (route.query.amenities as string).split(',')
    } else {
      searchParams.amenities = []
    }

    if (route.query.minRating) {
      searchParams.minRating = Number(route.query.minRating)
    } else {
      searchParams.minRating = null
    }

    if (route.query.page) {
      currentPage.value = Number(route.query.page)
    } else {
      currentPage.value = 1
    }
  }

  // 同步参数到 URL
  const syncToUrl = (options: { includePage?: boolean } = {}) => {
    const query: Record<string, string | undefined> = {
      keyword: searchParams.keyword || undefined,
      region: searchParams.selectedRegion.length > 0
        ? searchParams.selectedRegion.join(',')
        : undefined,
      checkIn: searchParams.checkIn || undefined,
      checkOut: searchParams.checkOut || undefined,
      guestCount: searchParams.guestCount > 1
        ? String(searchParams.guestCount)
        : undefined,
      type: searchParams.propertyType || undefined,
      minPrice: searchParams.minPrice ? String(searchParams.minPrice) : undefined,
      maxPrice: searchParams.maxPrice ? String(searchParams.maxPrice) : undefined,
      amenities: searchParams.amenities.length > 0
        ? searchParams.amenities.join(',')
        : undefined,
      minRating: searchParams.minRating ? String(searchParams.minRating) : undefined
    }

    if (options.includePage) {
      query.page = String(currentPage.value)
    }

    // 如果是搜索模式，添加 search 标记
    if (isSearchMode.value) {
      query.search = 'true'
    }

    router.replace({ path: '/homestays', query })
  }

  // 更新单个参数
  const updateParam = <K extends keyof SearchState>(key: K, value: SearchState[K]) => {
    searchParams[key] = value
  }

  // 批量更新参数
  const updateParams = (updates: Partial<SearchState>) => {
    Object.assign(searchParams, updates)
  }

  // 更新分页
  const updatePagination = (page?: number, size?: number) => {
    if (page !== undefined) currentPage.value = page
    if (size !== undefined) pageSize.value = size
  }

  // 设置结果
  const setResults = (results: any[], totalCount: number) => {
    searchResults.value = results
    total.value = totalCount
  }

  // 设置加载状态
  const setLoading = (loadingState: boolean) => {
    loading.value = loadingState
  }

  // 重置搜索参数
  const resetSearch = () => {
    Object.assign(searchParams, defaultSearchState)
    currentPage.value = 1
    total.value = 0
    searchResults.value = []
    searchError.value = null
  }

  // 设置完整搜索状态（用于同步组件状态）
  const setSearchState = (state: Partial<SearchState>) => {
    Object.assign(searchParams, state)
  }

  // 初始化：从 URL 恢复状态
  const initFromRoute = () => {
    syncFromUrl()
  }

  return {
    // 状态
    searchParams,
    currentPage,
    pageSize,
    total,
    searchResults,
    loading,
    searchError,
    hasError,

    // 计算属性
    isSearchMode,
    hasActiveSearch,

    // 方法
    clearError,
    setError,
    parseError,
    syncFromUrl,
    syncToUrl,
    updateParam,
    updateParams,
    updatePagination,
    setResults,
    setLoading,
    resetSearch,
    setSearchState,
    initFromRoute,

    // 常量
    SEARCH_TIMEOUT
  }
})
