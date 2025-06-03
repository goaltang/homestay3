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
                        <span>{{ formattedRating }}</span>
                        <span class="review-count">({{ formattedReviewCount }}条评价)</span>
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
                            <component :is="isFavorite ? 'Star' : 'StarFilled'" />
                        </el-icon>
                        {{ isFavorite ? '已收藏' : '收藏' }}
                    </el-button>
                </div>
            </div>

            <!-- 图片区域 - 重构 -->
            <div class="image-gallery-new" v-if="allProcessedImages.length > 0">
                <!-- 5+ images -->
                <div class="gallery-layout-5plus" v-if="allProcessedImages.length >= 5">
                    <div class="main-image-wrapper" @click="showAllPhotos">
                        <img :src="mainImageUrl" :alt="homestay.title" class="main-img" />
                        <div class="image-overlay"></div>
                    </div>
                    <div class="grid-image-wrapper">
                        <div v-for="(image, index) in gridImageUrls.slice(0, 4)" :key="index" class="grid-img-item"
                            @click="showAllPhotos">
                            <img :src="image" :alt="`${homestay.title} - 图片 ${index + 2}`" class="grid-img" />
                            <div class="image-overlay"></div>
                        </div>
                    </div>
                </div>

                <!-- 4 images -->
                <div v-else-if="allProcessedImages.length === 4" class="gallery-layout-4" @click="showAllPhotos">
                    <div v-for="(image, index) in allProcessedImages" :key="index" class="grid-img-item-4">
                        <img :src="image" :alt="`${homestay.title} - 图片 ${index + 1}`" class="grid-img-4" />
                        <div class="image-overlay"></div>
                    </div>
                </div>

                <!-- 3 images -->
                <div v-else-if="allProcessedImages.length === 3" class="gallery-layout-3" @click="showAllPhotos">
                    <div v-for="(image, index) in allProcessedImages" :key="index" class="grid-img-item-3">
                        <img :src="image" :alt="`${homestay.title} - 图片 ${index + 1}`" class="grid-img-3" />
                        <div class="image-overlay"></div>
                    </div>
                </div>

                <!-- 2 images -->
                <div v-else-if="allProcessedImages.length === 2" class="gallery-layout-2" @click="showAllPhotos">
                    <div v-for="(image, index) in allProcessedImages" :key="index" class="grid-img-item-2">
                        <img :src="image" :alt="`${homestay.title} - 图片 ${index + 1}`" class="grid-img-2" />
                        <div class="image-overlay"></div>
                    </div>
                </div>

                <!-- 1 image -->
                <div v-else-if="allProcessedImages.length === 1" class="gallery-layout-1" @click="showAllPhotos">
                    <div class="main-image-wrapper-single">
                        <img :src="mainImageUrl" :alt="homestay.title" class="main-img-single" />
                        <div class="image-overlay"></div>
                    </div>
                </div>

                <!-- '查看全部照片' 按钮 - 仅在多于一张图片时显示 -->
                <el-button class="view-all-photos" @click="showAllPhotos" v-if="allProcessedImages.length > 1">
                    <el-icon>
                        <Camera />
                    </el-icon>
                    查看全部 {{ homestay.images ? homestay.images.length : 0 }} 张照片
                </el-button>
            </div>
            <!-- Fallback if no images processed (should generally not happen if homestay exists) -->
            <div v-else class="no-images-placeholder">
                <el-empty description="暂无房源图片"></el-empty>
            </div>
            <!-- End of Image Gallery -->

            <!-- 内容区域 -->
            <div class="detail-content">
                <div class="content-main">
                    <!-- 房东信息 - 简单版 -->
                    <div class="host-brief">
                        <div class="host-avatar">
                            <img :src="hostDisplayAvatar" :alt="hostDisplayName">
                        </div>
                        <div class="host-brief-info">
                            <h3>{{ hostDisplayName }}</h3>
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
                                <h3>整套房源</h3>
                                <p>您将拥有整个空间，享受私密住宿体验。</p>
                            </div>
                        </div>
                        <div class="feature-item">
                            <el-icon>
                                <Location />
                            </el-icon>
                            <div class="feature-text">
                                <h3>绝佳位置</h3>
                                <p v-if="homestay.distanceFromCenter">距离市中心仅{{ homestay.distanceFromCenter }}公里。</p>
                                <p v-else>地理位置优越。</p>
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
                                <span>{{ amenity.label }}</span>
                            </div>
                        </div>
                    </div>

                    <el-divider />
                </div>

                <!-- 预订卡片 -->
                <div class="booking-card">
                    <div class="booking-card-header">
                        <div class="price-info">
                            <span class="price">¥{{ parsedPricePerNight }}</span>
                            <span class="night">/晚</span>
                        </div>
                        <div class="rating-info">
                            <el-rate v-model="numericRating" disabled text-color="#FF9900"
                                disabled-void-color="#C6D1DE" />
                            <span class="review-link">{{ formattedReviewCount }}条评价</span>
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
                            <el-select v-model="bookingDates.guests" placeholder="1位房客" :size="'large'"
                                class="guest-dropdown">
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
                            <div class="price-item">¥{{ parsedPricePerNight }} x {{ totalNights }}晚</div>
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

            <!-- 位置信息 - 全新设计 -->
            <div class="full-width-section location-section">
                <h2>位置信息</h2>

                <!-- 地址信息 -->
                <div class="address-info">
                    <div class="primary-location">
                        <el-icon class="location-icon">
                            <Location />
                        </el-icon>
                        <div class="location-text">
                            <div class="main-address">{{ formattedLocation }}</div>
                            <div class="detail-address" v-if="homestay.addressDetail">
                                {{ homestay.addressDetail }}
                            </div>
                        </div>
                    </div>

                    <div class="location-features" v-if="homestay.distanceFromCenter">
                        <div class="distance-info">
                            <el-icon>
                                <House />
                            </el-icon>
                            <span>距离市中心 {{ homestay.distanceFromCenter }} 公里</span>
                        </div>
                    </div>
                </div>

                <!-- 地图区域 -->
                <div class="map-section">
                    <div class="map-container" @click="openMapModal">
                        <!-- 加载状态 -->
                        <div v-if="mapData.isLoading" class="map-loading">
                            <el-skeleton :rows="5" animated />
                            <div class="loading-text">正在加载地图...</div>
                        </div>

                        <!-- 真实地图 -->
                        <div v-else-if="mapData.hasLocation && mapData.staticMapUrl" class="map-placeholder">
                            <div class="map-overlay">
                                <el-icon class="map-icon">
                                    <Location />
                                </el-icon>
                                <div class="map-text">
                                    <div class="map-title">查看详细地图</div>
                                    <div class="map-subtitle">点击打开交互式地图</div>
                                </div>
                            </div>
                            <img :src="mapData.staticMapUrl" alt="房源位置地图" class="map-image" @error="onMapImageError" />
                        </div>

                        <!-- fallback 地图 -->
                        <div v-else class="map-placeholder">
                            <div class="map-overlay">
                                <el-icon class="map-icon">
                                    <Location />
                                </el-icon>
                                <div class="map-text">
                                    <div class="map-title">查看地图</div>
                                    <div class="map-subtitle">点击查看位置信息</div>
                                </div>
                            </div>
                            <img src="https://picsum.photos/800/400?random=location" alt="位置地图" class="map-image" />
                        </div>
                    </div>
                </div>

                <!-- 周边信息 -->
                <div class="nearby-info" v-if="nearbyPlaces.length > 0">
                    <h3>周边设施</h3>
                    <div class="nearby-grid">
                        <div v-for="(place, index) in nearbyPlaces" :key="index" class="nearby-item">
                            <el-icon class="facility-icon">
                                <Location v-if="place.type === '地铁站'" />
                                <House v-else-if="place.type === '商场'" />
                                <Check v-else />
                            </el-icon>
                            <div class="facility-info">
                                <div class="facility-name">{{ place.name }}</div>
                                <div class="facility-distance">{{ formatDistance(place.distance) }}</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <el-divider />

            <!-- 房东详细信息（靠下）- 调整为全宽度 -->
            <div class="full-width-section host-section">
                <h2>旅居主人简介</h2>
                <!-- 添加 v-if="hostDetailInfo" 确保数据加载后再渲染 -->
                <div v-if="hostDetailInfo" class="host-detail-layout">
                    <!-- 左侧卡片: 头像, 姓名, 统计数据 -->
                    <div class="host-left-card">
                        <div class="host-profile-card">
                            <div class="host-profile-avatar">
                                <!-- 优先使用 hostDetailInfo 的头像 -->
                                <img :src="hostDetailInfo.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                                    :alt="hostDetailInfo.nickname || hostDetailInfo.username || '房东'">
                            </div>
                            <!-- 优先用 nickname，其次 username -->
                            <div class="host-name">{{ hostDetailInfo.nickname || hostDetailInfo.username || '房东' }}
                            </div>
                            <!-- Optional: Display verification status if available -->
                            <div class="host-verification" v-if="hostDetailInfo.verification_status === 'VERIFIED'">
                                <el-tag type="success" size="small"><el-icon>
                                        <Check />
                                    </el-icon> 已认证</el-tag>
                            </div>
                            <div class="host-verification" v-else-if="hostDetailInfo.verification_status">
                                <!-- Can customize display based on other statuses -->
                                <el-tag type="warning" size="small">{{ hostDetailInfo.verification_status }}</el-tag>
                            </div>
                        </div>

                        <!-- 调整统计数据项 -->
                        <div class="host-stats">
                            <!-- 评价数 -->
                            <div class="host-stat-item">
                                <div class="stat-label">评价</div>
                                <!-- Use reviewCount from hostDetailInfo -->
                                <div class="stat-value">{{ hostDetailInfo.reviewCount ?? 0 }} 条</div>
                            </div>
                            <!-- 房源数 -->
                            <div class="host-stat-item">
                                <div class="stat-label">房源</div>
                                <div class="stat-value">{{ hostDetailInfo.homestayCount ?? '-' }} 间</div>
                            </div>
                            <!-- 订单数 (可选展示) -->
                            <div class="host-stat-item"
                                v-if="hostDetailInfo.orderCount !== null && hostDetailInfo.orderCount !== undefined">
                                <div class="stat-label">订单</div>
                                <div class="stat-value">{{ hostDetailInfo.orderCount }} 单</div>
                            </div>
                        </div>
                    </div>

                    <!-- 右侧信息: 简介, 伙伴, 详情, 联系按钮 -->
                    <div class="host-right-info">
                        <!-- 简介 (使用 introduction 字段) -->
                        <div class="host-intro">
                            <!-- 使用与左侧卡片一致的名称逻辑 -->
                            <h3>关于 {{ hostDetailInfo.nickname || hostDetailInfo.username || '房东' }}</h3>
                            <p>{{ hostDetailInfo.introduction || '这位房东比较内向，还没有填写简介哦~' }}</p>
                        </div>

                        <!-- 接待伙伴 (使用计算属性 parsedCompanions) - v-if 会处理空数组 -->
                        <div class="host-companions" v-if="parsedCompanions.length > 0">
                            <h4>接待伙伴</h4>
                            <div class="companions-list">
                                <div class="companion-item" v-for="(companion, index) in parsedCompanions" :key="index">
                                    <div class="companion-avatar">
                                        <img :src="companion.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                                            :alt="companion.name">
                                    </div>
                                    <div class="companion-name">{{ companion.name }}</div>
                                </div>
                            </div>
                        </div>

                        <!-- 房东详情 - 移除回复率/时间，保留加入时间和语言 -->
                        <div class="host-details">
                            <h4>房东信息</h4>
                            <!-- 添加 v-if 判断是否有任何有效信息 -->
                            <div class="host-detail-info"
                                v-if="hostDetailInfo.hostSince || (parsedLanguages && parsedLanguages.length > 0)">
                                <!-- 加入时间 (格式化) -->
                                <div class="detail-item" v-if="hostDetailInfo.hostSince">
                                    <div class="detail-label">加入时间:</div>
                                    <div class="detail-value">{{ formatDisplayDate(hostDetailInfo.hostSince) }}</div>
                                </div>
                                <!-- 语言 (使用计算属性) - v-if 处理空数组 -->
                                <div class="detail-item" v-if="parsedLanguages.length > 0">
                                    <div class="detail-label">语言:</div>
                                    <div class="detail-value">{{ parsedLanguages.join(', ') }}</div>
                                </div>
                            </div>
                            <!-- 如果没有详细信息，可以显示提示 -->
                            <p v-else>暂无更多详细信息。</p>
                        </div>

                        <!-- 联系按钮 -->
                        <div class="host-contact">
                            <el-button type="primary" class="contact-button" @click="contactHost">
                                发送消息给房东
                            </el-button>
                        </div>

                        <!-- 安全提示 -->
                        <div class="host-safety-note">
                            <el-icon>
                                <InfoFilled />
                            </el-icon>
                            <p>为保障您的安全，请始终通过平台沟通，切勿在线下分享联系方式或付款。</p>
                        </div>
                    </div>
                </div>
                <!-- 如果 hostDetailInfo 加载失败或不存在，可以显示提示 -->
                <div v-else-if="!loading && homestay">
                    <p>暂无详细房东信息。</p> <!-- Or a loading spinner specifically for host info -->
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
                        {{ formattedRating }} · {{ formattedReviewCount }}条评价
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
                                    <img :src="getReviewerAvatar(review)" :alt="review.userName">
                                </div>
                                <div class="reviewer-details">
                                    <div class="reviewer-name">{{ review.userName }}</div>
                                    <div class="review-date">{{ formatDisplayDate(review.createTime) }}</div>
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
                    <el-empty description="暂无评价" v-if="formattedReviewCount === 0">
                        <span>这个房源还没有收到评价</span>
                    </el-empty>
                    <div class="loading-reviews" v-else>
                        <el-skeleton :rows="3" animated />
                    </div>
                </div>

                <!-- 加载更多按钮 -->
                <div class="load-more" v-if="homestay && reviews.length > 0 && reviews.length < totalReviewCount">
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
import request from '@/utils/request' // Assuming default request instance is okay
import { getHomestayById } from '@/api/homestay'
import { getHomestayReviews, getHomestayReviewStats } from '@/api/review'
import { createOrder } from '@/api/order'
import { getHomestayHostInfo } from '@/api/host'
import { useAuthStore } from '@/stores/auth'
import { useUserStore } from '@/stores/user'
import { regionData, codeToText } from 'element-china-area-data'
import {
    geocodeAddress,
    generateStaticMapUrl,
    addPrivacyOffset,
    searchNearbyPlaces,
    formatDistance,
    FACILITY_TYPES
} from '@/utils/mapService'

