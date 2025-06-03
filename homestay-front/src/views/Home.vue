<template>
  <div class="home-container">
    <!-- 搜索栏 -->
    <div class="search-container">
      <div class="search-bar">
        <div class="search-item location">
          <div class="label">地区</div>
          <el-cascader v-model="searchParams.selectedRegion" :options="regionOptions" placeholder="选择地区"
            :props="cascaderProps" clearable style="width: 100%;" />
        </div>
        <div class="search-item keyword">
          <div class="label">关键词</div>
          <el-input v-model="searchParams.keyword" placeholder="小区、地标、民宿名…" prefix-icon="Search" clearable />
        </div>
        <div class="search-item check-in">
          <div class="label">入住</div>
          <el-date-picker v-model="searchParams.checkIn" type="date" placeholder="添加日期" format="YYYY/MM/DD" />
        </div>
        <div class="search-item check-out">
          <div class="label">退房</div>
          <el-date-picker v-model="searchParams.checkOut" type="date" placeholder="添加日期" format="YYYY/MM/DD" />
        </div>
        <div class="search-item guests">
          <div class="label">人数</div>
          <el-input-number v-model="searchParams.guestCount" :min="1" :max="20" placeholder="添加人数" />
        </div>
        <el-button type="primary" class="search-button" @click="searchHomestays" :loading="loading">
          <el-icon>
            <Search />
          </el-icon>
        </el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-container">
      <div class="filter-scroll">
        <!-- "全部" 按钮 - 移除图标 -->
        <div class="filter-item" :class="{ active: !searchParams.propertyType }" @click="selectPropertyType(null)">
          <!-- <el-icon><House /></el-icon> -->
          <span>全部</span>
        </div>
        <!-- 动态类型按钮 - 移除图标 -->
        <div v-for="type in fetchedTypes" :key="type.id" class="filter-item"
          :class="{ active: searchParams.propertyType === type.name }" @click="selectPropertyType(type.name)">
          <!-- <img v-if="type.icon" :src="type.icon" :alt="type.name" class="filter-icon-img" /> -->
          <!-- <el-icon v-else><Menu /></el-icon> -->
          <span>{{ type.name }}</span>
        </div>
      </div>

      <div class="filter-actions">
        <el-popover placement="bottom" :width="300" trigger="click">
          <template #reference>
            <el-button class="filter-button">
              <el-icon>
                <Filter />
              </el-icon>
              筛选条件
            </el-button>
          </template>
          <div class="filter-popover">
            <div class="filter-section">
              <h4>价格范围</h4>
              <div class="price-range">
                <el-input-number v-model="searchParams.minPrice" :min="0" :step="100" placeholder="最低价"
                  controls-position="right" style="width: 110px;" />
                <span class="separator">-</span>
                <el-input-number v-model="searchParams.maxPrice" :min="0" :step="100" placeholder="最高价"
                  controls-position="right" style="width: 110px;" />
              </div>
            </div>
            <div class="filter-section">
              <h4>设施</h4>
              <div v-if="amenitiesLoading" class="amenities-loading">加载设施中...</div>
              <el-collapse v-else-if="groupedAmenities.length > 0" class="amenities-collapse">
                <el-collapse-item v-for="category in groupedAmenities" :key="category.code || category.name"
                  :title="category.name" :name="category.code || category.name">
                  <el-checkbox-group v-model="searchParams.amenities" class="amenities-checkbox-group">
                    <el-checkbox v-for="amenity in category.amenities" :key="amenity.value" :label="amenity.value"
                      :value="amenity.value">
                      {{ amenity.label }}
                    </el-checkbox>
                  </el-checkbox-group>
                </el-collapse-item>
              </el-collapse>
              <div v-else class="amenities-empty">暂无设施选项</div>
            </div>
            <div class="filter-actions-footer">
              <el-button @click="resetFilters">重置</el-button>
              <el-button type="primary" @click="applyFilters">应用</el-button>
            </div>
          </div>
        </el-popover>
      </div>
    </div>

    <!-- 在民宿列表之前添加区域标题 -->
    <div class="section-header">
      <h2>推荐民宿</h2>
      <el-button link @click="viewAllFeatured">查看全部</el-button>
    </div>

    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="3" animated />
      <el-skeleton :rows="3" animated />
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="homestays.length === 0" class="empty-result">
      <el-empty description="没有找到符合条件的民宿" />
    </div>

    <div v-else class="homestay-grid">
      <div v-for="homestay in homestays" :key="homestay.id" class="homestay-card"
        @click="viewHomestayDetails(homestay.id)">
        <div class="homestay-image-container" @click.stop="() => { }">
          <el-carousel height="220px" indicator-position="none" arrow="hover">
            <el-carousel-item v-for="(image, index) in getHomestayImages(homestay)" :key="index">
              <img :src="image" :alt="homestay.title" class="homestay-image" />
            </el-carousel-item>
          </el-carousel>
          <div class="favorite-button" @click.stop="toggleFavorite(homestay.id)">
            <el-icon :size="24" :color="isFavorite(homestay.id) ? '#ff385c' : '#fff'">
              <component :is="isFavorite(homestay.id) ? 'Star' : 'StarFilled'" />
            </el-icon>
          </div>
        </div>

        <div class="homestay-info">
          <div class="homestay-header">
            <h3 class="homestay-title">{{ homestay.title }}</h3>
            <div class="homestay-rating" v-if="homestay.rating">
              <el-icon>
                <Star />
              </el-icon>
              <span>{{ homestay.rating ? homestay.rating.toFixed(1) : '暂无评分' }}</span>
            </div>
          </div>

          <div class="homestay-location">{{ getHomestayLocation(homestay) }}</div>
          <div class="homestay-distance" v-if="homestay.distanceFromCenter">
            距离市中心 {{ homestay.distanceFromCenter }} 公里
          </div>
          <div class="homestay-type">{{ getHomestayTypeText(homestay.type) }}</div>
          <div class="homestay-dates">可预订日期</div>
          <div class="homestay-price">
            <span class="price">¥{{ calculatePrice(homestay) }}</span>
            <span class="night">/晚</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加已上架房源区域 -->
    <div class="section-header">
      <h2>最新上架</h2>
      <el-button link @click="viewAllActive">查看全部</el-button>
    </div>

    <div v-if="loadingActive" class="loading-container">
      <el-skeleton :rows="3" animated />
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="activeHomestays.length === 0" class="empty-result">
      <el-empty description="暂无最新上架的民宿" />
    </div>

    <div v-else class="homestay-grid">
      <div v-for="homestay in activeHomestays" :key="homestay.id" class="homestay-card"
        @click="viewHomestayDetails(homestay.id)">
        <div class="homestay-image-container" @click.stop="() => { }">
          <el-carousel height="220px" indicator-position="none" arrow="hover">
            <el-carousel-item v-for="(image, index) in getHomestayImages(homestay)" :key="index">
              <img :src="image" :alt="homestay.title" class="homestay-image" />
            </el-carousel-item>
          </el-carousel>
          <div class="favorite-button" @click.stop="toggleFavorite(homestay.id)">
            <el-icon :size="24" :color="isFavorite(homestay.id) ? '#ff385c' : '#fff'">
              <component :is="isFavorite(homestay.id) ? 'Star' : 'StarFilled'" />
            </el-icon>
          </div>
        </div>

        <div class="homestay-info">
          <div class="homestay-header">
            <h3 class="homestay-title">{{ homestay.title }}</h3>
            <div class="homestay-rating" v-if="homestay.rating">
              <el-icon>
                <Star />
              </el-icon>
              <span>{{ homestay.rating ? homestay.rating.toFixed(1) : '暂无评分' }}</span>
            </div>
          </div>

          <div class="homestay-location">{{ getHomestayLocation(homestay) }}</div>
          <div class="homestay-features">
            <span>{{ homestay.maxGuests }}人</span>
            <span>{{ getHomestayTypeText(homestay.type) }}</span>
          </div>
          <div class="homestay-price">
            <span class="price">¥{{ calculatePrice(homestay) }}</span>
            <span class="night">/晚</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Star, StarFilled, Filter, Location, House, Ship, Umbrella, Dessert, Bicycle, Sunrise, Menu } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { regionData, codeToText } from 'element-china-area-data'
