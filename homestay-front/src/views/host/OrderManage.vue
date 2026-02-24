<template>
    <div class="order-container">
        <h1>订单管理</h1>

        <!-- 统计数据 -->
        <el-row :gutter="20" class="stat-row">
            <el-col :span="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-content">
                        <div class="stat-title">总订单数</div>
                        <div class="stat-value">{{ orderStats.total }}</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-content">
                        <div class="stat-title">待确认订单</div>
                        <div class="stat-value">{{ orderStats.pending }}</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-content">
                        <div class="stat-title">已确认订单</div>
                        <div class="stat-value">{{ orderStats.confirmed }}</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-content">
                        <div class="stat-title">已支付订单</div>
                        <div class="stat-value">{{ orderStats.paid }}</div>
                    </div>
                </el-card>
            </el-col>
        </el-row>

        <!-- 自动状态管理统计 -->
        <el-row :gutter="20" class="stat-row auto-status-row">
            <el-col :span="5">
                <el-card shadow="hover" class="stat-card auto-status-card">
                    <div class="stat-content">
                        <div class="stat-title">
                            <el-icon>
                                <Clock />
                            </el-icon>
                            待自动入住
                        </div>
                        <div class="stat-value auto-checkin">{{ autoStatusStats.pendingAutoCheckIn || 0 }}</div>
                        <div class="stat-desc">今日22:00后自动入住</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="5">
                <el-card shadow="hover" class="stat-card auto-status-card">
                    <div class="stat-content">
                        <div class="stat-title">
                            <el-icon>
                                <CircleCheck />
                            </el-icon>
                            待自动完成
                        </div>
                        <div class="stat-value auto-complete">{{ autoStatusStats.pendingAutoComplete || 0 }}</div>
                        <div class="stat-desc">明日06:00后自动完成</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="5">
                <el-card shadow="hover" class="stat-card auto-status-card">
                    <div class="stat-content">
                        <div class="stat-title">
                            <el-icon>
                                <Warning />
                            </el-icon>
                            错过入住
                        </div>
                        <div class="stat-value missed-checkin">{{ autoStatusStats.missedCheckIn || 0 }}</div>
                        <div class="stat-desc">需要特别关注的订单</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="9">
                <el-card shadow="hover" class="stat-card auto-status-card management-card">
                    <div class="stat-content">
                        <div class="stat-title">
                            <el-icon>
                                <Setting />
                            </el-icon>
                            自动状态管理
                        </div>
                        <div class="management-actions">
                            <div class="action-group">
                                <el-button type="primary" size="small" @click="showAutoStatusConfig" plain>
                                    查看配置
                                </el-button>
                                <el-button type="success" size="small" @click="manualTriggerAutoStatus"
                                    :loading="autoStatusLoading" plain>
                                    手动执行
                                </el-button>
                                <el-button type="warning" size="small" @click="debugUserInfo" plain>
                                    权限调试
                                </el-button>
                            </div>
                            <div class="action-group">
                                <el-button type="info" size="small" @click="analyzeHistoryOrders" plain>
                                    分析历史订单
                                </el-button>
                                <el-button type="danger" size="small" @click="fixHistoryOrders" plain>
                                    修复历史订单
                                </el-button>
                            </div>
                        </div>
                    </div>
                </el-card>
            </el-col>
        </el-row>

        <el-row :gutter="20" class="stat-row">
            <el-col :span="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-content">
                        <div class="stat-title">已入住订单</div>
                        <div class="stat-value">{{ orderStats.checked_in }}</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-content">
                        <div class="stat-title">已完成订单</div>
                        <div class="stat-value">{{ orderStats.completed }}</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-content">
                        <div class="stat-title">已取消订单</div>
                        <div class="stat-value">{{ orderStats.cancelled }}</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-content">
                        <div class="stat-title">已拒绝订单</div>
                        <div class="stat-value">{{ orderStats.rejected }}</div>
                    </div>
                </el-card>
            </el-col>
        </el-row>

        <!-- 筛选器 -->
        <el-card class="filter-card">
            <div class="filter-header">
                <h3>订单筛选</h3>
            </div>
            <el-form :inline="true" :model="filterForm" class="filter-form">
                <el-form-item label="房源">
                    <el-select v-model="filterForm.homestayId" placeholder="全部房源" clearable filterable
                        :popper-append-to-body="false" style="width: 150px;">
                        <el-option v-for="item in homestayOptions" :key="item.id" :label="item.title"
                            :value="item.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="订单状态">
                    <el-select v-model="filterForm.status" placeholder="全部状态" clearable style="width: 120px;">
                        <el-option label="待确认" value="PENDING" />
                        <el-option label="已确认" value="CONFIRMED" />
                        <el-option label="已支付" value="PAID" />
                        <el-option label="已入住" value="CHECKED_IN" />
                        <el-option label="已完成" value="COMPLETED" />
                        <el-option label="已取消" value="CANCELLED" />
                        <el-option label="系统取消" value="CANCELLED_SYSTEM" />
                        <el-option label="用户取消" value="CANCELLED_BY_USER" />
                        <el-option label="已拒绝" value="REJECTED" />
                        <el-option label="退款中" value="REFUND_PENDING" />
                        <el-option label="已退款" value="REFUNDED" />
                        <el-option label="退款失败" value="REFUND_FAILED" />
                    </el-select>
                </el-form-item>
                <el-form-item label="入住日期">
                    <el-date-picker v-model="filterForm.dateRange" type="daterange" range-separator="至"
                        start-placeholder="开始日期" end-placeholder="结束日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD"
                        style="width: 300px;" />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleFilter">
                        <el-icon>
                            <Search />
                        </el-icon> 筛选
                    </el-button>
                    <el-button @click="resetFilter">
                        <el-icon>
                            <Refresh />
                        </el-icon> 重置
                    </el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <!-- 订单列表 -->
        <el-card class="order-list-card" v-loading="loading">
            <div class="list-header">
                <h3>订单列表</h3>
                <el-button type="primary" size="small" @click="fetchOrders">
                    <el-icon>
                        <Refresh />
                    </el-icon> 刷新
                </el-button>
            </div>

            <div v-if="orders.length === 0" class="empty-data">
                <el-empty description="暂无订单数据" />
            </div>

            <el-table v-else :data="orders" style="width: 100%" border stripe>
                <el-table-column prop="id" label="订单号" width="80" />
                <el-table-column label="房源名称" min-width="150">
                    <template #default="scope">
                        <el-tooltip :content="scope.row.homestayTitle || scope.row.homestayName" placement="top">
                            <span class="ellipsis-text">{{ scope.row.homestayTitle || scope.row.homestayName }}</span>
                        </el-tooltip>
                    </template>
                </el-table-column>
                <el-table-column prop="guestName" label="预订客户" width="100" />
                <el-table-column label="订单金额" width="100" align="right">
                    <template #default="scope">
                        <span class="price-value">¥{{ (scope.row.totalPrice || scope.row.totalAmount || 0).toFixed(2)
                            }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="入住日期" width="200">
                    <template #default="scope">
                        <div class="date-range">
                            <span>{{ formatDateString(scope.row.checkInDate) }}</span>
                            <el-divider direction="horizontal" content-position="center">至</el-divider>
                            <span>{{ formatDateString(scope.row.checkOutDate) }}</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="订单状态" width="100" align="center">
                    <template #default="scope">
                        <el-tag :type="getStatusType(scope.row.status)" effect="plain">
                            {{ getStatusText(scope.row) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="创建时间" width="150">
                    <template #default="scope">
                        {{ formatDateTime(scope.row.createTime || scope.row.createdTime) }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="240" fixed="right">
                    <template #default="scope">
                        <div class="action-buttons">
                            <el-button v-if="scope.row.status === 'PENDING'" type="success" size="small"
                                @click="handleConfirm(scope.row)">
                                确认
                            </el-button>
                            <el-button v-if="scope.row.status === 'PENDING'" type="danger" size="small"
                                @click="handleReject(scope.row)">
                                拒绝
                            </el-button>
                            <el-button v-if="scope.row.status === 'PAID'" type="primary" size="small"
                                @click="handleCheckIn(scope.row)">
                                办理入住
                            </el-button>
                            <el-button v-if="scope.row.status === 'CHECKED_IN'" type="info" size="small"
                                @click="handleComplete(scope.row)">
                                完成订单
                            </el-button>
                            <el-button
                                v-if="(scope.row.status === 'PENDING' || scope.row.status === 'CONFIRMED') && scope.row.paymentStatus === 'UNPAID'"
                                type="danger" size="small" @click="handleCancel(scope.row)">
                                取消
                            </el-button>
                            <el-button v-if="scope.row.status === 'PAID' || scope.row.status === 'READY_FOR_CHECKIN'"
                                type="danger" @click="handleCancel(scope.row)">
                                取消订单（将退款）
                            </el-button>
                            <el-button type="warning" size="small" @click="handleDetails(scope.row)">
                                详情
                            </el-button>
                        </div>
                    </template>
                </el-table-column>
            </el-table>

            <!-- 分页 -->
            <div class="pagination">
                <el-pagination v-model:currentPage="currentPage" v-model:page-size="pageSize"
                    :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" :total="total"
                    @size-change="handleSizeChange" @current-change="handleCurrentChange" background />
            </div>
        </el-card>

        <!-- 订单详情对话框 -->
        <el-dialog v-model="detailsDialogVisible" title="订单详情" width="60%">
            <div v-if="currentOrder" class="order-details">
                <el-descriptions :column="2" border>
                    <el-descriptions-item label="订单号" :span="2">{{ currentOrder.id }}</el-descriptions-item>
                    <el-descriptions-item label="房源名称" :span="2">{{ currentOrder.homestayTitle ||
                        currentOrder.homestayName
                    }}</el-descriptions-item>
                    <el-descriptions-item label="客户姓名">{{ currentOrder.guestName }}</el-descriptions-item>
                    <el-descriptions-item label="客户电话">{{ currentOrder.guestPhone }}</el-descriptions-item>
                    <el-descriptions-item label="入住日期">{{ currentOrder.checkInDate }}</el-descriptions-item>
                    <el-descriptions-item label="退房日期">{{ currentOrder.checkOutDate }}</el-descriptions-item>
                    <el-descriptions-item label="入住天数">{{ currentOrder.nights }}晚</el-descriptions-item>
                    <el-descriptions-item label="入住人数">{{ currentOrder.guestCount }}人</el-descriptions-item>
                    <el-descriptions-item label="订单金额" :span="2">
                        <span class="price">¥{{ (currentOrder.totalPrice || currentOrder.totalAmount ||
                            0).toFixed(2)
                            }}</span>
                    </el-descriptions-item>
                    <el-descriptions-item label="订单状态" :span="2">
                        <el-tag :type="getStatusType(currentOrder.status)">
                            {{ getStatusText(currentOrder) }}
                        </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="创建时间" :span="2">{{ currentOrder.createTime ||
                        currentOrder.createdTime
                        }}</el-descriptions-item>
                    <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark ||
                        currentOrder.remarks || '无'
                        }}</el-descriptions-item>
                </el-descriptions>

                <!-- 操作按钮也需要 currentOrder 存在 -->
                <div class="detail-actions" style="margin-top: 20px; text-align: right;">
                    <el-button v-if="currentOrder.status === 'PENDING'" type="success"
                        @click="handleConfirm(currentOrder)">
                        确认订单
                    </el-button>
                    <el-button v-if="currentOrder.status === 'PENDING'" type="danger"
                        @click="handleReject(currentOrder)">
                        拒绝订单
                    </el-button>
                    <el-button v-if="currentOrder.status === 'PAID'" type="primary"
                        @click="handleCheckIn(currentOrder)">
                        办理入住
                    </el-button>
                    <el-button v-if="currentOrder.status === 'CHECKED_IN'" type="info"
                        @click="handleComplete(currentOrder)">
                        完成订单
                    </el-button>
                    <el-button
                        v-if="(currentOrder.status === 'PENDING' || currentOrder.status === 'CONFIRMED') && currentOrder.paymentStatus === 'UNPAID'"
                        type="danger" @click="handleCancel(currentOrder)">
                        取消订单
                    </el-button>
                    <el-button v-if="currentOrder.status === 'PAID' || currentOrder.status === 'READY_FOR_CHECKIN'"
                        type="danger" @click="handleCancel(currentOrder)">
                        取消订单（将退款）
                    </el-button>
                    <el-button @click="detailsDialogVisible = false">关闭</el-button>
                </div>
            </div>
            <div v-else>加载中或无订单数据...</div>
        </el-dialog>

        <!-- 取消订单对话框 -->
        <el-dialog v-model="cancelDialogVisible" title="取消订单" width="40%">
            <el-form :model="cancelForm" ref="cancelFormRef">
                <el-form-item label="取消原因" prop="reason"
                    :rules="[{ required: true, message: '请输入取消原因', trigger: 'blur' }]">
                    <el-input v-model="cancelForm.reason" type="textarea" :rows="3" placeholder="请输入取消原因" />
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="cancelDialogVisible = false">取消</el-button>
                    <el-button type="danger" @click="confirmCancel" :loading="submitting">
                        确认取消订单
                    </el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 拒绝订单对话框 -->
        <el-dialog v-model="rejectDialogVisible" title="拒绝订单" width="40%">
            <el-form :model="rejectForm" ref="rejectFormRef">
                <el-form-item label="拒绝原因" prop="reason"
                    :rules="[{ required: true, message: '请输入拒绝原因', trigger: 'blur' }]">
                    <el-input v-model="rejectForm.reason" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="rejectDialogVisible = false">取消</el-button>
                    <el-button type="danger" @click="confirmReject" :loading="submitting">
                        确认拒绝订单
                    </el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 自动状态配置对话框 -->
        <el-dialog v-model="autoStatusConfigVisible" title="自动状态流转配置" width="60%">
            <div v-if="autoStatusConfig" class="auto-config-content">
                <el-descriptions title="配置信息" :column="2" border>
                    <el-descriptions-item label="自动入住时间">{{ autoStatusConfig.autoCheckInTime }}</el-descriptions-item>
                    <el-descriptions-item label="自动完成时间">{{ autoStatusConfig.autoCheckOutTime }}</el-descriptions-item>
                    <el-descriptions-item label="错过入住处理时间">{{ autoStatusConfig.cancelMissedCheckInTime
                        }}</el-descriptions-item>
                    <el-descriptions-item label="检查频率">{{ autoStatusConfig.checkInterval }}</el-descriptions-item>
                </el-descriptions>

                <el-divider content-position="left">
                    <strong>自动流转规则</strong>
                </el-divider>

                <div v-if="autoStatusConfig.rules" class="rules-content">
                    <el-card v-for="(rule, key) in autoStatusConfig.rules" :key="key" class="rule-card" shadow="never">
                        <template #header>
                            <span class="rule-title">{{ key }}</span>
                        </template>
                        <p class="rule-description">{{ rule }}</p>
                    </el-card>
                </div>

                <el-alert title="说明" type="info" :closable="false" style="margin-top: 20px;">
                    <p>系统会根据上述规则自动处理订单状态流转，减少人工干预，提高效率。</p>
                    <p>如有特殊情况需要人工处理，请及时联系系统管理员。</p>
                </el-alert>
            </div>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="autoStatusConfigVisible = false">关闭</el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Search, Refresh, Clock, CircleCheck, Warning, Setting } from '@element-plus/icons-vue'
import request from '@/utils/request'
import {
    getHostOrders,
    updateOrderStatus,
    confirmOrder,
    rejectOrder,
    cancelOrder,
    getHostOrderStats
} from '@/api/hostOrder'
import { getHostHomestayOptions } from '@/api/host'

// 定义订单项接口
interface HostOrderItem {
    id: number;
    status: string; // 考虑使用导入的 OrderStatus 类型
    paymentStatus: string | null; // 支付状态，可能为 null
    homestayTitle?: string;
    homestayName?: string; // 兼容不同字段名
    guestName?: string;
    guestPhone?: string;
    totalPrice?: number;
    totalAmount?: number; // 兼容不同字段名
    checkInDate?: string;
    checkOutDate?: string;
    nights?: number;
    guestCount?: number;
    createTime?: string;
    createdTime?: string; // 兼容不同字段名
    remark?: string;
    remarks?: string; // 兼容不同字段名
    // 退款相关字段
    refundType?: string;
    refundReason?: string;
    refundAmount?: number;
    refundInitiatedBy?: number;
    refundInitiatedByName?: string;
    refundInitiatedAt?: string;
    refundProcessedBy?: number;
    refundProcessedByName?: string;
    refundProcessedAt?: string;
    refundTransactionId?: string;
}

// 新增：定义房源选项接口
interface HomestayOption {
    id: number;
    title: string;
}

// 统计数据
const orderStats = reactive({
    total: 0,
    pending: 0,
    confirmed: 0,
    checked_in: 0,
    paid: 0,
    completed: 0,
    cancelled: 0,
    rejected: 0
})

// 自动状态统计数据
const autoStatusStats = reactive({
    pendingAutoCheckIn: 0,
    pendingAutoComplete: 0,
    missedCheckIn: 0
})

// 自动状态管理相关
const autoStatusLoading = ref(false)
const autoStatusConfigVisible = ref(false)
const autoStatusConfig = ref<any>({})

// 筛选表单
const filterForm = reactive({
    homestayId: null as number | null,
    status: null as string | null,
    dateRange: null as [string, string] | null
})

// 房源选项 - 修改类型定义
const homestayOptions = ref<HomestayOption[]>([])

// 页码相关
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 订单列表 - 使用定义的接口
const orders = ref<HostOrderItem[]>([])

// 详情对话框相关
const detailsDialogVisible = ref(false)
const currentOrder = ref<HostOrderItem | null>(null)

// 取消对话框相关
const cancelDialogVisible = ref(false)
const cancelFormRef = ref<FormInstance>()
const submitting = ref(false)
const cancelForm = reactive({
    reason: ''
})

// 拒绝订单相关
const rejectDialogVisible = ref(false)
const rejectFormRef = ref<FormInstance>()
const rejectForm = reactive({
    reason: ''
})

// 处理筛选
const handleFilter = () => {
    fetchOrders()
}

// 重置筛选
const resetFilter = () => {
    filterForm.homestayId = null
    filterForm.status = null
    filterForm.dateRange = null
    fetchOrders()
}

// 获取状态文本（包含退款类型信息）
const getStatusText = (order: HostOrderItem | string) => {
    let status: string;
    let refundType: string | undefined;

    if (typeof order === 'string') {
        status = order;
        refundType = undefined;
    } else {
        status = order.status;
        refundType = order.refundType;
    }

    // 处理退款状态，显示退款类型
    if (status === 'REFUNDED') {
        if (refundType) {
            const refundTypeMap: Record<string, string> = {
                'USER_REQUESTED': '已退款（用户申请）',
                'HOST_CANCELLED': '已退款（房东取消）',
                'ADMIN_INITIATED': '已退款（管理员发起）',
                'SYSTEM_AUTOMATIC': '已退款（系统自动）'
            };
            return refundTypeMap[refundType] || '已退款';
        }
        return '已退款';
    }

    if (status === 'REFUND_PENDING') {
        if (refundType) {
            const refundTypeMap: Record<string, string> = {
                'USER_REQUESTED': '退款中（用户申请）',
                'HOST_CANCELLED': '退款中（房东取消）',
                'ADMIN_INITIATED': '退款中（管理员发起）',
                'SYSTEM_AUTOMATIC': '退款中（系统自动）'
            };
            return refundTypeMap[refundType] || '退款中';
        }
        return '退款中';
    }

    const statusMap: Record<string, string> = {
        'PENDING': '待确认',
        'CONFIRMED': '已确认',
        'PAYMENT_PENDING': '待支付',
        'PAID': '已支付',
        'READY_FOR_CHECKIN': '待入住',
        'CHECKED_IN': '已入住',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消',
        'CANCELLED_SYSTEM': '系统取消',
        'CANCELLED_BY_USER': '用户取消',
        'CANCELLED_BY_HOST': '房东取消',
        'REJECTED': '已拒绝',
        'REFUND_FAILED': '退款失败'
    }
    return statusMap[status] || status
}

// 获取状态类型
const getStatusType = (status: string) => {
    const statusMap: Record<string, string> = {
        'PENDING': 'warning',
        'CONFIRMED': 'primary',
        'PAYMENT_PENDING': 'warning',
        'PAID': 'success',
        'READY_FOR_CHECKIN': 'primary',
        'CHECKED_IN': 'primary',
        'COMPLETED': 'info',
        'CANCELLED': 'danger',
        'CANCELLED_SYSTEM': 'danger',
        'CANCELLED_BY_USER': 'danger',
        'CANCELLED_BY_HOST': 'danger',
        'REJECTED': 'danger',
        'REFUND_PENDING': 'warning',
        'REFUNDED': 'info',
        'REFUND_FAILED': 'danger'
    }
    return statusMap[status] || ''
}

// 展示订单详情
const handleDetails = (order: HostOrderItem) => {
    currentOrder.value = order
    detailsDialogVisible.value = true
}

// 获取房源列表 - 修改函数实现
const fetchHomestayOptions = async () => {
    try {
        // 调用正确的 API
        const options = await getHostHomestayOptions()
        // 假设 options 直接就是 {id, title}[] 格式
        // 如果不是，可能需要解包，例如 options = options.data
        if (options && Array.isArray(options)) {
            homestayOptions.value = options.map(opt => ({ id: opt.id, title: opt.title }))
        } else {
            console.warn('getHostHomestayOptions 返回的数据格式不符合预期:', options)
            homestayOptions.value = []
        }

    } catch (error) {
        console.error('获取房源选项失败:', error)
        ElMessage.error('获取房源选项失败')
        homestayOptions.value = [] // 出错时清空
    }
}

// 修复后的确认订单函数
const confirmOrderFixed = async (id: number) => {
    console.log("正在使用修复后的确认订单函数", id);
    return request({
        url: `/api/orders/${id}/confirm`,
        method: "put"
    });
};

// 确认订单
const handleConfirm = async (order: HostOrderItem) => {
    if (!order || !order.id) {
        ElMessage.error('无效的订单数据')
        return
    }

    ElMessageBox.confirm('确认接受此订单吗？', '确认订单', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'success'
    }).then(async () => {
        loading.value = true
        try {
            console.log('确认订单ID:', order.id)
            // 使用修复后的函数
            await confirmOrderFixed(order.id)
            ElMessage.success('订单已确认')
            // 更新本地数据
            const index = orders.value.findIndex(item => item.id === order.id)
            if (index !== -1) {
                orders.value[index].status = 'CONFIRMED'
            }

            // 关闭详情对话框（如果打开）
            if (detailsDialogVisible.value) {
                detailsDialogVisible.value = false
            }

            updateStats()

            // 刷新订单列表
            fetchOrders()
        } catch (error: any) {
            console.error('确认订单失败:', error)
            ElMessage.error(error.response?.data?.message || '操作失败，请重试')
        } finally {
            loading.value = false
        }
    }).catch(() => { })
}

