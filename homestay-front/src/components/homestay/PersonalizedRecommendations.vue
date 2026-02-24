<template>
    <HomestaySection title="猜你喜欢" :homestays="homestays" :homestay-types="homestayTypes" :loading="loading"
        :empty-text="emptyText" :max-display="6" view-all-route="/homestays" :view-all-query="{ personalized: 'true' }"
        @homestay-click="handleHomestayClick" @view-all="handleViewAll" />
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { getPersonalizedRecommendations } from '../../api/recommendation'
import { getHomestayTypes, type HomestayType } from '../../api/homestay'
import { useUserStore } from '../../stores/user'
import HomestaySection from './HomestaySection.vue'

const userStore = useUserStore()
const homestays = ref<any[]>([])
const homestayTypes = ref<HomestayType[]>([])
const loading = ref(false)

// 计算空状态提示文本
const emptyText = computed(() => {
    if (!userStore.isAuthenticated) {
        return '登录后为您推荐专属房源'
    } else {
        return '暂无个性化推荐，多浏览和收藏房源来获得更精准的推荐'
    }
})

const emit = defineEmits<{
    homestayClick: [homestay: any]
    viewAll: [route: string, query: Record<string, any>]
}>()

// 加载个性化推荐数据
const loadPersonalizedRecommendations = async () => {
    console.log('🎯 个性化推荐组件调试信息:')
    console.log('- 用户认证状态:', userStore.isAuthenticated)
    console.log('- 用户信息:', userStore.userInfo)
    console.log('- 用户ID:', userStore.userInfo?.id)

    // 只有登录用户才加载个性化推荐
    if (!userStore.isAuthenticated || !userStore.userInfo?.id) {
        console.log('🎯 用户未登录或无用户ID，不加载个性化推荐')
        homestays.value = []
        return
    }

    loading.value = true
    try {
        console.log('🎯 开始加载个性化推荐，用户ID:', userStore.userInfo.id)
        const [homestaysResponse, typesResponse] = await Promise.all([
            getPersonalizedRecommendations(userStore.userInfo.id, 6),
            getHomestayTypes()
        ])

        const homestaysList = homestaysResponse.data?.content || homestaysResponse.data || []
        console.log('🎯 个性化推荐API返回数据:', JSON.stringify(homestaysList.map((h: any) => ({
            id: h.id,
            title: h.title
        })), null, 2))
        homestays.value = homestaysList.slice(0, 12)
        homestayTypes.value = typesResponse || []

        console.log('🎯 个性化推荐加载完成:', homestays.value.length, '条房源')
    } catch (error) {
        console.error('🎯 加载个性化推荐失败:', error)
        homestays.value = []
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

// 监听用户登录状态变化
watch(() => userStore.isAuthenticated, (newValue) => {
    if (newValue) {
        // 用户登录后加载个性化推荐
        loadPersonalizedRecommendations()
    } else {
        // 用户退出登录后清空推荐
        homestays.value = []
    }
})

// 组件挂载时加载数据
onMounted(() => {
    loadPersonalizedRecommendations()
})

// 暴露刷新方法供父组件调用
defineExpose({
    refresh: loadPersonalizedRecommendations
})
</script>

<style scoped>
/* 个性化推荐区域的特殊样式 */
:deep(.homestay-section-title) {
    color: #ff6b6b;
    position: relative;
}

:deep(.homestay-section-title::after) {
    content: '✨';
    margin-left: 8px;
    font-size: 0.9em;
}
</style>