import request from '@/utils/request'
import {
  getActiveHomestays,
  getHomestayTypes,
  type HomestayType,
  getAvailableAmenitiesGrouped,
  type AmenityOption,
  type AmenityCategoryOption
} from '@/api/homestay'

// 定义类型
interface Homestay {
  id: number
  title: string
  description: string
  pricePerNight: number
  price?: string
  maxGuests: number
  bedrooms?: number
  beds?: number
  bathrooms?: number
  amenities: string[]
  images: string[]
  rating?: number
  reviewCount?: number
  type: string
  status: string
  featured: boolean
  propertyType?: string
  distanceFromCenter?: number
  latitude?: number
  longitude?: number
  hostName?: string
  hostId?: number
  coverImage?: string
  provinceCode?: string
  cityCode?: string
  districtCode?: string
  addressDetail?: string
}

const router = useRouter()
const loading = ref(false)
const homestays = ref<Homestay[]>([])
const favorites = ref<number[]>([])
const activeHomestays = ref<Homestay[]>([])
const loadingActive = ref(false)

const fetchedTypes = ref<HomestayType[]>([])
const typesLoading = ref(false)
const availableAmenities = ref<AmenityOption[]>([])
const groupedAmenities = ref<AmenityCategoryOption[]>([])
const amenitiesLoading = ref(false)

