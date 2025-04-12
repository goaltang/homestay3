<template>
    <div class="dashboard-container">
        <!-- 统计卡片 -->
        <el-row :gutter="20">
            <el-col :span="6">
                <el-card shadow="hover" class="mgb20">
                    <div class="user-info">
                        <el-icon>
                            <User />
                        </el-icon>
                        <div class="user-info-cont">
                            <div class="user-info-name">{{ username }}</div>
                            <div>管理员</div>
                        </div>
                    </div>
                </el-card>
                <el-card shadow="hover" class="mgb20">
                    <template #header>
                        <div class="card-header">
                            <span>数据统计</span>
                        </div>
                    </template>
                    <div class="statistics">
                        <div class="data-item">
                            <div class="data-num">{{ statistics.total?.homestays || 0 }}</div>
                            <div class="data-title">房源总数</div>
                        </div>
                        <div class="data-item">
                            <div class="data-num">{{ statistics.total?.orders || 0 }}</div>
                            <div class="data-title">订单总数</div>
                        </div>
                        <div class="data-item">
                            <div class="data-num">{{ statistics.total?.users || 0 }}</div>
                            <div class="data-title">用户总数</div>
                        </div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="18">
                <el-row :gutter="20" class="mgb20">
                    <el-col :span="8">
                        <el-card shadow="hover" :body-style="{ padding: '0px' }">
                            <div class="grid-content grid-con-1">
                                <el-icon class="grid-con-icon">
                                    <House />
                                </el-icon>
                                <div class="grid-cont-right">
                                    <div class="grid-num">{{ statistics.today?.newHomestays || 0 }}</div>
                                    <div class="grid-title">今日新增房源</div>
                                </div>
                            </div>
                        </el-card>
                    </el-col>
                    <el-col :span="8">
                        <el-card shadow="hover" :body-style="{ padding: '0px' }">
                            <div class="grid-content grid-con-2">
                                <el-icon class="grid-con-icon">
                                    <ShoppingCart />
                                </el-icon>
                                <div class="grid-cont-right">
                                    <div class="grid-num">{{ statistics.today?.newOrders || 0 }}</div>
                                    <div class="grid-title">今日订单数</div>
                                </div>
                            </div>
                        </el-card>
                    </el-col>
                    <el-col :span="8">
                        <el-card shadow="hover" :body-style="{ padding: '0px' }">
                            <div class="grid-content grid-con-3">
                                <el-icon class="grid-con-icon">
                                    <User />
                                </el-icon>
                                <div class="grid-cont-right">
                                    <div class="grid-num">{{ statistics.today?.newUsers || 0 }}</div>
                                    <div class="grid-title">今日新增用户</div>
                                </div>
                            </div>
                        </el-card>
                    </el-col>
                </el-row>
                <el-card shadow="hover" class="mgb20">
                    <template #header>
                        <div class="card-header">
                            <span>订单趋势</span>
                        </div>
                    </template>
                    <div style="height: 280px">
                        <!-- 这里将使用 ECharts 图表 -->
                        <div ref="orderChart" style="width: 100%; height: 100%"></div>
                    </div>
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { User, House, ShoppingCart } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getStatistics, getOrderTrend } from '@/api/dashboard'
import { ElMessage } from 'element-plus'

const username = ref(localStorage.getItem('username') || '管理员')

// 统计数据
const statistics = reactive({
    total: {
        homestays: 0,
        orders: 0,
        users: 0
    },
    period: {
        newHomestays: 0,
        newOrders: 0,
        newUsers: 0
    },
    today: {
        newHomestays: 0,
        newOrders: 0,
        newUsers: 0
    }
})

// 图表相关
const orderChart = ref<HTMLElement | null>(null)
let myChart: echarts.ECharts | null = null

// 初始化图表
const initChart = (dates: string[], data: number[]) => {
    if (!orderChart.value) return

    myChart = echarts.init(orderChart.value)
    const option = {
        tooltip: {
            trigger: 'axis'
        },
        xAxis: {
            type: 'category',
            data: dates
        },
        yAxis: {
            type: 'value'
        },
        series: [
            {
                name: '订单数',
                type: 'line',
                smooth: true,
                data: data
            }
        ]
    }
    myChart.setOption(option)
}

// 获取统计数据
const fetchData = async () => {
    try {
        // 获取基础统计数据
        const statsRes = await getStatistics()
        Object.assign(statistics, statsRes)

        // 获取订单趋势数据
        const trendRes = await getOrderTrend(7) // 获取7天的数据
        initChart(trendRes.dates, trendRes.counts)
    } catch (error) {
        console.error('获取数据失败:', error)
        ElMessage.error('获取数据失败，请稍后重试')
    }
}

onMounted(() => {
    fetchData()

    // 监听窗口大小变化，重绘图表
    window.addEventListener('resize', () => {
        myChart?.resize()
    })
})
</script>

<style scoped lang="scss">
.dashboard-container {
    padding: 20px;

    .user-info {
        display: flex;
        align-items: center;
        padding: 20px;

        .el-icon {
            font-size: 50px;
            color: #409eff;
            margin-right: 20px;
        }

        .user-info-cont {
            .user-info-name {
                font-size: 20px;
                margin-bottom: 10px;
            }
        }
    }

    .statistics {
        display: flex;
        justify-content: space-around;
        padding: 20px 0;

        .data-item {
            text-align: center;

            .data-num {
                font-size: 24px;
                margin-bottom: 10px;
                color: #409eff;
            }

            .data-title {
                color: #999;
            }
        }
    }

    .grid-content {
        display: flex;
        align-items: center;
        height: 100px;
        padding: 20px;

        .grid-con-icon {
            font-size: 50px;
            width: 100px;
            height: 100px;
            text-align: center;
            line-height: 100px;
            color: #fff;
        }

        .grid-cont-right {
            flex: 1;
            text-align: center;
            color: #fff;

            .grid-num {
                font-size: 30px;
                margin-bottom: 10px;
            }
        }
    }

    .grid-con-1 {
        background: linear-gradient(to right, #3090FF, #5187ef);
    }

    .grid-con-2 {
        background: linear-gradient(to right, #40B3FF, #4cb6d8);
    }

    .grid-con-3 {
        background: linear-gradient(to right, #FF6C6C, #f56787);
    }

    .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
    }
}
</style>