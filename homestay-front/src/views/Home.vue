<template>
  <div class="home-container">
    <!-- 搜索栏 -->
    <div class="search-container">
      <div class="search-bar">
        <div class="search-item location">
          <div class="label">地点</div>
          <el-input v-model="searchParams.location" placeholder="搜索目的地" prefix-icon="Location" clearable />
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
        <div v-for="(type, index) in propertyTypes" :key="index" class="filter-item"
          :class="{ active: searchParams.propertyType === type.value }" @click="selectPropertyType(type.value)">
          <el-icon>
            <component :is="type.icon" />
          </el-icon>
          <span>{{ type.label }}</span>
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
                <el-input-number v-model="searchParams.minPrice" :min="0" :step="100" placeholder="最低价" />
                <span class="separator">-</span>
                <el-input-number v-model="searchParams.maxPrice" :min="0" :step="100" placeholder="最高价" />
              </div>
            </div>
            <div class="filter-section">
              <h4>房间和床铺</h4>
              <div class="room-filters">
                <div class="room-filter-item">
                  <span>卧室</span>
                  <el-input-number v-model="searchParams.bedrooms" :min="1" :max="10" :step="1" />
                </div>
                <div class="room-filter-item">
                  <span>床铺</span>
                  <el-input-number v-model="searchParams.beds" :min="1" :max="20" :step="1" />
                </div>
                <div class="room-filter-item">
                  <span>卫生间</span>
                  <el-input-number v-model="searchParams.bathrooms" :min="1" :max="10" :step="1" />
                </div>
              </div>
            </div>
            <div class="filter-section">
              <h4>设施</h4>
              <div class="amenities-list">
                <el-checkbox-group v-model="searchParams.amenities">
                  <el-checkbox label="WiFi">WiFi</el-checkbox>
                  <el-checkbox label="空调">空调</el-checkbox>
                  <el-checkbox label="厨房">厨房</el-checkbox>
                  <el-checkbox label="洗衣机">洗衣机</el-checkbox>
                  <el-checkbox label="停车位">停车位</el-checkbox>
                  <el-checkbox label="游泳池">游泳池</el-checkbox>
                </el-checkbox-group>
              </div>
            </div>
            <div class="filter-actions">
              <el-button @click="resetFilters">重置</el-button>
              <el-button type="primary" @click="applyFilters">应用</el-button>
            </div>
          </div>
        </el-popover>

        <el-switch v-model="showPriceWithTax" active-text="显示税费后价格" inactive-text="显示原价" class="price-switch" />
      </div>
    </div>

    <!-- 民宿列表 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="3" animated />
      <el-skeleton :rows="3" animated />
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="homestays.length === 0" class="empty-result">
      <el-empty description="没有找到符合条件的民宿" />
    </div>

    <div v-else class="homestay-grid">
      <div v-for="homestay in homestays" :key="homestay.id" class="homestay-card">
        <div class="homestay-image-container">
          <el-carousel height="220px" indicator-position="none" arrow="hover">
            <el-carousel-item v-for="(image, index) in getHomestayImages(homestay)" :key="index">
              <img :src="image" :alt="homestay.title" class="homestay-image" />
            </el-carousel-item>
          </el-carousel>
          <div class="favorite-button" @click="toggleFavorite(homestay.id)">
            <el-icon :size="24" :color="isFavorite(homestay.id) ? '#ff385c' : '#fff'">
              <component :is="isFavorite(homestay.id) ? 'Star' : 'StarFilled'" />
            </el-icon>
          </div>
        </div>

        <div class="homestay-info" @click="viewHomestayDetails(homestay.id)">
          <div class="homestay-header">
            <h3 class="homestay-title">{{ homestay.title }}</h3>
            <div class="homestay-rating">
              <el-icon>
                <Star />
              </el-icon>
              <span>{{ homestay.rating.toFixed(1) }}</span>
            </div>
          </div>

          <div class="homestay-location">{{ homestay.city }}, {{ homestay.country }}</div>
          <div class="homestay-distance">距离市中心{{ homestay.distanceFromCenter }}公里</div>
          <div class="homestay-dates">可预订日期</div>
          <div class="homestay-price">
            <span class="price">¥{{ calculatePrice(homestay.pricePerNight) }}</span>
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
import { Search, Star, StarFilled, Filter, Location, House, Ship, Umbrella, Dessert, Bicycle, Sunrise } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

