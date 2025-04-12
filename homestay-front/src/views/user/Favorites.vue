<template>
    <div class="favorites-container">
        <div class="favorites-header">
            <h1>我的收藏</h1>
        </div>

        <div v-if="loading" class="loading-container">
            <el-skeleton :rows="3" animated />
            <el-skeleton :rows="3" animated />
            <el-skeleton :rows="3" animated />
        </div>

        <div v-else-if="favorites.length === 0" class="empty-result">
            <el-empty description="您还没有收藏任何民宿">
                <el-button type="primary" @click="$router.push('/')">去浏览民宿</el-button>
            </el-empty>
        </div>

        <div v-else class="homestay-grid">
            <div v-for="homestay in favorites" :key="homestay.id" class="homestay-card"
                @click="viewHomestayDetails(homestay.id)">
                <div class="homestay-image-container" @click.stop="() => { }">
                    <el-carousel height="220px" indicator-position="none" arrow="hover">
                        <el-carousel-item v-for="(image, index) in getHomestayImages(homestay)" :key="index">
                            <img :src="image" :alt="homestay.title" class="homestay-image" />
                        </el-carousel-item>
                    </el-carousel>
                    <div class="favorite-button" @click.stop="toggleFavorite(homestay.id)">
                        <el-icon :size="24" color="#ff385c">
                            <StarFilled />
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
                    <div class="homestay-distance">距离市中心{{ homestay.distanceFromCenter }}公里</div>
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getHomestaysByIds } from '@/api/homestay'

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
    price?: number;
    province?: string;
    district?: string;
}

const router = useRouter()
const loading = ref(false)
const favorites = ref<Homestay[]>([])
const favoriteIds = ref<number[]>([])

onMounted(async () => {
    loadFavoriteIds()
    await fetchFavorites()
})

const loadFavoriteIds = () => {
    const saved = localStorage.getItem('favorites')
    if (saved) {
        favoriteIds.value = JSON.parse(saved)
    }
}

const fetchFavorites = async () => {
    if (favoriteIds.value.length === 0) {
        favorites.value = []
        return
    }

    loading.value = true
    try {
        // 尝试从后端获取收藏的民宿
        const response = await getHomestaysByIds(favoriteIds.value)
        console.log("获取收藏民宿成功:", response.data)

        if (Array.isArray(response.data)) {
            favorites.value = response.data
        } else if (response.data && response.data.length > 0) {
            favorites.value = response.data
        } else {
            console.warn("从API返回的数据格式不正确，使用模拟数据")
            useMockData()
        }
    } catch (error) {
        console.error('获取收藏民宿失败，使用模拟数据:', error)
        useMockData()
    } finally {
        loading.value = false
    }
}

// 使用模拟数据
const useMockData = () => {
    // 模拟数据
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
        }
    ]

    // 只保留收藏的民宿
    favorites.value = mockData.filter(homestay => favoriteIds.value.includes(homestay.id))
}

const getHomestayImages = (homestay: Homestay) => {
    const defaultImage = 'https://picsum.photos/300x200';

    // 如果没有图片数组或者图片数组为空
    if (!homestay.images || homestay.images.length === 0) {
        console.log(`房源ID ${homestay.id} 没有图片，使用默认图片`);
        return [defaultImage];
    }

    console.log(`房源ID ${homestay.id} 的图片:`, homestay.images);

    return homestay.images.map((image: string) => {
        if (!image) {
            console.warn(`房源ID ${homestay.id} 的图片路径为空，使用默认图片`);
            return defaultImage;
        }

        // 处理不同类型的图片路径
        if (typeof image === 'string') {
            // 如果已经是完整URL则直接返回
            if (image.startsWith('http')) {
                return image;
            }
            // 处理相对于后端的路径
            else if (image.startsWith('/uploads/')) {
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
                if (image.includes('avatars')) {
                    return `/api/uploads/avatars/${image.split('/').pop()}`;
                } else {
                    return `/api/uploads/homestays/${image}`;
                }
            }
            // 其他以/开头的路径
            else {
                return `/api${image}`;
            }
        }

        // 最后的默认情况
        return defaultImage;
    });
}

const viewHomestayDetails = (id: number) => {
    router.push(`/homestays/${id}`)
}

const toggleFavorite = (id: number) => {
    // 从收藏中移除
    favoriteIds.value = favoriteIds.value.filter(fav => fav !== id)
    favorites.value = favorites.value.filter(homestay => homestay.id !== id)

    // 保存到本地存储
    localStorage.setItem('favorites', JSON.stringify(favoriteIds.value))

    ElMessage.success('已从收藏中移除')
}

const getHomestayLocation = (homestay: Homestay) => {
    const parts = []

    if (homestay.province) parts.push(homestay.province)
    if (homestay.city) parts.push(homestay.city)
    if (homestay.district) parts.push(homestay.district)

    if (parts.length > 0) return parts.join(' · ')

    // 兼容旧数据结构
    if (homestay.location) return homestay.location
    if (homestay.city && homestay.country) return `${homestay.city}, ${homestay.country}`

    return '未知位置'
}

const calculatePrice = (homestay: Homestay) => {
    // 适配不同价格字段
    return homestay.price || homestay.pricePerNight || 0;
}
</script>

<style scoped>
.favorites-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px;
}

.favorites-header {
    margin-bottom: 24px;
}

.loading-container {
    padding: 24px;
}

.empty-result {
    padding: 48px 0;
    text-align: center;
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
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
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
.homestay-distance {
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

@media (max-width: 768px) {
    .homestay-grid {
        grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    }
}
</style>