// --- Start Local Type Definitions (Recommend exporting from API files later) ---

// Simplified Homestay Type (与后端 API 保持一致)
interface Homestay {
    id: number;
    title: string;
    price: number; // 每晚价格
    maxGuests: number;
    images?: (string | { url: string })[];
    coverImage?: string;
    reviewCount?: number;
    rating?: number | null;

    // 房东信息 (使用统一的字段名)
    ownerId?: number;
    ownerName?: string | null;
    ownerUsername?: string | null;
    ownerAvatar?: string;

    // 房源详情
    amenities?: any[];
    description?: string;
    propertyType?: string;
    bedrooms?: number;
    beds?: number;
    bathrooms?: number;
    type?: string;
    status?: string;
    minNights?: number;
    featured?: boolean;
    distanceFromCenter?: number;

    // 位置信息
    provinceCode?: string;
    cityCode?: string;
    districtCode?: string;
    addressDetail?: string;

    // 时间戳
    createdAt?: string | Date;
    updatedAt?: string | Date;
}

// Simplified Review Type
interface Review {
    id: number;
    userName: string;
    userAvatar?: string; // 修改字段名以匹配后端
    rating: number;
    content: string;
    createTime: string;
    response?: string;
    responseTime?: string;
}

// Simplified Review Stat Item Type
interface ReviewStatItem {
    name: string;
    score: number;
}

