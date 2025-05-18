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
                            <el-tag :type="getStatusType(order)">
                                {{ getStatusText(order) }}
                            </el-tag>
                            <div v-if="order.status === 'PENDING'" class="order-countdown warning">
                                <el-tooltip content="超过24小时未确认的订单将被自动取消">
                                    <span><i class="el-icon-time"></i> 剩余: {{ getCountdownTime(order.createTime, 24)
                                        }}</span>
                                </el-tooltip>
                            </div>
                            <div v-if="order.status === 'CONFIRMED'" class="order-countdown danger">
                                <el-tooltip content="超过2小时未支付的订单将被自动取消">
                                    <span><i class="el-icon-time"></i> 剩余: {{ getCountdownTime(order.updateTime, 2)
                                        }}</span>
                                </el-tooltip>
                            </div>
                        </div>
                    </div>

                    <div class="order-card-content">
                        <div class="homestay-image">
                            <img :src="processImageUrl(order.imageUrl)" :alt="order.homestayTitle"
                                @error="handleImageErrorEvent">
                        </div>
                        <div class="order-details">
                            <h3>{{ order.homestayTitle }}</h3>
                            <p class="date-info"><i class="el-icon-date"></i> {{ formatDateRange(order.checkInDate,
                                order.checkOutDate) }} · {{
                                    order.nights }}晚</p>
                            <p class="guest-info"><i class="el-icon-user"></i> {{ order.guestCount }}位房客</p>
                            <p class="host-info"><i class="el-icon-s-custom"></i> 房东: {{ order.hostName || '未知' }}</p>
                            <div class="price-container">
                                <span class="price-label">总价</span>
                                <span class="price-value">¥{{ order.totalAmount }}</span>
                            </div>
                        </div>
                    </div>

                    <div class="order-card-footer">
                        <div class="action-buttons">
                            <!-- 待支付 (已确认 & 未支付) -->
                            <template v-if="order.status === 'CONFIRMED' && order.paymentStatus === 'UNPAID'">
                                <el-button type="default" size="small"
                                    @click="viewOrderDetail(order.id)">查看详情</el-button>
                                <el-button type="danger" plain size="small"
                                    @click="cancelOrder(order.id)">取消订单</el-button>
                                <el-button type="primary" size="small" @click="goToPayment(order.id)">立即支付</el-button>
                                <!-- 保留支付提示，或根据需要调整 -->
                                <div v-if="order.status === 'CONFIRMED'" class="payment-reminder danger small-text">
                                    <el-tooltip content="超过2小时未支付的订单将被自动取消">
                                        <span><i class="el-icon-time"></i> 支付剩余: {{ getCountdownTime(order.updateTime,
                                            2)
                                            }}</span>
                                    </el-tooltip>
                                </div>
                            </template>

                            <!-- 已支付 (且未完成/未取消) -->
                            <template
                                v-else-if="order.paymentStatus === 'PAID' && order.status !== 'COMPLETED' && order.status !== 'CANCELLED'">
                                <el-button type="default" size="small"
                                    @click="viewOrderDetail(order.id)">查看详情</el-button>
                                <el-button type="primary" plain size="small">联系房东</el-button>
                                <!-- <el-button type="warning" plain size="small" @click="requestRefund(order.id)">申请退款</el-button> -->
                                <!-- 可选：申请退款按钮 -->
                            </template>

                            <!-- 新增：支付失败 -->
                            <template v-else-if="order.status === 'PAYMENT_FAILED' || order.paymentStatus === 'FAILED'">
                                <el-button type="default" size="small"
                                    @click="viewOrderDetail(order.id)">查看详情</el-button>
                                <el-button type="warning" plain size="small"
                                    @click="goToPayment(order.id)">重新支付</el-button>
                                <el-button type="danger" plain size="small"
                                    @click="cancelOrder(order.id)">取消订单</el-button>
                            </template>

                            <!-- 待确认 -->
                            <template v-else-if="order.status === 'PENDING'">
                                <el-button type="default" size="small"
                                    @click="viewOrderDetail(order.id)">查看详情</el-button>
                                <el-button type="danger" plain size="small"
                                    @click="cancelOrder(order.id)">取消订单</el-button>
                                <!-- 保留确认提示 -->
                                <div v-if="order.status === 'PENDING'" class="payment-reminder warning small-text">
                                    <el-tooltip content="超过24小时未确认的订单将被自动取消">
                                        <span><i class="el-icon-time"></i> 确认剩余: {{ getCountdownTime(order.createTime,
                                            24)
                                            }}</span>
                                    </el-tooltip>
                                </div>
                            </template>

                            <!-- 已完成 / 已取消 / 其他最终状态 -->
                            <template v-else>
                                <el-button type="default" size="small"
                                    @click="viewOrderDetail(order.id)">查看详情</el-button>
                                <!-- 添加评价按钮，仅在订单完成且未评价时显示 -->
                                <el-button v-if="order.status === 'COMPLETED' && !order.reviewed" type="primary" plain
                                    size="small" @click="openReviewModal(order)">
                                    评价订单
                                </el-button>
                                <!-- 可以添加一个已评价的提示或按钮 -->
                                <el-button v-else-if="order.status === 'COMPLETED' && order.reviewed" type="info" plain
                                    size="small" disabled>
                                    已评价
                                </el-button>
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

        <!-- 评价模态框 -->
        <ReviewForm v-if="currentReviewOrder" v-model:visible="reviewDialogVisible" :order-id="currentReviewOrder.id"
            :homestay-id="currentReviewOrder.homestayId" :homestay-title="currentReviewOrder.homestayTitle"
            @submit="handleSubmitReview" />
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyOrders, cancelOrder as apiCancelOrder } from '@/api/order'
import { getHomestayById } from '@/api/homestay'
import { submitReview } from '@/api/review'
import ReviewForm from '@/components/ReviewForm.vue'
import { getHomestayImageUrl, handleImageError } from '@/utils/image'
import { type OrderStatus, type PaymentStatus } from '@/types/index'

