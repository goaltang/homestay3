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
                        <span>{{ homestay.rating ? homestay.rating.toFixed(1) : '暂无评分' }}</span>
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
                <div class="gallery-container">
                    <div class="main-image" @click="showAllPhotos">
                        <img :src="getMainImage()" :alt="homestay.title" />
                        <div class="image-overlay">
                            <div class="overlay-content">
                                <el-icon>
                                    <ZoomIn />
                                </el-icon>
                            </div>
                        </div>
                    </div>
                    <div class="image-grid">
                        <div v-for="(image, index) in getSubImages()" :key="index" class="sub-image"
                            @click="showAllPhotos">
                            <img :src="image" :alt="`${homestay.title} - 图片 ${index + 1}`" />
                            <div class="image-overlay">
                                <div class="overlay-content">
                                    <el-icon>
                                        <ZoomIn />
                                    </el-icon>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <el-button class="view-all-photos" @click="showAllPhotos">
                    <el-icon>
                        <Camera />
                    </el-icon>
                    查看全部 {{ homestay.images ? homestay.images.length : 0 }} 张照片
                </el-button>
            </div>

            <!-- 内容区域 -->
            <div class="detail-content">
                <div class="content-main">
                    <!-- 房东信息 - 简单版 -->
                    <div class="host-brief">
                        <div class="host-avatar">
                            <img :src="homestay.hostAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                                :alt="homestay.hostName">
                        </div>
                        <div class="host-brief-info">
                            <h3>{{ homestay.hostName || '房东' }}是星级旅居主人</h3>
                            <p>星级旅居主人接待经验丰富、深获旅人好评，他们致力为旅人提供优质的住宿体验。</p>
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
                                <span>{{ formatAmenity(amenity) }}</span>
                            </div>
                        </div>
                    </div>

                    <el-divider />
                </div>

                <!-- 预订卡片 -->
                <div class="booking-card">
                    <div class="booking-card-header">
                        <div class="price-info">
                            <span class="price">¥{{ homestay.pricePerNight }}</span>
                            <span class="night">/晚</span>
                        </div>
                        <div class="rating-info">
                            <el-rate v-model="homestay.rating" disabled text-color="#FF9900"
                                disabled-void-color="#C6D1DE" />
                            <span class="review-link">{{ homestay.reviewCount }}条评价</span>
                        </div>
                    </div>

                    <div class="booking-form">
                        <div class="date-picker-container">
                            <div class="date-picker-header">
                                <div class="check-in-label">入住</div>
                                <div class="check-out-label">退房</div>
                            </div>
                            <el-date-picker v-model="bookingDateRange" type="daterange" range-separator="至"
                                start-placeholder="选择日期" end-placeholder="选择日期" format="YYYY/MM/DD"
                                :disabled-date="disablePastDates" @change="handleDateRangeChange" :size="'large'"
                                class="date-range-picker" />
                        </div>

                        <div class="guest-selector-container">
                            <div class="guest-selector-label">房客</div>
                            <el-select v-model="bookingDates.guests" placeholder="1位房客" @change="calculateTotalPrice"
                                :size="'large'" class="guest-dropdown">
                                <el-option v-for="i in homestay.maxGuests" :key="i" :label="`${i}位房客`" :value="i" />
                            </el-select>
                        </div>
                    </div>

                    <el-button type="primary" class="booking-button" @click="bookHomestay"
                        :disabled="!bookingDates.checkIn || !bookingDates.checkOut">
                        预订
                    </el-button>

                    <div class="price-breakdown" v-if="totalNights > 0">
                        <div class="price-row">
                            <div class="price-item">¥{{ homestay.pricePerNight }} x {{ totalNights }}晚</div>
                            <div class="price-value">¥{{ basePrice }}</div>
                        </div>
                        <div class="price-row">
                            <div class="price-item">清洁费</div>
                            <div class="price-value">¥{{ cleaningFee }}</div>
                        </div>
                        <div class="price-row">
                            <div class="price-item">服务费</div>
                            <div class="price-value">¥{{ serviceFee }}</div>
                        </div>
                        <div class="price-divider"></div>
                        <div class="price-row total">
                            <div class="price-item">总价</div>
                            <div class="price-value">¥{{ totalPrice }}</div>
                        </div>
                    </div>

                    <div class="booking-note">
                        <el-icon>
                            <InfoFilled />
                        </el-icon>
                        <span>预订前不会向您收取任何费用</span>
                    </div>
                </div>
            </div>

            <!-- 位置信息 - 调整为全宽度 -->
            <div class="full-width-section">
                <h2>位置信息</h2>
                <div class="map-container">
                    <img src="https://picsum.photos/800/400" alt="地图" class="map-placeholder" />
                </div>
                <p>{{ homestay.location }}</p>
                <p>距离市中心{{ homestay.distanceFromCenter }}公里</p>
            </div>

            <el-divider />

            <!-- 房东详细信息（靠下）- 调整为全宽度 -->
            <div class="full-width-section host-section">
                <h2>旅居主人简介</h2>
                <div class="host-detail-layout">
                    <div class="host-left-card">
                        <div class="host-profile-card">
                            <div class="host-profile-avatar">
                                <img :src="homestay.hostAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                                    :alt="homestay.hostName">
                                <div class="host-badge" v-if="true">
                                    <el-icon>
                                        <Star />
                                    </el-icon>
                                </div>
                            </div>
                            <div class="host-name">{{ homestay.hostName || 'Chandra Datt' }}</div>
                            <div class="host-badge-text">
                                <el-icon>
                                    <Trophy />
                                </el-icon>
                                <span>星级旅居主人</span>
                            </div>
                        </div>

                        <div class="host-stats">
                            <div class="host-stat-item">
                                <div class="stat-label">评分</div>
                                <div class="stat-value">{{ homestay.hostRating || '4.82' }}★</div>
                            </div>
                            <div class="host-stat-item">
                                <div class="stat-label">接待</div>
                                <div class="stat-value">{{ homestay.hostAccommodations || '435' }}间</div>
                            </div>
                            <div class="host-stat-item">
                                <div class="stat-label">接待经验</div>
                                <div class="stat-value">{{ homestay.hostYears || '3' }}年</div>
                            </div>
                        </div>
                    </div>

                    <div class="host-right-info">
                        <div class="host-intro">
                            <h3>{{ homestay.hostName || 'Chandra Datt' }}是星级旅居主人</h3>
                            <p>星级旅居主人接待经验丰富、深获旅人好评，他们致力为旅人提供优质的住宿体验。</p>
                        </div>

                        <div class="host-companions">
                            <h4>接待伙伴</h4>
                            <div class="companions-list">
                                <div class="companion-item" v-for="(companion, index) in hostCompanions" :key="index">
                                    <div class="companion-avatar">
                                        <img :src="companion.avatar" :alt="companion.name">
                                    </div>
                                    <div class="companion-name">{{ companion.name }}</div>
                                </div>
                            </div>
                        </div>

                        <div class="host-details">
                            <h4>旅居主人详情</h4>
                            <div class="host-detail-info">
                                <div class="detail-item">
                                    <div class="detail-label">回复率:</div>
                                    <div class="detail-value">{{ homestay.hostResponseRate || '98%' }}</div>
                                </div>
                                <div class="detail-item">
                                    <div class="detail-label">回复时间:</div>
                                    <div class="detail-value">{{ homestay.hostResponseTime || '几小时内' }}</div>
                                </div>
                            </div>
                        </div>

                        <div class="host-contact">
                            <el-button type="primary" class="contact-button" @click="contactHost">
                                发送讯息给旅居主人
                            </el-button>
                        </div>

                        <div class="host-safety-note">
                            <el-icon>
                                <InfoFilled />
                            </el-icon>
                            <p>为保障您的安全，发话前请通过平台，请勿透露旅客或旅居主人连络资料。</p>
                        </div>
                    </div>
                </div>

                <div class="host-footer">
                    <div class="host-footer-left">
                        <div class="host-small-avatar">
                            <img :src="homestay.hostAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                                :alt="homestay.hostName">
                            <div class="verified-badge" v-if="true">
                                <el-icon>
                                    <Check />
                                </el-icon>
                            </div>
                        </div>
                    </div>
                    <div class="host-footer-right">
                        <div class="host-title">旅居主人：{{ homestay.hostName || 'Chandra Datt' }}</div>
                        <div class="host-subtitle">星级旅居主人・{{ homestay.hostYears || '3' }}年接待经验</div>
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
                        {{ homestay.rating ? homestay.rating.toFixed(1) : '暂无评分' }} · {{ homestay.reviewCount }}条评价
                    </h2>
                </div>

                <!-- 评分统计 -->
                <div class="rating-summary" v-if="reviewStats.length > 0">
                    <div class="rating-bars">
                        <div class="rating-bar-item" v-for="(stat, index) in reviewStats" :key="index">
                            <span class="rating-name">{{ stat.name }}</span>
                            <div class="rating-bar-container">
                                <div class="rating-bar" :style="{ width: `${stat.score * 20}%` }"></div>
                            </div>
                            <span class="rating-score">{{ stat.score.toFixed(1) }}</span>
                        </div>
                    </div>
                </div>

                <!-- 评价列表 -->
                <div class="review-list" v-if="reviews.length > 0">
                    <div class="review-item" v-for="(review, index) in reviews" :key="index">
                        <div class="review-header">
                            <div class="reviewer-info">
                                <div class="avatar">
                                    <img :src="review.avatarUrl || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                                        :alt="review.userName">
                                </div>
                                <div class="reviewer-details">
                                    <div class="reviewer-name">{{ review.userName }}</div>
                                    <div class="review-date">{{ formatDateString(review.createTime) }}</div>
                                </div>
                            </div>
                            <div class="review-rating">
                                <el-rate v-model="review.rating" disabled text-color="#ff9900" />
                            </div>
                        </div>
                        <div class="review-content">
                            {{ review.content }}
                        </div>
                        <div class="host-response" v-if="review.response">
                            <div class="response-header">房东回复:</div>
                            <div class="response-content">{{ review.response }}</div>
                        </div>
                    </div>
                </div>

                <!-- 无评价提示 -->
                <div class="no-reviews" v-else>
                    <el-empty description="暂无评价" v-if="homestay.reviewCount === 0">
                        <span>这个房源还没有收到评价</span>
                    </el-empty>
                    <div class="loading-reviews" v-else>
                        <el-skeleton :rows="3" animated />
                    </div>
                </div>

                <!-- 加载更多按钮 -->
                <div class="load-more" v-if="reviews.length > 0 && reviews.length < homestay.reviewCount">
                    <el-button @click="loadMoreReviews">加载更多评价</el-button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled, Location, Share, Picture, House, Key, Check, InfoFilled, Trophy, ZoomIn, Camera } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getHomestayById } from '@/api/homestay'