// Simplified Booking Dates Type
interface BookingDates {
    checkIn: Date | null;
    checkOut: Date | null;
    guests: number;
}

// --- FINAL Host Detail Info Type based on API response ---
interface HostDetailInfoData {
    id: number; // Keep for internal use if needed, but don't display
    username?: string; // Use as fallback name
    nickname?: string | null; // Preferred name
    avatar?: string | null; // Host avatar
    introduction?: string | null; // Host introduction
    languages?: string[] | null; // Languages spoken (might be empty or null)
    companions?: Array<{ name: string, avatar?: string }> | null; // Companions (might be empty or null)
    hostSince?: string | Date | null; // Date joined
    // hostRating?: number | null; // API returns null, maybe rely on homestay rating or review stats
    homestayCount?: number | null; // Number of listings
    reviewCount?: number | null; // Number of reviews received by host
    // hostYears?: number | null; // API returns null
    // hostResponseRate?: string | null; // API returns null
    // hostResponseTime?: string | null; // API returns null
    orderCount?: number | null; // Number of orders
    verification_status?: string | null; // Optional: Verification status
    // Omit sensitive fields: email, phone, realName, idCard, occupation
    // Omit fields that are null and less critical: hostAccommodations, rating (use reviewCount/homestay rating)
}

// Type for createOrder payload (match API expectation)
interface CreateOrderPayload {
    homestayId: number;
    checkInDate: string;
    checkOutDate: string;
    guestCount: number;
    totalPrice: number;
    guestName: string;
    guestPhone: string;
}


// --- End Local Type Definitions ---

// --- Local Utility Functions ---
// Date Formatter for API (YYYY-MM-DD)
const formatDateString = (date: Date | string | null): string => {
    if (!date) return '';
    try {
        const d = typeof date === 'string' ? new Date(date) : date;
        if (!(d instanceof Date) || isNaN(d.getTime())) {
            console.error("Invalid date object for formatting:", date);
            return '';
        }
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0'); // Ensure 2 digits for month
        const day = String(d.getDate()).padStart(2, '0');   // Ensure 2 digits for day
        return `${year}-${month}-${day}`; // Use YYYY-MM-DD format
    } catch (e) {
        console.error("Error formatting date:", date, e);
        return '';
    }
};
// Optional: Create a separate function for display format if needed elsewhere
const formatDisplayDate = (date: Date | string | null): string => {
    if (!date) return '日期无效';
    try {
        const d = typeof date === 'string' ? new Date(date) : date;
        if (!(d instanceof Date) || isNaN(d.getTime())) return '日期无效';
        const year = d.getFullYear();
        const month = d.getMonth() + 1;
        const day = d.getDate();
        return `${year}年${month}月${day}日`;
    } catch (e) {
        return '日期格式错误';
    }
}
// --- End Local Utility Functions ---


// Refs and Stores
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore() // Initialize store
const userStore = useUserStore() // Initialize store

