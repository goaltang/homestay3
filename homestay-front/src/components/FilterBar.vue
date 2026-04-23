<template>
    <div class="filter-container">
        <div class="filter-scroll">
            <!-- "全部" 按钮 -->
            <div class="filter-item" :class="{ active: !selectedPropertyType }" @click="selectPropertyType(null)">
                <span>全部</span>
            </div>
            <!-- 动态类型按钮 -->
            <div v-for="type in propertyTypes" :key="type.id" class="filter-item"
                :class="{ active: selectedPropertyType === type.name }" @click="selectPropertyType(type.name)">
                <span>{{ type.name }}</span>
            </div>
            <!-- 分组筛选按钮 -->
            <div v-if="groups.length > 0" class="group-filter-divider"></div>
            <div v-for="group in groups" :key="group.id" class="filter-item group-item"
                :class="{ active: selectedGroupId === group.id }" @click="selectGroup(group.id)">
                <span :style="selectedGroupId === group.id && group.color ? { color: group.color } : {}">{{ group.name }}</span>
            </div>
        </div>

        <div class="filter-actions">
            <el-popover placement="bottom" :width="350" trigger="click">
                <template #reference>
                    <el-button class="filter-button">
                        <el-icon>
                            <Filter />
                        </el-icon>
                        价格与设施
                        <el-badge v-if="activeFilterCount > 0" :value="activeFilterCount" class="filter-badge" />
                    </el-button>
                </template>
                <div class="filter-popover">
                    <div class="filter-section">
                        <h4>价格范围</h4>
                        <div class="price-range">
                            <el-input-number v-model="filterParams.minPrice" :min="0" :step="50" placeholder="最低价"
                                controls-position="right" style="width: 110px;" />
                            <span class="separator">-</span>
                            <el-input-number v-model="filterParams.maxPrice" :min="0" :step="50" placeholder="最高价"
                                controls-position="right" style="width: 110px;" />
                        </div>
                    </div>
                    <div class="filter-section">
                        <h4>设施</h4>
                        <div v-if="amenitiesLoading" class="amenities-loading">加载设施中...</div>
                        <el-collapse v-else-if="groupedAmenities.length > 0" class="amenities-collapse">
                            <el-collapse-item v-for="category in groupedAmenities" :key="category.code || category.name"
                                :title="category.name" :name="category.code || category.name">
                                <el-checkbox-group v-model="filterParams.amenities" class="amenities-checkbox-group">
                                    <el-checkbox v-for="amenity in category.amenities" :key="amenity.value"
                                        :label="amenity.value" :value="amenity.value">
                                        {{ amenity.label }}
                                    </el-checkbox>
                                </el-checkbox-group>
                            </el-collapse-item>
                        </el-collapse>
                        <div v-else class="amenities-empty">暂无设施选项</div>
                    </div>
                    <div class="filter-actions-footer">
                        <el-button @click="handleResetFilters">清除筛选</el-button>
                        <el-button type="primary" @click="handleApplyFilters">应用筛选</el-button>
                    </div>
                </div>
            </el-popover>
        </div>
    </div>
</template>

<script setup lang="ts">
import { reactive, computed } from 'vue'
import { Filter } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// 定义类型
interface HomestayType {
    id: number
    name: string
    code: string
}

interface AmenityOption {
    value: string
    label: string
}

interface AmenityCategoryOption {
    code?: string
    name: string
    amenities: AmenityOption[]
}

interface GroupOption {
    id: number
    name: string
    color?: string
    homestayCount?: number
}

// 定义 props
interface Props {
    propertyTypes?: HomestayType[]
    groupedAmenities?: AmenityCategoryOption[]
    amenitiesLoading?: boolean
    selectedPropertyType?: string | null
    groups?: GroupOption[]
    selectedGroupId?: number | null
}

withDefaults(defineProps<Props>(), {
    propertyTypes: () => [],
    groupedAmenities: () => [],
    amenitiesLoading: false,
    selectedPropertyType: null,
    groups: () => [],
    selectedGroupId: null
})

// 定义 emits
const emit = defineEmits<{
    'property-type-change': [type: string | null]
    'filters-change': [filters: any]
    'filters-reset': []
    'group-change': [groupId: number | null]
}>()

// 响应式数据
const filterParams = reactive({
    minPrice: null as number | null,
    maxPrice: null as number | null,
    amenities: [] as string[]
})

// 计算属性
const activeFilterCount = computed(() => {
    let count = 0
    if (filterParams.minPrice !== null || filterParams.maxPrice !== null) count++
    if (filterParams.amenities.length > 0) count++
    return count
})

// 方法
const selectPropertyType = (typeName: string | null) => {
    emit('property-type-change', typeName)
}

const selectGroup = (groupId: number | null) => {
    emit('group-change', groupId)
}

const handleResetFilters = () => {
    filterParams.minPrice = null
    filterParams.maxPrice = null
    filterParams.amenities = []

    ElMessage.success('筛选条件已重置')
    emit('filters-reset')
}

const handleApplyFilters = () => {
    emit('filters-change', { ...filterParams })
}

// 暴露给父组件的方法
defineExpose({
    filterParams,
    resetFilters: handleResetFilters
})
</script>

<style scoped>
.filter-container {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    padding-bottom: 16px;
    border-bottom: 1px solid #ebebeb;
}

.filter-scroll {
    display: flex;
    overflow-x: auto;
    gap: 24px;
    padding-bottom: 8px;
    flex: 1;
}

.filter-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    opacity: 0.7;
    transition: opacity 0.2s;
    min-width: 56px;
    padding: 8px 0;
}

.filter-item:hover,
.filter-item.active {
    opacity: 1;
}

.filter-actions {
    display: flex;
    align-items: center;
    gap: 16px;
}

.filter-button {
    display: flex;
    align-items: center;
    gap: 8px;
}

.filter-badge {
    margin-left: 8px;
}

.group-filter-divider {
    width: 1px;
    background-color: #e4e7ed;
    align-self: stretch;
    margin: 0 8px;
}

.group-item {
    position: relative;
}

.group-item.active span {
    font-weight: 600;
}

.filter-popover {
    padding: 16px;
}

.filter-section {
    margin-bottom: 20px;
}

.filter-section h4 {
    margin-top: 0;
    margin-bottom: 12px;
    font-size: 14px;
    font-weight: 600;
    color: #333;
}

.price-range {
    display: flex;
    align-items: center;
    gap: 8px;
}

.price-range .el-input-number {
    width: 110px;
}

.separator {
    color: #909399;
    margin: 0 5px;
}

.amenities-collapse {
    border-top: none;
    border-bottom: none;
}

.amenities-collapse .el-collapse-item__header {
    font-size: 14px;
    font-weight: 600;
    border-bottom: 1px solid #ebebeb;
    height: 40px;
    line-height: 40px;
}

.amenities-collapse .el-collapse-item__wrap {
    border-bottom: none;
}

.amenities-collapse .el-collapse-item__content {
    padding-top: 10px;
    padding-bottom: 10px;
}

.amenities-checkbox-group {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px 16px;
}

.amenities-checkbox-group .el-checkbox {
    margin-right: 0;
    height: 26px;
    display: flex;
    align-items: center;
}

.amenities-checkbox-group .el-checkbox__label {
    font-size: 14px;
}

.amenities-loading,
.amenities-empty {
    color: #909399;
    font-size: 14px;
    padding: 15px 0;
    text-align: center;
}

.filter-actions-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 20px;
    padding-top: 16px;
    border-top: 1px solid #ebebeb;
}
</style>