import { getHomestayReviews, getHomestayReviewStats } from '@/api/review'
import { createOrder } from '@/api/order'
import { getHomestayHostInfo } from '@/api/host'
import { useAuthStore } from '@/stores/auth'
import { useUserStore } from '@/stores/user'

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
    coverImage?: string;
    rating: number;
    reviewCount: number;
    type: string;
    province: string;
    district: string;
    address: string;
    status: string;
    featured: boolean;
    propertyType: string;
    distanceFromCenter: number;
    latitude?: number;
    longitude?: number;
    hostName?: string;
    hostId?: number;
    hostAvatar?: string;
    hostResponseRate?: number;
    hostResponseTime?: string;
}

interface BookingDates {
    checkIn: Date | null;
    checkOut: Date | null;
    guests: number;
}

interface Review {
    id: number;
    userId: number;
    userName: string;
    avatarUrl?: string;
    rating: number;
    content: string;
    createTime: string;
    response?: string;
}

interface RatingStat {
    name: string;
    score: number;
}

// 定义房东类型
interface Host {
    id: number;
    name: string;
    avatar: string;
    rating: number;
    accommodations: number;
    years: number;
    responseRate: string;
    responseTime: string;
    companions: Array<{ name: string, avatar: string }>;
}

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const homestay = ref<Homestay | null>(null)
const isFavorite = ref(false)
const hostInfo = ref<Host | null>(null) // 新增房东信息状态

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

