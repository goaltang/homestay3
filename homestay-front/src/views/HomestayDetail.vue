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
                        <span>{{ reviewsComposable.formattedRating.value }}</span>
                        <span class="review-count">({{ reviewsComposable.formattedReviewCount.value }}条评价)</span>
                    </div>
                    <div class="location">
                        <el-icon>
                            <Location />
                        </el-icon>
                        <span>{{ formattedLocation }}</span>
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
                            <component :is="isFavorite ? 'StarFilled' : 'Star'" />
                        </el-icon>
                        {{ isFavorite ? '已收藏' : '收藏' }}
                    </el-button>
                </div>
            </div>

            <!-- 图片画廊组件 -->
            <ImageGallery :all-images="processedImages" :homestay-title="homestay.title"
                :total-image-count="processedImages.length" @show-all-photos="showAllPhotos"
                @go-home="$router.push('/')" />

            <!-- 内容区域 -->
            <div class="detail-content">
                <div class="content-main">
                    <!-- 房东简介组件 -->
                    <HostInfo :host-info="hostDetailInfo" :show-brief="true" :show-detail="false"
                        @scroll-to-detail="scrollToHostSection" />

                    <el-divider />

                    <!-- 房源特色功能 -->
                    <FeaturesList :features="homestay.suggestedFeatures || []" :show-fallback="true"
                        :property-type="homestay.type" :max-guests="homestay.maxGuests" :key-features="keyFeatures" />

                    <el-divider />

                    <!-- 描述 -->
                    <div class="description">
                        <h2>关于此房源</h2>
                        <p>{{ homestay.description }}</p>
                    </div>

                    <el-divider />

                    <!-- 房源所有设施 -->
                    <AmenitiesList :amenities="homestay.amenities || []" />

                    <el-divider />
                </div>

                <!-- 预订卡片组件 -->
                <BookingCard :price-per-night="pricePerNight" :rating="reviewsComposable.numericRating.value"
                    :review-count="reviewsComposable.formattedReviewCount.value" :max-guests="homestay.maxGuests"
                    :homestay-id="homestay.id!" :auto-confirm="homestay.autoConfirm"
                    :dates="bookingComposable.bookingDateRange.value" :guests="bookingComposable.bookingDates.guests"
                    @booking-confirmed="() => bookingComposable.bookHomestay()" @date-changed="handleDateRangeChange"
                    @guests-changed="(value) => bookingComposable.bookingDates.guests = value" />
            </div>

            <!-- 位置信息组件 -->
            <LocationInfo :formatted-location="formattedLocation" :address-detail="homestay.addressDetail"
                :distance-from-center="homestay.distanceFromCenter" :map-loading="mapComposable.mapData.value.isLoading"
                :has-location="mapComposable.mapData.value.hasLocation"
                :static-map-url="mapComposable.mapData.value.staticMapUrl"
                :nearby-places="mapComposable.nearbyPlaces.value" @open-map="mapComposable.openMapModal"
                @map-error="mapComposable.onMapImageError" />

            <el-divider />

            <!-- 房东详细信息组件 -->
            <HostInfo :host-info="hostDetailInfo" :show-brief="false" :show-detail="true" @contact-host="contactHost" />

            <el-divider />

            <!-- 须知事项及规则组件 -->
            <PoliciesAndRules 
                v-if="homestay"
                :check-in-time="homestay.checkInTime" 
                :check-out-time="homestay.checkOutTime" 
                :cancel-policy-type="homestay.cancelPolicyType" 
                :house-rules="homestay.houseRules" 
            />

            <el-divider />

            <!-- 评价部分组件 -->
            <ReviewsSection :reviews="reviewsComposable.reviews.value"
                :review-stats="reviewsComposable.reviewStats.value"
                :review-count="reviewsComposable.formattedReviewCount.value"
                :total-count="reviewsComposable.totalReviewCount.value"
                :rating="reviewsComposable.formattedRating.value"
                @load-more="() => reviewsComposable.loadMoreReviews(homestay!.id!)" />
        </div>
    </div>

    <!-- 聊天对话框 -->
    <ChatDialog />
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, StarFilled, Location, Share } from '@element-plus/icons-vue'
import { getHomestayById } from '@/api/homestay'
import { getHomestayHostInfo } from '@/api/host'
import { useAuthStore } from '@/stores/auth'
import { useFavoritesStore } from '@/stores/favorites'
import { useChatStore } from '@/stores/chat'
import { formatLocation, parsePrice, processImages } from '@/utils/homestayUtils'
import { useReviews } from '@/composables/useReviews'
import { useMap } from '@/composables/useMap'
import { useBooking } from '@/composables/useBooking'
import type { HomestayDetail } from '@/types/homestay'
import type { HostDTO } from '@/types/host'

