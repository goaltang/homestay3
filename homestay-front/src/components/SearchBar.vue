<template>
    <div class="search-container">
        <div class="search-wrapper">
            <div class="search-bar">
                <!-- 目的地搜索 -->
                <div class="search-item destination-item" @click="focusInput('location')">
                    <div class="label">目的地</div>
                    <div class="content">
                        <el-cascader ref="locationCascader" v-model="searchParams.selectedRegion"
                            :options="regionOptions" placeholder="你要去哪里？" :props="cascaderProps" clearable filterable
                            class="location-cascader" :show-all-levels="false" @change="handleLocationChange" />
                    </div>
                </div>

                <div class="search-divider"></div>

                <!-- 日期选择 -->
                <div class="search-item dates" @click="focusInput('dates')">
                    <div class="date-item check-in">
                        <div class="label">入住</div>
                        <div class="content">
                            <el-date-picker ref="checkInPicker" v-model="searchParams.checkIn" type="date"
                                placeholder="添加日期" format="M月D日" value-format="YYYY-MM-DD" class="date-picker"
                                :disabled-date="disabledCheckInDate" @change="handleCheckInChange" />
                        </div>
                    </div>
                    <div class="date-item check-out">
                        <div class="label">退房</div>
                        <div class="content">
                            <el-date-picker ref="checkOutPicker" v-model="searchParams.checkOut" type="date"
                                placeholder="添加日期" format="M月D日" value-format="YYYY-MM-DD" class="date-picker"
                                :disabled-date="disabledCheckOutDate" @change="handleCheckOutChange" />
                        </div>
                    </div>
                </div>

                <div class="search-divider"></div>

                <!-- 房客人数 -->
                <div class="search-item guests" @click="showGuestSelector = !showGuestSelector">
                    <div class="label">房客</div>
                    <div class="content">
                        <div class="guest-display">{{ guestDisplayText }}</div>
                        <el-popover v-model:visible="showGuestSelector" placement="bottom" :width="280" trigger="click">
                            <template #reference>
                                <div class="guest-selector-trigger"></div>
                            </template>
                            <div class="guest-selector">
                                <div class="guest-row">
                                    <div class="guest-info">
                                        <div class="guest-label">房客人数</div>
                                        <div class="guest-desc">选择入住人数</div>
                                    </div>
                                    <div class="guest-controls">
                                        <el-button circle size="small" :disabled="guestCounts.adults <= 1"
                                            @click="adjustGuestCount('adults', -1)">-</el-button>
                                        <span class="guest-count">{{ guestCounts.adults }}</span>
                                        <el-button circle size="small" :disabled="guestCounts.adults >= 20"
                                            @click="adjustGuestCount('adults', 1)">+</el-button>
                                    </div>
                                </div>
                            </div>
                        </el-popover>
                    </div>
                </div>

                <div class="search-divider"></div>

                <!-- 关键词搜索 -->
                <div class="search-item keyword-item" @click="focusKeyword">
                    <div class="label">关键词</div>
                    <div class="content">
                        <input ref="keywordInput" v-model="searchParams.keyword" placeholder="民宿名称、特色..."
                            class="keyword-input-native" @click.stop />
                    </div>
                </div>

                <!-- 操作按钮区域 -->
                <div class="search-actions-container">
                    <!-- 重置按钮 -->
                    <el-button class="reset-button" @click="handleReset" :loading="loading">
                        <el-icon>
                            <Refresh />
                        </el-icon>
                    </el-button>

                    <!-- 搜索按钮 -->
                    <div class="search-button-container">
                        <el-tooltip content="请先选择目的地" placement="top" :disabled="canSearch">
                            <el-button type="primary" class="search-button" @click="handleSearch" :loading="loading"
                                :disabled="!canSearch">
                                <el-icon>
                                    <Search />
                                </el-icon>
                                <span class="search-text">搜索</span>
                            </el-button>
                        </el-tooltip>
                    </div>
                </div>
            </div>

            <!-- 快捷搜索建议 -->
            <SearchSuggestions v-if="showSuggestions" @select="selectSuggestion" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { regionData } from 'element-china-area-data'
import SearchSuggestions from './SearchSuggestions.vue'

// 定义 props
interface Props {
    loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
    loading: false
})

// 定义 emits
const emit = defineEmits<{
    search: [params: any]
    reset: []
}>()

// 响应式数据
const searchParams = reactive({
    selectedRegion: [] as string[],
    keyword: '',
    checkIn: null as string | null,
    checkOut: null as string | null,
    guestCount: 1
})

const showGuestSelector = ref(false)
const showSuggestions = ref(false)
const keywordInput = ref()

// 客人数量管理
const guestCounts = reactive({
    adults: 1,
    children: 0,
    infants: 0
})

