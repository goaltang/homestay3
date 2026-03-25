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

        <!-- 错误状态 -->
        <div v-else-if="hasError" class="error-container">
            <el-result
                icon="error"
                :title="errorTitle"
                :sub-title="errorMessage"
            >
                <template #extra>
                    <el-button type="primary" @click="handleRetry">
                        <el-icon><Refresh /></el-icon>
                        重试
                    </el-button>
                    <el-button @click="handleClearSearch">清除搜索</el-button>
                </template>
            </el-result>
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
                <div class="summary-left">
                    <span class="result-count">找到 {{ total }} 个符合条件的民宿</span>
                    <span v-if="totalPages > 1" class="page-info">第 {{ currentPage }}/{{ totalPages }} 页</span>
                </div>
                <div class="summary-right">
                    <el-select v-model="currentSort" placeholder="排序方式" size="small" @change="handleSortChange">
                        <el-option label="最新发布" value="id,desc" />
                        <el-option label="价格低到高" value="pricePerNight,asc" />
                        <el-option label="价格高到低" value="pricePerNight,desc" />
                        <el-option label="评分高到低" value="rating,desc" />
                    </el-select>
                </div>
            </div>

            <!-- 搜索结果列表 -->
            <div class="homestay-grid" :class="{
                'single-result': results.length === 1,
                'double-result': results.length === 2
            }">
                <HomestayCard v-for="homestay in results" :key="homestay.id" :homestay="homestay"
                    :homestay-types="homestayTypes" @click="handleHomestayClick(homestay)" />
            </div>

            <!-- 分页组件 -->
            <div v-if="totalPages > 1" class="pagination-container">
                <el-pagination
                    :current-page="currentPage"
                    :page-size="pageSize"
                    :total="total"
                    :page-sizes="[12, 24, 36, 48]"
                    layout="total, sizes, prev, pager, next"
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
                />
            </div>
        </template>
    </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Close, Refresh } from '@element-plus/icons-vue'
import HomestayCard from './HomestayCard.vue'

// 错误类型
interface SearchError {
    type: 'network' | 'timeout' | 'server' | 'unknown'
    message: string
    code?: string | number
}

interface Props {
    results: any[]
    homestayTypes: any[]
    loading?: boolean
    total?: number
    currentPage?: number
    pageSize?: number
    sortBy?: string
    sortDirection?: string
    error?: SearchError | null
}

const props = withDefaults(defineProps<Props>(), {
    loading: false,
    total: 0,
    currentPage: 1,
    pageSize: 12,
    sortBy: 'id',
    sortDirection: 'desc',
    error: null
})

const emit = defineEmits<{
    homestayClick: [homestay: any]
    clearSearch: []
    pageChange: [page: number]
    sizeChange: [size: number]
    sortChange: [sortBy: string, sortDirection: string]
    retry: []
}>()

// 计算是否有错误
const hasError = computed(() => props.error !== null)

// 根据错误类型显示不同标题
const errorTitle = computed(() => {
    if (!props.error) return '搜索失败'
    switch (props.error.type) {
        case 'network':
            return '网络连接失败'
        case 'timeout':
            return '请求超时'
        case 'server':
            return '服务器错误'
        default:
            return '搜索失败'
    }
})

// 错误消息
const errorMessage = computed(() => {
    return props.error?.message || '请稍后重试'
})

// 计算总页数
const totalPages = computed(() => {
    return Math.ceil((props.total || 0) / (props.pageSize || 12))
})

// 当前排序方式（用于 v-model）
const currentSort = ref(`${props.sortBy},${props.sortDirection}`)

// 监听 props 变化同步
watch(() => [props.sortBy, props.sortDirection], ([newSortBy, newDir]) => {
    currentSort.value = `${newSortBy},${newDir}`
})

// 处理房源点击
const handleHomestayClick = (homestay: any) => {
    emit('homestayClick', homestay)
}

// 处理清除搜索
const handleClearSearch = () => {
    emit('clearSearch')
}

// 处理重试
const handleRetry = () => {
    emit('retry')
}

// 处理页码变化
const handleCurrentChange = (page: number) => {
    emit('pageChange', page)
}

// 处理每页数量变化
const handleSizeChange = (size: number) => {
    emit('sizeChange', size)
}

// 处理排序变化
const handleSortChange = (value: string) => {
    const [sortBy, sortDirection] = value.split(',')
    emit('sortChange', sortBy, sortDirection)
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

.error-container {
    padding: 40px 20px;
    background: #fff;
    border-radius: 8px;
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
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.search-summary-top {
    margin-bottom: 20px;
    margin-top: 0;
}

.summary-left {
    display: flex;
    align-items: center;
    gap: 12px;
}

.summary-right {
    display: flex;
    align-items: center;
}

.result-count {
    font-size: 14px;
}

.page-info {
    font-size: 14px;
    color: #909399;
    margin-left: 16px;
}

.pagination-container {
    margin-top: 24px;
    display: flex;
    justify-content: center;
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
