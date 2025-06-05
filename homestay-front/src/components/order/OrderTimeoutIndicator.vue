<template>
    <div class="timeout-indicator" :class="urgencyClass">
        <!-- 超时状态指示器 -->
        <div class="timeout-status">
            <el-icon :size="16" :color="statusColor">
                <Clock v-if="!isTimedOut" />
                <Warning v-else />
            </el-icon>
            <span class="status-text">{{ statusText }}</span>
        </div>

        <!-- 倒计时显示 -->
        <div class="countdown-display" v-if="!isTimedOut && remainingTime !== Infinity">
            <span class="countdown-text">{{ countdownText }}</span>
            <div class="progress-bar">
                <div class="progress-fill" :style="{ width: progressPercentage + '%', backgroundColor: statusColor }">
                </div>
            </div>
        </div>

        <!-- 预警消息 -->
        <div class="warning-message" v-if="showWarning">
            <el-icon>
                <InfoFilled />
            </el-icon>
            <span>{{ warningMessage }}</span>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watchEffect } from 'vue'
import { Clock, Warning, InfoFilled } from '@element-plus/icons-vue'
import {
    calculateRemainingTime,
    getTimeoutCountdownText,
    getTimeoutStatusColor,
    getOrderUrgency,
    setupTimeoutHandler,
    clearOrderTimers,
    isOrderTimedOut
} from '@/utils/orderTimeout'
import { OrderStatus } from '@/types/order'

interface Props {
    orderId: number
    orderStatus: OrderStatus
    createTime: string
    confirmTime?: string
    updateTime?: string
    showProgress?: boolean
    showWarning?: boolean
}

interface Emits {
    (e: 'timeout'): void
    (e: 'warning', remainingTime: number): void
}

const props = withDefaults(defineProps<Props>(), {
    showProgress: true,
    showWarning: true
})

const emit = defineEmits<Emits>()

// 响应式状态
const remainingTime = ref(0)
const isTimedOut = ref(false)
const urgency = ref<'low' | 'medium' | 'high' | 'critical'>('low')

// 更新间隔定时器
let updateInterval: ReturnType<typeof setInterval> | null = null

// 计算属性
const statusColor = computed(() => getTimeoutStatusColor(remainingTime.value))

const urgencyClass = computed(() => `urgency-${urgency.value}`)

const statusText = computed(() => {
    if (isTimedOut.value) return '已超时'
    if (remainingTime.value === Infinity) return '无时间限制'

    switch (props.orderStatus) {
        case OrderStatus.PENDING:
            return '等待确认'
        case OrderStatus.CONFIRMED:
            return '等待支付'
        case OrderStatus.PAYMENT_PENDING:
            return '支付处理中'
        default:
            return '正常'
    }
})

const countdownText = computed(() => {
    if (remainingTime.value === Infinity) return ''
    if (isTimedOut.value) return '已超时'

    const showSeconds = urgency.value === 'critical' || urgency.value === 'high'
    return getTimeoutCountdownText(remainingTime.value, showSeconds)
})

const progressPercentage = computed(() => {
    if (remainingTime.value === Infinity || isTimedOut.value) return 0

    // 计算总超时时间
    const totalTimeout = getTotalTimeout()
    if (totalTimeout === 0) return 0

    return Math.max(0, Math.min(100, (remainingTime.value / totalTimeout) * 100))
})

const warningMessage = computed(() => {
    if (!props.showWarning || urgency.value === 'low') return ''

    switch (urgency.value) {
        case 'critical':
            return '紧急：订单即将超时，请立即处理！'
        case 'high':
            return '注意：订单即将超时，请尽快处理'
        case 'medium':
            return '提醒：请注意订单处理时间'
        default:
            return ''
    }
})

// 方法
function getTotalTimeout(): number {
    switch (props.orderStatus) {
        case OrderStatus.PENDING:
            return 24 * 60 * 60 * 1000 // 24小时
        case OrderStatus.CONFIRMED:
        case OrderStatus.PAYMENT_PENDING:
            return 2 * 60 * 60 * 1000 // 2小时
        default:
            return 0
    }
}