const reviews = ref<Review[]>([])
const reviewStats = ref<RatingStat[]>([])
const reviewPage = ref(0)
const reviewSize = ref(5)
const loadingReviews = ref(false)

// 房东伙伴数据
const hostCompanions = ref([
    {
        name: 'Kamal',
        avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
    },
    {
        name: 'Gaurav',
        avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
    }
]);

const bookingDateRange = ref<[Date, Date] | null>(null)

onMounted(async () => {
    console.log('===== 房源详情页面已挂载 =====');
    const id = route.params.id;
    console.log('从路由获取的房源ID:', id);
    if (id) {
        try {
            console.log('开始获取房源详情...');
            await fetchHomestayDetails(Number(id));

            if (homestay.value?.id) {
                console.log('准备获取房东信息...');
                await fetchHostInfo(homestay.value.id);
            } else {
                console.warn('未获取到有效房源ID，跳过房东信息获取');
            }

            console.log('检查收藏状态...');
            checkFavoriteStatus();

            console.log('开始获取评价...');
            await fetchReviews(Number(id));

            console.log('===== 房源详情页面数据加载完成 =====');
        } catch (error) {
            console.error('房源详情页数据加载出错:', error);
            ElMessage.error('加载数据时出错，请刷新页面重试');
        }
    } else {
        console.error('未找到有效的房源ID');
        ElMessage.error('未找到房源信息');
    }
})

const fetchHomestayDetails = async (id: number) => {
    loading.value = true
    try {
        const response = await getHomestayById(id)
        const data = response.data

        // 适配后端返回的数据格式
        homestay.value = {
            id: data.id,
            title: data.title || '未命名房源',
            description: data.description || '暂无描述',
            location: `${data.province || ''} ${data.city || ''} ${data.district || ''}`,
            city: data.city || '',
            country: '中国',
            pricePerNight: parseFloat(data.price) || 0, // 确保价格是数字
            maxGuests: data.maxGuests || 1,
            bedrooms: 1, // 假设值
            beds: 1, // 假设值
            bathrooms: 1, // 假设值
            amenities: data.amenities || [],
            images: data.images || [data.coverImage].filter(Boolean),
            rating: data.rating || 4.5, // 假设值
            reviewCount: data.reviewCount || 0,
            latitude: 0, // 假设值
            longitude: 0, // 假设值
            hostName: data.ownerName || '房东',
            hostId: 1, // 假设值
            featured: data.featured,
            propertyType: data.type || '公寓',
            distanceFromCenter: 3.5 // 假设值
        }

        // 日志记录房源信息
        console.log('房源详情适配后:', homestay.value)

        // 设置默认值
        if (homestay.value) {
            // 确保价格是数字类型
            const pricePerNight = Number(homestay.value.pricePerNight) || 0
            cleaningFee.value = Math.round(pricePerNight * 0.1) // 清洁费为房价的10%
            serviceFee.value = Math.round(pricePerNight * 0.15) // 服务费为房价的15%

            // 记录价格信息
            console.log('价格设置:', {
                pricePerNight,
                cleaningFee: cleaningFee.value,
                serviceFee: serviceFee.value
            })
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
                amenities: ['WIFI', 'AC', 'TV', 'KITCHEN', 'WASHER', 'DRYER', 'PARKING', 'POOL'],
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

            // 设置默认清洁费和服务费
            const pricePerNight = Number(homestay.value.pricePerNight) || 0
            cleaningFee.value = Math.round(pricePerNight * 0.1)
            serviceFee.value = Math.round(pricePerNight * 0.15)
        }
    } finally {
        loading.value = false
        console.log('房源详情:', homestay.value)
    }
}