const loading = ref(true)
const homestay = ref<Homestay | null>(null)
const hostDetailInfo = ref<HostDetailInfoData | null>(null) // Use FINAL type
const isFavorite = ref(false)
const reviews = ref<Review[]>([])
const reviewStats = ref<ReviewStatItem[]>([])
const totalReviewCount = ref(0) // 添加总评价数量
const reviewsPage = ref(1)
const reviewsPageSize = 5
const allProcessedImages = ref<string[]>([])

// 地图相关数据
const mapData = ref({
    lat: 0,
    lng: 0,
    staticMapUrl: '',
    isLoading: false,
    hasLocation: false
})
const nearbyPlaces = ref<Array<{ name: string, type: string, distance: number, address: string }>>([])
const showMapModal = ref(false)

// Booking related refs
const bookingDates = reactive<BookingDates>({
    checkIn: null,
    checkOut: null,
    guests: 1
});
const bookingDateRange = ref<[Date, Date] | null>(null)

// Computed properties
const mainImageUrl = computed(() => allProcessedImages.value.length > 0 ? allProcessedImages.value[0] : 'https://via.placeholder.com/800x600?text=No+Image');
const gridImageUrls = computed(() => allProcessedImages.value.slice(1));

// Computed property for parsed price per night
const parsedPricePerNight = computed(() => {
    // Ensure homestay and price exist and are valid numbers
    // 尝试多个可能的价格字段名
    if (homestay.value) {
        let priceValue: number | undefined;

        // 尝试不同的价格字段名称
        if (typeof homestay.value.price !== 'undefined' && homestay.value.price !== null) {
            priceValue = Number(homestay.value.price);
        } else if (typeof (homestay.value as any).pricePerNight !== 'undefined' && (homestay.value as any).pricePerNight !== null) {
            priceValue = Number((homestay.value as any).pricePerNight);
        }

        if (priceValue !== undefined && !isNaN(priceValue)) {
            return priceValue;
        }
    }
    return 0; // Default to 0 if no valid price
});

// Computed property for total nights
const totalNights = computed(() => {
    if (bookingDates.checkIn && bookingDates.checkOut) {
        const checkInTime = bookingDates.checkIn.getTime();
        const checkOutTime = bookingDates.checkOut.getTime();
        if (checkOutTime > checkInTime) {
            const diffTime = checkOutTime - checkInTime;
            return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        }
    }
    return 0;
});

// Computed property for base price
const basePrice = computed(() => {
    return parsedPricePerNight.value * totalNights.value;
});

// Computed property for cleaning fee
const cleaningFee = computed(() => {
    // Calculate only if there are nights booked and a valid price
    if (totalNights.value > 0 && parsedPricePerNight.value > 0) {
        // Use Math.round for rounding, adjust if needed
        return Math.round(parsedPricePerNight.value * 0.1);
    }
    return 0;
});

// Computed property for service fee
const serviceFee = computed(() => {
    // Calculate only if base price is positive
    if (basePrice.value > 0) {
        // Use Math.round, adjust if needed
        return Math.round(basePrice.value * 0.05);
    }
    return 0;
});

// Computed property for total price
const totalPrice = computed(() => {
    // Ensure all components are numbers before summing
    return basePrice.value + cleaningFee.value + serviceFee.value;
});

// Use computed properties for languages and companions (handle null/empty array)
const parsedLanguages = computed((): string[] => {
    // API response shows languages is [], which is already an array
    return hostDetailInfo.value?.languages && Array.isArray(hostDetailInfo.value.languages)
        ? hostDetailInfo.value.languages
        : [];
});

const parsedCompanions = computed((): Array<{ name: string, avatar?: string }> => {
    // API response shows companions is [], which is already an array
    return hostDetailInfo.value?.companions && Array.isArray(hostDetailInfo.value.companions)
        // Optional: add validation if needed: && hostDetailInfo.value.companions.every(item => ...)
        ? hostDetailInfo.value.companions
        : [];
});

// Computed property for formatted location display
const formattedLocation = computed(() => {
    if (!homestay.value) return '位置待更新';

    const parts = [];

    // 使用省市区代码组合位置信息
    if (homestay.value.provinceCode && codeToText[homestay.value.provinceCode]) {
        parts.push(codeToText[homestay.value.provinceCode]);
    }
    if (homestay.value.cityCode && codeToText[homestay.value.cityCode]) {
        // 避免重复省份（如直辖市）
        if (!parts.includes(codeToText[homestay.value.cityCode])) {
            parts.push(codeToText[homestay.value.cityCode]);
        }
    }
    if (homestay.value.districtCode && codeToText[homestay.value.districtCode]) {
        parts.push(codeToText[homestay.value.districtCode]);
    }

    if (parts.length > 0) {
        return parts.join(' · ');
    }

    // 后备方案：使用详细地址字段
    return homestay.value.addressDetail || '位置待更新';
});

// Computed property for formatted rating display
const formattedRating = computed(() => {
    console.log('=== formattedRating 计算过程 ===');
    console.log('reviewStats.value:', reviewStats.value);
    console.log('totalReviewCount.value:', totalReviewCount.value);
    console.log('reviews.value.length:', reviews.value.length);

    // 优先使用评价统计中的平均评分
    if (reviewStats.value.length > 0) {
        const avgRating = reviewStats.value.reduce((sum, stat) => sum + stat.score, 0) / reviewStats.value.length;
        console.log('使用评价统计平均评分:', avgRating.toFixed(1));
        return avgRating.toFixed(1);
    }

    // 如果有评价但没有评价统计，根据评价数量给出默认评分
    if (totalReviewCount.value > 0 || reviews.value.length > 0) {
        // 如果有具体评价且有评分，计算平均评分
        if (reviews.value.length > 0) {
            const ratingsWithScores = reviews.value.filter(review => review.rating && review.rating > 0);
            console.log('评价中有评分的条目:', ratingsWithScores);
            if (ratingsWithScores.length > 0) {
                const avgRating = ratingsWithScores.reduce((sum, review) => sum + review.rating, 0) / ratingsWithScores.length;
                console.log('使用评价中的平均评分:', avgRating.toFixed(1));
                return avgRating.toFixed(1);
            }
        }
        // 如果有评价但没有具体评分，给出默认好评评分
        console.log('使用默认好评评分: 4.5');
        return '4.5'; // 默认好评评分
    }

    // 后备方案：使用 homestay 中的评分（可能为 undefined）
    if (homestay.value && homestay.value.rating) {
        const rating = Number(homestay.value.rating);
        const result = isNaN(rating) ? '暂无评分' : rating.toFixed(1);
        console.log('使用homestay评分:', result);
        return result;
    }

    console.log('最终返回: 暂无评分');
    return '暂无评分';
});

