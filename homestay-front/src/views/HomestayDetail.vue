<template>
    <div class="detail-container">
        <div v-if="loading" class="loading-container">
            <el-skeleton :rows="10" animated />
        </div>

        <div v-else-if="!homestay" class="empty-result">
            <el-empty description="未找到民宿信息">
                <el-button @click="$router.push('/')">返回首页</el-button>
            </el-empty>
        </div>

        <div v-else class="homestay-detail">
            <!-- 标题区域 -->
            <div class="detail-header">
                <h1>{{ homestay.title }}</h1>
                <div class="detail-subheader">
                    <div class="rating">
                        <el-icon>
                            <Star />
                        </el-icon>
                        <span>{{ homestay.rating.toFixed(1) }}</span>
                        <span class="review-count">({{ homestay.reviewCount }}条评价)</span>
                    </div>
                    <div class="location">
                        <el-icon>
                            <Location />
                        </el-icon>
                        <span>{{ homestay.location }}</span>
                    </div>
                </div>
                <div class="actions">
                    <el-button type="text" @click="shareHomestay">
                        <el-icon>
                            <Share />
                        </el-icon>
                        分享
                    </el-button>
                    <el-button type="text" @click="toggleFavorite">
                        <el-icon>
                            <component :is="isFavorite ? 'Star' : 'StarFilled'" />
                        </el-icon>
                        {{ isFavorite ? '已收藏' : '收藏' }}
                    </el-button>
                </div>
            </div>

            <!-- 图片区域 -->
            <div class="image-gallery">
                <div class="main-image">
                    <img :src="getMainImage()" :alt="homestay.title" />
                </div>
                <div class="image-grid">
                    <div v-for="(image, index) in getSubImages()" :key="index" class="sub-image">
                        <img :src="image" :alt="homestay.title" />
                    </div>
                </div>
                <el-button class="view-all-photos" @click="showAllPhotos">
                    <el-icon>
                        <Picture />
                    </el-icon>
                    查看所有照片
                </el-button>
            </div>

            <!-- 内容区域 -->
            <div class="detail-content">
                <div class="content-main">
                    <!-- 房东信息 -->
                    <div class="host-info">
                        <h2>{{ homestay.hostName }}的{{ homestay.propertyType }}</h2>
                        <div class="capacity-info">
                            <span>{{ homestay.maxGuests }}位房客</span>
                            <span>{{ homestay.bedrooms }}间卧室</span>
                            <span>{{ homestay.beds }}张床</span>
                            <span>{{ homestay.bathrooms }}间卫生间</span>
                        </div>
                    </div>

                    <el-divider />

                    <!-- 特色 -->
                    <div class="features">
                        <div class="feature-item">
                            <el-icon>
                                <House />
                            </el-icon>
                            <div class="feature-text">
                                <h3>整套{{ homestay.propertyType }}</h3>
                                <p>您将拥有整个空间，享受私密住宿体验。</p>
                            </div>
                        </div>
                        <div class="feature-item">
                            <el-icon>
                                <Location />
                            </el-icon>
                            <div class="feature-text">
                                <h3>绝佳位置</h3>
                                <p>距离市中心仅{{ homestay.distanceFromCenter }}公里。</p>
                            </div>
                        </div>
                        <div class="feature-item">
                            <el-icon>
                                <Key />
                            </el-icon>
                            <div class="feature-text">
                                <h3>自助入住</h3>
                                <p>使用密码钥匙盒自助入住。</p>
                            </div>
                        </div>
                    </div>

                    <el-divider />

                    <!-- 描述 -->
                    <div class="description">
                        <h2>关于此房源</h2>
                        <p>{{ homestay.description }}</p>
                    </div>

                    <el-divider />

                    <!-- 设施 -->
                    <div class="amenities">
                        <h2>房源设施</h2>
                        <div class="amenities-grid">
                            <div v-for="(amenity, index) in homestay.amenities" :key="index" class="amenity-item">
                                <el-icon>
                                    <Check />
                                </el-icon>
                                <span>{{ amenity }}</span>
                            </div>
                        </div>
                    </div>

                    <el-divider />

                    <!-- 位置 -->
                    <div class="location-section">
                        <h2>位置信息</h2>
                        <div class="map-container">
                            <img src="https://picsum.photos/800/400" alt="地图" class="map-placeholder" />
                        </div>
                        <p>{{ homestay.location }}</p>
                        <p>距离市中心{{ homestay.distanceFromCenter }}公里</p>
                    </div>
                </div>

                <!-- 预订卡片 -->
                <div class="booking-card">
                    <div class="booking-price">
                        <span class="price">¥{{ homestay.pricePerNight }}</span>
                        <span class="night">/晚</span>
                    </div>
                    <div class="booking-rating">
                        <el-icon>
                            <Star />
                        </el-icon>
                        <span>{{ homestay.rating.toFixed(1) }}</span>
                        <span class="review-count">({{ homestay.reviewCount }}条评价)</span>
                    </div>

                    <div class="booking-dates">
                        <div class="date-picker">
                            <div class="date-input check-in">
                                <div class="label">入住</div>
                                <el-date-picker v-model="bookingDates.checkIn" type="date" placeholder="选择日期"
                                    format="YYYY/MM/DD" :disabled-date="disablePastDates"
                                    @change="calculateTotalPrice" />
                            </div>
                            <div class="date-input check-out">
                                <div class="label">退房</div>
                                <el-date-picker v-model="bookingDates.checkOut" type="date" placeholder="选择日期"
                                    format="YYYY/MM/DD" :disabled-date="disableInvalidDates"
                                    @change="calculateTotalPrice" />
                            </div>
                        </div>
                        <div class="guest-input">
                            <el-select v-model="bookingDates.guests" placeholder="选择人数" @change="calculateTotalPrice">
                                <el-option v-for="i in homestay.maxGuests" :key="i" :label="`${i}位房客`" :value="i" />
                            </el-select>
                        </div>
                    </div>

                    <el-button type="primary" class="booking-button" @click="bookHomestay"
                        :disabled="!bookingDates.checkIn || !bookingDates.checkOut">
                        预订
                    </el-button>

                    <div v-if="totalNights > 0" class="price-breakdown">
                        <div class="price-item">
                            <span>¥{{ homestay.pricePerNight }} x {{ totalNights }}晚</span>
                            <span>¥{{ basePrice }}</span>
                        </div>
                        <div class="price-item">
                            <span>清洁费</span>
                            <span>¥{{ cleaningFee }}</span>
                        </div>
                        <div class="price-item">
                            <span>服务费</span>
                            <span>¥{{ serviceFee }}</span>
                        </div>
                        <div class="price-item total">
                            <span>总价</span>
                            <span>¥{{ totalPrice }}</span>
                        </div>
                    </div>

                    <div class="booking-note">
                        预订前不会向您收取任何费用
                    </div>
                </div>
            </div>

            <el-divider />

            <!-- 评价 -->
            <div class="reviews-section">
                <div class="reviews-header">
                    <h2>
                        <el-icon>
                            <Star />
                        </el-icon>
                        {{ homestay.rating.toFixed(1) }} · {{ homestay.reviewCount }}条评价
                    </h2>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, StarFilled, Location, Share, Picture, House, Key, Check } from '@element-plus/icons-vue'
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