const searchParams = reactive({
  selectedRegion: [] as string[],
  keyword: '',
  checkIn: null,
  checkOut: null,
  guestCount: 1,
  minPrice: null,
  maxPrice: null,
  propertyType: null as string | null,
  amenities: [] as string[],
  minRating: null
})

// 地区选择器选项
const regionOptions = regionData

// 地区选择器配置
const cascaderProps = {
  value: 'value',
  label: 'label',
  children: 'children',
  checkStrictly: true
}

onMounted(async () => {
  await Promise.all([
    fetchFeaturedHomestays(),
    fetchActiveHomestays(),
    fetchHomestayTypes(),
    fetchGroupedAmenities()
  ])
  loadFavorites()
})

const fetchFeaturedHomestays = async () => {
  loading.value = true
  try {
    const response = await request.get('/api/homestays/featured')
    homestays.value = response.data
  } catch (error) {
    console.error('获取推荐民宿失败:', error)
    ElMessage.error('获取推荐民宿失败')

    // 使用模拟数据（当后端服务未启动时）
    if (process.env.NODE_ENV === 'development') {
      console.log('使用模拟数据')
      homestays.value = [
        {
          id: 1,
          title: '湖畔小屋',
          description: '美丽的湖畔小屋，宁静舒适',
          pricePerNight: 488,
          price: '488',
          maxGuests: 2,
          bedrooms: 1,
          beds: 1,
          bathrooms: 1,
          amenities: ['WiFi', '空调', '厨房', '停车位'],
          images: ['https://picsum.photos/800/600?random=1'],
          rating: 4.9,
          reviewCount: 128,
          type: 'SHARED',
          status: 'ACTIVE',
          featured: true,
          propertyType: '小屋',
          distanceFromCenter: 1.5,
          latitude: 30.2636,
          longitude: 120.1686,
          hostName: '张三',
          hostId: 1001,
          coverImage: 'https://picsum.photos/800/600?random=1',
          provinceCode: '330000',
          cityCode: '330100',
          districtCode: '330106',
          addressDetail: '西湖区某街道'
        },
        {
          id: 2,
          title: '海景公寓',
          description: '一线海景，舒适公寓',
          pricePerNight: 688,
          price: '688',
          maxGuests: 4,
          bedrooms: 2,
          beds: 2,
          bathrooms: 1,
          amenities: ['WiFi', '空调', '厨房', '停车位', '游泳池'],
          images: ['https://picsum.photos/800/600?random=2'],
          rating: 4.8,
          reviewCount: 92,
          type: 'ENTIRE',
          status: 'ACTIVE',
          featured: true,
          propertyType: '公寓',
          distanceFromCenter: 2.0,
          latitude: 24.4459,
          longitude: 118.0657,
          hostName: '李四',
          hostId: 1002,
          coverImage: 'https://picsum.photos/800/600?random=2',
          provinceCode: '350000',
          cityCode: '350200',
          districtCode: '350203',
          addressDetail: '思明区某街道'
        },
        {
          id: 3,
          title: '山间小木屋',
          description: '清新的山间小木屋，远离城市喧嚣',
          pricePerNight: 588,
          price: '588',
          maxGuests: 3,
          bedrooms: 1,
          beds: 2,
          bathrooms: 1,
          amenities: ['WiFi', '空调', '厨房', '停车位', '烧烤架'],
          images: ['https://picsum.photos/800/600?random=3'],
          rating: 4.7,
          reviewCount: 78,
          type: 'PRIVATE',
          status: 'ACTIVE',
          featured: true,
          propertyType: '木屋',
          distanceFromCenter: 5.0,
          latitude: 30.5603,
          longitude: 119.8808,
          hostName: '王五',
          hostId: 1003,
          coverImage: 'https://picsum.photos/800/600?random=3',
          provinceCode: '330000',
          cityCode: '330500',
          districtCode: '330523',
          addressDetail: '德清县某街道'
        }
      ]
    }
  } finally {
    loading.value = false
  }
}