// 办理入住
const handleCheckIn = async (order: HostOrderItem) => {
    if (!order || !order.id) {
        ElMessage.error('无效的订单数据')
        return
    }

    ElMessageBox.confirm('确认为客户办理入住吗？', '办理入住', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'info'
    }).then(async () => {
        loading.value = true
        try {
            console.log('入住订单ID:', order.id)
            await updateOrderStatus(order.id, 'CHECKED_IN')
            ElMessage.success('已办理入住')
            // 更新本地数据
            const index = orders.value.findIndex(item => item.id === order.id)
            if (index !== -1) {
                orders.value[index].status = 'CHECKED_IN'
            }

            // 关闭详情对话框（如果打开）
            if (detailsDialogVisible.value) {
                detailsDialogVisible.value = false
            }

            updateStats()

            // 刷新订单列表
            fetchOrders()
        } catch (error: any) {
            console.error('办理入住失败:', error)
            ElMessage.error(error.response?.data?.message || '操作失败，请重试')
        } finally {
            loading.value = false
        }
    }).catch(() => { })
}

// 完成订单
const handleComplete = async (order: HostOrderItem) => {
    if (!order || !order.id) {
        ElMessage.error('无效的订单数据')
        return
    }

    ElMessageBox.confirm('确认完成此订单吗？', '完成订单', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'success'
    }).then(async () => {
        loading.value = true
        try {
            console.log('完成订单ID:', order.id)
            await updateOrderStatus(order.id, 'COMPLETED')
            ElMessage.success('订单已完成')
            // 更新本地数据
            const index = orders.value.findIndex(item => item.id === order.id)
            if (index !== -1) {
                orders.value[index].status = 'COMPLETED'
            }

            // 关闭详情对话框（如果打开）
            if (detailsDialogVisible.value) {
                detailsDialogVisible.value = false
            }

            updateStats()

            // 刷新订单列表
            fetchOrders()
        } catch (error: any) {
            console.error('完成订单失败:', error)
            ElMessage.error(error.response?.data?.message || '操作失败，请重试')
        } finally {
            loading.value = false
        }
    }).catch(() => { })
}