interface OrderItem {
    id: number
    orderNumber: string
    homestayId: number
    homestayTitle: string
    hostName: string
    imageUrl?: string
    guestCount: number
    checkInDate: string
    checkOutDate: string
    nights: number
    totalAmount: number
    status: OrderStatus
    paymentStatus: PaymentStatus
    createTime: string
    updateTime: string
    reviewed?: boolean
}

const router = useRouter()

const loading = ref(true)
const orders = ref<OrderItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const activeTab = ref('all')

// --- 评价模态框状态 ---
const reviewDialogVisible = ref(false);
const currentReviewOrder = ref<OrderItem | null>(null);

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

        let orderData = response.data
        let rawOrders: any[] = [];

        // --- 数据提取逻辑，保持不变 ---
        if (orderData.data && orderData.data.content) {
            rawOrders = orderData.data.content
            total.value = orderData.data.totalElements
        } else if (orderData.data && Array.isArray(orderData.data.content)) {
            rawOrders = orderData.data.content
            total.value = orderData.data.totalElements || orderData.data.content.length
        } else if (orderData.content) {
            rawOrders = orderData.content
            total.value = orderData.totalElements || orderData.content.length
        } else if (Array.isArray(orderData)) {
            rawOrders = orderData
            total.value = orderData.length
        } else if (Array.isArray(orderData.content)) {
            rawOrders = orderData.content
            total.value = orderData.totalElements || orderData.totalPages * orderData.size
        } else {
            console.warn('未识别的订单数据格式:', orderData)
            rawOrders = []
            total.value = 0
        }
        // --- 数据提取逻辑结束 ---

        // --- 数据映射 --- 
        orders.value = rawOrders.map((order: any): OrderItem => ({
            id: order.id,
            orderNumber: order.orderNumber,
            homestayId: order.homestayId,
            homestayTitle: order.homestayTitle,
            hostName: order.hostName,
            imageUrl: order.imageUrl, // 后端DTO似乎没有这个，依赖 updateOrderImages
            guestCount: order.guestCount,
            checkInDate: order.checkInDate,
            checkOutDate: order.checkOutDate,
            nights: order.nights,
            totalAmount: order.totalAmount,
            status: order.status as OrderStatus,
            paymentStatus: order.paymentStatus as PaymentStatus,
            createTime: order.createTime,
            updateTime: order.updateTime,
            reviewed: order.reviewed ?? false
        }));

        console.log('处理后的订单数据 (含reviewed):', orders.value);
        console.log('总数量:', total.value);

        await updateOrderImages(); // 图片更新逻辑保持不变

    } catch (error) {
        console.error('获取订单列表失败:', error)
        ElMessage.error('获取订单列表失败，请刷新页面重试')
        orders.value = []
        total.value = 0
    } finally {
        loading.value = false
    }
}

// 更新订单的房源图片
const updateOrderImages = async () => {
    if (!orders.value.length) return

    // 使用Promise.all并行获取所有订单的房源图片
    const updatePromises = orders.value.map(async (order) => {
        if (!order.homestayId) return
        try {
            const response = await getHomestayById(order.homestayId)
            if (response && response.data) {
                // 更新订单图片为房源的封面图或第一张图片
                const coverImage = response.data.coverImage ||
                    (response.data.images && response.data.images.length > 0 ?
                        response.data.images[0] : null)

                if (coverImage) {
                    order.imageUrl = coverImage
                    console.log(`订单${order.id}更新图片: ${order.imageUrl}`)
                }
            }
        } catch (error) {
            console.error(`获取订单${order.id}对应的房源详情失败:`, error)
        }
    })

    // 等待所有图片更新完成
    await Promise.all(updatePromises)
}