// 定义类型
interface Homestay {
  id: number;
  title: string;
  description: string;
  location: string;
  city: string;
  country: string;
  pricePerNight: number;
  maxGuests: number;
  bedrooms: number;
  beds: number;
  bathrooms: number;
  amenities: string[];
  images: string[];
  rating: number;
  reviewCount: number;
  latitude: number;
  longitude: number;
  hostName: string;
  hostId?: number;
  featured: boolean;
  propertyType: string;
  distanceFromCenter: number;
}

const router = useRouter()
const loading = ref(false)
const homestays = ref<Homestay[]>([])
const favorites = ref<number[]>([])
const showPriceWithTax = ref(false)

const propertyTypes = [
  { label: '全部', value: '', icon: 'House' },
  { label: '树屋', value: '树屋', icon: 'Umbrella' },
  { label: '海景房', value: '海景房', icon: 'Ship' },
  { label: '小木屋', value: '小木屋', icon: 'House' },
  { label: '湖景房', value: '湖景房', icon: 'Sunrise' },
  { label: '公寓', value: '公寓', icon: 'House' },
  { label: '别墅', value: '别墅', icon: 'House' },
  { label: '城堡', value: '城堡', icon: 'House' },
  { label: '农场', value: '农场', icon: 'Bicycle' },
  { label: '豪宅', value: '豪宅', icon: 'House' },
  { label: '岛屿', value: '岛屿', icon: 'Ship' },
  { label: '露营', value: '露营', icon: 'Umbrella' }
]

const searchParams = reactive({
  location: '',
  checkIn: null,
  checkOut: null,
  guestCount: 1,
  minPrice: null,
  maxPrice: null,
  propertyType: '',
  amenities: [],
  bedrooms: null,
  beds: null,
  bathrooms: null,
  minRating: null
})

onMounted(async () => {
  await fetchFeaturedHomestays()
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
          title: '大理古城树屋',
          description: '位于大理古城的特色树屋，俯瞰洱海美景',
          location: '云南大理',
          city: '大理',
          country: '中国',
          pricePerNight: 688,
          maxGuests: 2,
          bedrooms: 1,
          beds: 1,
          bathrooms: 1,
          amenities: ['WiFi', '空调', '厨房', '洗衣机'],
          images: ['https://picsum.photos/800/600?random=1'],
          rating: 4.9,
          reviewCount: 128,
          latitude: 25.606486,
          longitude: 100.267638,
          hostName: '李明',
          featured: true,
          propertyType: '树屋',
          distanceFromCenter: 2.5
        },
        {
          id: 2,
          title: '杭州山顶树屋',
          description: '杭州山顶的豪华树屋，可以俯瞰西湖全景',
          location: '浙江杭州',
          city: '杭州',
          country: '中国',
          pricePerNight: 1288,
          maxGuests: 4,
          bedrooms: 2,
          beds: 2,
          bathrooms: 2,
          amenities: ['WiFi', '空调', '厨房', '停车位', '游泳池'],
          images: ['https://picsum.photos/800/600?random=2'],
          rating: 4.8,
          reviewCount: 96,
          latitude: 30.259924,
          longitude: 120.130742,
          hostName: '张伟',
          featured: true,
          propertyType: '树屋',
          distanceFromCenter: 5.2
        },
        {
          id: 3,
          title: '三亚海景房',
          description: '三亚湾一线海景房，步行5分钟到海滩',
          location: '海南三亚',
          city: '三亚',
          country: '中国',
          pricePerNight: 1688,
          maxGuests: 6,
          bedrooms: 3,
          beds: 3,
          bathrooms: 2,
          amenities: ['WiFi', '空调', '厨房', '洗衣机', '停车位', '游泳池'],
          images: ['https://picsum.photos/800/600?random=3'],
          rating: 4.7,
          reviewCount: 215,
          latitude: 18.252847,
          longitude: 109.511909,
          hostName: '王芳',
          featured: true,
          propertyType: '海景房',
          distanceFromCenter: 1.8
        }
      ]
    }
  } finally {
    loading.value = false
  }
}

