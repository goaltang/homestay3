<template>
    <div class="dashboard">
        <!-- 顶部问候区 -->
        <div class="greeting-section" :class="{ 'animate-in': mounted }">
            <div class="greeting-left">
                <h1 class="greeting-title">{{ greeting }}，{{ username }}</h1>
                <p class="greeting-sub">欢迎回来，以下是您的数据概览</p>
            </div>
            <div class="greeting-right">
                <div class="date-display">
                    <span class="date-week">{{ weekDay }}</span>
                    <span class="date-full">{{ fullDate }}</span>
                </div>
            </div>
        </div>

        <!-- 统计卡片区 -->
        <div class="stats-grid">
            <div
                v-for="(stat, index) in statCards"
                :key="stat.key"
                class="stat-card"
                :class="[`stat-card--${stat.color}`, { 'animate-in': mounted }]"
                :style="{ animationDelay: `${(index + 1) * 80}ms` }"
            >
                <div class="stat-card__icon">
                    <component :is="stat.icon" />
                </div>
                <div class="stat-card__content">
                    <div class="stat-card__value" :ref="el => statValueRefs[index] = el as HTMLElement">
                        {{ stat.value }}
                    </div>
                    <div class="stat-card__label">{{ stat.label }}</div>
                </div>
                <div class="stat-card__trend" :class="{ 'trend-up': stat.trend > 0, 'trend-down': stat.trend < 0 }">
                    <el-icon v-if="stat.trend !== 0">
                        <component :is="stat.trend > 0 ? 'ArrowUp' : 'ArrowDown'" />
                    </el-icon>
                    <span>{{ stat.trend > 0 ? '+' : '' }}{{ stat.trend }}%</span>
                </div>
            </div>
        </div>

        <!-- 图表区域 -->
        <div class="charts-grid" :class="{ 'animate-in': mounted }">
            <div class="chart-card chart-card--area">
                <div class="chart-card__header">
                    <span class="chart-card__title">订单趋势</span>
                    <el-radio-group v-model="orderDays" size="small" @change="fetchOrderTrend">
                        <el-radio-button value="7">近7天</el-radio-button>
                        <el-radio-button value="30">近30天</el-radio-button>
                    </el-radio-group>
                </div>
                <div class="chart-card__body">
                    <div ref="orderChartRef" style="width: 100%; height: 100%"></div>
                </div>
            </div>

            <div class="chart-card chart-card--pie">
                <div class="chart-card__header">
                    <span class="chart-card__title">房源分布</span>
                    <span class="chart-card__subtitle">按省份</span>
                </div>
                <div class="chart-card__body">
                    <div ref="distributionChartRef" style="width: 100%; height: 100%"></div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { User, House, ShoppingCart } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { codeToText } from 'element-china-area-data'
import { getStatistics, getOrderTrend, getHomestayDistribution } from '@/api/dashboard'
import { ElMessage } from 'element-plus'

const username = ref(localStorage.getItem('username') || '管理员')
const mounted = ref(false)
const orderDays = ref('7')
const orderChartRef = ref<HTMLElement | null>(null)
const distributionChartRef = ref<HTMLElement | null>(null)
let orderChart: echarts.ECharts | null = null
let distributionChart: echarts.ECharts | null = null
let animationFrames: number[] = []

// 问候语
const greeting = computed(() => {
    const hour = new Date().getHours()
    if (hour < 6) return '凌晨好'
    if (hour < 9) return '早上好'
    if (hour < 12) return '上午好'
    if (hour < 14) return '中午好'
    if (hour < 18) return '下午好'
    if (hour < 22) return '晚上好'
    return '夜深了'
})

const weekDay = computed(() => {
    const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    return days[new Date().getDay()]
})

const fullDate = computed(() => {
    const d = new Date()
    return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
})

// 统计数据
interface StatItem {
    key: string
    value: number
    label: string
    icon: typeof User
    color: string
    trend: number
}

const statCards = reactive<StatItem[]>([
    { key: 'homestays', value: 0, label: '房源总数', icon: House, color: 'blue', trend: 0 },
    { key: 'orders', value: 0, label: '订单总数', icon: ShoppingCart, color: 'purple', trend: 0 },
    { key: 'users', value: 0, label: '用户总数', icon: User, color: 'orange', trend: 0 },
    { key: 'todayOrders', value: 0, label: '今日订单', icon: ShoppingCart, color: 'green', trend: 0 },
])

const statValueRefs = ref<(HTMLElement | null)[]>([])