const getMainImage = () => {
    // 始终优先使用封面图片作为主图显示
    if (homestay.value && homestay.value.coverImage) {
        console.log('使用封面图片作为主图:', homestay.value.coverImage);
        return processImageUrl(homestay.value.coverImage);
    }

    // 没有封面图片时才使用图片集的第一张
    if (homestay.value && homestay.value.images && homestay.value.images.length > 0) {
        const validImages = homestay.value.images.filter(img => img !== null && img !== undefined);
        if (validImages.length > 0) {
            return processImageUrl(validImages[0]);
        }
    }

    // 都没有时使用默认图片
    return 'https://picsum.photos/800/600';
}

// 处理图片路径
const processImageUrl = (image: string) => {
    if (!image) {
        return 'https://picsum.photos/800/600';
    }

    // 检查图片路径格式
    if (typeof image === 'string') {
        if (image.startsWith('http')) {
            return image;
        } else if (image.startsWith('/uploads/')) {
            return `/api${image}`;
        } else if (image.startsWith('/homestays/')) {
            return `/api${image}`;
        } else if (!image.startsWith('/')) {
            // 如果是纯文件名，检查是否有效
            const filename = image.split('/').pop();
            if (!filename || filename.trim() === '') {
                return 'https://picsum.photos/800/600';
            }
            return `/api/uploads/homestays/${filename}`;
        } else {
            // 其他以/开头的路径
            return `/api${image}`;
        }
    }

    return 'https://picsum.photos/800/600';
}

const getSubImages = () => {
    // 子图仅使用图片集的图片，不使用封面图片
    if (!homestay.value || !homestay.value.images || homestay.value.images.length === 0) {
        // 没有图片集时使用默认图片
        return [
            'https://picsum.photos/400/300?random=1',
            'https://picsum.photos/400/300?random=2',
            'https://picsum.photos/400/300?random=3',
            'https://picsum.photos/400/300?random=4'
        ];
    }

    // 过滤有效图片
    const validImages = homestay.value.images.filter(img => img !== null && img !== undefined);

    // 确保有至少4张图片显示
    if (validImages.length < 4) {
        const defaultImages = [
            'https://picsum.photos/400/300?random=1',
            'https://picsum.photos/400/300?random=2',
            'https://picsum.photos/400/300?random=3',
            'https://picsum.photos/400/300?random=4'
        ];

        // 组合实际图片和默认图片
        const combinedImages = [...validImages];
        for (let i = validImages.length; i < 4; i++) {
            combinedImages.push(defaultImages[i - validImages.length]);
        }

        return combinedImages.map(processImageUrl);
    }

    return validImages.map(processImageUrl);
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

    // 确保夜数是正数
    totalNights.value = Math.max(diffDays, 0)

    // 确保价格是数字类型
    const pricePerNight = Number(homestay.value.pricePerNight) || 0
    console.log('计算价格使用的每晚价格:', pricePerNight)

    // 计算基本价格
    basePrice.value = pricePerNight * totalNights.value

    // 计算服务费和清洁费
    cleaningFee.value = Math.round(pricePerNight * 0.1) // 清洁费为房价的10%
    serviceFee.value = Math.round(basePrice.value * 0.15) // 服务费为总价的15%

    // 计算总价
    totalPrice.value = basePrice.value + cleaningFee.value + serviceFee.value

    // 确保所有价格都是有效数字
    if (isNaN(basePrice.value)) basePrice.value = 0
    if (isNaN(cleaningFee.value)) cleaningFee.value = 0
    if (isNaN(serviceFee.value)) serviceFee.value = 0
    if (isNaN(totalPrice.value)) totalPrice.value = 0

    console.log('价格计算:', {
        夜数: totalNights.value,
        每晚价格: pricePerNight,
        基本价格: basePrice.value,
        清洁费: cleaningFee.value,
        服务费: serviceFee.value,
        总价: totalPrice.value
    })
}

// 实现一个更可靠的formatDate函数
const formatDate = (date: Date) => {
    if (!date) return '';

    try {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');

        return `${year}-${month}-${day}`;
    } catch (error) {
        console.error('日期格式化错误:', error);
        return date.toISOString().split('T')[0]; // 备用格式
    }
};

// 格式化日期字符串为本地显示格式（用于评论等显示）
const formatDateString = (dateString: string) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' });
};

