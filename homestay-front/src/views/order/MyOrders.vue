<template>
    <div class="my-orders-container">
        <div class="page-header">
            <h1>我的订单</h1>
        </div>

        <div class="filters-bar">
            <div class="status-tabs">
                <el-tabs v-model="activeTab" @tab-change="handleTabChange">
                    <el-tab-pane label="全部订单" name="all"></el-tab-pane>
                    <el-tab-pane label="待确认" name="PENDING"></el-tab-pane>
                    <el-tab-pane label="已确认" name="CONFIRMED"></el-tab-pane>
                    <el-tab-pane label="已支付" name="PAID"></el-tab-pane>
                    <el-tab-pane label="已完成" name="COMPLETED"></el-tab-pane>
                    <el-tab-pane label="已取消" name="CANCELLED"></el-tab-pane>
                </el-tabs>
            </div>
        </div>

        <div class="order-list-section">
            <div v-if="loading" class="loading-container">
                <el-skeleton :rows="5" animated />
            </div>

            <div v-else-if="!orders.length" class="empty-container">
                <el-empty description="没有找到订单">
                    <template #description>
                        <p>{{ getEmptyDescription() }}</p>
                    </template>
                    <el-button type="primary" @click="goToHomepage">查找房源</el-button>
                </el-empty>
            </div>

            <template v-else>
                <div v-for="order in orders" :key="order.id" class="order-card">
                    <div class="order-card-header">
                        <div class="order-info">
                            <span class="order-number">订单号: {{ order.orderNumber }}</span>
                            <span class="order-date">下单时间: {{ formatDate(order.createTime) }}</span>
                        </div>
                        <div class="order-status">
                            <el-tag :type="getStatusType(order.status)">
                                {{ getStatusText(order.status) }}
                            </el-tag>
                        </div>
                    </div>

                    <div class="order-card-content">
                        <div class="homestay-image">
                            <img :src="order.imageUrl || 'https://picsum.photos/400/300'" :alt="order.homestayTitle">
                        </div>
                        <div class="order-details">
                            <h3>{{ order.homestayTitle }}</h3>
                            <p class="date-info">{{ formatDateRange(order.checkInDate, order.checkOutDate) }} · {{
                                order.nights }}晚</p>
                            <p class="guest-info">{{ order.guestCount }}位房客</p>
                            <div class="price-container">
                                <span class="price-label">总价</span>
                                <span class="price-value">¥{{ order.totalAmount }}</span>
                            </div>
                        </div>
                    </div>

                    <div class="order-card-footer">
                        <div class="action-buttons">
                            <!-- 待确认状态 -->
                            <template v-if="order.status === 'PENDING'">
                                <el-button type="default" @click="viewOrderDetail(order.id)">查看详情</el-button>
                                <el-button type="danger" plain @click="cancelOrder(order.id)">取消订单</el-button>
                            </template>

                            <!-- 已确认状态 -->
                            <template v-else-if="order.status === 'CONFIRMED'">
                                <el-button type="default" @click="viewOrderDetail(order.id)">查看详情</el-button>
                                <el-button type="danger" plain @click="cancelOrder(order.id)">取消订单</el-button>
                                <el-button type="primary" @click="goToPayment(order.id)">立即支付</el-button>
                            </template>

                            <!-- 已支付状态 -->
                            <template v-else-if="order.status === 'PAID'">
                                <el-button type="default" @click="viewOrderDetail(order.id)">查看详情</el-button>
                                <el-button type="primary" plain>联系房东</el-button>
                            </template>

                            <!-- 其他状态 -->
                            <template v-else>
                                <el-button type="default" @click="viewOrderDetail(order.id)">查看详情</el-button>
                            </template>
                        </div>
                    </div>
                </div>

                <div class="pagination-container">
                    <el-pagination background layout="prev, pager, next" :total="total" :page-size="pageSize"
                        :current-page="currentPage" @current-change="handlePageChange" />
                </div>
            </template>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyOrders, cancelOrder as apiCancelOrder } from '@/api/order'

