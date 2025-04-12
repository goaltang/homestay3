<template>
    <div class="dashboard-container">
        <h1>房东控制台</h1>
        <el-row :gutter="20">
            <el-col :span="6">
                <el-card shadow="hover" class="dashboard-card">
                    <div class="dashboard-card-content">
                        <h3>我的房源</h3>
                        <div class="dashboard-stat">{{ homestayCount || 0 }}</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card shadow="hover" class="dashboard-card">
                    <div class="dashboard-card-content">
                        <h3>待处理订单</h3>
                        <div class="dashboard-stat">{{ pendingOrderCount || 0 }}</div>
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
                        <div class="dashboard-stat">{{ newReviewCount || 0 }}</div>
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
                        <el-table-column prop="id" label="订单号" width="100" />
                        <el-table-column prop="homestayTitle" label="房源名称" />
                        <el-table-column prop="checkInDate" label="入住日期" width="120" />
                        <el-table-column prop="checkOutDate" label="离店日期" width="120" />
                        <el-table-column prop="guestName" label="客人姓名" width="120" />
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
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

// 统计数据
const homestayCount = ref(0)
const pendingOrderCount = ref(0)
const monthlyEarnings = ref(0)
const newReviewCount = ref(0)

// 最近订单
const recentOrders = ref([
    {
        id: 'ORD1001',
        homestayTitle: '测试房源1',
        checkInDate: '2023-07-15',
        checkOutDate: '2023-07-18',
        guestName: '张三',
        totalAmount: 860,
        status: 'CONFIRMED'
    },
    {
        id: 'ORD1002',
        homestayTitle: '测试房源2',
        checkInDate: '2023-07-20',
        checkOutDate: '2023-07-22',
        guestName: '李四',
        totalAmount: 560,
        status: 'PENDING'
    }
])

// 订单状态对应的标签类型
const getOrderStatusType = (status: string): string => {
    const types: Record<string, string> = {
        'PENDING': 'warning',
        'CONFIRMED': 'success',
        'CANCELLED': 'danger',
        'COMPLETED': 'info'
    }
    return types[status] || 'info'
}

// 订单状态对应的文本
const getOrderStatusText = (status: string): string => {
    const texts: Record<string, string> = {
        'PENDING': '待确认',
        'CONFIRMED': '已确认',
        'CANCELLED': '已取消',
        'COMPLETED': '已完成'
    }
    return texts[status] || '未知状态'
}

// 获取数据
const fetchDashboardData = () => {
    // 模拟数据
    homestayCount.value = 3
    pendingOrderCount.value = 2
    monthlyEarnings.value = 2560
    newReviewCount.value = 5
}

onMounted(() => {
    fetchDashboardData()
})
</script>

<style scoped>
.dashboard-container {
    padding: 20px;
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