// Computed property for review count display  
const formattedReviewCount = computed(() => {
    // 优先使用从评价接口获取的总数量
    if (totalReviewCount.value > 0) {
        return totalReviewCount.value;
    }

    // 其次使用当前已加载的评价数量
    if (reviews.value.length > 0) {
        return reviews.value.length;
    }

    // 后备方案：使用 homestay 中的评价数量（可能为 undefined）
    if (homestay.value && homestay.value.reviewCount) {
        const count = Number(homestay.value.reviewCount);
        return isNaN(count) ? 0 : count;
    }

    return 0;
});

// 为 el-rate 组件提供数值类型的评分
const numericRating = computed(() => {
    const rating = formattedRating.value;
    if (rating === '暂无评分') {
        return 0;
    }
    return parseFloat(rating);
});

// Methods
const disablePastDates = (date: Date) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return date.getTime() < today.getTime();
};


const handleDateRangeChange = (dates: [Date, Date] | null) => {
    if (dates && dates.length === 2 && dates[0] && dates[1]) {
        if (dates[1].getTime() <= dates[0].getTime()) {
            ElMessage.warning('退房日期必须在入住日期之后');
            bookingDateRange.value = null;
            bookingDates.checkIn = null;
            bookingDates.checkOut = null;
        } else {
            bookingDates.checkIn = dates[0];
            bookingDates.checkOut = dates[1];
        }
    } else {
        bookingDates.checkIn = null;
        bookingDates.checkOut = null;
    }
}

const bookHomestay = async () => {
    // 1. 检查登录状态
    if (!authStore.isAuthenticated) {
        ElMessageBox.confirm('您需要登录才能预订，是否现在登录？', '提示', {
            confirmButtonText: '去登录',
            cancelButtonText: '取消',
            type: 'warning',
        }).then(() => {
            router.push({ path: '/login', query: { redirect: route.fullPath } });
        }).catch(() => {
            ElMessage.info('取消预订');
        });
        return;
    }

    // 2. 检查日期和民宿信息
    if (!bookingDates.checkIn || !bookingDates.checkOut || !homestay.value || totalNights.value <= 0) {
        ElMessage.warning('请先选择有效的入住和退房日期');
        return;
    }

    // 3. 检查用户信息 (如果确认页需要展示或预填)
    if (!userStore.userInfo) {
        // 可以尝试获取，或者提示用户稍后在确认页填写
        console.warn('用户信息未加载，可能需要在确认页处理');
        // 如果确认页强依赖用户信息，这里可以阻止跳转或先获取用户信息
        // await userStore.fetchUserInfo(); // 假设有这个 action
        // if (!userStore.userInfo) { ElMessage.error('无法获取用户信息'); return; }
    }

    // 4. 准备传递给确认页的数据
    const bookingDetails = {
        homestayId: homestay.value!.id,
        checkInDate: formatDateString(bookingDates.checkIn),
        checkOutDate: formatDateString(bookingDates.checkOut),
        guestCount: bookingDates.guests,
        totalPrice: totalPrice.value,
        // 可以考虑传递更多信息供确认页展示，例如：
        // homestayTitle: homestay.value.title,
        // pricePerNight: parsedPricePerNight.value,
        // cleaningFee: cleaningFee.value,
        // serviceFee: serviceFee.value
        // totalNights: totalNights.value
    };

    console.log("准备跳转到订单确认页，传递数据:", bookingDetails);

    // 5. 跳转到订单确认页，使用 query 参数传递数据
    // 注意：如果数据复杂或包含敏感信息，考虑使用 Pinia store 或 route state 传递
    router.push({
        path: '/order/confirm',
        query: {
            homestayId: bookingDetails.homestayId.toString(),
            checkIn: bookingDetails.checkInDate,
            checkOut: bookingDetails.checkOutDate,
            guests: bookingDetails.guestCount.toString(),
            price: bookingDetails.totalPrice.toString(),
            // 其他需要传递的参数...
        }
    });

    // --- 原来的 createOrder 调用逻辑需要移动到订单确认页 --- 
    /*
    const orderPayload: CreateOrderPayload = {
        homestayId: homestay.value!.id,
        checkInDate: formatDateString(bookingDates.checkIn),
        checkOutDate: formatDateString(bookingDates.checkOut),
        guestCount: bookingDates.guests,
        totalPrice: totalPrice.value,
        guestName: userStore.userInfo?.username || '用户信息缺失',
        guestPhone: userStore.userInfo?.phone || '用户信息缺失'
    };
    if (orderPayload.guestName === '用户信息缺失' || orderPayload.guestPhone === '用户信息缺失') { 
        ElMessage.warning('缺少必要的住客信息，请完善个人资料');
        // 考虑跳转到用户中心或提供修改入口
        return; 
    }
    console.log("准备创建订单，数据:", orderPayload);
    try {
        const response = await createOrder(orderPayload);
        ElMessage.success('订单创建成功！');

        if (response?.data?.id) {
            console.log(`订单创建成功，跳转到成功页面，订单ID: ${response.data.id}`);
            router.push(`/orders/submit-success/${response.data.id}`);
        } else {
            console.warn('创建订单成功，但未收到订单ID，跳转到用户订单列表');
            router.push('/user/orders');
        }
    } catch (error: any) { 
        console.error("创建订单时出错:", error);
        let message = '创建订单失败，请稍后重试';
        if (error.response?.data?.message) {
            message = error.response.data.message;
        } else if (error.message) {
            message = error.message;
        }
        ElMessage.error(message);
    }
    */
};

