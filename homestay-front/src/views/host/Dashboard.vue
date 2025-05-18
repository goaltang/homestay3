<template>
    <div class="dashboard-container">
        <h1>房东控制台</h1>
        <div v-if="loading" class="loading-overlay">
            <el-icon class="is-loading" :size="30">
                <Loading />
            </el-icon>
            <span>加载数据中...</span>
        </div>

        <div v-else>
            <el-row :gutter="20">
                <el-col :span="6">
                    <el-card shadow="hover" class="dashboard-card">
                        <div class="dashboard-card-content">
                            <h3>我的房源</h3>
                            <div class="dashboard-stat">{{ statsData?.homestayCount || 0 }}</div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card shadow="hover" class="dashboard-card">
                        <div class="dashboard-card-content">
                            <h3>待处理订单</h3>
                            <div class="dashboard-stat">{{ statsData?.pendingOrders || 0 }}</div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card shadow="hover" class="dashboard-card">
                        <div class="dashboard-card-content">
                            <h3>本月收入</h3>
                            <div class="dashboard-stat">¥{{ monthlyEarnings || 0 }}</div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card shadow="hover" class="dashboard-card">
                        <div class="dashboard-card-content">
                            <h3>新评价</h3>
                            <div class="dashboard-stat">{{ statsData?.reviewCount || 0 }}</div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>

            <div class="dashboard-recent">
                <el-card class="dashboard-recent-card">
                    <template #header>
                        <div class="card-header">
                            <span>最近订单</span>
                            <el-button class="button" text @click="$router.push('/host/orders')">查看全部</el-button>
                        </div>
                    </template>
                    <div v-if="recentOrders && recentOrders.length > 0">
                        <el-table :data="recentOrders" style="width: 100%">
                            <el-table-column prop="orderNumber" label="订单号" width="180" />
                            <el-table-column prop="homestayTitle" label="房源名称" />
                            <el-table-column prop="checkInDate" label="入住日期" width="120" />
                            <el-table-column prop="checkOutDate" label="离店日期" width="120" />
                            <el-table-column prop="totalAmount" label="金额" width="120">
                                <template #default="{ row }">
                                    ¥{{ row.totalAmount }}
                                </template>
                            </el-table-column>
                            <el-table-column prop="status" label="状态" width="100">
                                <template #default="{ row }">
                                    <el-tag :type="getOrderStatusType(row.status)">
                                        {{ getOrderStatusText(row.status) }}
                                    </el-tag>
                                </template>
                            </el-table-column>
                        </el-table>
                    </div>
                    <div v-else class="empty-data">
                        暂无最近订单数据
                    </div>
                </el-card>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import {
    getHostStatistics,
    getHostRecentOrders,
    getHostMonthlyEarnings,
    type HostStatisticsData,
    type HostOrderData
} from '@/api/host'

const loading = ref(false)
const statsData = ref<HostStatisticsData | null>(null)
const monthlyEarnings = ref<number>(0)
const recentOrders = ref<HostOrderData[]>([])

const getOrderStatusType = (status: string): string => {
    const types: Record<string, string> = {
        'PENDING': 'warning',
        'CONFIRMED': 'primary',
        'PAID': 'success',
        'CHECKED_IN': 'success',
        'COMPLETED': 'info',
        'CANCELLED': 'danger',
        'REJECTED': 'danger'
    }
    return types[status] || 'info'
}

const getOrderStatusText = (status: string): string => {
    const texts: Record<string, string> = {
        'PENDING': '待确认',
        'CONFIRMED': '已确认',
        'PAID': '已支付',
        'CHECKED_IN': '已入住',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消',
        'REJECTED': '已拒绝'
    }
    return texts[status] || status
}

const fetchDashboardData = async () => {
    loading.value = true;
    try {
        const [statsResponse, ordersResponse, earningsResponse] = await Promise.all([
            getHostStatistics(),
            getHostRecentOrders(5),
            getHostMonthlyEarnings()
        ]);

        statsData.value = statsResponse;
        recentOrders.value = ordersResponse;
        monthlyEarnings.value = earningsResponse;

        console.log("Dashboard 统计数据:", statsData.value);
        console.log("Dashboard 最近订单:", recentOrders.value);
        console.log("Dashboard 本月收入:", monthlyEarnings.value);

    } catch (error) {
        console.error("获取控制台数据失败:", error);
        ElMessage.error("加载控制台数据失败，请稍后重试");
    } finally {
        loading.value = false;
    }
}

onMounted(() => {
    fetchDashboardData()
})
</script>

<style scoped>
.dashboard-container {
    padding: 20px;
}

.loading-overlay {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 300px;
    flex-direction: column;
    gap: 10px;
    color: #606266;
}

.dashboard-card {
    height: 120px;
    margin-bottom: 20px;
}

.dashboard-card-content {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    height: 100%;
}

.dashboard-card h3 {
    margin: 0;
    color: #606266;
    font-size: 16px;
}

.dashboard-stat {
    font-size: 28px;
    font-weight: bold;
    color: #303133;
    margin-top: 10px;
}

.dashboard-recent {
    margin-top: 20px;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.empty-data {
    text-align: center;
    color: #909399;
    padding: 30px 0;
}
</style>