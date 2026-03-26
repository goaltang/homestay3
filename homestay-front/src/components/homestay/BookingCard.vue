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
                    @change="handleDateRangeChange" @calendar-change="handleCalendarChange" :size="'large'"
                    class="date-range-picker" :loading="loadingDates" popper-class="booking-date-popper" />
            </div>

            <div class="guest-selector-container">
                <div class="guest-selector-label">房客</div>
                <el-select v-model="localGuests" placeholder="1位房客" :size="'large'" class="guest-dropdown">
                    <el-option v-for="i in maxGuests" :key="i" :label="`${i}位房客`" :value="i" />
                </el-select>
            </div>

            <!-- 日期可用性提示 -->
            <div class="date-availability-hint" v-if="!loadingDates">
                <el-icon>
                    <InfoFilled />
                </el-icon>
                <span v-if="unavailableDates.length > 0">
                    部分日期已被预订，置灰日期无法选择
                </span>
                <span v-else>
                    当前房源暂无已被预订的日期
                </span>
            </div>

            <!-- 最长可选区间提示 -->
            <div class="max-stay-hint" v-if="confirmedCheckIn && maxCheckOutDate && !loadingDates">
                <el-icon><Clock /></el-icon>
                <span>
                    从 {{ formatDateToString(confirmedCheckIn) }} 起最多可住至 {{ formatDateToString(maxCheckOutDate) }}
                    <span class="nights-count">（最多 {{ maxNights }} 晚）</span>
                </span>
            </div>

            <!-- 自动修正内联提示 -->
            <div class="auto-fix-hint" v-if="autoFixMessage">
                <el-icon><WarningFilled /></el-icon>
                <span>{{ autoFixMessage }}</span>
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
            <div v-if="isCalculating" class="price-loading-placeholder" style="margin: 20px 0;">
                <el-skeleton animated :rows="3" />
            </div>
            <template v-else-if="calculatedFees">
                <div class="price-row">
                    <div class="price-item">¥{{ pricePerNight }} x {{ calculatedFees.nights }}晚</div>
                    <div class="price-value">¥{{ calculatedFees.basePrice }}</div>
                </div>
                <div class="price-row" v-if="calculatedFees.cleaningFee > 0">
                    <div class="price-item">清洁费</div>
                    <div class="price-value">¥{{ calculatedFees.cleaningFee }}</div>
                </div>
                <div class="price-row" v-if="calculatedFees.serviceFee > 0">
                    <div class="price-item">服务费</div>
                    <div class="price-value">¥{{ calculatedFees.serviceFee }}</div>
                </div>
                <div class="price-divider"></div>
                <div class="price-row total">
                    <div class="price-item">总价</div>
                    <div class="price-value">¥{{ calculatedFees.totalPrice }}</div>
                </div>
            </template>
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
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { InfoFilled, Lightning, Clock, WarningFilled } from '@element-plus/icons-vue'
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
    calculatedFees?: any
    isCalculating?: boolean
}

