<template>
    <el-card class="homestay-card" :body-style="{ padding: '0px' }">
        <div class="homestay-image-container" @click="handleCardClick">
            <el-carousel v-if="images.length > 1" height="220px" indicator-position="none" arrow="hover">
                <el-carousel-item v-for="(image, index) in images" :key="index">
                    <img :src="image" :alt="homestay.title || homestay.name" class="homestay-image" />
                </el-carousel-item>
            </el-carousel>
            <img v-else :src="images[0]" :alt="homestay.title || homestay.name" class="homestay-image" />

            <div v-if="isUserLoggedIn" class="favorite-button" @click.stop="toggleFavorite">
                <el-icon :size="iconSize" :color="isFavorite ? '#ff385c' : '#fff'">
                    <component :is="isFavorite ? 'StarFilled' : 'Star'" />
                </el-icon>
            </div>
        </div>

        <div class="homestay-info" @click="handleCardClick">
            <div class="homestay-header">
                <h3 class="homestay-title">{{ homestay.title || homestay.name }}</h3>
                <div class="homestay-rating" v-if="rating">
                    <el-icon>
                        <Star />
                    </el-icon>
                    <span>{{ rating }}</span>
                </div>
            </div>

            <div class="homestay-location">{{ location }}</div>
            <div class="homestay-distance" v-if="homestay.distanceFromCenter">
                距离市中心 {{ homestay.distanceFromCenter }} 公里
            </div>
            <div class="group-tag" v-if="homestay.groupName">
                <span class="group-badge" :style="{ backgroundColor: homestay.groupColor || '#409eff' }">
                    {{ homestay.groupName }}
                </span>
            </div>
            <div class="homestay-features" v-if="features.length > 0">
                <span v-for="feature in features" :key="feature" class="feature-tag">{{ feature }}</span>
            </div>
            <div class="booking-mode-tag" v-if="homestay.autoConfirm">
                <el-icon class="auto-confirm-icon" :size="14">
                    <Lightning />
                </el-icon>
                <span>即时预订</span>
            </div>
            <div class="homestay-price">
                <span class="price">¥{{ price }}</span>
                <span class="night">/晚</span>
            </div>
        </div>
    </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Star, StarFilled, Lightning } from '@element-plus/icons-vue'
import { useFavoritesStore } from '@/stores/favorites'
import { useUserStore } from '@/stores/user'
import { codeToText } from 'element-china-area-data'
import { formatPropertyType } from '@/utils/homestayUtils'

interface Props {
    homestay: any
    iconSize?: number
    showCarousel?: boolean
    homestayTypes?: any[]  // 添加房源类型数据
}

const props = withDefaults(defineProps<Props>(), {
    iconSize: 24,
    showCarousel: true,
    homestayTypes: () => []
})

const emit = defineEmits<{
    cardClick: [homestay: any]
}>()

const router = useRouter()
const favoritesStore = useFavoritesStore()
const userStore = useUserStore()

// 计算属性
const isFavorite = computed(() => favoritesStore.isFavorite(props.homestay.id))
const isUserLoggedIn = computed(() => userStore.isAuthenticated)

const images = computed(() => {
    const defaultImage = 'https://picsum.photos/300/200'

    // 图片URL处理函数
    const processImageUrl = (imageUrl: string): string => {
        if (!imageUrl) return defaultImage

        // 如果是完整的HTTP/HTTPS URL，直接返回
        if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) {
            return imageUrl
        }

        // 如果是相对路径，构建完整的后端API URL
        // 确保URL格式正确
        const cleanUrl = imageUrl.startsWith('/') ? imageUrl : `/${imageUrl}`
        return `http://localhost:8080${cleanUrl}`
    }

    // 优先使用封面图片（这是房源卡片应该显示的主要图片）
    if (props.homestay.coverImage) {
        return [processImageUrl(props.homestay.coverImage)]
    }

    // 如果没有封面图片，才使用图片集的第一张图片
    if (props.homestay.images && Array.isArray(props.homestay.images) && props.homestay.images.length > 0) {
        const firstImage = props.homestay.images[0]
        if (typeof firstImage === 'string') {
            return [processImageUrl(firstImage)]
        }
        return [processImageUrl(firstImage?.url || firstImage?.path) || defaultImage]
    }

    return [defaultImage]
})