// 取消订单
const handleCancel = (order: HostOrderItem) => {
    if (!order || !order.id) {
        ElMessage.error('无效的订单数据')
        return
    }

    console.log('取消订单:', order)
    currentOrder.value = order
    cancelForm.reason = ''
    cancelDialogVisible.value = true
}

// 确认取消订单
const confirmCancel = async () => {
    if (!cancelFormRef.value) return
    if (!currentOrder.value) {
        ElMessage.error('当前没有选中的订单')
        return
    }

    await cancelFormRef.value.validate(async (valid: boolean) => {
        if (valid) {
            submitting.value = true

            try {
                console.log('取消订单ID:', currentOrder.value!.id, '原因:', cancelForm.reason)
                await cancelOrder(currentOrder.value!.id, cancelForm.reason)

                // 更新本地数据
                const index = orders.value.findIndex(item => item.id === currentOrder.value!.id)
                if (index !== -1) {
                    orders.value[index].status = 'CANCELLED'
                }

                ElMessage.success('订单已取消')
                cancelDialogVisible.value = false

                if (detailsDialogVisible.value) {
                    detailsDialogVisible.value = false
                }

                updateStats()
                fetchOrders()
            } catch (error: any) {
                console.error('取消订单失败:', error)
                ElMessage.error(error.message || '取消订单失败，请重试')
            } finally {
                submitting.value = false
            }
        }
    })
}