// 地区选择器选项和配置
const regionOptions = regionData
const cascaderProps = {
    value: 'value',
    label: 'label',
    children: 'children',
    checkStrictly: true
}

// 计算属性
const guestDisplayText = computed(() => {
    return `${guestCounts.adults}位房客`
})

const canSearch = computed(() => {
    // 主流平台策略：必须有目的地才能搜索
    // 地区是必填项，日期和关键词是可选的
    return searchParams.selectedRegion.length > 0
})

// 搜索提示文本
const searchTooltipText = computed(() => {
    if (!searchParams.selectedRegion.length) {
        return '请先选择目的地'
    }
    return ''
})

// 日期验证
const disabledCheckInDate = (time: Date) => {
    return time.getTime() < Date.now() - 8.64e7 // 不能选择昨天之前的日期
}

const disabledCheckOutDate = (time: Date) => {
    if (!searchParams.checkIn) return time.getTime() < Date.now()
    return time.getTime() <= new Date(searchParams.checkIn).getTime()
}

// 方法
const focusInput = (type: string) => {
    showSuggestions.value = type === 'location'
}

const focusKeyword = () => {
    nextTick(() => {
        if (keywordInput.value) {
            keywordInput.value.focus()
        }
    })
}

const handleLocationChange = () => {
    showSuggestions.value = false
}

const handleCheckInChange = (value: string) => {
    if (value && searchParams.checkOut) {
        const checkIn = new Date(value)
        const checkOut = new Date(searchParams.checkOut)
        if (checkOut <= checkIn) {
            const nextDay = new Date(checkIn)
            nextDay.setDate(nextDay.getDate() + 1)
            searchParams.checkOut = nextDay.toISOString().split('T')[0]
        }
    }
}

const handleCheckOutChange = (value: string | null) => {
    // 退房日期变更逻辑
}

const adjustGuestCount = (type: 'adults' | 'children' | 'infants', delta: number) => {
    guestCounts[type] = Math.max(0, guestCounts[type] + delta)
    if (type === 'adults' && guestCounts.adults < 1) {
        guestCounts.adults = 1
    }
    searchParams.guestCount = guestCounts.adults + guestCounts.children
}

const selectSuggestion = (suggestion: { label: string; value: string[] }) => {
    searchParams.selectedRegion = suggestion.value
    showSuggestions.value = false
}

const handleSearch = () => {
    emit('search', { ...searchParams })
}

const handleReset = () => {
    // 重置搜索条件
    searchParams.selectedRegion = []
    searchParams.keyword = ''
    searchParams.checkIn = null
    searchParams.checkOut = null
    searchParams.guestCount = 1

    // 重置客人数量
    guestCounts.adults = 1
    guestCounts.children = 0
    guestCounts.infants = 0

    // 关闭相关弹窗
    showGuestSelector.value = false
    showSuggestions.value = false

    ElMessage.success('搜索条件已重置')
    emit('reset')
}

// 暴露给父组件的方法
defineExpose({
    searchParams,
    resetParams: handleReset
})
</script>

<style scoped>
.search-container {
    margin-bottom: 24px;
    border-radius: 40px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    background-color: white;
    position: sticky;
    top: 0;
    z-index: 100;
    padding: 10px 0;
}

.search-wrapper {
    background: white;
    border-radius: 32px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08), 0 4px 12px rgba(0, 0, 0, 0.05);
    padding: 8px;
    max-width: 900px;
    margin: 0 auto;
}

.search-bar {
    display: flex;
    align-items: center;
    background: white;
    border-radius: 32px;
    min-height: 66px;
    transition: box-shadow 0.2s ease;
}

.search-bar:hover {
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.18);
}

.search-item {
    flex: 1;
    padding: 14px 24px;
    border-radius: 32px;
    cursor: pointer;
    transition: all 0.2s ease;
    position: relative;
    display: flex;
    flex-direction: column;
    justify-content: center;
    min-height: 66px;
}

.search-item:hover {
    background-color: #f7f7f7;
    transform: translateY(-1px);
}

.search-item:focus-within {
    background-color: #f7f7f7;
    box-shadow: 0 0 0 2px rgba(255, 56, 92, 0.2);
}

.search-item.dates {
    display: flex;
    flex: 2.2;
    flex-direction: row;
    align-items: center;
    padding: 14px 12px;
}

/* 搜索栏项目宽度优化 */
.search-item.destination-item {
    flex: 1.6;
}

.search-item.keyword-item {
    flex: 1.4;
}

.search-item.dates {
    flex: 2.2;
}

.search-item.guests {
    flex: 0.8;
}

/* 确保占位符文本样式统一 */
:deep(.el-input__inner::placeholder),
:deep(.el-cascader .el-input__inner::placeholder) {
    color: #c0c4cc !important;
    font-weight: normal !important;
}