const fetchReviewsAndStats = async (homestayId: number) => {
    try {
        // 获取评价列表
        const reviewsResponse = await getHomestayReviews(homestayId, {
            page: reviewsPage.value - 1, // 转换为 0-based
            size: reviewsPageSize
        });

        if (reviewsResponse?.data?.content) {
            if (reviewsPage.value === 1) {
                reviews.value = reviewsResponse.data.content;
            } else {
                reviews.value.push(...reviewsResponse.data.content);
            }

            // 更新总评价数量
            if (reviewsResponse.data.totalElements !== undefined) {
                totalReviewCount.value = reviewsResponse.data.totalElements;
                console.log(`总评价数量: ${totalReviewCount.value}`);
            }
        }

        // 获取评价统计
        const statsResponse = await getHomestayReviewStats(homestayId);
        console.log('评价统计接口响应:', statsResponse);
        if (statsResponse?.data) {
            console.log('评价统计原始数据:', statsResponse.data);
            reviewStats.value = [
                { name: '清洁度', score: statsResponse.data.cleanlinessRating || 0 },
                { name: '准确性', score: statsResponse.data.accuracyRating || 0 },
                { name: '沟通', score: statsResponse.data.communicationRating || 0 },
                { name: '位置', score: statsResponse.data.locationRating || 0 },
                { name: '入住', score: statsResponse.data.checkInRating || 0 },
                { name: '性价比', score: statsResponse.data.valueRating || 0 }
            ].filter(stat => stat.score > 0); // 只显示有评分的项目

            console.log('处理后的评价统计:', reviewStats.value);
        } else {
            console.warn('评价统计接口未返回数据或返回空数据');
        }
    } catch (error) {
        console.error('获取评价数据失败:', error);
        // 不抛出错误，只是无法显示评价数据
    }
};

const loadMoreReviews = () => {
    if (homestay.value && reviews.value.length < totalReviewCount.value) {
        reviewsPage.value++;
        fetchReviewsAndStats(homestay.value.id);
    } else {
        ElMessage.info('没有更多评价了');
    }
};

// Main data fetching function
const fetchData = async () => {
    loading.value = true;
    const homestayId = Number(route.params.id);
    console.log(`Fetching data for Homestay ID: ${homestayId}`);
    if (isNaN(homestayId) || homestayId <= 0) { /* ... invalid ID handling ... */ return; }

    try {
        // Reset state
        homestay.value = null; hostDetailInfo.value = null; reviews.value = [];
        reviewStats.value = []; totalReviewCount.value = 0; reviewsPage.value = 1; allProcessedImages.value = [];
        bookingDateRange.value = null; bookingDates.checkIn = null; bookingDates.checkOut = null;
        bookingDates.guests = 1;
        // 重置地图数据
        mapData.value = { lat: 0, lng: 0, staticMapUrl: '', isLoading: false, hasLocation: false };
        nearbyPlaces.value = [];

        // 1. Fetch base homestay info
        const homestayResponse = await getHomestayById(homestayId);
        if (!homestayResponse?.data) { throw new Error('未找到民宿信息'); }
        homestay.value = homestayResponse.data;

        // 添加调试日志，查看后端返回的实际数据结构
        if (homestay.value) {
            console.log("=== 后端返回的民宿数据结构 ===");
            console.log("完整数据:", homestay.value);
            console.log("价格字段 (price):", homestay.value.price);
            console.log("价格字段 (pricePerNight):", (homestay.value as any).pricePerNight);
            console.log("位置相关字段:");
            console.log("  - addressDetail:", homestay.value.addressDetail);
            console.log("  - provinceCode:", homestay.value.provinceCode);
            console.log("  - cityCode:", homestay.value.cityCode);
            console.log("  - districtCode:", homestay.value.districtCode);
            console.log("评价相关字段:");
            console.log("  - rating:", homestay.value.rating);
            console.log("  - reviewCount:", homestay.value.reviewCount);
            console.log("房东相关字段:");
            console.log("  - ownerName:", homestay.value.ownerName);
            console.log("  - ownerUsername:", homestay.value.ownerUsername);
            console.log("  - ownerAvatar:", homestay.value.ownerAvatar);
            console.log("===============================");
        }

        if (homestay.value) {
            console.log("Base homestay info received:", homestay.value);

            // 2. Process images
            const rawImageUrls = homestay.value.images && Array.isArray(homestay.value.images)
                ? homestay.value.images.map((img: string | { url: string }) => typeof img === 'string' ? img : img?.url).filter(Boolean) as string[]
                : [];
            const coverImageUrl = homestay.value.coverImage;
            let finalImages: string[] = [];
            if (coverImageUrl) {
                finalImages.push(coverImageUrl);
                rawImageUrls.forEach(imgUrl => { if (imgUrl !== coverImageUrl) { finalImages.push(imgUrl); } });
            } else { finalImages = rawImageUrls; }
            allProcessedImages.value = finalImages;
            console.log(`Processed ${allProcessedImages.value.length} images.`);
        }

        // 3. Fetch initial reviews and stats
        await fetchReviewsAndStats(homestayId);

        // 4. Fetch detailed host info
        const hostLookupId = homestayId;
        if (hostLookupId) {
            console.log(`Attempting to fetch host info using homestay ID: ${hostLookupId}`);
            try {
                const hostInfoResponse = await getHomestayHostInfo(hostLookupId);
                if (hostInfoResponse?.data) {
                    hostDetailInfo.value = hostInfoResponse.data;
                    console.log("Detailed host info received:", hostDetailInfo.value);

                    // Check if homestay.value exists before accessing it
                    if (homestay.value && hostDetailInfo.value) { // Add null check for hostDetailInfo.value
                        // hostDetailInfo 已获取，但不再直接修改 homestay.value 的字段
                        // 使用计算属性 hostDisplayName 和 hostDisplayAvatar 来显示房东信息
                        console.log("详细房东信息已获取，将通过计算属性显示");
                    }
                } else { console.warn(`No detailed host info returned for homestay ${hostLookupId}`); }
            } catch (hostError) { console.error(`获取详细房东信息失败 (Homestay ID: ${hostLookupId}):`, hostError); }
        } else { console.warn("无法获取详细房东信息，缺少有效的 homestay ID"); }

        // 5. 初始化地图数据
        await initializeMap();

        // 6. Calculate initial price - No need to call, computed properties handle it
        // calculateTotalPrice();

        // 7. Check favorite status (if implemented)
        // checkFavoriteStatus();

    } catch (error: any) {
        console.error("获取民宿详情过程中发生错误:", error);
        ElMessage.error(error.message || '加载民宿信息时出错，请稍后重试');
        homestay.value = null; hostDetailInfo.value = null; // Clear on error
    } finally {
        loading.value = false;
    }
};

// Placeholder methods
const shareHomestay = () => { ElMessage.info('分享功能待实现'); };
const toggleFavorite = () => { isFavorite.value = !isFavorite.value; ElMessage.info('收藏功能待实现 (状态未持久化)'); };
const contactHost = () => { ElMessage.info('联系房东功能待实现'); };
const showAllPhotos = () => { ElMessage.info('查看全部照片功能待实现'); };