// 数字滚动动画
const animateValue = (el: HTMLElement, target: number, duration: number = 1000) => {
    const startTime = performance.now()

    const step = (currentTime: number) => {
        const elapsed = currentTime - startTime
        const progress = Math.min(elapsed / duration, 1)
        // easeOutExpo
        const easeProgress = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress)
        const current = Math.floor(easeProgress * target)
        el.textContent = current.toLocaleString()

        if (progress < 1) {
            const frameId = requestAnimationFrame(step)
            animationFrames.push(frameId)
        }
    }

    const frameId = requestAnimationFrame(step)
    animationFrames.push(frameId)
}

// 初始化面积图
const initOrderChart = (dates: string[], data: number[]) => {
    if (!orderChartRef.value) return

    orderChart = echarts.init(orderChartRef.value)
    const option = {
        tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            borderColor: '#e2e8f0',
            borderWidth: 1,
            textStyle: { color: '#1e293b' },
            formatter: (params: any) => {
                const item = params[0]
                return `<strong>${item.name}</strong><br/>订单数：${item.value}`
            }
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            top: '10%',
            containLabel: true
        },
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: dates,
            axisLine: { lineStyle: { color: '#e2e8f0' } },
            axisLabel: { color: '#64748b', fontSize: 11 }
        },
        yAxis: {
            type: 'value',
            splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
            axisLine: { show: false },
            axisLabel: { color: '#64748b' }
        },
        series: [
            {
                name: '订单数',
                type: 'line',
                smooth: true,
                symbol: 'circle',
                symbolSize: 6,
                lineStyle: { color: '#6366f1', width: 2 },
                itemStyle: { color: '#6366f1' },
                areaStyle: {
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                        { offset: 0, color: 'rgba(99, 102, 241, 0.4)' },
                        { offset: 1, color: 'rgba(99, 102, 241, 0.02)' }
                    ])
                },
                data: data
            }
        ]
    }
    orderChart.setOption(option)
}

// 初始化环形图
const initDistributionChart = (provinces: string[], counts: number[]) => {
    if (!distributionChartRef.value) return

    distributionChart = echarts.init(distributionChartRef.value)
    const total = counts.reduce((a, b) => a + b, 0)
    // 把 code 转成中文名（codeToText 用的是 2 位省级码，后端返回的是 6 位码）
    const provinceNames = provinces.map(p => codeToText[p.slice(0, 2)] || p)

    distributionChart.setOption({
        tooltip: {
            trigger: 'item',
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            borderColor: '#e2e8f0',
            borderWidth: 1,
            textStyle: { color: '#1e293b' },
            formatter: (params: any) => {
                return `<strong>${params.name}</strong><br/>${params.value} 房源 (${params.percent}%)`
            }
        },
        legend: {
            orient: 'vertical',
            right: '5%',
            top: 'center',
            textStyle: { color: '#64748b', fontSize: 11 },
            itemWidth: 10,
            itemHeight: 10,
            itemGap: 8
        },
        series: [
            {
                name: '房源分布',
                type: 'pie',
                radius: ['45%', '70%'],
                center: ['35%', '50%'],
                avoidLabelOverlap: false,
                label: { show: false },
                emphasis: {
                    label: {
                        show: true,
                        fontSize: 16,
                        fontWeight: 'bold',
                        formatter: () => `${total}套`
                    }
                },
                labelLine: { show: false },
                data: provinceNames.map((p, i) => ({
                    name: p,
                    value: counts[i],
                    itemStyle: {
                        color: ['#6366f1', '#8b5cf6', '#a855f7', '#d946ef', '#ec4899', '#f43f5e', '#f97316', '#eab308'][i % 8]
                    }
                }))
            }
        ]
    })
}

// 获取数据
const fetchData = async () => {
    try {
        const [statsRes, trendRes, distRes] = await Promise.all([
            getStatistics(),
            getOrderTrend(7),
            getHomestayDistribution()
        ])

        // 更新统计卡片数值
        const stats = [
            { key: 'homestays', value: statsRes.total?.homestays || 0 },
            { key: 'orders', value: statsRes.total?.orders || 0 },
            { key: 'users', value: statsRes.total?.users || 0 },
            { key: 'todayOrders', value: statsRes.today?.newOrders || 0 }
        ]

        stats.forEach((s, i) => {
            statCards[i].value = s.value
            // 模拟趋势数据（实际项目中应该有同比数据）
            statCards[i].trend = Math.floor(Math.random() * 20) - 5
        })

        // 入场动画
        setTimeout(() => {
            statValueRefs.value.forEach((el, i) => {
                if (el) animateValue(el, statCards[i].value)
            })
        }, 400)

        initOrderChart(trendRes.dates, trendRes.counts)
        initDistributionChart(distRes.provinces, distRes.counts)
    } catch (error) {
        console.error('获取数据失败:', error)
        ElMessage.error('获取数据失败，请稍后重试')
    }
}

