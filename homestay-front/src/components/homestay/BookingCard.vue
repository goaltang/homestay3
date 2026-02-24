<template>
    <div class="booking-card">
        <div class="booking-card-header">
            <div class="price-info">
                <span class="price">¥{{ pricePerNight }}</span>
                <span class="night">/晚</span>
            </div>
            <div class="rating-info">
                <el-rate v-model="numericRating" disabled text-color="#FF9900" disabled-void-color="#C6D1DE" />
                <span class="review-link">{{ reviewCount }}条评价</span>
            </div>
        </div>

        <div class="booking-form">
            <div class="date-picker-container">
                <div class="date-picker-header">
                    <div class="check-in-label">入住</div>
                    <div class="check-out-label">退房</div>
                </div>
                <el-date-picker v-model="localDateRange" type="daterange" range-separator="至" start-placeholder="选择日期"
                    end-placeholder="选择日期" format="YYYY/MM/DD" :disabled-date="disablePastDates"
                    @change="handleDateRangeChange" :size="'large'" class="date-range-picker" :loading="loadingDates" />
            </div>

            <div class="guest-selector-container">
                <div class="guest-selector-label">房客</div>
                <el-select v-model="localGuests" placeholder="1位房客" :size="'large'" class="guest-dropdown">
                    <el-option v-for="i in maxGuests" :key="i" :label="`${i}位房客`" :value="i" />
                </el-select>
            </div>

            <!-- 日期可用性提示 -->
            <div class="date-availability-hint" v-if="!loadingDates">
                <el-text type="info" size="small">
                    <el-icon>
                        <InfoFilled />
                    </el-icon>
                    <span v-if="unavailableDates.length > 0">
                        已有 {{ unavailableDates.length }} 个日期被预订，灰色日期无法选择
                    </span>
                    <span v-else>
                        当前该房源暂无已预订日期
                    </span>
                </el-text>
            </div>
        </div>

        <!-- 自动确认模式提示 -->
        <div class="booking-mode-notice" v-if="autoConfirm">
            <el-icon class="auto-confirm-icon" :size="16">
                <Lightning />
            </el-icon>
            <span>即时预订 - 确认后立即生效</span>
        </div>

        <el-button type="primary" class="booking-button" @click="handleBooking"
            :disabled="!localDateRange || !localDateRange[0] || !localDateRange[1]">
            {{ autoConfirm ? '立即预订' : '申请预订' }}
        </el-button>

        <div class="price-breakdown" v-if="totalNights > 0">
            <div class="price-row">
                <div class="price-item">¥{{ pricePerNight }} x {{ totalNights }}晚</div>
                <div class="price-value">¥{{ basePrice }}</div>
            </div>
            <div class="price-row">
                <div class="price-item">清洁费</div>
                <div class="price-value">¥{{ cleaningFee }}</div>
            </div>
            <div class="price-row">
                <div class="price-item">服务费</div>
                <div class="price-value">¥{{ serviceFee }}</div>
            </div>
            <div class="price-divider"></div>
            <div class="price-row total">
                <div class="price-item">总价</div>
                <div class="price-value">¥{{ totalPrice }}</div>
            </div>
        </div>

        <div class="booking-note">
            <el-icon>
                <InfoFilled />
            </el-icon>
            <span>预订前不会向您收取任何费用</span>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { InfoFilled, Lightning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getHomestayUnavailableDates } from '@/api/homestay'

// Props
interface Props {
    pricePerNight: number
    rating: number
    reviewCount: number
    maxGuests: number
    homestayId: number
    autoConfirm?: boolean
    dates?: [Date, Date] | null
    guests?: number
}

const props = withDefaults(defineProps<Props>(), {
    autoConfirm: false,
    dates: null,
    guests: 1
})

// Emits
const emit = defineEmits<{
    'booking-confirmed': []
    'date-changed': [checkIn: Date | null, checkOut: Date | null]
    'guests-changed': [guests: number]
}>()

// Local reactive data
const localDateRange = ref<[Date, Date] | null>(null)
const localGuests = ref(props.guests)

// 不可用日期
const unavailableDates = ref<string[]>([])
const loadingDates = ref(false)