interface BookingDates {
    checkIn: Date | null;
    checkOut: Date | null;
    guests: number;
}

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const homestay = ref<Homestay | null>(null)
const isFavorite = ref(false)

const bookingDates = reactive<BookingDates>({
    checkIn: null,
    checkOut: null,
    guests: 1
})

const totalNights = ref(0)
const basePrice = ref(0)
const cleaningFee = ref(0)
const serviceFee = ref(0)
const totalPrice = ref(0)

onMounted(async () => {
    const id = route.params.id
    if (id) {
        await fetchHomestayDetails(Number(id))
        checkFavoriteStatus()
    }
})

const fetchHomestayDetails = async (id: number) => {
    loading.value = true
    try {
        const response = await request.get(`/api/homestays/${id}`)
        homestay.value = response.data
        // 设置默认值
        if (homestay.value) {
            cleaningFee.value = Math.round(homestay.value.pricePerNight * 0.1) // 清洁费为房价的10%
        }
    } catch (error) {
        console.error('获取民宿详情失败:', error)
        ElMessage.error('获取民宿详情失败')

        // 使用模拟数据（当后端服务未启动时）
        if (process.env.NODE_ENV === 'development') {
            console.log('使用模拟数据')
            homestay.value = {
                id: id,
                title: '示例民宿详情',
                description: '这是一个示例民宿详情，用于在后端服务未启动时展示界面。这个美丽的民宿位于风景如画的地区，提供舒适的住宿环境和各种便利设施。从这里，您可以欣赏到壮丽的自然风光，体验当地的文化和美食。',
                location: '示例位置',
                city: '示例城市',
                country: '中国',
                pricePerNight: 888,
                maxGuests: 4,
                bedrooms: 2,
                beds: 2,
                bathrooms: 1,
                amenities: ['WiFi', '空调', '厨房', '洗衣机', '停车位'],
                images: [
                    'https://picsum.photos/800/600?random=10',
                    'https://picsum.photos/800/600?random=11',
                    'https://picsum.photos/800/600?random=12',
                    'https://picsum.photos/800/600?random=13',
                    'https://picsum.photos/800/600?random=14'
                ],
                rating: 4.8,
                reviewCount: 120,
                latitude: 30.0,
                longitude: 120.0,
                hostName: '示例房东',
                featured: true,
                propertyType: '公寓',
                distanceFromCenter: 3.5
            }
        }
    } finally {
        loading.value = false
    }
}