// 修复后的拒绝订单函数
const rejectOrderFixed = async (id: number, reason: string) => {
    console.log("正在使用修复后的拒绝订单函数", id, reason);
    return request({
        url: `/api/orders/${id}/reject`,
        method: "put",
        data: { reason }
    });
};

// 处理拒绝订单
const handleReject = (order: HostOrderItem) => {
    if (!order || !order.id) {
        ElMessage.error('无效的订单数据')
        return
    }

    console.log('拒绝订单:', order)
    currentOrder.value = order
    rejectForm.reason = ''
    rejectDialogVisible.value = true
}

// 确认拒绝订单
const confirmReject = async () => {
    if (!rejectFormRef.value) return
    if (!currentOrder.value) {
        ElMessage.error('当前没有选中的订单')
        return
    }

    await rejectFormRef.value.validate(async (valid: boolean) => {
        if (valid) {
            submitting.value = true

            try {
                console.log('拒绝订单ID:', currentOrder.value!.id, '原因:', rejectForm.reason)
                await rejectOrderFixed(currentOrder.value!.id, rejectForm.reason)

                // 更新本地数据
                const index = orders.value.findIndex(item => item.id === currentOrder.value!.id)

                if (index !== -1) {
                    orders.value[index].status = 'REJECTED'
                }

                ElMessage.success('订单已拒绝')
                rejectDialogVisible.value = false

                if (detailsDialogVisible.value) {
                    detailsDialogVisible.value = false
                }

                updateStats()
                fetchOrders()
            } catch (error: any) {
                console.error('拒绝订单失败:', error)
                ElMessage.error(error.response?.data?.message || error.message || '拒绝订单失败，请重试')
            } finally {
                submitting.value = false
            }
        }
    })
}