function updateStatus(): void {
    remainingTime.value = calculateRemainingTime(
        props.orderStatus,
        props.createTime,
        props.confirmTime,
        props.updateTime
    )

    isTimedOut.value = isOrderTimedOut(
        props.orderStatus,
        props.createTime,
        props.confirmTime,
        props.updateTime
    )

    urgency.value = getOrderUrgency(
        props.orderStatus,
        props.createTime,
        props.confirmTime,
        props.updateTime
    )
}

function startUpdateTimer(): void {
    // 根据紧急程度决定更新频率
    let interval: number
    switch (urgency.value) {
        case 'critical':
            interval = 1000 // 1秒
            break
        case 'high':
            interval = 5000 // 5秒
            break
        case 'medium':
            interval = 30000 // 30秒
            break
        default:
            interval = 60000 // 1分钟
    }

    updateInterval = setInterval(updateStatus, interval)
}

function stopUpdateTimer(): void {
    if (updateInterval) {
        clearInterval(updateInterval)
        updateInterval = null
    }
}

// 生命周期
onMounted(() => {
    // 初始化状态
    updateStatus()

    // 设置超时处理器
    setupTimeoutHandler(
        props.orderId,
        props.orderStatus,
        props.createTime,
        props.confirmTime,
        props.updateTime,
        () => {
            emit('timeout')
            updateStatus()
        },
        (warningTime) => {
            emit('warning', warningTime)
            updateStatus()
        }
    )

    // 开始定时更新
    startUpdateTimer()
})

onUnmounted(() => {
    // 清理定时器
    stopUpdateTimer()
    clearOrderTimers(props.orderId)
})

// 监听紧急程度变化，调整更新频率
watchEffect(() => {
    if (updateInterval) {
        stopUpdateTimer()
        startUpdateTimer()
    }
})
</script>

<style scoped>
.timeout-indicator {
    padding: 12px;
    border-radius: 8px;
    border: 1px solid #e4e7ed;
    background-color: #fafafa;
    transition: all 0.3s ease;
}

.urgency-low {
    border-color: #67c23a;
    background-color: #f0f9ff;
}

.urgency-medium {
    border-color: #409eff;
    background-color: #ecf5ff;
}

.urgency-high {
    border-color: #e6a23c;
    background-color: #fdf6ec;
    animation: pulse-warning 2s infinite;
}

.urgency-critical {
    border-color: #f56c6c;
    background-color: #fef0f0;
    animation: pulse-critical 1s infinite;
}

@keyframes pulse-warning {

    0%,
    100% {
        box-shadow: 0 0 0 0 rgba(230, 162, 60, 0.4);
    }

    50% {
        box-shadow: 0 0 10px 5px rgba(230, 162, 60, 0.2);
    }
}

@keyframes pulse-critical {

    0%,
    100% {
        box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.4);
    }

    50% {
        box-shadow: 0 0 15px 8px rgba(245, 108, 108, 0.3);
    }
}

.timeout-status {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
}

.status-text {
    font-weight: 500;
    font-size: 14px;
}

.countdown-display {
    margin-bottom: 8px;
}

.countdown-text {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
}

.progress-bar {
    width: 100%;
    height: 4px;
    background-color: #e4e7ed;
    border-radius: 2px;
    margin-top: 8px;
    overflow: hidden;
}

.progress-fill {
    height: 100%;
    transition: width 0.3s ease, background-color 0.3s ease;
    border-radius: 2px;
}

.warning-message {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 12px;
    background-color: rgba(230, 162, 60, 0.1);
    border: 1px solid #e6a23c;
    border-radius: 4px;
    font-size: 13px;
    color: #e6a23c;
}

.urgency-critical .warning-message {
    background-color: rgba(245, 108, 108, 0.1);
    border-color: #f56c6c;
    color: #f56c6c;
}
</style>