const getMainImage = () => {
    if (!homestay.value || !homestay.value.images || homestay.value.images.length === 0) {
        return 'https://picsum.photos/800/600'
    }

    const image = homestay.value.images[0]
    if (image.startsWith('http')) {
        return image
    }
    return `https://picsum.photos/800/600?random=${homestay.value.id}-0`
}

const getSubImages = () => {
    if (!homestay.value || !homestay.value.images || homestay.value.images.length <= 1) {
        return Array(4).fill('https://picsum.photos/400/300')
    }

    return homestay.value.images.slice(1, 5).map((image: string, index: number) => {
        if (image.startsWith('http')) {
            return image
        }
        return `https://picsum.photos/400/300?random=${homestay.value?.id}-${index + 1}`
    })
}

const showAllPhotos = () => {
    ElMessage.info('查看所有照片功能正在开发中')
}

const shareHomestay = () => {
    ElMessage.info('分享功能正在开发中')
}

const toggleFavorite = () => {
    isFavorite.value = !isFavorite.value
    ElMessage.success(isFavorite.value ? '已添加到收藏' : '已从收藏中移除')

    // 保存收藏状态
    const favorites = JSON.parse(localStorage.getItem('favorites') || '[]')
    if (homestay.value) {
        const id = homestay.value.id

        if (isFavorite.value) {
            if (!favorites.includes(id)) {
                favorites.push(id)
            }
        } else {
            const index = favorites.indexOf(id)
            if (index !== -1) {
                favorites.splice(index, 1)
            }
        }

        localStorage.setItem('favorites', JSON.stringify(favorites))
    }
}

const checkFavoriteStatus = () => {
    if (!homestay.value) return

    const favorites = JSON.parse(localStorage.getItem('favorites') || '[]')
    isFavorite.value = favorites.includes(homestay.value.id)
}

const disablePastDates = (date: Date) => {
    return date < new Date()
}

const disableInvalidDates = (date: Date) => {
    if (!bookingDates.checkIn) {
        return date < new Date()
    }
    return date <= bookingDates.checkIn
}

const calculateTotalPrice = () => {
    if (!bookingDates.checkIn || !bookingDates.checkOut || !homestay.value) {
        totalNights.value = 0
        basePrice.value = 0
        totalPrice.value = 0
        return
    }

    const checkIn = new Date(bookingDates.checkIn)
    const checkOut = new Date(bookingDates.checkOut)
    const diffTime = checkOut.getTime() - checkIn.getTime()
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

    totalNights.value = diffDays
    basePrice.value = homestay.value.pricePerNight * diffDays
    serviceFee.value = Math.round(basePrice.value * 0.15) // 服务费为总价的15%
    totalPrice.value = basePrice.value + cleaningFee.value + serviceFee.value
}

const bookHomestay = () => {
    if (!bookingDates.checkIn || !bookingDates.checkOut) {
        ElMessage.warning('请选择入住和退房日期')
        return
    }

    ElMessage.success('预订请求已提交，预订功能正在开发中')
}
</script>

<style scoped>
.detail-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px;
}

.loading-container {
    padding: 24px;
}

.empty-result {
    padding: 48px 0;
    text-align: center;
}

.detail-header {
    margin-bottom: 24px;
}

.detail-header h1 {
    font-size: 26px;
    margin-bottom: 8px;
}

.detail-subheader {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;
}

.rating,
.location {
    display: flex;
    align-items: center;
    gap: 4px;
}