const props = withDefaults(defineProps<Props>(), {
    autoConfirm: false,
    dates: null,
    guests: 1,
    isCalculating: false
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

// 新增：最长可选退房日计算相关状态
const maxCheckOutDate = ref<Date | null>(null)
const confirmedCheckIn = ref<Date | null>(null)  // 已确认的入住日（用户选定后不再变更）
const isAutoFixing = ref(false)  // 标记是否正在自动修正
const autoFixMessage = ref('')  // 自动修正提示文字
const pendingCheckIn = ref<Date | null>(null)  // 用户选了第一个日期后暂存（用于高亮计算）

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

const maxNights = computed(() => {
    if (confirmedCheckIn.value && maxCheckOutDate.value) {
        const diffTime = maxCheckOutDate.value.getTime() - confirmedCheckIn.value.getTime()
        return Math.ceil(diffTime / (1000 * 60 * 60 * 24))
    }
    return 0
})

// 改为由外部通过 computedFees 传入算价结果，不再进行本地本地算费以免导致各处算价不统一的业务灾难

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

/**
 * 基于已选入住日计算最长可选退房日
 * 算法：找到从入住日起第一个冲突日，该日期自身可作为退房日。
 */
const computeMaxCheckOutDate = (checkIn: Date): Date | null => {
    if (!unavailableDates.value.length) return null

    const checkInStr = formatDateToString(checkIn)
    const sortedUnavailable = [...unavailableDates.value].sort()

    // 找第一个 > checkInStr 的不可用日期（不包含入住日本身）
    const firstConflictIdx = sortedUnavailable.findIndex(d => d > checkInStr)

    if (firstConflictIdx === -1) {
        // 没有冲突日期，可以选任意远的退房日
        return null
    }

    const conflictDateStr = sortedUnavailable[firstConflictIdx]
    const [y, m, d] = conflictDateStr.split('-').map(Number)
    return new Date(y, m - 1, d)
}

/**
 * 检测所选日期范围是否跨越冲突日期
 * 注意：退房日=冲突日期本身是允许的（即早晨退房不再续住）
 */
const detectConflictInRange = (checkIn: Date, checkOut: Date): { hasConflict: boolean; conflictDate: Date | null } => {
    const checkInStr = formatDateToString(checkIn)
    const checkOutStr = formatDateToString(checkOut)

    // 找范围内第一个冲突日期（大于入住日且小于退房日的不可用日期）
    // 注意：d < checkOutStr 而不是 d <= checkOutStr
    // 因为退房日当天不算"入住在冲突日期"
    const conflictDateStr = unavailableDates.value.find(d => d > checkInStr && d < checkOutStr)

    if (conflictDateStr) {
        const [y, m, d] = conflictDateStr.split('-').map(Number)
        return { hasConflict: true, conflictDate: new Date(y, m - 1, d) }
    }
    return { hasConflict: false, conflictDate: null }
}

// 增强的日期禁用逻辑
const disablePastDates = (date: Date) => {
    // 禁用过去日期
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const dateTime = date.getTime()
    if (dateTime < today.getTime()) return true

    const dateStr = formatDateToString(date)
    const isUnavailable = unavailableDates.value.includes(dateStr)

    // 当用户开始选择入住日，或者处于部分选中的状态
    // 我们限制只能选择入住日之后、冲突日及之前的日期
    const activeCheckIn = pendingCheckIn.value || 
                          (localDateRange.value && !localDateRange.value[1] ? localDateRange.value[0] : null) || 
                          confirmedCheckIn.value

    if (activeCheckIn) {
        const checkInTime = activeCheckIn.getTime()
        if (dateTime > checkInTime) {
            const maxDate = computeMaxCheckOutDate(activeCheckIn)
            if (maxDate) {
                // 如果当前日期在最大可退房日期（即首个冲突日）之后，一律禁用
                if (dateTime > maxDate.getTime()) {
                    return true
                }
                // 允许把第一个冲突日作为退房日（返回 false，即使它在 unavailableDates 中）
                if (dateTime === maxDate.getTime()) {
                    return false
                }
            }
        }
    }

    // 调试信息（仅在有不可用日期时输出，避免过度日志）
    if (unavailableDates.value.length > 0 && isUnavailable) {
        console.log('日期 ' + dateStr + '(禁用状态: ' + isUnavailable + ')')
    }

    return isUnavailable
}

/**
 * 用户在 daterange 模式选了第一个日期时触发
 * 立即计算高亮范围，不等两个日期都选完
 */
const handleCalendarChange = (dates: Date[]) => {
    if (dates && dates.length >= 1 && dates[0]) {
        pendingCheckIn.value = dates[0]
    }
}

// 格式化日期为字符串（YYYY-MM-DD格式）
const formatDateToString = (date: Date): string => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
}

const handleDateRangeChange = (dates: [Date, Date] | null) => {
    // 选择完成后，清除临时的 pendingCheckIn
    pendingCheckIn.value = null
    autoFixMessage.value = ''

    if (dates && dates.length === 2 && dates[0] && dates[1]) {
        const [newCheckIn, newCheckOut] = dates

        // 先检查基本有效性
        if (newCheckOut.getTime() <= newCheckIn.getTime()) {
            ElMessage.warning('退房日期必须在入住日期之后')
            localDateRange.value = null
            confirmedCheckIn.value = null
            maxCheckOutDate.value = null
            emit('date-changed', null, null)
            return
        }

        // 入住日确认后：计算并显示最长可选区间
        if (!confirmedCheckIn.value || confirmedCheckIn.value.getTime() !== newCheckIn.getTime()) {
            confirmedCheckIn.value = newCheckIn
            maxCheckOutDate.value = computeMaxCheckOutDate(newCheckIn)
        }

        // 检测是否跨越冲突日期
        const { hasConflict, conflictDate } = detectConflictInRange(newCheckIn, newCheckOut)

        if (hasConflict && conflictDate && maxCheckOutDate.value) {
            // 自动修正：退房日设为冲突日期前一天
            isAutoFixing.value = true
            const autoCheckOut = maxCheckOutDate.value

            // 内联提示（非弹窗）
            const month = autoCheckOut.getMonth() + 1
            const day = autoCheckOut.getDate()
            autoFixMessage.value = `已为你调整至最晚可离店日期 ${month}月${day}日，此后日期已被预订`

            localDateRange.value = [newCheckIn, autoCheckOut]

            nextTick(() => {
                isAutoFixing.value = false
            })

            emit('date-changed', newCheckIn, autoCheckOut)
        } else {
            autoFixMessage.value = ''
            emit('date-changed', dates[0], dates[1])
        }
    } else {
        confirmedCheckIn.value = null
        maxCheckOutDate.value = null
        autoFixMessage.value = ''
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
    font-size: 14px;
    font-weight: 500;
    color: #909399;
    margin-left: 2px;
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
    margin-bottom: 24px;
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
    padding: 10px 16px 16px;
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
    margin: 4px 16px 16px;
    padding: 8px 12px;
    background-color: #fafafa;
    border-radius: 6px;
    display: flex;
    align-items: center;
    gap: 6px;
}

.date-availability-hint .el-icon {
    font-size: 14px;
    color: #909399;
}

/* 已预订日期斜线纹理 */
.date-range-picker :deep(.el-date-table td.disabled) {
    position: relative;
    background-color: #f5f5f5 !important;
}

.date-range-picker :deep(.el-date-table td.disabled::before) {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: repeating-linear-gradient(
        -45deg,
        transparent,
        transparent 4px,
        rgba(180, 180, 180, 0.5) 4px,
        rgba(180, 180, 180, 0.5) 8px
    );
    pointer-events: none;
    z-index: 1;
}

.date-range-picker :deep(.el-date-table td.disabled .el-date-table-cell__text) {
    color: #c0c0c0;
    background-color: transparent;
    position: relative;
    z-index: 2;
}

.date-range-picker :deep(.el-date-table td.disabled:hover .el-date-table-cell) {
    background-color: transparent !important;
}

/* 最长可选区间提示 */
.max-stay-hint {
    margin: 4px 16px 16px;
    padding: 8px 12px;
    background-color: #f0f9ff;
    border-radius: 6px;
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #409eff;
}

.max-stay-hint .el-icon {
    font-size: 14px;
}

.max-stay-hint .nights-count {
    color: #79bbff;
    font-size: 12px;
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
    margin-bottom: 16px;
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
    justify-content: center;
    gap: 6px;
    margin-bottom: 16px;
    font-size: 14px;
    color: #52c41a;
    font-weight: 500;
}

.auto-confirm-icon {
    color: #52c41a;
}
/* 自动修正内联提示 */
.auto-fix-hint {
    margin: 4px 16px 16px;
    padding: 8px 12px;
    background-color: #fffbe6;
    border-radius: 6px;
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #d48806;
    animation: fadeSlideIn 0.3s ease;
}

.auto-fix-hint .el-icon {
    font-size: 14px;
    flex-shrink: 0;
}

@keyframes fadeSlideIn {
    from {
        opacity: 0;
        transform: translateY(-4px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}
</style>

<!-- 全局样式：覆盖 teleported 到 body 的 DatePicker popper -->
<style>
/* === 已预订日期样式（斜线纹理 + 置灰 + tooltip） === */
.booking-date-popper .el-date-table td.disabled {
    position: relative;
    cursor: not-allowed !important;
}

.booking-date-popper .el-date-table td.disabled > div,
.booking-date-popper .el-date-table td.disabled > span {
    cursor: not-allowed !important;
}

/* 斜线纹理覆盖层 */
.booking-date-popper .el-date-table td.disabled::before {
    content: '';
    position: absolute;
    top: 2px;
    left: 2px;
    right: 2px;
    bottom: 2px;
    background: repeating-linear-gradient(
        -45deg,
        transparent,
        transparent 3px,
        rgba(160, 160, 160, 0.35) 3px,
        rgba(160, 160, 160, 0.35) 6px
    );
    border-radius: 4px;
    pointer-events: none;
    z-index: 1;
}

/* 禁用日期文字样式 */
.booking-date-popper .el-date-table td.disabled .el-date-table-cell__text {
    color: #b0b0b0 !important;
    background-color: transparent !important;
    position: relative;
    z-index: 2;
}

/* hover 时的 CSS tooltip */
.booking-date-popper .el-date-table td.disabled::after {
    content: '该日期已被预订';
    position: absolute;
    bottom: calc(100% + 6px);
    left: 50%;
    transform: translateX(-50%);
    background: rgba(48, 49, 51, 0.92);
    color: #fff;
    font-size: 12px;
    padding: 4px 10px;
    border-radius: 4px;
    white-space: nowrap;
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.2s ease;
    z-index: 999;
}

/* tooltip 小三角 */
.booking-date-popper .el-date-table td.disabled:hover::after {
    opacity: 1;
}

</style>