const fetchActiveHomestays = async () => {
  loadingActive.value = true
  try {
    const response = await getActiveHomestays({ size: 6 })
    if (response.data?.data) {
      // 确保不显示重复房源
      const activeHomestayData = response.data.data;
      const featuredIds = homestays.value.map(h => h.id);
      activeHomestays.value = activeHomestayData.filter((home: Homestay) => !featuredIds.includes(home.id));
    } else if (response.data) {
      const activeHomestayData = response.data;
      const featuredIds = homestays.value.map(h => h.id);
      activeHomestays.value = activeHomestayData.filter((home: Homestay) => !featuredIds.includes(home.id));
    } else {
      activeHomestays.value = []
    }
  } catch (error) {
    console.error('获取已上架房源失败:', error)
  } finally {
    loadingActive.value = false
  }
}

const fetchHomestayTypes = async () => {
  typesLoading.value = true
  try {
    const response = await getHomestayTypes()
    fetchedTypes.value = response || []
    console.log("Fetched homestay types:", fetchedTypes.value)
  } catch (error) {
    console.error("获取房源类型失败:", error)
    ElMessage.error("加载房源类型失败")
    fetchedTypes.value = []
  } finally {
    typesLoading.value = false
  }
}

const fetchGroupedAmenities = async () => {
  amenitiesLoading.value = true;
  try {
    groupedAmenities.value = await getAvailableAmenitiesGrouped();
  } catch (error) {
    groupedAmenities.value = [];
  } finally {
    amenitiesLoading.value = false;
  }
}

