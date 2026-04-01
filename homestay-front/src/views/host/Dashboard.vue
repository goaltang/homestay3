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
                            <h3>房源分组</h3>
                            <div class="dashboard-stat">{{ groupCount }}</div>
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
                        <el-table :data="recentOrders" style="width: 100%" stripe>
                            <el-table-column prop="orderNumber" label="订单号" width="140" />
                            <el-table-column label="房源名称" min-width="150">
                                <template #default="{ row }">
                                    <el-tooltip :content="row.homestayTitle || row.homestayName" placement="top">
                                        <span class="ellipsis-text">{{ row.homestayTitle || row.homestayName }}</span>
                                    </el-tooltip>
                                </template>
                            </el-table-column>
                            <el-table-column label="入住日期" width="200">
                                <template #default="{ row }">
                                    <div class="date-range">
                                        <span>{{ formatDate(row.checkInDate) }}</span>
                                        <span class="date-separator">至</span>
                                        <span>{{ formatDate(row.checkOutDate) }}</span>
                                    </div>
                                </template>
                            </el-table-column>
                            <el-table-column prop="totalAmount" label="金额" width="100" align="right">
                                <template #default="{ row }">
                                    <span class="amount">¥{{ formatAmount(row.totalAmount) }}</span>
                                </template>
                            </el-table-column>
                            <el-table-column prop="status" label="状态" width="100" align="center">
                                <template #default="{ row }">
                                    <el-tag :type="getOrderStatusType(row.status)" effect="plain">
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
import { getHomestayGroups } from '@/api/homestay'

const loading = ref(false)
const statsData = ref<HostStatisticsData | null>(null)
const monthlyEarnings = ref<number>(0)
const recentOrders = ref<HostOrderData[]>([])
const groupCount = ref(0)

const getOrderStatusType = (status: string): string => {
    const types: Record<string, string> = {
        'PENDING': 'warning',
        'CONFIRMED': 'primary',
        'PAID': 'success',
        'READY_FOR_CHECKIN': 'success',
        'CHECKED_IN': 'success',
        'COMPLETED': 'info',
        'CANCELLED': 'danger',
        'CANCELLED_BY_USER': 'danger',
        'CANCELLED_SYSTEM': 'danger',
        'REJECTED': 'danger',
        'PAYMENT_PENDING': 'warning'
    }
    return types[status] || 'info'
}

const getOrderStatusText = (status: string): string => {
    const texts: Record<string, string> = {
        'PENDING': '待确认',
        'CONFIRMED': '已确认',
        'PAID': '已支付',
        'READY_FOR_CHECKIN': '待入住',
        'CHECKED_IN': '已入住',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消',
        'CANCELLED_BY_USER': '用户取消',
        'CANCELLED_SYSTEM': '系统取消',
        'REJECTED': '已拒绝',
        'PAYMENT_PENDING': '待支付'
    }
    return texts[status] || status
}

const formatDate = (dateString: string): string => {
    if (!dateString) return '-'
    const date = new Date(dateString)
    if (isNaN(date.getTime())) return dateString
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${month}-${day}`
}

const formatAmount = (amount: number | string): string => {
    if (amount === null || amount === undefined) return '0.00'
    const num = typeof amount === 'string' ? parseFloat(amount) : amount
    return isNaN(num) ? '0.00' : num.toFixed(2)
}

const fetchDashboardData = async () => {
    loading.value = true;
    try {
        const [statsResponse, ordersResponse, earningsResponse, groupsResponse] = await Promise.all([
            getHostStatistics(),
            getHostRecentOrders(5),
            getHostMonthlyEarnings(),
            getHomestayGroups()
        ]);

        statsData.value = statsResponse;
        recentOrders.value = ordersResponse;
        monthlyEarnings.value = earningsResponse;
        groupCount.value = (groupsResponse || []).length;

        console.log("Dashboard 统计数据:", statsData.value);
        console.log("Dashboard 最近订单:", recentOrders.value);
        console.log("Dashboard 本月收入:", monthlyEarnings.value);
        console.log("Dashboard 房源分组数:", groupCount.value);

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

.ellipsis-text {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 150px;
    display: inline-block;
}

.date-range {
    display: flex;
    align-items: center;
    gap: 6px;
}

.date-separator {
    color: #909399;
    font-size: 12px;
}

.amount {
    font-weight: 600;
    color: #E6A23C;
}
</style>