const bookHomestay = async () => {
    // 检查用户是否已登录
    const authStore = useAuthStore();
    if (!authStore.isAuthenticated) {
        ElMessageBox.confirm('您需要登录才能预订房源', '提示', {
            confirmButtonText: '去登录',
            cancelButtonText: '取消',
            type: 'info'
        }).then(() => {
            router.push({
                path: '/login',
                query: { redirect: route.fullPath }
            });
        }).catch(() => { });
        return;
    }

    // 验证是否已选择日期
    if (!bookingDates.checkIn || !bookingDates.checkOut) {
        ElMessage.warning('请选择入住和退房日期');
        return;
    }

    // 确保价格已计算
    calculateTotalPrice();

    // 准备订单预览数据
    try {
        const orderPreviewData = {
            homestayId: homestay.value!.id,
            homestayTitle: homestay.value!.title,
            imageUrl: homestay.value!.images && homestay.value!.images.length > 0
                ? homestay.value!.images[0]
                : 'https://picsum.photos/400/300',
            address: homestay.value!.location,
            checkInDate: bookingDates.checkIn.toISOString().split('T')[0],
            checkOutDate: bookingDates.checkOut.toISOString().split('T')[0],
            nights: totalNights.value,
            guestCount: bookingDates.guests,
            price: homestay.value!.pricePerNight,
            baseAmount: basePrice.value,
            cleaningFee: cleaningFee.value,
            serviceFee: serviceFee.value,
            totalAmount: totalPrice.value
        };

        // 将订单预览数据存储到localStorage
        localStorage.setItem('orderPreviewData', JSON.stringify(orderPreviewData));

        // 跳转到订单确认页面
        router.push({
            path: '/order/confirm',
            query: {
                homestayId: homestay.value!.id.toString(),
                title: homestay.value!.title,
                image: homestay.value!.coverImage,
                address: homestay.value!.address || homestay.value!.location,
                checkIn: bookingDates.checkIn ? formatDate(bookingDates.checkIn) : '',
                checkOut: bookingDates.checkOut ? formatDate(bookingDates.checkOut) : '',
                nights: totalNights.value.toString(),
                guests: bookingDates.guests.toString(),
                price: homestay.value!.pricePerNight.toString(),
                baseAmount: basePrice.value.toString(),
                cleaningFee: cleaningFee.value.toString(),
                serviceFee: serviceFee.value.toString(),
                totalAmount: totalPrice.value.toString()
            }
        });
    } catch (error) {
        console.error('准备订单数据时出错:', error);
        ElMessage.error('预订过程中发生错误，请稍后重试');
    }
};

const fetchReviews = async (homestayId: number) => {
    if (loadingReviews.value) return;
    loadingReviews.value = true;

    try {
        // 获取评价列表
        const response = await getHomestayReviews(homestayId, {
            page: reviewPage.value,
            size: reviewSize.value
        });

        if (reviewPage.value === 0) {
            reviews.value = response.data.content || [];

            // 获取评价统计
            try {
                const statsResponse = await getHomestayReviewStats(homestayId);
                if (statsResponse.data && statsResponse.data.detailedRatings) {
                    const detailedRatings = statsResponse.data.detailedRatings;
                    reviewStats.value = [
                        { name: '清洁度', score: detailedRatings.cleanlinessRating || 0 },
                        { name: '准确性', score: detailedRatings.accuracyRating || 0 },
                        { name: '通信', score: detailedRatings.communicationRating || 0 },
                        { name: '位置', score: detailedRatings.locationRating || 0 },
                        { name: '入住', score: detailedRatings.checkInRating || 0 },
                        { name: '性价比', score: detailedRatings.valueRating || 0 },
                    ];
                }
            } catch (statsError) {
                console.error('获取评价统计失败:', statsError);
            }
        } else {
            reviews.value = [...reviews.value, ...(response.data.content || [])];
        }

        reviewPage.value++;
    } catch (error) {
        console.error('获取评价失败:', error);

        // 如果是开发环境，使用模拟数据
        if (process.env.NODE_ENV === 'development') {
            const mockReviews = [
                {
                    id: 1,
                    userId: 101,
                    userName: '张先生',
                    rating: 5,
                    content: '非常棒的住宿体验，房间干净整洁，设施齐全，位置也很方便。房东很热情，给了我们很多当地的旅游建议。下次来还会选择这里。',
                    createTime: '2023-03-15T14:30:00',
                    avatarUrl: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                },
                {
                    id: 2,
                    userId: 102,
                    userName: '李女士',
                    rating: 4.5,
                    content: '房间比照片上看起来要小一些，但是整体还是很满意的。床很舒适，周围环境也很安静。',
                    createTime: '2023-02-22T10:15:00',
                    avatarUrl: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
                    response: '感谢您的评价，关于房间大小的问题我们已经更新了更准确的描述，希望您下次再来入住！'
                },
                {
                    id: 3,
                    userId: 103,
                    userName: '王先生',
                    rating: 4,
                    content: '位置很好，离地铁站很近，购物也方便。就是空调有点吵，希望能改进。',
                    createTime: '2023-01-10T18:45:00',
                    avatarUrl: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                }
            ];

            reviewStats.value = [
                { name: '清洁度', score: 4.8 },
                { name: '准确性', score: 4.7 },
                { name: '通信', score: 4.9 },
                { name: '位置', score: 4.6 },
                { name: '入住', score: 4.8 },
                { name: '性价比', score: 4.5 },
            ];

            reviews.value = reviewPage.value === 0 ? mockReviews : [...reviews.value, ...mockReviews];
            reviewPage.value++;
        }
    } finally {
        loadingReviews.value = false;
    }
};