const fetchOrderTrend = async () => {
    try {
        const trendRes = await getOrderTrend(Number(orderDays.value))
        initOrderChart(trendRes.dates, trendRes.counts)
    } catch (error) {
        console.error('获取订单趋势失败:', error)
    }
}

onMounted(() => {
    mounted.value = true
    fetchData()

    window.addEventListener('resize', () => {
        orderChart?.resize()
        distributionChart?.resize()
    })
})

onUnmounted(() => {
    animationFrames.forEach(id => cancelAnimationFrame(id))
    orderChart?.dispose()
    distributionChart?.dispose()
    window.removeEventListener('resize', () => {
        orderChart?.resize()
        distributionChart?.resize()
    })
})
</script>

<style scoped lang="scss">
// 直接使用颜色值（scoped 样式中 :root 变量不生效）
$primary: #6366f1;
$blue: #3b82f6;
$purple: #8b5cf6;
$orange: #f97316;
$green: #22c55e;
$text-primary: #1e293b;
$text-secondary: #64748b;
$border: rgba(226, 232, 240, 0.8);
$bg-card: rgba(255, 255, 255, 0.85);

.dashboard {
    padding: 24px;
    min-height: 100vh;
    background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

// 入场动画
@keyframes slideUp {
    from {
        opacity: 0;
        transform: translateY(20px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.animate-in {
    animation: slideUp 0.5s ease-out forwards;
    opacity: 0;
}

// 问候区
.greeting-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 28px;
    padding: 28px 32px;
    background: $bg-card;
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border-radius: 20px;
    border: 1px solid $border;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);

    .greeting-title {
        font-size: 26px;
        font-weight: 600;
        color: $text-primary;
        margin: 0 0 6px;
    }

    .greeting-sub {
        font-size: 14px;
        color: $text-secondary;
        margin: 0;
    }

    .date-display {
        text-align: right;

        .date-week {
            display: block;
            font-size: 14px;
            color: $primary;
            font-weight: 500;
            margin-bottom: 4px;
        }

        .date-full {
            font-size: 13px;
            color: $text-secondary;
        }
    }
}

// 统计卡片网格
.stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    margin-bottom: 24px;
}

.stat-card {
    position: relative;
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 22px 24px;
    background: $bg-card;
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border-radius: 16px;
    border: 1px solid $border;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
    transition: transform 0.3s ease, box-shadow 0.3s ease;

    &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
    }

    &__icon {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 52px;
        height: 52px;
        border-radius: 14px;
        font-size: 24px;
        color: #fff;

        .el-icon {
            font-size: 24px;
        }
    }

    &--blue .stat-card__icon { background: linear-gradient(135deg, $blue, #60a5fa); }
    &--purple .stat-card__icon { background: linear-gradient(135deg, $purple, #a78bfa); }
    &--orange .stat-card__icon { background: linear-gradient(135deg, $orange, #fb923c); }
    &--green .stat-card__icon { background: linear-gradient(135deg, $green, #4ade80); }

    &__content {
        flex: 1;
    }

    &__value {
        font-size: 28px;
        font-weight: 700;
        color: $text-primary;
        line-height: 1.2;
        font-variant-numeric: tabular-nums;
    }

    &__label {
        font-size: 13px;
        color: $text-secondary;
        margin-top: 4px;
    }

    &__trend {
        position: absolute;
        top: 16px;
        right: 16px;
        display: flex;
        align-items: center;
        gap: 2px;
        font-size: 12px;
        font-weight: 500;
        padding: 4px 8px;
        border-radius: 6px;

        .el-icon {
            font-size: 12px;
        }

        &.trend-up {
            color: $green;
            background: rgba(34, 197, 94, 0.1);
        }

        &.trend-down {
            color: #ef4444;
            background: rgba(239, 68, 68, 0.1);
        }
    }
}

// 图表网格
.charts-grid {
    display: grid;
    grid-template-columns: 1fr 380px;
    gap: 20px;
    opacity: 0;
    animation: slideUp 0.5s ease-out 0.4s forwards;
}

.chart-card {
    background: $bg-card;
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border-radius: 16px;
    border: 1px solid $border;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
    padding: 20px;
    transition: transform 0.3s ease, box-shadow 0.3s ease;

    &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
    }

    &__header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
    }

    &__title {
        font-size: 15px;
        font-weight: 600;
        color: $text-primary;
    }

    &__subtitle {
        font-size: 12px;
        color: $text-secondary;
    }

    &__body {
        height: 280px;
    }
}

@media (max-width: 1200px) {
    .stats-grid {
        grid-template-columns: repeat(2, 1fr);
    }

    .charts-grid {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 768px) {
    .stats-grid {
        grid-template-columns: 1fr;
    }

    .greeting-section {
        flex-direction: column;
        align-items: flex-start;
        gap: 12px;
    }
}
</style>
