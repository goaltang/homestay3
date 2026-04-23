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
            <HomestayCard v-for="homestay in favorites" :key="homestay.id" :homestay="homestay" :homestay-types="[]"
                @card-click="viewHomestayDetails" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getHomestaysByIds } from '@/api/homestay'
import { getUserFavorites } from '@/api/favorites'
import { useFavoritesStore } from '@/stores/favorites'
import HomestayCard from '@/components/homestay/HomestayCard.vue'

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
const favoritesStore = useFavoritesStore()
const loading = ref(false)
const favorites = ref<Homestay[]>([])

// 计算属性，从store获取收藏ID列表
const favoriteIds = computed(() => favoritesStore.favoriteIds)

onMounted(async () => {
    await fetchFavorites()
})

const fetchFavorites = async () => {
    loading.value = true
    try {
        // 优先使用新的收藏API
        const response = await getUserFavorites()
        console.log("获取收藏民宿成功:", response.data)

        if (response.data && response.data.success && Array.isArray(response.data.data)) {
            favorites.value = response.data.data
            // 同步收藏ID到store
            favoritesStore.favoriteIds = response.data.data.map((h: any) => h.id)
            favoritesStore.saveFavorites()
        } else {
            console.warn("从收藏API返回的数据格式不正确，尝试旧方法")
            await fetchFavoritesLegacy()
        }
    } catch (error) {
        console.error('获取收藏民宿失败，尝试旧方法:', error)
        await fetchFavoritesLegacy()
    } finally {
        loading.value = false
    }
}

const fetchFavoritesLegacy = async () => {
    if (favoriteIds.value.length === 0) {
        favorites.value = []
        return
    }

    try {
        // 使用旧方法获取民宿详情
        const response = await getHomestaysByIds(favoriteIds.value)
        console.log("获取收藏民宿成功(旧方法):", response.data)

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

const viewHomestayDetails = (homestay: any) => {
    router.push(`/homestays/${homestay.id}`)
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



@media (max-width: 768px) {
    .homestay-grid {
        grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    }
}
</style>