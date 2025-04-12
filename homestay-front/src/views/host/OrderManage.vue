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
                        <el-option v-for="item in homestayOptions" :key="item.value" :label="item.label"
                            :value="item.value" />
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
                        <el-option label="已拒绝" value="REJECTED" />
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
                            {{ getStatusText(scope.row.status) }}
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
                            <el-button v-if="scope.row.status === 'CONFIRMED'" type="primary" size="small"
                                @click="handleCheckIn(scope.row)">
                                办理入住
                            </el-button>
                            <el-button v-if="scope.row.status === 'CHECKED_IN'" type="info" size="small"
                                @click="handleComplete(scope.row)">
                                完成订单
                            </el-button>
                            <el-button v-if="['PENDING', 'CONFIRMED'].includes(scope.row.status)" type="danger"
                                size="small" @click="handleCancel(scope.row)">
                                取消
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
            <div v-if="currentOrder.id" class="order-details">
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
                        <span class="price">¥{{ (currentOrder.totalPrice || currentOrder.totalAmount || 0).toFixed(2)
                            }}</span>
                    </el-descriptions-item>
                    <el-descriptions-item label="订单状态" :span="2">
                        <el-tag :type="getStatusType(currentOrder.status)">
                            {{ getStatusText(currentOrder.status) }}
                        </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="创建时间" :span="2">{{ currentOrder.createTime || currentOrder.createdTime
                        }}</el-descriptions-item>
                    <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || currentOrder.remarks || '无'
                        }}</el-descriptions-item>
                </el-descriptions>

                <!-- 添加操作按钮 -->
                <div class="detail-actions">
                    <el-button v-if="currentOrder.status === 'PENDING'" type="success"
                        @click="handleConfirm(currentOrder)">
                        确认订单
                    </el-button>
                    <el-button v-if="currentOrder.status === 'CONFIRMED'" type="primary"
                        @click="handleCheckIn(currentOrder)">
                        办理入住
                    </el-button>
                    <el-button v-if="currentOrder.status === 'CHECKED_IN'" type="info"
                        @click="handleComplete(currentOrder)">
                        完成订单
                    </el-button>
                    <el-button v-if="['PENDING', 'CONFIRMED'].includes(currentOrder.status)" type="danger"
                        @click="handleCancel(currentOrder)">
                        取消订单
                    </el-button>
                    <el-button @click="detailsDialogVisible = false">关闭</el-button>
                </div>
            </div>
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
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import {
    getHostOrders,
    updateOrderStatus,
    confirmOrder,
    rejectOrder,
    getHostOrderStats
} from '@/api/hostOrder'
import { getOwnerHomestays } from '@/api/homestay'

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

// 筛选表单
const filterForm = reactive({
    homestayId: null as number | null,
    status: null as string | null,
    dateRange: null as [string, string] | null
})

// 房源选项
const homestayOptions = ref<{ value: number, label: string }[]>([])

// 页码相关
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 订单列表 - 移除测试数据，仅从后端获取
const orders = ref<any[]>([])

// 详情对话框相关
const detailsDialogVisible = ref(false)
const currentOrder = ref<any>({})

// 取消对话框相关
const cancelDialogVisible = ref(false)
const cancelFormRef = ref<FormInstance>()
const submitting = ref(false)
const cancelForm = reactive({
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

// 获取状态文本
const getStatusText = (status: string) => {
    const statusMap: Record<string, string> = {
        'PENDING': '待确认',
        'CONFIRMED': '已确认',
        'CHECKED_IN': '已入住',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消',
        'REJECTED': '已拒绝',
        'PAID': '已支付'
    }
    return statusMap[status] || status
}

// 获取状态类型
const getStatusType = (status: string) => {
    const statusMap: Record<string, string> = {
        'PENDING': 'warning',
        'CONFIRMED': 'success',
        'CHECKED_IN': 'primary',
        'COMPLETED': 'info',
        'CANCELLED': 'danger',
        'REJECTED': 'danger',
        'PAID': 'success'
    }
    return statusMap[status] || ''
}

// 展示订单详情
const handleDetails = (order: any) => {
    currentOrder.value = order
    detailsDialogVisible.value = true
}

// 获取房东的房源列表
const fetchHomestays = async () => {
    try {
        const response = await getOwnerHomestays()
        if (response.data && Array.isArray(response.data)) {
            homestayOptions.value = response.data.map((item: any) => ({
                value: item.id,
                label: item.title
            }))
        }
    } catch (error) {
        console.error('获取房源列表失败:', error)
        ElMessage.error('获取房源列表失败')
    }
}

// 确认订单
const handleConfirm = async (order: any) => {
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
            await confirmOrder(order.id)
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
const handleCheckIn = async (order: any) => {
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
const handleComplete = async (order: any) => {
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
const handleCancel = (order: any) => {
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
    if (!currentOrder.value || !currentOrder.value.id) {
        ElMessage.error('无效的订单数据')
        return
    }

    await cancelFormRef.value.validate(async (valid) => {
        if (valid) {
            submitting.value = true

            try {
                console.log('拒绝订单ID:', currentOrder.value.id, '原因:', cancelForm.reason)
                await rejectOrder(currentOrder.value.id, cancelForm.reason)

                // 更新本地数据
                const index = orders.value.findIndex(item => item.id === currentOrder.value.id)
                if (index !== -1) {
                    orders.value[index].status = 'CANCELLED'
                }

                ElMessage.success('订单已取消')
                cancelDialogVisible.value = false

                // 关闭详情对话框（如果打开）
                if (detailsDialogVisible.value) {
                    detailsDialogVisible.value = false
                }

                updateStats()

                // 刷新订单列表
                fetchOrders()
            } catch (error: any) {
                console.error('取消订单失败:', error)
                ElMessage.error(error.response?.data?.message || '操作失败，请重试')
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
            orderStats.cancelled = orders.value.filter(item => item.status === 'CANCELLED').length;
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
        orderStats.cancelled = orders.value.filter(item => item.status === 'CANCELLED').length;
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

onMounted(() => {
    fetchHomestays()
    fetchOrders()
    updateStats()
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
</style>