const searchHomestays = async () => {
  loading.value = true
  try {
    // 1. 从 selectedRegion 提取地区编码
    const regionCodes = searchParams.selectedRegion
    const provinceCode = regionCodes && regionCodes.length > 0 ? regionCodes[0] : null
    const cityCode = regionCodes && regionCodes.length > 1 ? regionCodes[1] : null
    const districtCode = regionCodes && regionCodes.length > 2 ? regionCodes[2] : null

    // 2. 创建后端期望的数据结构
    const backendRequestData: Record<string, any> = {
      provinceCode: provinceCode,
      cityCode: cityCode,
      districtCode: districtCode,
      keyword: searchParams.keyword || null,
      propertyType: searchParams.propertyType || null,
      minPrice: searchParams.minPrice,
      maxPrice: searchParams.maxPrice,
      checkInDate: searchParams.checkIn,
      checkOutDate: searchParams.checkOut,
      minGuests: searchParams.guestCount > 0 ? searchParams.guestCount : null,
      requiredAmenities: searchParams.amenities && searchParams.amenities.length > 0 ? searchParams.amenities : null,
    };

    console.log("准备发送的搜索参数 (地区选择器+关键词):", backendRequestData)

    // 3. 发送请求
    const response = await request.post('/api/homestays/search', backendRequestData)

    // 4. 处理响应 (保持不变，确保能处理后端返回的 DTO)
    const backendHomestays = response.data as any[];
    homestays.value = backendHomestays.map(dto => ({
      ...dto,
      amenities: dto.amenities ? dto.amenities.map((a: any) => a.label || a.value || a) : []
    })) as Homestay[];

    if (homestays.value.length === 0) {
      ElMessage.info('没有找到符合条件的民宿')
    }
  } catch (error) {
    console.error('搜索民宿失败:', error)
    ElMessage.error('搜索民宿失败')

    // 使用模拟数据（当后端服务未启动时）
    if (process.env.NODE_ENV === 'development') {
      console.log('使用模拟数据进行搜索')
      // 模拟搜索结果
      const mockData: Homestay[] = [
        {
          id: 1,
          title: '大理古城树屋',
          description: '位于大理古城的特色树屋，俯瞰洱海美景',
          pricePerNight: 688,
          maxGuests: 2,
          bedrooms: 1,
          beds: 1,
          bathrooms: 1,
          amenities: ['WiFi', '空调', '厨房', '洗衣机'],
          images: ['https://picsum.photos/800/600?random=1'],
          rating: 4.9,
          reviewCount: 128,
          type: 'SHARED',
          status: 'ACTIVE',
          featured: true,
          propertyType: '树屋',
          distanceFromCenter: 2.5,
          latitude: 25.606486,
          longitude: 100.267638,
          hostName: '李明',
          hostId: 1001,
          coverImage: 'https://picsum.photos/800/600?random=1',
          provinceCode: '530000',
          cityCode: '532900',
          districtCode: '532922',
          addressDetail: '大理古城某街道'
        },
        {
          id: 2,
          title: '杭州山顶树屋',
          description: '杭州山顶的豪华树屋，可以俯瞰西湖全景',
          pricePerNight: 1288,
          maxGuests: 4,
          bedrooms: 2,
          beds: 2,
          bathrooms: 2,
          amenities: ['WiFi', '空调', '厨房', '停车位', '游泳池'],
          images: ['https://picsum.photos/800/600?random=2'],
          rating: 4.8,
          reviewCount: 96,
          type: 'ENTIRE',
          status: 'ACTIVE',
          featured: true,
          propertyType: '树屋',
          distanceFromCenter: 5.2,
          latitude: 30.259924,
          longitude: 120.130742,
          hostName: '张伟',
          hostId: 1002,
          coverImage: 'https://picsum.photos/800/600?random=2',
          provinceCode: '330000',
          cityCode: '330100',
          districtCode: '330106',
          addressDetail: '西湖区某街道'
        },
        {
          id: 3,
          title: '三亚海景房',
          description: '三亚湾一线海景房，步行5分钟到海滩',
          pricePerNight: 1688,
          maxGuests: 6,
          bedrooms: 3,
          beds: 3,
          bathrooms: 2,
          amenities: ['WiFi', '空调', '厨房', '洗衣机', '停车位', '游泳池'],
          images: ['https://picsum.photos/800/600?random=3'],
          rating: 4.7,
          reviewCount: 215,
          type: 'ENTIRE',
          status: 'ACTIVE',
          featured: true,
          propertyType: '海景房',
          distanceFromCenter: 1.8,
          latitude: 18.252847,
          longitude: 109.511909,
          hostName: '王芳',
          hostId: 1003,
          coverImage: 'https://picsum.photos/800/600?random=3',
          provinceCode: '460000',
          cityCode: '460200',
          districtCode: '460202',
          addressDetail: '三亚湾某街道'
        },
        {
          id: 4,
          title: '吉林雪山脚下的小木屋',
          description: '位于吉林雪山脚下的温馨小木屋，冬季可滑雪',
          pricePerNight: 888,
          maxGuests: 4,
          bedrooms: 2,
          beds: 2,
          bathrooms: 1,
          amenities: ['WiFi', '空调', '厨房', '壁炉'],
          images: ['https://picsum.photos/800/600?random=4'],
          rating: 4.6,
          reviewCount: 78,
          type: 'PRIVATE',
          status: 'ACTIVE',
          featured: true,
          propertyType: '小木屋',
          distanceFromCenter: 15.0,
          latitude: 42.032758,
          longitude: 127.505062,
          hostName: '赵强',
          hostId: 1004,
          coverImage: 'https://picsum.photos/800/600?random=4',
          provinceCode: '220000',
          cityCode: '220200',
          districtCode: '220202',
          addressDetail: '长白山区某街道'
        }
      ]

      // 根据搜索条件过滤模拟数据
      homestays.value = mockData.filter(homestay => {
        // 按地区编码筛选
        const regionCodes = searchParams.selectedRegion
        if (regionCodes && regionCodes.length > 0) {
          if (regionCodes[0] && homestay.provinceCode !== regionCodes[0]) return false;
          if (regionCodes[1] && homestay.cityCode !== regionCodes[1]) return false;
          if (regionCodes[2] && homestay.districtCode !== regionCodes[2]) return false;
        }

        // 按关键词筛选 (标题、描述、详细地址)
        if (searchParams.keyword &&
          !(homestay.title?.includes(searchParams.keyword) ||
            homestay.description?.includes(searchParams.keyword) ||
            homestay.addressDetail?.includes(searchParams.keyword))) {
          return false
        }

        // 按价格筛选
        if (searchParams.minPrice && homestay.pricePerNight < searchParams.minPrice) {
          return false
        }
        if (searchParams.maxPrice && homestay.pricePerNight > searchParams.maxPrice) {
          return false
        }

        // 按人数筛选
        if (searchParams.guestCount && homestay.maxGuests < searchParams.guestCount) {
          return false
        }

        // 按房型筛选
        if (searchParams.propertyType && homestay.propertyType !== searchParams.propertyType) {
          return false
        }

        // 按设施筛选
        if (searchParams.amenities && searchParams.amenities.length > 0) {
          for (const amenity of searchParams.amenities) {
            if (!homestay.amenities.includes(amenity)) {
              return false
            }
          }
        }

        return true
      })

      if (homestays.value.length === 0) {
        ElMessage.info('没有找到符合条件的民宿')
      }
    }
  } finally {
    loading.value = false
  }
}

