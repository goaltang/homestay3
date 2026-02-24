<template>
    <div class="homestay-section">
        <!-- 区域标题和操作 -->
        <div class="section-header">
            <h2 class="section-title">{{ title }}</h2>
            <div class="section-actions">
                <el-button v-if="showViewAll && homestays.length > 0" link @click="handleViewAll">
                    查看全部
                </el-button>
            </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
            <el-skeleton :rows="3" animated />
            <el-skeleton :rows="3" animated />
        </div>

        <!-- 空状态 -->
        <div v-else-if="homestays.length === 0" class="empty-result">
            <el-empty :description="emptyText" />
        </div>

        <!-- 房源列表 -->
        <div v-else class="homestay-grid">
            <HomestayCard v-for="homestay in displayHomestays" :key="homestay.id" :homestay="homestay"
                :homestay-types="homestayTypes" @card-click="handleHomestayClick" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import HomestayCard from './HomestayCard.vue'

interface Props {
    title: string                    // 区域标题
    homestays: any[]                // 房源数据
    homestayTypes?: any[]           // 房源类型数据  
    loading?: boolean               // 加载状态
    emptyText?: string              // 空状态文本
    showViewAll?: boolean           // 是否显示查看全部按钮
    maxDisplay?: number             // 最大显示数量
    viewAllRoute?: string           // 查看全部的路由
    viewAllQuery?: Record<string, any> // 查看全部的查询参数
}

const props = withDefaults(defineProps<Props>(), {
    homestayTypes: () => [],
    loading: false,
    emptyText: '暂无房源数据',
    showViewAll: true,
    maxDisplay: 6,
    viewAllRoute: '/homestays',
    viewAllQuery: () => ({})
})

const emit = defineEmits<{
    homestayClick: [homestay: any]
    viewAll: [route: string, query: Record<string, any>]
}>()

const router = useRouter()

// 显示的房源列表（限制数量）
const displayHomestays = computed(() => {
    if (!Array.isArray(props.homestays)) {
        console.warn('homestays is not an array:', props.homestays)
        return []
    }
    return props.homestays.slice(0, props.maxDisplay)
})

// 处理房源点击
const handleHomestayClick = (homestay: any) => {
    emit('homestayClick', homestay)
    router.push(`/homestays/${homestay.id}`)
}

// 处理查看全部
const handleViewAll = () => {
    emit('viewAll', props.viewAllRoute, props.viewAllQuery)
    router.push({
        path: props.viewAllRoute,
        query: props.viewAllQuery
    })
}
</script>

<style scoped>
.homestay-section {
    margin-bottom: 40px;
}

.section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;
}

.section-title {
    margin: 0;
    font-size: 24px;
    font-weight: 600;
    color: #222;
}

.section-actions {
    display: flex;
    align-items: center;
    gap: 12px;
}

.loading-container {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 20px;
}

.empty-result {
    text-align: center;
    padding: 40px 20px;
}

.homestay-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .section-header {
        flex-direction: column;
        align-items: flex-start;
        gap: 12px;
    }

    .homestay-grid {
        grid-template-columns: 1fr;
        gap: 16px;
    }

    .section-title {
        font-size: 20px;
    }
}
</style>