/* Element Plus 组件样式重置 */
:deep(.location-cascader) {
    width: 100%;
    border: none !important;
    box-shadow: none !important;
    background: transparent !important;
}

:deep(.location-cascader .el-input__wrapper) {
    border: none !important;
    box-shadow: none !important;
    background: transparent !important;
    padding: 0 !important;
}

:deep(.location-cascader .el-input__inner) {
    color: #717171 !important;
    font-size: 14px !important;
    font-weight: normal !important;
    height: auto !important;
    line-height: 1.4 !important;
}

:deep(.date-picker) {
    width: 100%;
    border: none !important;
    box-shadow: none !important;
    background: transparent !important;
}

:deep(.date-picker .el-input__wrapper) {
    border: none !important;
    box-shadow: none !important;
    background: transparent !important;
    padding: 0 !important;
    min-width: 85px !important;
    /* 确保有足够空间显示"12月31日" */
    width: 100% !important;
}

:deep(.date-picker .el-input__inner) {
    color: #717171 !important;
    font-size: 14px !important;
    font-weight: normal !important;
    height: auto !important;
    line-height: 1.4 !important;
    text-align: left !important;
    min-width: 85px !important;
    /* 确保有足够空间显示完整日期 */
    overflow: visible !important;
    text-overflow: clip !important;
}

/* 关键词输入框样式优化 */
.search-item.keyword-item .content {
    display: flex;
    align-items: center;
}

.keyword-input-native {
    width: 100%;
    border: none;
    outline: none;
    background: transparent;
    color: #717171;
    font-size: 14px;
    padding: 0;
    font-family: inherit;
    line-height: 1.4;
}

.keyword-input-native::placeholder {
    color: #c0c4cc;
}

.date-item {
    flex: 1;
    padding: 0 12px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    min-height: 38px;
    min-width: 0;
    /* 允许弹性收缩 */
    position: relative;
}

.date-item .content {
    flex: 1;
    min-width: 85px;
    /* 确保日期输入框有足够空间 */
}

.search-item .label {
    font-weight: 600;
    font-size: 12px;
    color: #222;
    margin-bottom: 4px;
    line-height: 1;
}

.search-item .content {
    color: #717171;
    font-size: 14px;
    display: flex;
    align-items: center;
    min-height: 20px;
}

.search-divider {
    width: 1px;
    height: 32px;
    background-color: #dddddd;
    margin: 0;
}

.guest-display {
    color: #717171;
    font-size: 14px;
    cursor: pointer;
}

.guest-selector-trigger {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    cursor: pointer;
}

.guest-selector {
    padding: 16px 0;
}

.guest-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
}

.guest-info {
    flex: 1;
}

.guest-label {
    font-weight: 600;
    font-size: 16px;
    color: #222;
}

.guest-desc {
    font-size: 14px;
    color: #717171;
}

.guest-controls {
    display: flex;
    align-items: center;
    gap: 16px;
}

.guest-count {
    font-size: 16px;
    font-weight: 600;
    min-width: 24px;
    text-align: center;
}

/* 搜索操作按钮区域样式 */
.search-actions-container {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px;
}

.reset-button {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f7f7f7;
    border: 1px solid #e5e5e5;
    color: #666;
    transition: all 0.2s ease;
}

.reset-button:hover {
    background: #ff385c;
    color: white;
    border-color: #ff385c;
    transform: scale(1.04);
}

.search-button-container {
    padding: 0;
}

.search-button {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #ff385c 0%, #e61e4d 100%);
    border: none;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08), 0 4px 12px rgba(0, 0, 0, 0.05);
    transition: all 0.2s ease;
}

.search-button:hover {
    transform: scale(1.04);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.18);
}

.search-text {
    display: none;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .search-wrapper {
        margin: 0 16px;
        padding: 4px;
    }

    .search-bar {
        flex-direction: column;
        min-height: auto;
        gap: 1px;
    }

    .search-item {
        padding: 16px 20px;
        border-radius: 0;
        background: white;
    }

    .search-item:first-child {
        border-radius: 12px 12px 0 0;
    }

    .search-item:last-of-type {
        border-radius: 0 0 12px 12px;
    }

    .search-actions-container {
        justify-content: center;
        gap: 12px;
        padding: 16px;
    }

    .reset-button {
        width: 40px;
        height: 40px;
    }

    .search-button {
        width: 56px;
        height: 56px;
    }

    .search-divider {
        width: 100%;
        height: 1px;
    }

    .search-item.dates {
        flex-direction: column;
        gap: 16px;
    }

    .date-item {
        padding: 0;
    }

    .search-button-container {
        padding: 16px 20px;
        background: white;
        border-radius: 0 0 12px 12px;
    }

    .search-button {
        width: 100%;
        height: 48px;
        border-radius: 12px;
    }

    .search-text {
        display: inline;
        margin-left: 8px;
    }
}
</style>