const selectPropertyType = (typeName: string | null) => {
  console.log('[Home.vue] selectPropertyType - typeName:', typeName);
  searchParams.propertyType = typeName
  searchHomestays()
}

const resetFilters = () => {
  searchParams.minPrice = null
  searchParams.maxPrice = null
  searchParams.amenities = []
}

const applyFilters = () => {
  searchHomestays()
}

const calculatePrice = (homestay: any) => {
  // 添加适配逻辑处理不同的价格字段
  return homestay.price || homestay.pricePerNight || 0;
}

const getHomestayImages = (homestay: Homestay) => {
  const defaultImage = 'https://picsum.photos/800/600?random=' + homestay.id;

  // 首页应该始终优先使用封面图片
  if (homestay.coverImage) {
    //console.log(`房源ID ${homestay.id} 使用封面图片作为首页展示:`, homestay.coverImage);
    return [processImageUrl(homestay.coverImage, homestay.id)];
  }

  // 仅在没有封面图片时才使用图片集
  if (homestay.images && homestay.images.length > 0) {
    console.log(`房源ID ${homestay.id} 没有封面图片，使用图片集:`, homestay.images);
    const validImages = homestay.images.filter(image => image !== null && image !== undefined && image !== '');
    if (validImages.length > 0) {
      return [processImageUrl(validImages[0], homestay.id)];
    }
  }

  // 都没有时使用默认图片
  console.log(`房源ID ${homestay.id} 没有任何有效图片，使用默认图片`);
  return [defaultImage];
}

