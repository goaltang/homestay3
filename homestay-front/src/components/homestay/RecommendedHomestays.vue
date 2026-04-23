<template>
    <HomestaySection title="推荐民宿" :homestays="homestays" :loading="loading" empty-text="暂无推荐民宿"
        :max-display="6" view-all-route="/homestays" :view-all-query="{ featured: 'true' }"
        @homestay-click="handleHomestayClick" @view-all="handleViewAll" />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getRecommendedHomestays } from '../../api/recommendation'
import HomestaySection from './HomestaySection.vue'

const homestays = ref<any[]>([])
const loading = ref(false)

const emit = defineEmits<{
    homestayClick: [homestay: any]
    viewAll: [route: string, query: Record<string, any>]
}>()

// 加载推荐民宿数据（使用通用推荐算法）
const loadRecommendedHomestays = async () => {
    loading.value = true
    try {
        const homestaysResponse = await getRecommendedHomestays(6)
        homestays.value = homestaysResponse.data || []
    } catch (error) {
        console.error('加载推荐民宿失败:', error)
        homestays.value = []
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
    loadRecommendedHomestays()
})

// 暴露刷新方法供父组件调用
defineExpose({
    refresh: loadRecommendedHomestays
})
</script>