// 地图相关方法
const openMapModal = () => {
    if (mapData.value.hasLocation) {
        showMapModal.value = true;
        ElMessage.info('打开交互式地图功能待实现');
        // 这里可以实现：
        // 1. 打开地图模态框
        // 2. 显示高德地图交互式组件
        // 3. 显示精确位置和周边设施
        // 4. 提供路线规划功能
    } else {
        ElMessage.warning('地图数据加载中，请稍后再试');
    }
};

const onMapImageError = (event: Event) => {
    console.warn('地图图片加载失败，使用fallback图片');
    const img = event.target as HTMLImageElement;
    img.src = 'https://picsum.photos/800/400?random=map-fallback';
};

// Lifecycle hook
onMounted(() => {
    fetchData();
});

const getReviewerAvatar = (review: Review): string => {
    console.log('评价用户头像处理:', review);
    console.log('userAvatar字段:', review.userAvatar);

    if (review.userAvatar) {
        // 如果头像URL是相对路径，需要添加服务器前缀
        if (review.userAvatar.startsWith('/')) {
            const avatarUrl = `http://localhost:8080${review.userAvatar}`;
            console.log('处理后的头像URL:', avatarUrl);
            return avatarUrl;
        }
        // 如果已经是完整URL，直接返回
        console.log('使用原始头像URL:', review.userAvatar);
        return review.userAvatar;
    }

    console.log('使用默认头像');
    return 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png';
};

// 添加计算属性来处理房东信息显示
const hostDisplayName = computed(() => {
    if (!homestay.value) return '暂无房东信息';
    return homestay.value.ownerName || homestay.value.ownerUsername || '暂无房东信息';
});

const hostDisplayAvatar = computed(() => {
    if (!homestay.value) return 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png';
    return homestay.value.ownerAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png';
});

// 地图相关功能
const initializeMap = async () => {
    if (!homestay.value) return;

    console.log('=== 初始化地图 ===');
    mapData.value.isLoading = true;

    try {
        // 构建完整地址用于地理编码
        const fullAddress = `${formattedLocation.value} ${homestay.value.addressDetail || ''}`;
        console.log('地理编码地址:', fullAddress);

        // 使用省市区信息进行地理编码
        let geocodeResult = null;
        if (homestay.value.provinceCode && homestay.value.cityCode) {
            geocodeResult = await geocodeAddress(
                homestay.value.provinceCode,
                homestay.value.cityCode,
                homestay.value.districtCode || '',
                homestay.value.addressDetail
            );
        }

        if (geocodeResult) {
            console.log('地理编码成功:', geocodeResult);

            // 为保护隐私，添加随机偏移
            const offsetLocation = addPrivacyOffset(geocodeResult.lat, geocodeResult.lng);

            mapData.value.lat = offsetLocation.lat;
            mapData.value.lng = offsetLocation.lng;
            mapData.value.hasLocation = true;

            // 生成静态地图URL
            mapData.value.staticMapUrl = generateStaticMapUrl(
                offsetLocation.lat,
                offsetLocation.lng,
                800,
                400,
                15
            );

            console.log('静态地图URL:', mapData.value.staticMapUrl);

            // 搜索周边设施
            await searchNearbyFacilities(offsetLocation.lat, offsetLocation.lng);

        } else {
            console.warn('地理编码失败，使用默认位置');
            // 使用城市中心作为默认位置（这里可以根据城市代码设置默认坐标）
            await setDefaultLocation();
        }
    } catch (error) {
        console.error('初始化地图失败:', error);
        await setDefaultLocation();
    } finally {
        mapData.value.isLoading = false;
    }
};

// 设置默认位置
const setDefaultLocation = async () => {
    // 根据省市区代码设置一些常见城市的默认坐标
    const defaultLocations: Record<string, { lat: number, lng: number, name: string }> = {
        '1101': { lat: 39.9042, lng: 116.4074, name: '北京市' }, // 北京
        '3101': { lat: 31.2304, lng: 121.4737, name: '上海市' }, // 上海
        '4403': { lat: 22.5431, lng: 114.0579, name: '深圳市' }, // 深圳
        '4401': { lat: 23.1291, lng: 113.2644, name: '广州市' }, // 广州
        '4602': { lat: 20.0444, lng: 110.1989, name: '三亚市' }  // 三亚（海南）
    };

    const cityCode = homestay.value?.cityCode;
    const defaultLocation = cityCode ? defaultLocations[cityCode] : null;

    if (defaultLocation) {
        console.log('使用默认城市位置:', defaultLocation.name);
        mapData.value.lat = defaultLocation.lat;
        mapData.value.lng = defaultLocation.lng;
        mapData.value.hasLocation = true;

        // 添加隐私偏移
        const offsetLocation = addPrivacyOffset(defaultLocation.lat, defaultLocation.lng);
        mapData.value.staticMapUrl = generateStaticMapUrl(
            offsetLocation.lat,
            offsetLocation.lng,
            800,
            400,
            12
        );
    } else {
        // 最后的fallback - 使用北京天安门
        console.log('使用北京作为默认位置');
        mapData.value.lat = 39.9042;
        mapData.value.lng = 116.4074;
        mapData.value.staticMapUrl = 'https://picsum.photos/800/400?random=map';
    }
};

// 搜索周边设施
const searchNearbyFacilities = async (lat: number, lng: number) => {
    try {
        console.log('搜索周边设施...');
        const facilities = await searchNearbyPlaces(lat, lng, ['地铁站', '商场', '医院']);
        nearbyPlaces.value = facilities;
        console.log('周边设施:', facilities);
    } catch (error) {
        console.error('搜索周边设施失败:', error);
        // 设置一些模拟数据
        nearbyPlaces.value = [
            { name: '地铁站', type: '交通', distance: 500, address: '步行约5分钟' },
            { name: '购物中心', type: '购物', distance: 800, address: '步行约8分钟' },
            { name: '医院', type: '医疗', distance: 1200, address: '步行约12分钟' }
        ];
    }
};

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

.image-gallery-new {
    position: relative;
    margin-bottom: 32px;
    border-radius: 16px;
    overflow: hidden;
    cursor: pointer;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.gallery-layout-5plus {
    display: grid;
    grid-template-columns: 2fr 1fr;
    grid-template-rows: 500px;
    gap: 4px;
    height: 500px;
}

.main-image-wrapper {
    grid-column: 1 / 2;
    grid-row: 1 / 2;
    position: relative;
    overflow: hidden;
    height: 100%;
}

.main-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
    transition: transform 0.3s ease;
}