// 更新统计数据
const updateStats = async () => {
    try {
        // 使用后端的getHostOrderStats接口获取统计数据
        const response = await getHostOrderStats();
        if (response && response.data) {
            // 从响应中获取统计数据
            orderStats.pending = response.data.pending || 0;
            orderStats.confirmed = response.data.confirmed || 0;
            orderStats.checked_in = response.data.checkedIn || 0;
            orderStats.paid = response.data.paid || 0;
            orderStats.completed = response.data.completed || 0;
            orderStats.cancelled = response.data.cancelled || 0;
            orderStats.rejected = response.data.rejected || 0;
            orderStats.total = response.data.total || 0;
        } else {
            // 如果API不可用，则使用本地数据计算
            orderStats.pending = orders.value.filter(item => item.status === 'PENDING').length;
            orderStats.confirmed = orders.value.filter(item => item.status === 'CONFIRMED').length;
            orderStats.checked_in = orders.value.filter(item => item.status === 'CHECKED_IN').length;
            orderStats.paid = orders.value.filter(item => item.status === 'PAID').length;
            orderStats.completed = orders.value.filter(item => item.status === 'COMPLETED').length;
            orderStats.cancelled = orders.value.filter(item => ['CANCELLED', 'CANCELLED_SYSTEM', 'CANCELLED_BY_USER'].includes(item.status)).length;
            orderStats.rejected = orders.value.filter(item => item.status === 'REJECTED').length;
            orderStats.total = orders.value.length;
        }
    } catch (error) {
        console.error('获取订单统计失败:', error);
        // 降级为使用本地数据计算
        orderStats.pending = orders.value.filter(item => item.status === 'PENDING').length;
        orderStats.confirmed = orders.value.filter(item => item.status === 'CONFIRMED').length;
        orderStats.checked_in = orders.value.filter(item => item.status === 'CHECKED_IN').length;
        orderStats.paid = orders.value.filter(item => item.status === 'PAID').length;
        orderStats.completed = orders.value.filter(item => item.status === 'COMPLETED').length;
        orderStats.cancelled = orders.value.filter(item => ['CANCELLED', 'CANCELLED_SYSTEM', 'CANCELLED_BY_USER'].includes(item.status)).length;
        orderStats.rejected = orders.value.filter(item => item.status === 'REJECTED').length;
        orderStats.total = orders.value.length;
    }
}