const rating = computed(() => {
    if (props.homestay.rating) {
        return typeof props.homestay.rating === 'number'
            ? props.homestay.rating.toFixed(1)
            : props.homestay.rating
    }
    return null
})

const location = computed(() => {
    if (props.homestay.location) return props.homestay.location

    const parts = []
    if (props.homestay.provinceCode && codeToText[props.homestay.provinceCode]) {
        parts.push(codeToText[props.homestay.provinceCode])
    }
    if (props.homestay.cityCode && codeToText[props.homestay.cityCode]) {
        if (!parts.includes(codeToText[props.homestay.cityCode])) {
            parts.push(codeToText[props.homestay.cityCode])
        }
    }
    return parts.length > 0 ? parts.join(' · ') : '位置待更新'
})

// 类型转码功能
const getHomestayTypeText = (typeCode: string | undefined): string => {
    if (!typeCode) return '未知类型'

    // 从传入的房源类型列表中查找
    const foundType = props.homestayTypes.find(t => t.code === typeCode)

    if (foundType) {
        return foundType.name || typeCode
    }

    // 如果找不到匹配项，返回原始代码
    return typeCode
}

const features = computed(() => {
    const features = []

    if (props.homestay.maxGuests) {
        features.push(`${props.homestay.maxGuests}人`)
    }

    // 使用类型转码功能
    if (props.homestay.propertyType || props.homestay.type) {
        const typeCode = props.homestay.propertyType || props.homestay.type
        features.push(formatPropertyType(typeCode))
    }

    if (props.homestay.amenities && Array.isArray(props.homestay.amenities)) {
        const amenityNames = props.homestay.amenities.slice(0, 2).map((amenity: any) =>
            typeof amenity === 'string' ? amenity : amenity.label || amenity.name
        )
        features.push(...amenityNames)
    }

    return features.slice(0, 4)
})

const price = computed(() => {
    return props.homestay.price || props.homestay.pricePerNight || 0
})

// 方法
const toggleFavorite = async () => {
    try {
        await favoritesStore.toggleFavorite(props.homestay.id)
    } catch (error) {
        console.error('切换收藏状态失败:', error)
    }
}

const handleCardClick = () => {
    emit('cardClick', props.homestay)
    router.push(`/homestays/${props.homestay.id}`)
}
</script>

<style scoped>
.homestay-card {
    margin-bottom: 20px;
    cursor: pointer;
    transition: transform 0.2s, box-shadow 0.2s;
    overflow: hidden;
    border-radius: 12px;
}

.homestay-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.homestay-image-container {
    position: relative;
    height: 220px;
    cursor: pointer;
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
    transition: background-color 0.2s;
}

.favorite-button:hover {
    background-color: rgba(0, 0, 0, 0.5);
}

.homestay-info {
    padding: 16px;
    cursor: pointer;
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
    margin-left: 8px;
}

.homestay-location,
.homestay-distance {
    color: #717171;
    font-size: 14px;
    margin-bottom: 4px;
}

.homestay-features {
    display: flex;
    gap: 8px;
    margin-bottom: 8px;
    flex-wrap: wrap;
}

.feature-tag {
    color: #717171;
    font-size: 14px;
    margin-bottom: 4px;
}

.group-tag {
    margin-bottom: 6px;
}

.group-badge {
    display: inline-block;
    color: #fff;
    font-size: 12px;
    padding: 2px 10px;
    border-radius: 10px;
    font-weight: 500;
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

/* 自动确认标签样式 */
.booking-mode-tag {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    background: linear-gradient(135deg, #67c23a 0%, #5daf34 100%);
    color: white;
    padding: 4px 8px;
    border-radius: 12px;
    font-size: 12px;
    font-weight: 500;
    margin-top: 8px;
    box-shadow: 0 2px 4px rgba(103, 194, 58, 0.3);
}

.auto-confirm-icon {
    color: white;
}

@media (max-width: 768px) {
    .homestay-image-container {
        height: 180px;
    }

    .homestay-info {
        padding: 12px;
    }

    .homestay-title {
        font-size: 14px;
    }
}
</style>