const loadMoreReviews = () => {
    fetchReviews(Number(route.params.id));
};

const formatAmenity = (amenity: string) => {
    // 转换设施名称，避免显示原始代码或字母
    if (typeof amenity !== 'string') return '';

    // 常见设施映射
    const amenityMap: Record<string, string> = {
        'WIFI': 'WiFi',
        'AC': '空调',
        'TV': '电视',
        'KITCHEN': '厨房',
        'WASHER': '洗衣机',
        'DRYER': '烘干机',
        'PARKING': '停车位',
        'POOL': '游泳池',
        'HOT_TUB': '热水浴缸',
        'GYM': '健身房',
        'BREAKFAST': '早餐',
        'WORKSPACE': '工作区',
        'HEATING': '暖气',
        'PETS_ALLOWED': '允许宠物',
        'SMOKING_ALLOWED': '允许吸烟',
        'ELEVATOR': '电梯',
        'FIREPLACE': '壁炉'
    };

    return amenityMap[amenity.toUpperCase()] || amenity;
};

// 联系房东
const contactHost = () => {
    ElMessage.success('即将联系房东，功能开发中...');
};

const handleDateRangeChange = (val: [Date, Date] | null) => {
    if (val) {
        bookingDates.checkIn = val[0]
        bookingDates.checkOut = val[1]
        calculateTotalPrice()
    } else {
        bookingDates.checkIn = null
        bookingDates.checkOut = null
        totalNights.value = 0
        basePrice.value = 0
        totalPrice.value = 0
    }
}

// 获取房东信息
const fetchHostInfo = async (homestayId: number) => {
    try {
        console.log(`开始获取房东信息，房源ID: ${homestayId}`)
        const response = await getHomestayHostInfo(homestayId)
        const data = response.data

        // 适配房东数据
        hostInfo.value = {
            id: data.id || 1,
            name: data.name || homestay.value?.hostName || '房东',
            avatar: data.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
            rating: data.rating || 4.8,
            accommodations: data.accommodations || 435,
            years: data.years || 3,
            responseRate: data.responseRate || '98%',
            responseTime: data.responseTime || '几小时内',
            companions: data.companions || [
                {
                    name: 'Kamal',
                    avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                },
                {
                    name: 'Gaurav',
                    avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                }
            ]
        }

        console.log('获取房东信息成功:', hostInfo.value)

        // 更新homestay中的房东信息
        if (homestay.value) {
            homestay.value.hostName = hostInfo.value.name
            homestay.value.hostAvatar = hostInfo.value.avatar
            homestay.value.hostRating = hostInfo.value.rating.toString()
            homestay.value.hostAccommodations = hostInfo.value.accommodations.toString()
            homestay.value.hostYears = hostInfo.value.years.toString()
            homestay.value.hostResponseRate = hostInfo.value.responseRate
            homestay.value.hostResponseTime = hostInfo.value.responseTime
        }

        // 更新房东伙伴数据
        hostCompanions.value = hostInfo.value.companions
    } catch (error) {
        console.error('获取房东信息失败:', error)

        // 设置默认房东信息
        if (homestay.value) {
            hostInfo.value = {
                id: 1,
                name: homestay.value.hostName || 'Chandra Datt',
                avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
                rating: 4.8,
                accommodations: 435,
                years: 3,
                responseRate: '98%',
                responseTime: '几小时内',
                companions: [
                    {
                        name: 'Kamal',
                        avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                    },
                    {
                        name: 'Gaurav',
                        avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                    }
                ]
            }

            // 更新homestay中的房东信息
            homestay.value.hostName = hostInfo.value.name
            homestay.value.hostAvatar = hostInfo.value.avatar
            homestay.value.hostRating = hostInfo.value.rating.toString()
            homestay.value.hostAccommodations = hostInfo.value.accommodations.toString()
            homestay.value.hostYears = hostInfo.value.years.toString()
            homestay.value.hostResponseRate = hostInfo.value.responseRate
            homestay.value.hostResponseTime = hostInfo.value.responseTime

            // 更新房东伙伴数据
            hostCompanions.value = hostInfo.value.companions
        }
    }
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
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.gallery-container {
    position: relative;
    height: 500px;
    display: grid;
    grid-template-columns: 1fr 1fr;
    grid-template-areas:
        "main grid1"
        "main grid2";
    gap: 4px;
}

.main-image {
    grid-area: main;
    height: 100%;
    cursor: pointer;
    position: relative;
    overflow: hidden;
}

.main-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s ease;
}

.main-image:hover img {
    transform: scale(1.05);
}

.image-grid {
    display: grid;
    grid-template-rows: 1fr 1fr;
    gap: 4px;
    height: 100%;
}

.sub-image {
    position: relative;
    overflow: hidden;
    cursor: pointer;
}

.sub-image:nth-child(1) {
    grid-area: grid1;
}

.sub-image:nth-child(2) {
    grid-area: grid2;
}

.sub-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s ease;
}

