<template>
    <HomestaySection title="热门民宿" :homestays="homestays" :homestay-types="homestayTypes" :loading="loading"
        empty-text="暂无热门民宿" :max-display="6" view-all-route="/homestays" :view-all-query="{ type: 'popular' }"
        @homestay-click="handleHomestayClick" @view-all="handleViewAll" />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getPopularHomestays } from '../../api/recommendation'
import { getHomestayTypes } from '../../api/homestay'
import HomestaySection from './HomestaySection.vue'

const homestays = ref<any[]>([])
const homestayTypes = ref<any[]>([])
const loading = ref(false)

const emit = defineEmits<{
    homestayClick: [homestay: any]
    viewAll: [route: string, query: Record<string, any>]
}>()

// 加载热门民宿数据
const loadPopularHomestays = async () => {
    loading.value = true
    try {
        const response = await getPopularHomestays()
        homestays.value = response.data || []
        console.log('热门民宿加载完成:', homestays.value.length, '条房源')
        console.log('🔥 热门民宿API返回数据:', JSON.stringify(homestays.value.map((h: any) => ({
            id: h.id,
            title: h.title,
            pricePerNight: h.pricePerNight,
            rating: h.rating
        })), null, 2))

        const typesResponse = await getHomestayTypes()
        homestayTypes.value = typesResponse || []
    } catch (error) {
        console.error('加载热门民宿失败:', error)
        // 提供降级方案：使用模拟数据
        homestays.value = [
            {
                id: 1,
                title: '热门海景公寓',
                pricePerNight: 588,
                maxGuests: 4,
                bedrooms: 2,
                beds: 2,
                bathrooms: 1,
                amenities: ['WiFi', '空调', '海景', '停车位'],
                images: ['https://picsum.photos/800/600?random=101'],
                rating: 4.8,
                reviewCount: 156,
                type: 'ENTIRE',
                status: 'ACTIVE',
                featured: true,
                hostName: '热门房东',
                hostId: 2001
            },
            {
                id: 2,
                title: '精品设计民宿',
                pricePerNight: 688,
                maxGuests: 6,
                bedrooms: 3,
                beds: 3,
                bathrooms: 2,
                amenities: ['WiFi', '空调', '设计师装修', '花园'],
                images: ['https://picsum.photos/800/600?random=102'],
                rating: 4.9,
                reviewCount: 203,
                type: 'ENTIRE',
                status: 'ACTIVE',
                featured: true,
                hostName: '设计房东',
                hostId: 2002
            }
        ]
        homestayTypes.value = []
    } finally {
        loading.value = false
    }
}

// 处理房源点击
const handleHomestayClick = (homestay: any) => {
    emit('homestayClick', homestay)
}

// 处理查看全部
const handleViewAll = (route: string, query: Record<string, any>) => {
    emit('viewAll', route, query)
}

// 组件挂载时加载数据
onMounted(() => {
    loadPopularHomestays()
})

// 暴露刷新方法供父组件调用
defineExpose({
    refresh: loadPopularHomestays
})
</script>