.main-image-wrapper:hover .main-img {
    transform: scale(1.03);
}

.grid-image-wrapper {
    grid-column: 2 / 3;
    grid-row: 1 / 2;
    display: grid;
    grid-template-columns: 1fr 1fr;
    grid-template-rows: 1fr 1fr;
    gap: 4px;
    height: 100%;
}

.grid-img-item {
    position: relative;
    overflow: hidden;
    height: 100%;
}

.grid-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
    transition: transform 0.3s ease;
}

.grid-img-item:hover .grid-img {
    transform: scale(1.03);
}

.gallery-layout-fallback .main-image-wrapper {
    height: 500px;
}

.gallery-layout-fallback .main-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.image-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.1);
    opacity: 0;
    transition: opacity 0.3s ease;
    pointer-events: none;
}

.main-image-wrapper:hover .image-overlay,
.grid-img-item:hover .image-overlay {
    opacity: 1;
}

.view-all-photos {
    position: absolute;
    bottom: 16px;
    right: 16px;
    background-color: rgba(255, 255, 255, 0.9);
    border: 1px solid rgba(0, 0, 0, 0.1);
    border-radius: 8px;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    font-weight: 500;
    font-size: 14px;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
    transition: all 0.2s ease;
    z-index: 2;
}

.view-all-photos:hover {
    background-color: white;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.25);
    transform: scale(1.02);
}

@media (max-width: 992px) {
    .gallery-layout-5plus {
        grid-template-columns: 1fr;
        grid-template-rows: auto auto;
        height: auto;
    }

    .main-image-wrapper {
        height: 400px;
    }

    .grid-image-wrapper {
        height: 250px;
        grid-template-columns: repeat(4, 1fr);
        grid-template-rows: 1fr;
    }

    .grid-img-item {
        height: 100%;
    }
}

@media (max-width: 768px) {
    .gallery-layout-5plus {
        height: auto;
        display: flex;
        flex-direction: column;
    }

    .main-image-wrapper {
        height: 300px;
        width: 100%;
    }

    .grid-image-wrapper {
        display: none;
    }

    .view-all-photos {
        bottom: 10px;
        right: 10px;
        padding: 6px 10px;
        font-size: 12px;
    }
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

.host-name {
    font-size: 20px;
    font-weight: 600;
    margin-bottom: 8px;
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
    .gallery-layout-5plus {
        height: auto;
        display: flex;
        flex-direction: column;
    }

    .main-image-wrapper {
        height: 300px;
        width: 100%;
    }

    .grid-image-wrapper {
        display: none;
    }

    .view-all-photos {
        bottom: 10px;
        right: 10px;
        padding: 6px 10px;
        font-size: 12px;
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

/* New Styles for 4-image layout (2x2 grid) */
.gallery-layout-4 {
    display: grid;
    grid-template-columns: 1fr 1fr;
    /* 2 columns */
    grid-template-rows: 250px 250px;
    /* 2 rows, adjust height as needed */
    gap: 4px;
    height: 504px;
    /* 250 + 250 + 4 */
}

.grid-img-item-4 {
    position: relative;
    overflow: hidden;
    height: 100%;
    /* Fill grid cell height */
}

.grid-img-4 {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
    transition: transform 0.3s ease;
}

.grid-img-item-4:hover .grid-img-4 {
    transform: scale(1.03);
}

.no-images-placeholder {
    height: 300px;
    /* Example height */
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #f5f7fa;
    border-radius: 12px;
    margin-bottom: 32px;
}

/* --- End New Layout Styles --- */

.image-overlay {}

/* 新增全宽度部分的样式 */
.full-width-section.location-section {
    margin: 48px 0;
}

.full-width-section.location-section h2 {
    font-size: 22px;
    margin-bottom: 16px;
}

.address-info {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;
}

.primary-location {
    display: flex;
    align-items: center;
    gap: 8px;
}

.location-icon {
    font-size: 24px;
    color: #409EFF;
}

.location-text {
    display: flex;
    flex-direction: column;
}

.main-address {
    font-size: 16px;
    font-weight: 600;
}

.detail-address {
    font-size: 14px;
    color: #717171;
}

.location-features {
    display: flex;
    align-items: center;
    gap: 16px;
}

.distance-info {
    display: flex;
    align-items: center;
    gap: 8px;
}

.map-section {
    display: flex;
    align-items: center;
    gap: 16px;
}

.map-container {
    width: 100%;
    height: 400px;
    border-radius: 12px;
    overflow: hidden;
    position: relative;
}

.map-placeholder {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.map-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    color: white;
    transition: opacity 0.3s ease;
    opacity: 0;
    pointer-events: none;
}

.map-overlay:hover {
    opacity: 1;
}

.map-icon {
    font-size: 24px;
    margin-bottom: 8px;
}

.map-text {
    text-align: center;
}

.map-title {
    font-size: 16px;
    font-weight: 600;
}

.map-subtitle {
    font-size: 14px;
    color: #717171;
}

.map-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.nearby-info {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.nearby-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
}

.nearby-item {
    display: flex;
    align-items: center;
    gap: 8px;
}

.nearby-item .el-icon {
    font-size: 24px;
    color: #409EFF;
}

.nearby-item span {
    font-size: 14px;
    color: #717171;
}

.map-loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 20px;
    background-color: rgba(255, 255, 255, 0.8);
    border-radius: 12px;
    margin-bottom: 20px;
}

.loading-text {
    font-size: 14px;
    color: #717171;
    margin-top: 12px;
}

.facility-icon {
    font-size: 24px;
    color: #409EFF;
    flex-shrink: 0;
}

.facility-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.facility-name {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
}

.facility-distance {
    font-size: 14px;
    color: #717171;
}

/* 地图容器交互效果 */
.map-container {
    cursor: pointer;
    transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.map-container:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.map-container:hover .map-overlay {
    opacity: 1;
}

/* 周边设施网格优化 */
.nearby-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 16px;
    margin-top: 16px;
}

.nearby-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    background-color: #f8f9fa;
    border-radius: 8px;
    transition: background-color 0.3s ease;
}

.nearby-item:hover {
    background-color: #e9ecef;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .map-container {
        height: 300px;
    }

    .nearby-grid {
        grid-template-columns: 1fr;
        gap: 12px;
    }

    .nearby-item {
        padding: 12px;
    }

    .facility-name {
        font-size: 14px;
    }

    .facility-distance {
        font-size: 12px;
    }
}
</style>