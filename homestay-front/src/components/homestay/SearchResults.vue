<template>
    <div class="search-results">
        <!-- 搜索状态头部 -->
        <div class="section-header">
            <h2>搜索结果</h2>
            <div class="section-actions">
                <el-tag type="info" size="small">搜索模式</el-tag>
                <el-button link @click="handleClearSearch" size="small">
                    <el-icon>
                        <Close />
                    </el-icon>
                    清除所有
                </el-button>
            </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
            <el-skeleton :rows="3" animated />
            <el-skeleton :rows="3" animated />
        </div>

        <!-- 空状态 -->
        <div v-else-if="results.length === 0" class="empty-result">
            <el-empty description="没有找到符合条件的民宿">
                <template #extra>
                    <el-button @click="handleClearSearch">重新搜索</el-button>
                </template>
            </el-empty>
        </div>

        <!-- 有结果时显示统计和列表 -->
        <template v-else>
            <!-- 结果统计 -->
            <div class="search-summary search-summary-top">
                <span class="result-count">找到 {{ results.length }} 个符合条件的民宿</span>
            </div>

            <!-- 搜索结果列表 -->
            <div class="homestay-grid" :class="{
                'single-result': results.length === 1,
                'double-result': results.length === 2
            }">
                <HomestayCard v-for="homestay in results" :key="homestay.id" :homestay="homestay"
                    :homestay-types="homestayTypes" @click="handleHomestayClick(homestay)" />
            </div>
        </template>
    </div>
</template>

<script setup lang="ts">
import { Close } from '@element-plus/icons-vue'
import HomestayCard from './HomestayCard.vue'

interface Props {
    results: any[]
    homestayTypes: any[]
    loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
    loading: false
})

const emit = defineEmits<{
    homestayClick: [homestay: any]
    clearSearch: []
}>()

// 处理房源点击
const handleHomestayClick = (homestay: any) => {
    emit('homestayClick', homestay)
}

// 处理清除搜索
const handleClearSearch = () => {
    emit('clearSearch')
}
</script>

<style scoped>
.search-results {
    margin-bottom: 40px;
}

.section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin: 40px 0 24px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;
}

.section-header h2 {
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
    margin: 20px 0;
}

.empty-result {
    text-align: center;
    padding: 40px 20px;
}

.homestay-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 20px;
    margin: 20px 0;
    justify-items: stretch;
}

/* 当只有1-2个结果时，限制最大宽度避免过度拉伸 */
.homestay-grid.single-result {
    grid-template-columns: minmax(300px, 400px);
    justify-content: start;
}

.homestay-grid.double-result {
    grid-template-columns: repeat(2, minmax(300px, 400px));
    justify-content: start;
}

.search-summary {
    margin-bottom: 16px;
    padding: 12px 16px;
    background-color: #f8f9fa;
    border-radius: 8px;
    border-left: 4px solid #409eff;
}

.search-summary-top {
    margin-bottom: 20px;
    margin-top: 0;
}

.result-count {
    font-size: 14px;
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
}
</style>