.sub-image:hover img {
    transform: scale(1.05);
}

.image-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(to bottom, rgba(0, 0, 0, 0) 70%, rgba(0, 0, 0, 0.3) 100%);
    opacity: 0;
    transition: opacity 0.3s ease;
    display: flex;
    align-items: center;
    justify-content: center;
}

.main-image:hover .image-overlay,
.sub-image:hover .image-overlay {
    opacity: 1;
}

.overlay-content {
    color: white;
    font-size: 24px;
    background-color: rgba(0, 0, 0, 0.5);
    width: 48px;
    height: 48px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    transform: translateY(20px);
    opacity: 0;
    transition: all 0.3s ease 0.1s;
}

.main-image:hover .overlay-content,
.sub-image:hover .overlay-content {
    transform: translateY(0);
    opacity: 1;
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
    padding: 8px 16px;
    font-weight: 500;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    transition: transform 0.2s ease, box-shadow 0.2s ease;
    z-index: 2;
}

.view-all-photos:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
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

/* 房东简介样式（简单版 - 靠上） */
.host-brief {
    display: flex;
    align-items: center;
    gap: 16px;
    margin: 24px 0;
    padding: 16px;
    background-color: #f8f8f8;
    border-radius: 12px;
}

.host-avatar {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    overflow: hidden;
    flex-shrink: 0;
}

.host-avatar img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.host-brief-info h3 {
    margin: 0 0 8px 0;
    font-size: 16px;
    font-weight: 600;
}

.host-brief-info p {
    margin: 0;
    color: #484848;
    font-size: 14px;
    line-height: 1.4;
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
.reviews-section h2,
.host-detail-section h2 {
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
    height: 400px;
}

.map-placeholder {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

/* 房东详细信息样式（详细版 - 靠下） */
.host-section {
    padding: 32px 0;
    border-top: 1px solid #ebebeb;
    border-bottom: 1px solid #ebebeb;
}

.host-section h2 {
    margin-bottom: 24px;
    font-size: 22px;
    font-weight: 600;
}

.host-detail-layout {
    display: grid;
    grid-template-columns: 280px 1fr;
    gap: 32px;
}

.host-left-card {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.host-profile-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 24px;
    border: 1px solid #ebebeb;
    border-radius: 12px;
    text-align: center;
}

.host-profile-avatar {
    position: relative;
    width: 120px;
    height: 120px;
    margin-bottom: 16px;
}

.host-profile-avatar img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: 50%;
}

.host-badge {
    position: absolute;
    bottom: 0;
    right: 0;
    width: 32px;
    height: 32px;
    background-color: #ff385c;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    border: 2px solid white;
}

.host-name {
    font-size: 20px;
    font-weight: 600;
    margin-bottom: 8px;
}

.host-badge-text {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 14px;
    color: #717171;
}

.host-stats {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    border: 1px solid #ebebeb;
    border-radius: 12px;
    overflow: hidden;
}

.host-stat-item {
    padding: 16px 12px;
    text-align: center;
    border-right: 1px solid #ebebeb;
}

.host-stat-item:last-child {
    border-right: none;
}

.stat-label {
    font-size: 14px;
    color: #717171;
    margin-bottom: 4px;
}

.stat-value {
    font-size: 18px;
    font-weight: 600;
}

.host-right-info {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.host-intro h3 {
    font-size: 18px;
    font-weight: 600;
    margin: 0 0 12px 0;
}

.host-intro p {
    margin: 0;
    line-height: 1.6;
    color: #484848;
}

.host-companions h4,
.host-details h4 {
    font-size: 16px;
    margin: 0 0 16px 0;
}

.companions-list {
    display: flex;
    gap: 16px;
}

.companion-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
}

.companion-avatar {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    overflow: hidden;
}

.companion-avatar img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.companion-name {
    font-size: 14px;
}

.host-detail-info {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
}

.detail-item {
    display: flex;
    align-items: center;
    gap: 8px;
}

.detail-label {
    font-size: 14px;
    color: #717171;
}

.detail-value {
    font-size: 14px;
    font-weight: 600;
}

.host-contact {
    margin-top: 8px;
}

.contact-button {
    padding: 12px 24px;
    font-size: 16px;
}

.host-safety-note {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 16px;
    background-color: #f8f8f8;
    border-radius: 8px;
    margin-top: 16px;
}

.host-safety-note .el-icon {
    color: #717171;
    flex-shrink: 0;
    margin-top: 3px;
}

.host-safety-note p {
    margin: 0;
    font-size: 14px;
    color: #717171;
    line-height: 1.4;
}

.host-footer {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-top: 32px;
    padding-top: 24px;
    border-top: 1px solid #ebebeb;
}

.host-small-avatar {
    position: relative;
    width: 48px;
    height: 48px;
}

.host-small-avatar img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: 50%;
}

.verified-badge {
    position: absolute;
    bottom: -2px;
    right: -2px;
    width: 18px;
    height: 18px;
    background-color: #0084ff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 10px;
    border: 2px solid white;
}

.host-title {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 4px;
}