// 处理图片URL的辅助函数
const processImageUrl = (image: string, homestayId: number) => {
  if (!image) {
    console.warn(`房源ID ${homestayId} 的图片路径为空，使用默认图片`);
    return 'https://picsum.photos/800/600?random=' + homestayId;
  }

  try {
    // 处理不同类型的图片路径
    if (typeof image === 'string') {
      // 如果已经是完整URL则直接返回
      if (image.startsWith('http')) {
        return image;
      }

      // 防止路径重复
      if (image.includes('/api/api/')) {
        // 修复重复的/api/前缀
        image = image.replace('/api/api/', '/api/');
      }

      // 处理相对于后端的路径
      if (image.startsWith('/uploads/')) {
        return `/api${image}`;
      }
      // 处理avatars目录的图片
      else if (image.startsWith('/avatars/')) {
        return `/api/uploads${image}`;
      }
      // 处理homestays目录下的图片
      else if (image.startsWith('/homestays/')) {
        return `/api${image}`;
      }
      // 处理直接是文件名的情况
      else if (!image.startsWith('/')) {
        // 确保文件名有效
        const filename = image.split('/').pop();
        if (!filename || filename.trim() === '') {
          return 'https://picsum.photos/800/600?random=' + homestayId;
        }

        if (image.includes('avatars')) {
          return `/api/uploads/avatars/${filename}`;
        } else {
          return `/api/uploads/homestays/${filename}`;
        }
      }
      // 其他以/开头的路径
      else {
        return `/api${image}`;
      }
    }
  } catch (error) {
    console.error(`处理图片URL时出错:`, error);
  }

  // 最后的默认情况
  return 'https://picsum.photos/800/600?random=' + homestayId;
}

const viewHomestayDetails = (id: number) => {
  router.push(`/homestays/${id}`)
}

const toggleFavorite = (id: number) => {
  if (isFavorite(id)) {
    favorites.value = favorites.value.filter(fav => fav !== id)
  } else {
    favorites.value.push(id)
  }
  saveFavorites()
}

const isFavorite = (id: number) => {
  return favorites.value.includes(id)
}

const loadFavorites = () => {
  const saved = localStorage.getItem('favorites')
  if (saved) {
    favorites.value = JSON.parse(saved)
  }
}

const saveFavorites = () => {
  localStorage.setItem('favorites', JSON.stringify(favorites.value))
}

const viewAllFeatured = () => {
  router.push('/homestay/list?featured=true')
}

const viewAllActive = () => {
  router.push('/homestay/list?status=ACTIVE')
}

const getHomestayTypeText = (typeCode: string | undefined): string => {
  if (!typeCode) return '未知类型';

  // 从已获取的 fetchedTypes 列表中查找
  // fetchedTypes 列表包含 code 和 name, 房源数据中的 type 字段是 code
  // 因此，应该使用 code 进行匹配
  const foundType = fetchedTypes.value.find(t => t.code === typeCode);

  if (foundType) {
    // 如果找到匹配的 code，返回对应的 name (中文名称)
    return foundType.name || typeCode; // Fallback to code if name is somehow missing
  }

  // 如果在列表中找不到匹配的 code
  console.warn(`[Home.vue] 未能在 fetchedTypes 列表中找到类型代码: ${typeCode}`);
  // 返回原始代码，以便调试
  return typeCode;
};

const getHomestayLocation = (homestay: Homestay): string => {
  const parts = [];
  if (homestay.provinceCode && codeToText[homestay.provinceCode]) {
    parts.push(codeToText[homestay.provinceCode]);
  }
  if (homestay.cityCode && codeToText[homestay.cityCode]) {
    // 避免重复省份（如直辖市）
    if (!parts.includes(codeToText[homestay.cityCode])) {
      parts.push(codeToText[homestay.cityCode]);
    }
  }
  // 区县信息通常不需要在首页卡片显示，保持简洁
  // if (homestay.districtCode && codeToText[homestay.districtCode]) {
  //     parts.push(codeToText[homestay.districtCode]);
  // }

  if (parts.length > 0) {
    return parts.join(' · ');
  }

  // // 兼容旧数据 (如果需要)
  // if (homestay.city && homestay.country) return `${homestay.city}, ${homestay.country}`;
  // if (homestay.location) return homestay.location;

  return '位置待更新';
};
</script>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.search-container {
  margin-bottom: 24px;
  border-radius: 40px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  background-color: white;
  position: sticky;
  top: 0;
  z-index: 100;
  padding: 10px 0;
}