// 组件导入
import ImageGallery from '@/components/homestay/ImageGallery.vue'
import BookingCard from '@/components/homestay/BookingCard.vue'
import ReviewsSection from '@/components/homestay/ReviewsSection.vue'
import LocationInfo from '@/components/homestay/LocationInfo.vue'
import FeaturesList from '@/components/homestay/FeaturesList.vue'
import AmenitiesList from '@/components/homestay/AmenitiesList.vue'
import HostInfo from '@/components/homestay/HostInfo.vue'
import PoliciesAndRules from '@/components/homestay/PoliciesAndRules.vue'
import ChatDialog from '@/components/chat/ChatDialog.vue'

// 基础状态
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const favoritesStore = useFavoritesStore()
const chatStore = useChatStore()

const loading = ref(true)
const homestay = ref<HomestayDetail | null>(null)
const hostDetailInfo = ref<HostDTO | null>(null)

// 组合式函数
const reviewsComposable = useReviews()
const mapComposable = useMap()

// 计算属性
const formattedLocation = computed(() => formatLocation(homestay.value))
const pricePerNight = computed(() => parsePrice(homestay.value))
const processedImages = computed(() => processImages(homestay.value))
const isFavorite = computed(() => homestay.value ? favoritesStore.isFavorite(homestay.value.id!) : false)

// 预订相关
const bookingComposable = useBooking(homestay, pricePerNight)

// 修复预订组件的日期处理
const handleDateRangeChange = (checkIn: Date | null, checkOut: Date | null) => {
    console.log('handleDateRangeChange called:', { checkIn, checkOut })
    if (checkIn && checkOut) {
        bookingComposable.bookingDateRange.value = [checkIn, checkOut]
        bookingComposable.bookingDates.checkIn = checkIn
        bookingComposable.bookingDates.checkOut = checkOut
        console.log('Updated booking dates:', bookingComposable.bookingDates)
    } else {
        bookingComposable.bookingDateRange.value = null
        bookingComposable.bookingDates.checkIn = null
        bookingComposable.bookingDates.checkOut = null
        console.log('Cleared booking dates')
    }
}

// 关键特色
const keyFeatures = computed(() => {
    if (!homestay.value?.amenities) return []
    const features: string[] = []
    const amenityList = homestay.value.amenities

    // 提取特色服务
    const specialServices = amenityList.filter((a: any) => a.categoryName === '特色服务').slice(0, 2).map((a: any) => a.label)
    features.push(...specialServices)

    // 补充便利设施
    if (features.length < 2) {
        const convenience = amenityList.filter((a: any) => a.categoryName === '便利设施').slice(0, 2 - features.length).map((a: any) => a.label)
        features.push(...convenience)
    }

    return features.slice(0, 3)
})