// 处理图片URL
const processImageUrl = (url?: string) => {
    return getHomestayImageUrl(url);
}

// 处理图片加载错误事件处理器
const handleImageErrorEvent = (event: Event) => {
    handleImageError(event, 'homestay');
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
            '确定要取消该订单吗？取消后将无法恢复',
            '取消订单',
            {
                confirmButtonText: '确定取消',
                cancelButtonText: '再想想',
                type: 'warning',
                distinguishCancelAndClose: true,
                closeOnClickModal: false
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

            // 显示更详细的成功消息，并提供跳转链接
            ElMessageBox.alert(
                '订单已成功取消，您可以继续浏览其他房源',
                '取消成功',
                {
                    confirmButtonText: '查看其他房源',
                    callback: (action: string) => {
                        if (action === 'confirm') {
                            router.push('/')
                        } else {
                            // 刷新当前页面的订单列表
                            fetchOrders()
                        }
                    }
                }
            )
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

// 前往支付页面 (恢复原始逻辑)
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

// 获取订单状态对应的样式类型 (更正逻辑)
const getStatusType = (order: OrderItem): string => {
    const status = order.status as OrderStatus;
    const paymentStatus = order.paymentStatus as PaymentStatus;

    // 优先处理最终/关键状态
    if (status?.startsWith('CANCELLED') || status === 'REJECTED') return 'danger';
    if (paymentStatus === 'REFUNDED') return 'warning'; // 已退款用 warning
    if (status === 'COMPLETED') return 'info'; // 已完成用 info
    if (status === 'PAYMENT_FAILED' || paymentStatus === 'FAILED') return 'danger'; // 支付失败用 danger

    // 处理支付成功状态
    if (paymentStatus === 'PAID') {
        // 如果后端有 CHECKED_IN 状态
        if (status === 'COMPLETED') return 'info'; // 完成状态用 info
        // Add CHECKED_IN check if applicable
        // if (status === 'CHECKED_IN') return 'primary';
        return 'success'; // 其他 PAID 相关状态用 success
    }

    // 处理待支付状态 (已确认但未支付)
    if (status === 'CONFIRMED' && (paymentStatus === 'UNPAID' || paymentStatus === null || paymentStatus === undefined)) {
        return 'warning'; // 待支付用 warning
    }
    // 处理支付处理中状态
    if (status === 'PAYMENT_PENDING') {
        return 'warning'; // 支付处理中也用 warning
    }

    // 处理待确认
    if (status === 'PENDING') return 'info'; // 待确认用 info

    console.warn(`Unhandled order state in getStatusType: status=${status}, paymentStatus=${order.paymentStatus}`);
    return 'info'; // Fallback
}

// 获取订单状态文本 (更正逻辑)
const getStatusText = (order: OrderItem): string => {
    const status = order.status as OrderStatus;
    const paymentStatus = order.paymentStatus as PaymentStatus;

    // 优先处理最终/关键状态
    if (status?.startsWith('CANCELLED')) return '已取消';
    if (status === 'REJECTED') return '已拒绝';
    if (paymentStatus === 'REFUNDED') return '已退款';
    if (status === 'COMPLETED') return '已完成';
    if (status === 'PAYMENT_FAILED' || paymentStatus === 'FAILED') return '支付失败'; // 支付失败文本

    // 处理支付成功状态驱动的状态
    if (paymentStatus === 'PAID') {
        if (status === 'COMPLETED') return '已完成';
        // Add CHECKED_IN check if applicable
        // if (status === 'CHECKED_IN') return '已入住';
        return '已支付'; // 明确是支付状态驱动
    }

    // 处理待支付
    if (status === 'CONFIRMED' && (paymentStatus === 'UNPAID' || paymentStatus === null || paymentStatus === undefined)) {
        return '待支付';
    }
    // 处理支付处理中
    if (status === 'PAYMENT_PENDING') {
        return '待支付'; // 支付处理中也显示待支付
    }

    // 处理待确认
    if (status === 'PENDING') {
        return '待确认';
    }

    // 处理完成 (如果前面没匹配到)
    if (status === 'COMPLETED') return '已完成';

    // Fallback
    console.warn(`Unhandled order state in getStatusText: status=${status}, paymentStatus=${order.paymentStatus}`);
    return '未知状态';
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

// 计算倒计时
const getCountdownTime = (startTimeStr: string, hoursLimit: number) => {
    const startTime = new Date(startTimeStr).getTime();
    const currentTime = new Date().getTime();
    const limitTime = startTime + hoursLimit * 60 * 60 * 1000;
    const remainingTime = limitTime - currentTime;

    if (remainingTime <= 0) {
        return "即将自动取消";
    }

    // 计算剩余小时和分钟
    const hours = Math.floor(remainingTime / (60 * 60 * 1000));
    const minutes = Math.floor((remainingTime % (60 * 60 * 1000)) / (60 * 1000));

    return `${hours}小时${minutes}分钟`;
}

// --- 评价相关方法 ---
const openReviewModal = (order: OrderItem) => {
    console.log('Opening review modal for order:', order);
    currentReviewOrder.value = order;
    reviewDialogVisible.value = true;
};

const handleSubmitReview = async (reviewData: any) => {
    console.log('Received review data from form:', reviewData);
    if (!currentReviewOrder.value) return;

    // 确认 reviewData 中包含所有需要的信息，特别是 orderId
    // 我们的 ReviewForm 应该已经包含了 orderId 和 homestayId
    // const payload = { ...reviewData }; 

    try {
        const response = await submitReview(reviewData); // 直接传递表单数据
        console.log('Submit review response:', response);
        ElMessage.success('评价提交成功！');
        reviewDialogVisible.value = false;
        // 评价成功后可以刷新订单列表，或者更新按钮状态（如果需要）
        fetchOrders(); // 重新获取订单，如果后端会标记订单已评价
    } catch (error: any) {
        console.error('提交评价失败:', error);
        let errorMessage = '提交评价失败，请稍后重试';
        if (error.response && error.response.data && error.response.data.message) {
            errorMessage = error.response.data.message; // 显示后端返回的错误信息
        }
        ElMessage.error(errorMessage);
        // 不关闭弹窗，让用户可以修改
    }
};

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
    background-color: #f9f9f9;
}

.page-header {
    margin-bottom: 32px;
    text-align: center;
}

.page-header h1 {
    font-size: 32px;
    font-weight: 600;
    margin: 0;
    color: #333;
    position: relative;
    display: inline-block;
}

.page-header h1::after {
    content: '';
    position: absolute;
    bottom: -10px;
    left: 50%;
    transform: translateX(-50%);
    width: 60px;
    height: 3px;
    background-color: #409EFF;
    border-radius: 3px;
}

.filters-bar {
    margin-bottom: 24px;
    background-color: white;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
    padding: 16px 20px;
}

.loading-container,
.empty-container {
    padding: 60px;
    background-color: white;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
    margin-bottom: 24px;
    text-align: center;
}

.order-card {
    background-color: white;
    border-radius: 12px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    margin-bottom: 24px;
    overflow: hidden;
    transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.order-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.order-card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    border-bottom: 1px solid #f0f0f0;
    background-color: #fafafa;
}

.order-info {
    display: flex;
    gap: 24px;
}

.order-number {
    font-weight: 500;
    color: #333;
}

.order-date {
    color: #606266;
}

.order-status {
    display: flex;
    align-items: center;
    gap: 12px;
}

.order-card-content {
    padding: 24px 20px;
    display: flex;
    gap: 24px;
}

.homestay-image {
    width: 180px;
    height: 130px;
    border-radius: 10px;
    overflow: hidden;
    flex-shrink: 0;
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.homestay-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.4s ease;
}

.homestay-image:hover img {
    transform: scale(1.05);
}

.order-details {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
}

.order-details h3 {
    margin: 0 0 12px 0;
    font-size: 20px;
    font-weight: 600;
    color: #333;
}

.date-info,
.guest-info {
    margin: 8px 0;
    color: #606266;
    display: flex;
    align-items: center;
    gap: 6px;
}

.price-container {
    margin-top: 12px;
    display: flex;
    align-items: center;
}

.price-label {
    margin-right: 8px;
    color: #606266;
}

.price-value {
    font-size: 20px;
    font-weight: 700;
    color: #ff6b6b;
}

.order-card-footer {
    padding: 16px 20px;
    border-top: 1px solid #f0f0f0;
    display: flex;
    justify-content: flex-end;
    background-color: #fafafa;
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

.order-countdown {
    margin-left: 8px;
    font-size: 12px;
    color: #606266;
    padding: 2px 8px;
    background-color: #f8f8f8;
    border-radius: 4px;
}

.payment-reminder {
    margin-top: 8px;
    font-size: 12px;
    color: #606266;
    font-style: italic;
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

/* Add styles for small-text if needed */
.small-text {
    font-size: 12px;
    color: #909399;
    /* Adjust color as needed */
    margin-left: 10px;
}

.payment-reminder.danger {
    color: var(--el-color-danger);
}

.payment-reminder.warning {
    color: var(--el-color-warning);
}
</style>