// Watch for prop changes
watch(() => props.dates, (newDates) => {
    localDateRange.value = newDates
}, { immediate: true })

watch(() => props.guests, (newGuests) => {
    localGuests.value = newGuests
}, { immediate: true })

// Computed properties
const numericRating = computed(() => {
    return isNaN(props.rating) ? 0 : props.rating
})

const totalNights = computed(() => {
    if (localDateRange.value && localDateRange.value[0] && localDateRange.value[1]) {
        const checkInTime = localDateRange.value[0].getTime()
        const checkOutTime = localDateRange.value[1].getTime()
        if (checkOutTime > checkInTime) {
            const diffTime = checkOutTime - checkInTime
            return Math.ceil(diffTime / (1000 * 60 * 60 * 24))
        }
    }
    return 0
})

const basePrice = computed(() => {
    return props.pricePerNight * totalNights.value
})

const cleaningFee = computed(() => {
    if (totalNights.value > 0 && props.pricePerNight > 0) {
        return Math.round(props.pricePerNight * 0.1)
    }
    return 0
})

const serviceFee = computed(() => {
    if (basePrice.value > 0) {
        return Math.round(basePrice.value * 0.05)
    }
    return 0
})

const totalPrice = computed(() => {
    return basePrice.value + cleaningFee.value + serviceFee.value
})

// Methods
const fetchUnavailableDates = async () => {
    if (!props.homestayId) return

    loadingDates.value = true
    try {
        const response = await getHomestayUnavailableDates(props.homestayId)

        // 适配新的后端响应格式
        const responseData = response.data || {}
        let dates = responseData.data || []

        console.log('后端API完整响应:', response.data)

        // 确保日期格式为字符串数组 (YYYY-MM-DD)
        if (Array.isArray(dates)) {
            unavailableDates.value = dates.map(date => {
                if (typeof date === 'string') {
                    // 如果已经是字符串，确保格式正确
                    return date.includes('-') ? date : formatStringToStandardDate(date)
                } else if (date instanceof Date) {
                    // 如果是Date对象，转换为YYYY-MM-DD格式
                    return formatDateToString(date)
                } else if (typeof date === 'object' && date !== null) {
                    // 如果是类似 [2024, 12, 25] 的数组格式，转换为字符串
                    if (Array.isArray(date) && date.length >= 3) {
                        const [year, month, day] = date
                        return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
                    }
                    // 如果是对象格式，尝试提取年月日
                    if (date.year && date.month && date.day) {
                        return `${date.year}-${String(date.month).padStart(2, '0')}-${String(date.day).padStart(2, '0')}`
                    }
                }
                return String(date) // 其他情况尝试转为字符串
            })
        } else {
            unavailableDates.value = []
        }

        console.log('处理后的不可用日期:', unavailableDates.value)
        console.log('不可用日期数量:', unavailableDates.value.length)

        // 显示错误信息（如果有）
        if (responseData.error) {
            console.warn('后端返回错误信息:', responseData.error)
        }

    } catch (error) {
        console.error('获取不可用日期失败:', error)
        unavailableDates.value = []
    } finally {
        loadingDates.value = false
    }
}

// 辅助函数：格式化字符串日期为标准格式
const formatStringToStandardDate = (dateStr: string): string => {
    try {
        const date = new Date(dateStr)
        if (isNaN(date.getTime())) {
            return dateStr // 如果无法解析，返回原始字符串
        }
        return formatDateToString(date)
    } catch {
        return dateStr
    }
}

// 组件挂载时获取不可用日期
onMounted(() => {
    fetchUnavailableDates()
})

// 增强的日期禁用逻辑
const disablePastDates = (date: Date) => {
    // 禁用过去日期
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    if (date.getTime() < today.getTime()) return true

    // 禁用已预订日期
    const dateStr = formatDateToString(date)
    const isUnavailable = unavailableDates.value.includes(dateStr)

    // 调试信息（仅在有不可用日期时输出，避免过度日志）
    if (unavailableDates.value.length > 0 && isUnavailable) {
        console.log('日期 ' + dateStr + ' 已被预订，禁用选择')
    }

    return isUnavailable
}

// 格式化日期为字符串（YYYY-MM-DD格式）
const formatDateToString = (date: Date): string => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
}

