<template>
    <div class="search-suggestions">
        <div class="suggestion-title">热门目的地</div>
        <div class="suggestion-items">
            <div v-for="suggestion in popularDestinations" :key="suggestion.label" class="suggestion-item"
                @click="$emit('select', suggestion)">
                {{ suggestion.label }}
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

// 定义 emits
defineEmits<{
    select: [suggestion: { label: string; value: string[] }]
}>()

// 热门目的地建议
const popularDestinations = ref([
    { label: '北京', value: ['110000', '110100'] },
    { label: '上海', value: ['310000', '310100'] },
    { label: '杭州', value: ['330000', '330100'] },
    { label: '成都', value: ['510000', '510100'] },
    { label: '西安', value: ['610000', '610100'] },
    { label: '厦门', value: ['350000', '350200'] }
])
</script>

<style scoped>
.search-suggestions {
    background: white;
    border-radius: 12px;
    padding: 20px;
    margin-top: 16px;
    box-shadow: 0 2px 16px rgba(0, 0, 0, 0.12);
}

.suggestion-title {
    font-size: 16px;
    font-weight: 600;
    color: #222;
    margin-bottom: 16px;
}

.suggestion-items {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.suggestion-item {
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

@media (max-width: 480px) {
    .suggestion-items {
        flex-direction: column;
    }

    .suggestion-item {
        text-align: center;
    }
}
</style>