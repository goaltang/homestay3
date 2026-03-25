<template>
    <div class="search-suggestions">
        <!-- 最近搜索 -->
        <div v-if="showRecentSearches" class="suggestion-section">
            <div class="suggestion-header">
                <span class="suggestion-title">最近搜索</span>
                <el-button link size="small" @click="handleClearRecent">清除</el-button>
            </div>
            <div class="suggestion-items">
                <div v-for="item in recentSearches" :key="item.label + '-' + item.value.join(',')"
                    class="suggestion-item recent-item" @click="$emit('select', item)">
                    <el-icon class="recent-icon"><Clock /></el-icon>
                    <span class="item-label">{{ item.label }}</span>
                    <el-icon class="close-icon" @click.stop="handleRemoveRecent(item)"><Close /></el-icon>
                </div>
            </div>
        </div>

        <!-- 热门目的地 -->
        <div v-if="hasPopularDestinations" class="suggestion-section">
            <div class="suggestion-title">{{ showRecentSearches ? '热门目的地' : '热门目的地' }}</div>
            <div class="suggestion-items">
                <div v-for="suggestion in popularDestinations" :key="suggestion.label + '-' + suggestion.value.join(',')"
                    class="suggestion-item" @click="$emit('select', suggestion)">
                    <el-icon class="hot-icon"><Star /></el-icon>
                    {{ suggestion.label }}
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { Clock, Star, Close } from '@element-plus/icons-vue'
import { useSearchSuggestions } from '@/composables/useSearchSuggestions'

// 定义 emits
const emit = defineEmits<{
    select: [suggestion: { label: string; value: string[] }]
}>()

const {
    popularDestinations,
    recentSearches,
    showRecentSearches,
    hasPopularDestinations,
    clearRecentSearches,
    removeRecentSearch
} = useSearchSuggestions()

// 选择目的地
const handleSelect = (suggestion: { label: string; value: string[] }) => {
    emit('select', suggestion)
}

// 清除最近搜索
const handleClearRecent = () => {
    clearRecentSearches()
}

// 删除单条最近搜索
const handleRemoveRecent = (item: { label: string; value: string[] }) => {
    removeRecentSearch(item)
}
</script>

<style scoped>
.search-suggestions {
    background: white;
    border-radius: 12px;
    padding: 20px;
    margin-top: 16px;
    box-shadow: 0 2px 16px rgba(0, 0, 0, 0.12);
}

.suggestion-section {
    margin-bottom: 16px;
}

.suggestion-section:last-child {
    margin-bottom: 0;
}

.suggestion-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
}

.suggestion-title {
    font-size: 16px;
    font-weight: 600;
    color: #222;
    margin-bottom: 12px;
}

.suggestion-header .suggestion-title {
    margin-bottom: 0;
}

.suggestion-items {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.suggestion-item {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 8px 16px;
    background: #f7f7f7;
    border-radius: 20px;
    cursor: pointer;
    font-size: 14px;
    color: #222;
    transition: all 0.2s ease;
}

.suggestion-item:hover {
    background: #ff385c;
    color: white;
}

.suggestion-item .hot-icon {
    color: #ff385c;
}

.suggestion-item:hover .hot-icon {
    color: white;
}

.recent-item {
    background: #f0f0f0;
}

.recent-icon {
    color: #909399;
    font-size: 14px;
}

.suggestion-item:hover .recent-icon {
    color: white;
}

.item-label {
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.close-icon {
    font-size: 12px;
    color: #c0c4cc;
    margin-left: 4px;
}

.close-icon:hover {
    color: #ff385c;
}

@media (max-width: 480px) {
    .suggestion-items {
        flex-direction: column;
    }

    .suggestion-item {
        text-align: center;
        justify-content: center;
    }
}
</style>