// 页码变化
const handleCurrentChange = (page: number) => {
    currentPage.value = page
    fetchOrders()
}

// 每页数量变化
const handleSizeChange = (size: number) => {
    pageSize.value = size
    fetchOrders()
}

// 获取订单列表
const fetchOrders = async () => {
    loading.value = true

    try {
        // 构建查询参数
        const params: any = {
            page: currentPage.value - 1,
            size: pageSize.value,
        }

        if (filterForm.homestayId) {
            params.homestayId = filterForm.homestayId
        }

        if (filterForm.status) {
            params.status = filterForm.status
        }

        if (filterForm.dateRange && Array.isArray(filterForm.dateRange) && filterForm.dateRange.length === 2) {
            params.startDate = filterForm.dateRange[0]
            params.endDate = filterForm.dateRange[1]
        }

        console.log('发送查询参数:', params)

        try {
            // 尝试使用房东专用接口
            const response = await getHostOrders(params)
            console.log('订单API响应:', response)
            // 直接访问响应数据(可能是data属性或者本身就是数据)
            if (response && response.data) {
                // 检查响应数据结构
                console.log('响应数据结构:', JSON.stringify(response.data).substring(0, 500))

                // Spring Data JPA 分页格式
                if (response.data.data && response.data.data.content) {
                    orders.value = response.data.data.content
                    total.value = response.data.data.totalElements || response.data.data.content.length
                }
                // 普通数组格式
                else if (Array.isArray(response.data)) {
                    orders.value = response.data
                    total.value = response.data.length
                }
                // 包含分页信息的格式
                else if (response.data.content) {
                    orders.value = response.data.content
                    total.value = response.data.totalElements || response.data.content.length
                }
                // 嵌套的data对象
                else if (response.data.data) {
                    if (Array.isArray(response.data.data)) {
                        orders.value = response.data.data
                        total.value = response.data.data.length
                    } else {
                        orders.value = [response.data.data]
                        total.value = 1
                    }
                }
                // 直接是数据对象本身
                else {
                    orders.value = [response.data]
                    total.value = 1
                }
            } else if (Array.isArray(response)) {
                // 直接返回数组
                orders.value = response
                total.value = response.length
            } else {
                console.warn('未能从响应中提取订单数据:', response)
                orders.value = []
                total.value = 0
            }

            // 更新统计数据
            updateStats()
        } catch (apiError) {
            console.error('房东订单接口不可用，尝试使用备用方法:', apiError)
            ElMessage.warning('订单接口请求异常，请稍后再试')
            orders.value = []
            total.value = 0
        }
    } catch (error) {
        console.error('获取订单列表失败:', error)
        ElMessage.error('获取订单列表失败')
        orders.value = []
        total.value = 0
    } finally {
        loading.value = false
    }
}