interface OrderItem {
    id: number
    orderNumber: string
    homestayId: number
    homestayTitle: string
    imageUrl?: string
    guestCount: number
    checkInDate: string
    checkOutDate: string
    nights: number
    totalAmount: number
    status: string
    createTime: string
}

const router = useRouter()

const loading = ref(true)
const orders = ref<OrderItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const activeTab = ref('all')

// 获取订单列表
const fetchOrders = async () => {
    loading.value = true
    try {
        const params: any = {
            page: currentPage.value - 1,
            size: pageSize.value
        }

        // 如果不是全部订单，则添加状态过滤
        if (activeTab.value !== 'all') {
            params.status = activeTab.value
        }

        const response = await getMyOrders(params)
        console.log('获取到订单数据:', response)

        // 处理不同格式的响应数据
        let orderData = response.data

        // 处理嵌套在data.data字段中的情况
        if (orderData.data && orderData.data.content) {
            orders.value = orderData.data.content
            total.value = orderData.data.totalElements
        }
        // 处理嵌套在data字段中的情况
        else if (orderData.data && Array.isArray(orderData.data.content)) {
            orders.value = orderData.data.content
            total.value = orderData.data.totalElements || orderData.data.content.length
        }
        // 处理直接包含content字段的情况
        else if (orderData.content) {
            orders.value = orderData.content
            total.value = orderData.totalElements || orderData.content.length
        }
        // 处理直接是数组的情况
        else if (Array.isArray(orderData)) {
            orders.value = orderData
            total.value = orderData.length
        }
        // 处理从后端标准Spring Data格式的情况
        else if (Array.isArray(orderData.content)) {
            orders.value = orderData.content
            total.value = orderData.totalElements || orderData.totalPages * orderData.size
        }
        // 默认情况
        else {
            console.warn('未识别的订单数据格式:', orderData)
            orders.value = []
            total.value = 0
        }

        // 日志订单数据供调试
        console.log('处理后的订单数据:', orders.value)
        console.log('总数量:', total.value)
    } catch (error) {
        console.error('获取订单列表失败:', error)
        ElMessage.error('获取订单列表失败，请刷新页面重试')
        orders.value = []
        total.value = 0
    } finally {
        loading.value = false
    }
}

// 处理标签页切换
const handleTabChange = () => {
    currentPage.value = 1
    fetchOrders()
}

// 处理分页变化
const handlePageChange = (page: number) => {
    currentPage.value = page
    fetchOrders()
}

// 取消订单
const cancelOrder = async (orderId: number) => {
    try {
        await ElMessageBox.confirm(
            '确定要取消该订单吗？',
            '取消订单',
            {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }
        )

        // 显示取消中的加载状态
        const loadingInstance = ElMessage({
            type: 'info',
            message: '订单取消中...',
            duration: 0
        })

        try {
            const response = await apiCancelOrder(orderId)
            console.log('取消订单响应:', response)

            // 关闭加载消息
            ElMessage.closeAll()

            // 显示成功消息
            ElMessage.success('订单已取消')

            // 刷新列表
            fetchOrders()
        } catch (apiError: any) {
            // 关闭加载消息
            ElMessage.closeAll()

            // 显示具体错误信息
            if (apiError.response && apiError.response.data && apiError.response.data.error) {
                ElMessage.error(`取消订单失败: ${apiError.response.data.error}`)
            } else {
                ElMessage.error('取消订单失败，请重试')
            }
            console.error('取消订单API错误:', apiError)
        }
    } catch (error: any) {
        // 用户取消了确认对话框，不做处理
        if (error !== 'cancel' && error !== 'close') {
            console.error('取消订单确认框错误:', error)
        }
    }
}

// 查看订单详情
const viewOrderDetail = (orderId: number) => {
    router.push(`/orders/${orderId}`)
}

// 前往支付页面
const goToPayment = (orderId: number) => {
    router.push(`/orders/${orderId}/pay`)
}