.search-bar {
  display: flex;
  align-items: center;
  height: 66px;
  padding: 0 8px;
}

.search-item {
  flex: 1;
  padding: 0 16px;
  border-right: 1px solid #ebebeb;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.search-item:last-of-type {
  border-right: none;
}

.search-item .label {
  font-weight: 600;
  font-size: 12px;
  margin-bottom: 4px;
}

/* 为人数选择的 el-input-number 设置特定样式 */
.search-item.guests .el-input-number {
  width: 100%;
  /* 尝试让其填满父容器，或者指定一个具体像素值如 120px */
}

/* 确保 el-input-number 内部的 input 元素正常显示 */
.search-item.guests .el-input-number .el-input__inner {
  text-align: center;
  /* 数字居中显示 */
}

.search-button {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 8px;
}

.filter-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebebeb;
}

.filter-scroll {
  display: flex;
  overflow-x: auto;
  gap: 24px;
  padding-bottom: 8px;
  flex: 1;
}

.filter-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0.7;
  transition: opacity 0.2s;
  min-width: 56px;
  padding: 8px 0;
}

.filter-item:hover,
.filter-item.active {
  opacity: 1;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.filter-button {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-popover {
  padding: 16px;
}

.filter-section {
  margin-bottom: 20px;
}

.filter-section h4 {
  margin-top: 0;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.price-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-range .el-input-number {
  width: 110px;
}

.separator {
  color: #909399;
  margin: 0 5px;
}

.amenities-collapse {
  border-top: none;
  border-bottom: none;
}

.amenities-collapse .el-collapse-item__header {
  font-size: 14px;
  font-weight: 600;
  border-bottom: 1px solid #ebebeb;
  height: 40px;
  line-height: 40px;
}

.amenities-collapse .el-collapse-item__wrap {
  border-bottom: none;
}

.amenities-collapse .el-collapse-item__content {
  padding-top: 10px;
  padding-bottom: 10px;
}

.amenities-checkbox-group {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px 16px;
}

.amenities-checkbox-group .el-checkbox {
  margin-right: 0;
  height: 26px;
  display: flex;
  align-items: center;
}

.amenities-checkbox-group .el-checkbox__label {
  font-size: 14px;
}

.amenities-loading,
.amenities-empty {
  color: #909399;
  font-size: 14px;
  padding: 15px 0;
  text-align: center;
}

.filter-actions-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #ebebeb;
}

.homestay-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.homestay-card {
  border-radius: 12px;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: pointer;
}

.homestay-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.homestay-image-container {
  position: relative;
  height: 220px;
}

.homestay-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.favorite-button {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 10;
  cursor: pointer;
  background-color: rgba(0, 0, 0, 0.3);
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.homestay-info {
  padding: 16px;
}

.homestay-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.homestay-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.homestay-rating {
  display: flex;
  align-items: center;
  gap: 4px;
}

.homestay-location,
.homestay-distance,
.homestay-dates,
.homestay-type {
  color: #717171;
  font-size: 14px;
  margin-bottom: 4px;
}

.homestay-price {
  margin-top: 8px;
  font-size: 16px;
}

.homestay-price .price {
  font-weight: 600;
}

.homestay-price .night {
  font-weight: normal;
}

.homestay-features {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
}

.homestay-features span {
  margin-bottom: 0;
  display: inline;
}

.loading-container {
  padding: 24px;
}

.empty-result {
  padding: 48px 0;
  text-align: center;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .search-bar {
    flex-direction: column;
    height: auto;
    padding: 16px;
  }

  .search-item {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #ebebeb;
    padding: 12px 0;
  }

  .search-button {
    margin-top: 16px;
    width: 100%;
    border-radius: 8px;
    height: 48px;
  }

  .homestay-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  }
}

/* 确保 el-input-number 的按钮可见 */
:deep(.el-input-number__decrease),
:deep(.el-input-number__increase) {
  display: inline-flex;
  /* 或者 block, 根据需要调整，确保不是 none */
}
</style>