// 格式化日期字符串为 (MM-DD) 格式
const formatDateString = (dateString: string) => {
    if (!dateString) return ''
    const date = new Date(dateString)
    const month = date.getMonth() + 1
    const day = date.getDate()
    return `${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`
}

// 格式化日期时间字符串
const formatDateTime = (dateTimeString: string) => {
    if (!dateTimeString) return ''
    const date = new Date(dateTimeString)
    const year = date.getFullYear()
    const month = date.getMonth() + 1
    const day = date.getDate()
    const hours = date.getHours()
    const minutes = date.getMinutes()
    return `${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')} ${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`
}

// 获取自动状态统计
const fetchAutoStatusStats = async () => {
    try {
        const response = await request({
            url: '/api/host/order-auto-status/stats',
            method: 'get'
        })
        if (response && response.data) {
            autoStatusStats.pendingAutoCheckIn = response.data.pendingAutoCheckIn || 0
            autoStatusStats.pendingAutoComplete = response.data.pendingAutoComplete || 0
            autoStatusStats.missedCheckIn = response.data.missedCheckIn || 0
        }
    } catch (error: any) {
        console.error('获取自动状态统计失败:', error)
        // 如果是403错误，说明权限不足，隐藏自动管理模块
        if (error.response?.status === 403) {
            console.warn('当前用户没有访问自动状态管理的权限')
            // 可以设置一个标志来隐藏自动管理模块
        }
        // 不显示错误消息，避免干扰用户
    }
}

// 显示自动状态配置
const showAutoStatusConfig = async () => {
    try {
        const response = await request({
            url: '/api/host/order-auto-status/config',
            method: 'get'
        })
        if (response && response.data) {
            autoStatusConfig.value = response.data
            autoStatusConfigVisible.value = true
        }
    } catch (error) {
        console.error('获取自动状态配置失败:', error)
        ElMessage.error('获取配置信息失败')
    }
}

// 调试：检查当前用户权限信息
const debugUserInfo = async () => {
    try {
        const response = await request({
            url: '/api/host/order-auto-status/debug/user-info',
            method: 'get'
        })
        console.log('🔍 当前用户权限信息:', response.data)
        ElMessage.info('权限信息已输出到控制台')
    } catch (error: any) {
        console.error('获取用户权限信息失败:', error)
        console.log('错误状态码:', error.response?.status)
    }
}

// 手动触发自动状态流转
const manualTriggerAutoStatus = async () => {
    ElMessageBox.confirm('确认手动执行一次自动状态流转吗？', '手动执行', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'info'
    }).then(async () => {
        autoStatusLoading.value = true
        try {
            await request({
                url: '/api/host/order-auto-status/trigger',
                method: 'post'
            })
            ElMessage.success('自动状态流转执行完成')
            // 刷新统计数据
            fetchAutoStatusStats()
            fetchOrders()
            updateStats()
        } catch (error: any) {
            console.error('手动触发自动状态流转失败:', error)
            ElMessage.error(error.response?.data?.error || '执行失败，请重试')
        } finally {
            autoStatusLoading.value = false
        }
    }).catch(() => { })
}