// 前往首页
const goToHomepage = () => {
    router.push('/')
}

// 获取不同状态下的空订单描述
const getEmptyDescription = () => {
    switch (activeTab.value) {
        case 'PENDING':
            return '暂无待确认的订单'
        case 'CONFIRMED':
            return '暂无已确认的订单'
        case 'PAID':
            return '暂无已支付的订单'
        case 'COMPLETED':
            return '暂无已完成的订单'
        case 'CANCELLED':
            return '暂无已取消的订单'
        default:
            return '暂无订单记录'
    }
}

// 获取订单状态对应的样式类型
const getStatusType = (status: string) => {
    switch (status) {
        case 'PENDING':
            return 'info'
        case 'CONFIRMED':
            return 'success'
        case 'PAID':
            return 'success'
        case 'COMPLETED':
            return 'success'
        case 'CANCELLED':
            return 'danger'
        default:
            return 'info'
    }
}

// 获取订单状态文本
const getStatusText = (status: string) => {
    switch (status) {
        case 'PENDING':
            return '待确认'
        case 'CONFIRMED':
            return '已确认'
        case 'PAID':
            return '已支付'
        case 'COMPLETED':
            return '已完成'
        case 'CANCELLED':
            return '已取消'
        default:
            return '未知状态'
    }
}

// 格式化日期
const formatDate = (dateString: string) => {
    if (!dateString) return ''
    const date = new Date(dateString)
    return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

// 格式化日期范围
const formatDateRange = (checkInDate: string, checkOutDate: string) => {
    if (!checkInDate || !checkOutDate) return ''

    const formatDate = (dateString: string) => {
        const date = new Date(dateString)
        return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
    }

    return `${formatDate(checkInDate)} - ${formatDate(checkOutDate)}`
}

// 初始化
onMounted(() => {
    fetchOrders()
})
</script>

<style scoped>
.my-orders-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 40px 20px;
}

.page-header {
    margin-bottom: 32px;
}

.page-header h1 {
    font-size: 28px;
    margin: 0;
    color: #303133;
}

.filters-bar {
    margin-bottom: 24px;
    border-bottom: 1px solid #e4e7ed;
}

.loading-container,
.empty-container {
    padding: 40px;
    background-color: white;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
    margin-bottom: 24px;
}

.order-card {
    background-color: white;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
    margin-bottom: 24px;
    overflow: hidden;
}

.order-card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    border-bottom: 1px solid #f0f0f0;
    background-color: #f8f9fa;
}

.order-info {
    display: flex;
    gap: 24px;
}

.order-number {
    font-weight: 500;
}

.order-date {
    color: #606266;
}

.order-card-content {
    padding: 20px;
    display: flex;
    gap: 20px;
}

.homestay-image {
    width: 160px;
    height: 120px;
    border-radius: 8px;
    overflow: hidden;
    flex-shrink: 0;
}

.homestay-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.order-details {
    flex: 1;
}

.order-details h3 {
    margin: 0 0 8px 0;
    font-size: 18px;
    color: #303133;
}

.date-info,
.guest-info {
    margin: 4px 0;
    color: #606266;
}

.price-container {
    display: flex;
    align-items: center;
    margin-top: 12px;
}

.price-label {
    margin-right: 8px;
    color: #606266;
}

.price-value {
    font-size: 18px;
    font-weight: bold;
    color: #f56c6c;
}

.order-card-footer {
    padding: 16px 20px;
    border-top: 1px solid #f0f0f0;
    display: flex;
    justify-content: flex-end;
}

.action-buttons {
    display: flex;
    gap: 12px;
}

.pagination-container {
    margin-top: 32px;
    display: flex;
    justify-content: center;
}

@media (max-width: 768px) {
    .order-card-content {
        flex-direction: column;
    }

    .homestay-image {
        width: 100%;
        height: 200px;
    }

    .order-info {
        flex-direction: column;
        gap: 4px;
    }

    .action-buttons {
        flex-wrap: wrap;
    }
}
</style>