// 事件处理
const shareHomestay = () => ElMessage.info('分享功能待实现')
const toggleFavorite = async () => {
    if (!homestay.value) {
        ElMessage.warning('房源信息加载中，请稍后再试')
        return
    }

    try {
        await favoritesStore.toggleFavorite(homestay.value.id!)
    } catch (error) {
        console.error('切换收藏状态失败:', error)
        ElMessage.error('操作失败，请稍后重试')
    }
}
const contactHost = async () => {
    if (!authStore.isAuthenticated) {
        ElMessage.warning("请先登录后再联系房东");
        router.push("/login");
        return;
    }
    if (!homestay.value?.ownerId) {
        ElMessage.error("无法获取房东信息");
        return;
    }
    await chatStore.openChatDialog(homestay.value.id, homestay.value.ownerId);
}
const showAllPhotos = () => ElMessage.info('查看全部照片功能待实现')

const scrollToHostSection = () => {
    const hostSection = document.querySelector('.host-section')
    if (hostSection) {
        hostSection.scrollIntoView({ behavior: 'smooth', block: 'start' })
        hostSection.classList.add('highlight-section')
        setTimeout(() => hostSection.classList.remove('highlight-section'), 2000)
    }
}

// 主要数据获取函数
const fetchData = async () => {
    loading.value = true
    const homestayId = Number(route.params.id)
    const referringCriteriaFromQuery = route.query.referring_criteria as string | undefined

    if (isNaN(homestayId) || homestayId <= 0) {
        ElMessage.error('无效的民宿ID')
        return
    }

    try {
        // 重置所有状态
        homestay.value = null
        hostDetailInfo.value = null
        reviewsComposable.resetReviews()
        mapComposable.resetMap()
        bookingComposable.resetBooking()

        // 获取基础民宿信息
        const homestayResponse = await getHomestayById(homestayId, referringCriteriaFromQuery)
        if (!homestayResponse?.data) {
            throw new Error('未找到民宿信息')
        }
        homestay.value = homestayResponse.data as HomestayDetail

        console.log("民宿基础信息已获取:", {
            id: homestay.value.id,
            title: homestay.value.title,
            suggestedFeatures: homestay.value.suggestedFeatures?.length || 0,
            price: homestay.value.price,
            images: homestay.value.images?.length || 0
        })

        // 并行获取其他数据
        const promises = [
            reviewsComposable.fetchReviewsAndStats(homestayId),
            mapComposable.initializeMap(homestay.value)
        ]

        // 获取房东信息
        if (homestayId) {
            promises.push(
                getHomestayHostInfo(homestayId).then(response => {
                    if (response?.data) {
                        hostDetailInfo.value = response.data
                        console.log("详细房东信息已获取:", hostDetailInfo.value)
                    }
                }).catch(error => {
                    console.error('获取详细房东信息失败:', error)
                })
            )
        }

        await Promise.all(promises)

    } catch (error: any) {
        console.error("获取民宿详情过程中发生错误:", error)
        ElMessage.error(error.message || '加载民宿信息时出错，请稍后重试')
        homestay.value = null
        hostDetailInfo.value = null
    } finally {
        loading.value = false
    }
}

// 组件挂载
onMounted(() => {
    fetchData()
})
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

.detail-content {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: 48px;
    margin-bottom: 48px;
}

.description h2 {
    font-size: 22px;
    margin-bottom: 16px;
}

.description p {
    line-height: 1.6;
}

/* 高亮动画效果 */
.highlight-section {
    animation: highlightPulse 2s ease-in-out;
    border-radius: 8px;
}

@keyframes highlightPulse {
    0% {
        box-shadow: 0 0 0 0 rgba(64, 158, 255, 0.4);
        background-color: rgba(64, 158, 255, 0.1);
    }

    50% {
        box-shadow: 0 0 20px 10px rgba(64, 158, 255, 0.2);
        background-color: rgba(64, 158, 255, 0.05);
    }

    100% {
        box-shadow: 0 0 0 0 rgba(64, 158, 255, 0);
        background-color: transparent;
    }
}

/* 响应式设计 */
@media (max-width: 768px) {
    .detail-container {
        padding: 16px;
    }

    .detail-content {
        grid-template-columns: 1fr;
        gap: 24px;
    }
}
</style>