.review-count {
    color: #717171;
}

.actions {
    display: flex;
    gap: 16px;
}

.image-gallery {
    position: relative;
    margin-bottom: 48px;
}

.main-image {
    height: 400px;
    overflow: hidden;
    border-radius: 12px;
}

.main-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.image-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    grid-template-rows: repeat(2, 1fr);
    gap: 8px;
    position: absolute;
    top: 0;
    right: 0;
    width: 50%;
    height: 400px;
}

.sub-image {
    overflow: hidden;
    border-radius: 0;
}

.sub-image:nth-child(1) {
    border-top-right-radius: 12px;
}

.sub-image:nth-child(2) {
    border-bottom-right-radius: 12px;
}

.sub-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.view-all-photos {
    position: absolute;
    bottom: 16px;
    right: 16px;
    background-color: white;
    border-radius: 8px;
    display: flex;
    align-items: center;
    gap: 8px;
}

.detail-content {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: 48px;
    margin-bottom: 48px;
}

.host-info h2 {
    font-size: 22px;
    margin-bottom: 8px;
}

.capacity-info {
    display: flex;
    gap: 16px;
    color: #717171;
}

.features {
    margin: 24px 0;
}

.feature-item {
    display: flex;
    align-items: flex-start;
    gap: 16px;
    margin-bottom: 24px;
}

.feature-item .el-icon {
    font-size: 24px;
}

.feature-text h3 {
    margin: 0 0 4px 0;
    font-size: 16px;
}

.feature-text p {
    margin: 0;
    color: #717171;
}

.description h2,
.amenities h2,
.location-section h2,
.reviews-section h2 {
    font-size: 22px;
    margin-bottom: 16px;
}

.description p {
    line-height: 1.6;
}

.amenities-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
}

.amenity-item {
    display: flex;
    align-items: center;
    gap: 8px;
}

.map-container {
    margin-bottom: 16px;
    border-radius: 12px;
    overflow: hidden;
}

.map-placeholder {
    width: 100%;
    height: 300px;
    object-fit: cover;
}

.booking-card {
    position: sticky;
    top: 100px;
    border: 1px solid #dddddd;
    border-radius: 12px;
    padding: 24px;
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.booking-price {
    margin-bottom: 8px;
}

.booking-price .price {
    font-size: 22px;
    font-weight: 600;
}

.booking-price .night {
    font-size: 16px;
    font-weight: normal;
}

.booking-rating {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-bottom: 24px;
}

.booking-dates {
    margin-bottom: 24px;
}

.date-picker {
    display: grid;
    grid-template-columns: 1fr 1fr;
    border: 1px solid #dddddd;
    border-radius: 8px;
    overflow: hidden;
    margin-bottom: 12px;
}

.date-input {
    padding: 12px;
}

.date-input.check-in {
    border-right: 1px solid #dddddd;
}

.date-input .label {
    font-size: 12px;
    font-weight: 600;
    margin-bottom: 4px;
}

.guest-input {
    margin-top: 12px;
}

.guest-input :deep(.el-select) {
    width: 100%;
}

.booking-button {
    width: 100%;
    margin-bottom: 24px;
}

.price-breakdown {
    margin-bottom: 24px;
}

.price-item {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
}

.price-item.total {
    font-weight: 600;
    border-top: 1px solid #dddddd;
    padding-top: 12px;
    margin-top: 12px;
}

.booking-note {
    text-align: center;
    color: #717171;
    font-size: 14px;
}

.reviews-header {
    display: flex;
    align-items: center;
    gap: 8px;
}

.reviews-header h2 {
    display: flex;
    align-items: center;
    gap: 8px;
}

/* 响应式调整 */
@media (max-width: 768px) {
    .detail-content {
        grid-template-columns: 1fr;
        gap: 32px;
    }

    .booking-card {
        position: static;
        margin-top: 32px;
    }

    .image-grid {
        display: none;
    }

    .main-image {
        width: 100%;
    }
}

@media (max-width: 480px) {
    .detail-subheader {
        flex-direction: column;
        align-items: flex-start;
        gap: 8px;
    }

    .image-gallery {
        display: grid;
        grid-template-columns: 1fr;
        grid-template-rows: 1fr;
        height: auto;
    }

    .main-image {
        height: 300px;
    }

    .image-grid {
        display: none;
    }

    .amenities-grid {
        grid-template-columns: 1fr;
    }
}
</style>