const searchHomestays = async () => {
  loading.value = true
  try {
    const response = await request.post('/api/homestays/search', searchParams)
    homestays.value = response.data
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
      const mockData = [
        {
          id: 1,
          title: '大理古城树屋',
          description: '位于大理古城的特色树屋，俯瞰洱海美景',
          location: '云南大理',
          city: '大理',
          country: '中国',
          pricePerNight: 688,
          maxGuests: 2,
          bedrooms: 1,
          beds: 1,
          bathrooms: 1,
          amenities: ['WiFi', '空调', '厨房', '洗衣机'],
          images: ['https://picsum.photos/800/600?random=1'],
          rating: 4.9,
          reviewCount: 128,
          latitude: 25.606486,
          longitude: 100.267638,
          hostName: '李明',
          featured: true,
          propertyType: '树屋',
          distanceFromCenter: 2.5
        },
        {
          id: 2,
          title: '杭州山顶树屋',
          description: '杭州山顶的豪华树屋，可以俯瞰西湖全景',
          location: '浙江杭州',
          city: '杭州',
          country: '中国',
          pricePerNight: 1288,
          maxGuests: 4,
          bedrooms: 2,
          beds: 2,
          bathrooms: 2,
          amenities: ['WiFi', '空调', '厨房', '停车位', '游泳池'],
          images: ['https://picsum.photos/800/600?random=2'],
          rating: 4.8,
          reviewCount: 96,
          latitude: 30.259924,
          longitude: 120.130742,
          hostName: '张伟',
          featured: true,
          propertyType: '树屋',
          distanceFromCenter: 5.2
        },
        {
          id: 3,
          title: '三亚海景房',
          description: '三亚湾一线海景房，步行5分钟到海滩',
          location: '海南三亚',
          city: '三亚',
          country: '中国',
          pricePerNight: 1688,
          maxGuests: 6,
          bedrooms: 3,
          beds: 3,
          bathrooms: 2,
          amenities: ['WiFi', '空调', '厨房', '洗衣机', '停车位', '游泳池'],
          images: ['https://picsum.photos/800/600?random=3'],
          rating: 4.7,
          reviewCount: 215,
          latitude: 18.252847,
          longitude: 109.511909,
          hostName: '王芳',
          featured: true,
          propertyType: '海景房',
          distanceFromCenter: 1.8
        },
        {
          id: 4,
          title: '吉林雪山脚下的小木屋',
          description: '位于吉林雪山脚下的温馨小木屋，冬季可滑雪',
          location: '吉林长白山',
          city: '吉林',
          country: '中国',
          pricePerNight: 888,
          maxGuests: 4,
          bedrooms: 2,
          beds: 2,
          bathrooms: 1,
          amenities: ['WiFi', '空调', '厨房', '壁炉'],
          images: ['https://picsum.photos/800/600?random=4'],
          rating: 4.6,
          reviewCount: 78,
          latitude: 42.032758,
          longitude: 127.505062,
          hostName: '赵强',
          featured: true,
          propertyType: '小木屋',
          distanceFromCenter: 15.0
        }
      ]

      // 根据搜索条件过滤模拟数据
      homestays.value = mockData.filter(homestay => {
        // 按位置筛选
        if (searchParams.location && !homestay.location.includes(searchParams.location)) {
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

        // 按卧室数筛选
        if (searchParams.bedrooms && homestay.bedrooms < searchParams.bedrooms) {
          return false
        }

        // 按床数筛选
        if (searchParams.beds && homestay.beds < searchParams.beds) {
          return false
        }

        // 按卫生间数筛选
        if (searchParams.bathrooms && homestay.bathrooms < searchParams.bathrooms) {
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

const selectPropertyType = (type: string) => {
  searchParams.propertyType = type
  searchHomestays()
}

const resetFilters = () => {
  searchParams.minPrice = null
  searchParams.maxPrice = null
  searchParams.bedrooms = null
  searchParams.beds = null
  searchParams.bathrooms = null
  searchParams.amenities = []
}

const applyFilters = () => {
  searchHomestays()
}

const calculatePrice = (price: number) => {
  if (showPriceWithTax.value) {
    // 假设税费是10%
    return Math.round(price * 1.1)
  }
  return price
}

const getHomestayImages = (homestay: Homestay) => {
  // 如果没有图片，使用默认图片
  if (!homestay.images || homestay.images.length === 0) {
    return ['/images/default-homestay.jpg']
  }

  // 处理图片路径
  return homestay.images.map((image: string) => {
    // 如果图片路径已经是完整URL，直接返回
    if (image.startsWith('http')) {
      return image
    }
    // 否则使用占位图片
    return `https://picsum.photos/800/600?random=${homestay.id}-${homestay.images.indexOf(image)}`
  })
}

const viewHomestayDetails = (id: number) => {
  router.push(`/homestay/${id}`)
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
  cursor: pointer;
  opacity: 0.7;
  transition: opacity 0.2s;
  min-width: 56px;
}

.filter-item:hover,
.filter-item.active {
  opacity: 1;
}

.filter-item .el-icon {
  font-size: 24px;
  margin-bottom: 8px;
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
  margin-bottom: 16px;
}

.filter-section h4 {
  margin-top: 0;
  margin-bottom: 12px;
}

.price-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.separator {
  color: #909399;
}

.room-filters {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.room-filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.amenities-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
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
.homestay-dates {
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
</style>