// 分析历史订单
const analyzeHistoryOrders = async () => {
    autoStatusLoading.value = true
    try {
        const response = await request({
            url: '/api/host/order-auto-status/analyze-history',
            method: 'get'
        })

        if (response && response.data) {
            const data = response.data
            ElMessageBox.alert(
                `分析结果：
                
需要修复入住状态的订单：${data.shouldBeCheckedInCount} 个
需要修复完成状态的订单：${data.shouldBeCompletedCount} 个
总计问题订单：${data.totalIssues} 个

建议：${data.recommendation}`,
                '历史订单分析结果',
                {
                    confirmButtonText: '了解',
                    type: data.totalIssues > 0 ? 'warning' : 'success'
                }
            )
        }
    } catch (error: any) {
        console.error('分析历史订单失败:', error)
        ElMessage.error(error.response?.data?.error || '分析失败，请重试')
    } finally {
        autoStatusLoading.value = false
    }
}

// 修复历史订单
const fixHistoryOrders = async () => {
    ElMessageBox.confirm(
        '此操作将自动修复过去100天内需要状态转换的历史订单。这可能会影响已支付但未入住的订单状态，确认执行吗？',
        '修复历史订单',
        {
            confirmButtonText: '确认修复',
            cancelButtonText: '取消',
            type: 'warning'
        }
    ).then(async () => {
        autoStatusLoading.value = true
        try {
            const response = await request({
                url: '/api/host/order-auto-status/fix-history',
                method: 'post'
            })

            if (response && response.data) {
                const data = response.data
                if (data.success) {
                    ElMessage.success(data.message)
                    // 刷新数据
                    fetchAutoStatusStats()
                    fetchOrders()
                    updateStats()
                } else {
                    ElMessage.error(data.error || data.warning || '修复过程中出现问题')
                }

                // 显示详细结果
                if (data.totalFixed > 0 || data.failedCount > 0) {
                    ElMessageBox.alert(
                        `修复完成：
                        
修复入住状态：${data.fixedCheckedIn} 个
修复完成状态：${data.fixedCompleted} 个
总计修复：${data.totalFixed} 个
失败：${data.failedCount} 个`,
                        '修复结果',
                        {
                            confirmButtonText: '了解',
                            type: data.success ? 'success' : 'warning'
                        }
                    )
                }
            }
        } catch (error: any) {
            console.error('修复历史订单失败:', error)
            ElMessage.error(error.response?.data?.error || '修复失败，请重试')
        } finally {
            autoStatusLoading.value = false
        }
    }).catch(() => { })
}

onMounted(() => {
    fetchHomestayOptions()
    fetchOrders()
    updateStats()
    fetchAutoStatusStats()
})
</script>

<style scoped>
.order-container {
    padding: 20px;
}

.stat-row {
    margin-bottom: 20px;
}

.stat-card {
    height: 100px;
}

.stat-content {
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
}

.stat-title {
    font-size: 14px;
    color: #606266;
    margin-bottom: 10px;
}

.stat-value {
    font-size: 24px;
    font-weight: bold;
    color: #409EFF;
}

.filter-card {
    margin-bottom: 20px;
}

.order-list-card {
    min-height: 500px;
}

.pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
}

.empty-data {
    padding: 40px;
    text-align: center;
    color: #909399;
}

.order-details .price {
    color: #ff6600;
    font-weight: bold;
    font-size: 16px;
}

.detail-actions {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
    gap: 10px;
}

.filter-header {
    margin-bottom: 10px;
    font-size: 16px;
    font-weight: bold;
}

.filter-form {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
}

.ellipsis-text {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.date-range {
    display: flex;
    align-items: center;
}

.price-value {
    font-weight: bold;
}

/* 自动状态管理样式 */
.auto-status-row {
    border-top: 2px solid #f0f2f5;
    padding-top: 20px;
    margin-top: 20px;
}

.auto-status-card {
    border: 1px solid #e8f4fd;
    background: linear-gradient(135deg, #f8fbff 0%, #f0f8ff 100%);
    height: 140px;
}

.auto-status-card .el-card__body {
    height: 100%;
    padding: 8px 12px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
}

.auto-status-card .stat-title {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 5px;
    color: #1890ff;
    font-weight: 600;
    font-size: 13px;
    text-align: center;
    margin-bottom: 0;
}

.auto-status-card .stat-value {
    margin: 5px 0;
    font-size: 24px;
    font-weight: bold;
    text-align: center;
}

.auto-status-card .stat-value.auto-checkin {
    color: #52c41a;
}

.auto-status-card .stat-value.auto-complete {
    color: #1890ff;
}

.auto-status-card .stat-value.missed-checkin {
    color: #ff4d4f;
}

.stat-desc {
    font-size: 11px;
    color: #8c8c8c;
    margin: 0;
    line-height: 1.2;
    text-align: center;
}

.management-card {
    height: 140px !important;
}

.management-actions {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-top: 10px;
    width: 100%;
}

.action-group {
    display: flex;
    gap: 6px;
    width: 100%;
}

.management-actions .el-button {
    flex: 1;
    margin: 0;
    padding: 6px 8px;
    font-size: 11px;
    height: 28px;
    border-radius: 4px;
    white-space: nowrap;
}

/* 配置对话框样式 */
.auto-config-content {
    padding: 10px 0;
}

.rules-content {
    margin-top: 16px;
}

.rule-card {
    margin-bottom: 12px;
    border: 1px solid #e8f4fd;
}

.rule-title {
    font-weight: 600;
    color: #1890ff;
}

.rule-description {
    margin: 0;
    color: #666;
    line-height: 1.6;
}
</style>