const handleDateRangeChange = (dates: [Date, Date] | null) => {
    if (dates && dates.length === 2 && dates[0] && dates[1]) {
        if (dates[1].getTime() <= dates[0].getTime()) {
            ElMessage.warning('退房日期必须在入住日期之后')
            localDateRange.value = null
            emit('date-changed', null, null)
        } else {
            emit('date-changed', dates[0], dates[1])
        }
    } else {
        emit('date-changed', null, null)
    }
}

const handleBooking = () => {
    if (!localDateRange.value || !localDateRange.value[0] || !localDateRange.value[1]) {
        ElMessage.warning('请先选择有效的入住和退房日期')
        return
    }

    if (totalNights.value <= 0) {
        ElMessage.warning('请选择有效的住宿日期')
        return
    }

    emit('booking-confirmed')
}

// Watch guests change
watch(localGuests, (newGuests) => {
    emit('guests-changed', newGuests)
})
</script>

<style scoped>
.booking-card {
    border-radius: 16px;
    border: 1px solid #e0e0e0;
    padding: 24px;
    box-shadow: 0 6px 24px rgba(0, 0, 0, 0.08);
    background-color: white;
    position: sticky;
    top: 20px;
    height: fit-content;
    transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.booking-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
}

.booking-card-header {
    margin-bottom: 24px;
}

.price-info {
    display: flex;
    align-items: baseline;
    margin-bottom: 12px;
}

.price {
    font-size: 28px;
    font-weight: 700;
    color: #303133;
}

.night {
    font-size: 16px;
    font-weight: normal;
    color: #606266;
    margin-left: 4px;
}

.rating-info {
    display: flex;
    align-items: center;
    gap: 8px;
}

.review-link {
    font-size: 14px;
    color: #606266;
    text-decoration: underline;
    cursor: pointer;
}

.booking-form {
    margin-bottom: 20px;
    border: 1px solid #e0e0e0;
    border-radius: 12px;
    overflow: hidden;
}

.date-picker-container {
    border-bottom: 1px solid #e0e0e0;
}

.date-picker-header {
    display: flex;
    padding: 10px 16px 0;
}

.check-in-label,
.check-out-label {
    font-size: 12px;
    font-weight: 600;
    color: #606266;
    flex: 1;
}

.date-range-picker {
    width: 100%;
    border: none;
}

.date-range-picker :deep(.el-input__wrapper) {
    box-shadow: none !important;
    padding: 8px 16px 16px;
}

.guest-selector-container {
    padding: 10px 16px;
}

.guest-selector-label {
    font-size: 12px;
    font-weight: 600;
    color: #606266;
    margin-bottom: 5px;
}

.guest-dropdown {
    width: 100%;
}

.guest-dropdown :deep(.el-input__wrapper) {
    box-shadow: none !important;
}

.date-availability-hint {
    margin-top: 8px;
    padding: 8px 12px;
    background-color: #f5f7fa;
    border-radius: 6px;
    display: flex;
    align-items: center;
    gap: 6px;
}

.date-availability-hint .el-icon {
    font-size: 14px;
    color: #909399;
}

.booking-button {
    width: 100%;
    height: 48px;
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 20px;
    border-radius: 8px;
}

.price-breakdown {
    margin-bottom: 20px;
}

.price-row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
    font-size: 14px;
    color: #606266;
}

.price-row.total {
    font-weight: 600;
    color: #303133;
    font-size: 16px;
}

.price-divider {
    height: 1px;
    background-color: #e0e0e0;
    margin: 12px 0;
}

.booking-note {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: #909399;
    padding: 12px 16px;
    background-color: #f7f8fa;
    border-radius: 8px;
}

.booking-note .el-icon {
    font-size: 14px;
}

/* 自动确认模式样式 */
.booking-mode-notice {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    background: linear-gradient(135deg, #f0f9ff 0%, #e0f7fa 100%);
    border: 1px solid #67c23a;
    border-radius: 8px;
    margin-bottom: 16px;
    font-size: 14px;
    color: #2c7b2f;
    font-weight: 500;
}

.auto-confirm-icon {
    color: #67c23a;
}
</style>