.host-subtitle {
    font-size: 14px;
    color: #717171;
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

.reviews-section {
    margin-top: 24px;
}

.rating-summary {
    margin: 24px 0;
}

.rating-bars {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
}

.rating-bar-item {
    display: flex;
    align-items: center;
    gap: 8px;
}

.rating-name {
    flex: 0 0 80px;
}

.rating-bar-container {
    flex: 1;
    height: 4px;
    background-color: #eee;
    border-radius: 2px;
}

.rating-bar {
    height: 100%;
    background-color: #333;
    border-radius: 2px;
}

.rating-score {
    width: 30px;
    text-align: right;
}

.review-list {
    margin-top: 32px;
}

.review-item {
    margin-bottom: 32px;
    padding-bottom: 24px;
    border-bottom: 1px solid #eee;
}

.review-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 16px;
}

.reviewer-info {
    display: flex;
    align-items: center;
    gap: 16px;
}

.avatar {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    overflow: hidden;
}

.avatar img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.reviewer-name {
    font-weight: 600;
    margin-bottom: 4px;
}

.review-date {
    color: #717171;
    font-size: 14px;
}

.review-content {
    line-height: 1.6;
    margin-bottom: 16px;
}

.host-response {
    background-color: #f7f7f7;
    padding: 16px;
    border-radius: 8px;
    margin-top: 16px;
}

.response-header {
    font-weight: 600;
    margin-bottom: 8px;
}

.no-reviews {
    margin: 48px 0;
    text-align: center;
}

.load-more {
    text-align: center;
    margin-top: 32px;
}

/* 预订卡片样式 - 优化后 */
.booking-card {
    border-radius: 16px;
    border: 1px solid #e0e0e0;
    padding: 24px;
    box-shadow: 0 6px 24px rgba(0, 0, 0, 0.08);
    background-color: white;
    position: sticky;
    top: 20px;
    height: fit-content;
    transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.booking-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
}

.booking-card-header {
    margin-bottom: 24px;
}

.price-info {
    display: flex;
    align-items: baseline;
    margin-bottom: 12px;
}

.price {
    font-size: 28px;
    font-weight: 700;
    color: #303133;
}

.night {
    font-size: 16px;
    font-weight: normal;
    color: #606266;
    margin-left: 4px;
}

.rating-info {
    display: flex;
    align-items: center;
    gap: 8px;
}

.review-link {
    font-size: 14px;
    color: #606266;
    text-decoration: underline;
    cursor: pointer;
}

.booking-form {
    margin-bottom: 20px;
    border: 1px solid #e0e0e0;
    border-radius: 12px;
    overflow: hidden;
}

.date-picker-container {
    border-bottom: 1px solid #e0e0e0;
}

.date-picker-header {
    display: flex;
    padding: 10px 16px 0;
}

.check-in-label,
.check-out-label {
    font-size: 12px;
    font-weight: 600;
    color: #606266;
    flex: 1;
}

.date-range-picker {
    width: 100%;
    border: none;
}

.date-range-picker :deep(.el-input__wrapper) {
    box-shadow: none !important;
    padding: 8px 16px 16px;
}

.guest-selector-container {
    padding: 10px 16px;
}

.guest-selector-label {
    font-size: 12px;
    font-weight: 600;
    color: #606266;
    margin-bottom: 5px;
}

.guest-dropdown {
    width: 100%;
}

.guest-dropdown :deep(.el-input__wrapper) {
    box-shadow: none !important;
}

.booking-button {
    width: 100%;
    height: 48px;
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 20px;
    border-radius: 8px;
}

.price-breakdown {
    margin-bottom: 20px;
}

.price-row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
    font-size: 14px;
    color: #606266;
}

.price-row.total {
    font-weight: 600;
    color: #303133;
    font-size: 16px;
}

.price-divider {
    height: 1px;
    background-color: #e0e0e0;
    margin: 12px 0;
}

.booking-note {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: #909399;
    padding: 12px 16px;
    background-color: #f7f8fa;
    border-radius: 8px;
}

.booking-note .el-icon {
    font-size: 14px;
}

@media (max-width: 768px) {
    .gallery-container {
        height: auto;
        display: flex;
        flex-direction: column;
    }

    .main-image {
        height: 300px;
    }

    .image-grid {
        position: relative;
        display: grid;
        grid-template-columns: 1fr 1fr;
        grid-template-rows: 150px;
        width: 100%;
    }

    .booking-card {
        position: static;
        margin-top: 20px;
        width: 100%;
    }
}

/* 新增全宽度部分的样式 */
.full-width-section {
    margin: 48px 0;
}

.full-width-section h2 {
    font-size: 22px;
    margin-bottom: 16px;
}

.host-info-simple {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;
}

.host-avatar-large {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    overflow: hidden;
}

.host-avatar-large img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.host-info-text h3 {
    margin: 0 0 8px 0;
    font-size: 16px;
    font-weight: 600;
}

.host-info-text p {
    margin: 0;
    font-size: 14px;
    color: #484848;
    line-height: 1.4;
}

.host-summary {
    padding-top: 12px;
    border-top: 1px solid #ebebeb;
}

.host-summary-item {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    font-size: 14px;
    color: #484848;
}

.host-summary-item div {
    margin-bottom: 8px;
}
</style>