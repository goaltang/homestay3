<template>
    <div class="order-detail-container">
        <div v-if="loading" class="loading-container">
            <el-skeleton :rows="10" animated />
        </div>
        <div v-else-if="!orderData" class="error-container">
            <el-empty description="订单不存在">
                <template #description>
                    <p>未找到订单信息，请返回订单列表</p>
                </template>
                <el-button type="primary" @click="goToOrders">查看我的订单</el-button>
            </el-empty>
        </div>
        <div v-else class="order-content">
            <div class="order-header">
                <h1>订单详情</h1>
                <div class="order-status">
                    <el-tag :type="getStatusType(orderData.status)">
                        {{ getStatusText(orderData.status) }}
                    </el-tag>
                </div>
            </div>

            <!-- 订单状态流程提示 -->
            <div class="order-status-flow">
                <el-steps :active="getStatusStep(orderData.status)" finish-status="success" simple>
                    <el-step title="预订申请" />
                    <el-step title="房东确认" />
                    <el-step title="支付订单" />
                    <el-step title="入住" />
                    <el-step title="完成" />
                </el-steps>
            </div>

            <!-- 订单信息卡片 -->
            <div class="order-summary-card">
                <div class="card-header">
                    <h2>订单信息</h2>
                    <span class="order-number">订单号: {{ orderData.orderNumber }}</span>
                </div>
                <div class="homestay-info">
                    <div class="homestay-image">
                        <img :src="orderData.imageUrl || 'https://picsum.photos/400/300'" alt="房源图片">
                    </div>
                    <div class="homestay-details">
                        <h3>{{ orderData.homestayTitle }}</h3>
                        <p>{{ orderData.address }}</p>
                        <p>{{ formatDateRange(orderData.checkInDate, orderData.checkOutDate) }}</p>
                        <p>{{ orderData.guestCount }}位房客 · {{ orderData.nights }}晚</p>
                    </div>
                </div>
            </div>

            <!-- 预订状态提示 -->
            <div class="status-notice" v-if="orderData.status === 'PENDING'">
                <el-alert title="等待房东确认" type="warning" description="您的预订申请已提交，正在等待房东确认。房东将在24小时内确认您的预订。" show-icon
                    :closable="false" />
            </div>

            <div class="status-notice" v-else-if="orderData.status === 'CONFIRMED'">
                <el-alert title="房东已确认预订" type="success" description="房东已确认您的预订，请尽快完成支付以确保您的住宿。" show-icon
                    :closable="false" />
                <div class="pay-button-container">
                    <el-button type="primary" @click="goToPayment">
                        立即支付
                    </el-button>
                </div>
            </div>

            <div class="status-notice" v-else-if="orderData.status === 'REJECTED'">
                <el-alert title="预订被拒绝" type="error" description="很抱歉，房东拒绝了您的预订申请。" show-icon :closable="false" />
                <div class="reject-reason" v-if="orderData.remark">
                    <h4>拒绝原因:</h4>
                    <p>{{ extractRejectReason(orderData.remark) }}</p>
                </div>
            </div>

            <!-- 旅客信息 -->
            <div class="guest-info-section">
                <h2>旅客信息</h2>
                <div class="info-item">
                    <span class="label">联系人:</span>
                    <span>{{ orderData.guestName }}</span>
                </div>
                <div class="info-item">
                    <span class="label">联系电话:</span>
                    <span>{{ orderData.guestPhone }}</span>
                </div>
                <div class="info-item" v-if="orderData.remark && orderData.status !== 'REJECTED'">
                    <span class="label">备注:</span>
                    <span>{{ orderData.remark }}</span>
                </div>
            </div>

            <!-- 价格详情 -->
            <div class="price-details-section">
                <h2>价格详情</h2>
                <div class="price-item">
                    <span>每晚价格 x {{ orderData.nights }}晚</span>
                    <span>¥{{ orderData.price * orderData.nights }}</span>
                </div>
                <div class="price-total">
                    <span>总价</span>
                    <span>¥{{ orderData.totalAmount }}</span>
                </div>
            </div>

            <!-- 按钮操作区 -->
            <div class="action-buttons">
                <el-button @click="goToOrders">返回订单列表</el-button>
                <el-button type="danger" plain v-if="canCancel" @click="confirmCancel">
                    取消订单
                </el-button>
                <el-button type="primary" v-if="orderData.status === 'CONFIRMED'" @click="goToPayment">
                    立即支付
                </el-button>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, cancelOrder } from '../../api/order'

interface OrderData {
    id: number
    orderNumber: string
    homestayId: number
    homestayTitle: string
    imageUrl?: string
    address?: string
    guestId: number
    guestName: string
    guestPhone: string
    checkInDate: string
    checkOutDate: string
    nights: number
    guestCount: number
    price: number
    totalAmount: number
    status: string
    remark?: string
    createTime: string
    updateTime: string
}

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const orderData = ref<OrderData | null>(null)

// 获取订单详情
const fetchOrderDetail = async () => {
    try {
        const orderId = Number(route.params.id)
        if (isNaN(orderId)) {
            ElMessage.error('无效的订单ID')
            return
        }

        const response = await getOrderDetail(orderId)
        orderData.value = response.data
    } catch (error: any) {
        console.error('获取订单详情失败:', error)
        ElMessage.error('获取订单详情失败，请重试')
    } finally {
        loading.value = false
    }
}

// 计算是否可以取消订单
const canCancel = computed(() => {
    if (!orderData.value) return false

    // 只有待确认、已确认的订单可以取消
    return ['PENDING', 'CONFIRMED'].includes(orderData.value.status)
})

// 获取订单状态的步骤
const getStatusStep = (status: string) => {
    const statusSteps: Record<string, number> = {
        'PENDING': 1,       // 预订申请(步骤1)
        'CONFIRMED': 2,     // 房东确认(步骤2)
        'REJECTED': 1,      // 被拒绝(保持在步骤1)
        'CANCELLED': 1,     // 已取消(保持在步骤1)
        'PAID': 3,          // 已支付(步骤3)
        'CHECKED_IN': 4,    // 已入住(步骤4)
        'COMPLETED': 5      // 已完成(步骤5)
    }
    return statusSteps[status] || 0
}

// 获取状态显示的类型
const getStatusType = (status: string) => {
    const statusTypes: Record<string, string> = {
        'PENDING': 'warning',
        'CONFIRMED': 'success',
        'REJECTED': 'danger',
        'CANCELLED': 'info',
        'PAID': 'success',
        'CHECKED_IN': 'success',
        'COMPLETED': 'success'
    }
    return statusTypes[status] || 'info'
}

// 获取状态显示文本
const getStatusText = (status: string) => {
    const statusTexts: Record<string, string> = {
        'PENDING': '待确认',
        'CONFIRMED': '已确认',
        'REJECTED': '已拒绝',
        'CANCELLED': '已取消',
        'PAID': '已支付',
        'CHECKED_IN': '已入住',
        'COMPLETED': '已完成'
    }
    return statusTexts[status] || status
}

// 格式化日期范围
const formatDateRange = (checkIn: string, checkOut: string) => {
    if (!checkIn || !checkOut) return ''

    const checkInDate = new Date(checkIn)
    const checkOutDate = new Date(checkOut)

    return `${checkInDate.getMonth() + 1}月${checkInDate.getDate()}日 - ${checkOutDate.getMonth() + 1}月${checkOutDate.getDate()}日`
}

// 提取拒绝原因
const extractRejectReason = (remark: string) => {
    if (!remark) return ''

    const reasonMatch = remark.match(/拒绝原因: (.+)/)
    return reasonMatch ? reasonMatch[1] : remark
}

// 确认取消订单
const confirmCancel = async () => {
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

        if (!orderData.value) return

        try {
            // 使用PUT方法更新订单状态为取消，而不是使用特定的取消API
            const response = await fetch(`/api/orders/${orderData.value.id}/status`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('token')}`
                },
                body: JSON.stringify({ status: 'CANCELLED' })
            });

            const result = await response.json();
            if (response.ok) {
                ElMessage.success('订单已成功取消');
                // 刷新订单数据
                fetchOrderDetail();
            } else {
                throw new Error(result.message || '取消订单失败');
            }
        } catch (error: any) {
            console.error('API调用错误:', error);
            ElMessage.error(`取消订单失败: ${error.message || '请稍后重试'}`);
        }
    } catch (error: any) {
        if (error !== 'cancel') {
            console.error('取消订单失败:', error)
            ElMessage.error('取消订单失败，请重试')
        }
    }
}

// 前往支付页面
const goToPayment = () => {
    if (!orderData.value) return

    router.push(`/orders/${orderData.value.id}/pay`)
}

// 前往订单列表
const goToOrders = () => {
    router.push('/orders')
}

onMounted(() => {
    fetchOrderDetail()
})
</script>

<style scoped>
.order-detail-container {
    max-width: 800px;
    margin: 0 auto;
    padding: 40px 20px;
}

.loading-container,
.error-container {
    padding: 40px;
    text-align: center;
}

.order-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
}

h1 {
    font-size: 28px;
    margin: 0;
}

.order-status-flow {
    margin-bottom: 32px;
}

.order-summary-card {
    background-color: #f8f9fa;
    border-radius: 12px;
    padding: 24px;
    margin-bottom: 32px;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}

.card-header h2 {
    margin: 0;
    font-size: 18px;
}

.order-number {
    color: #666;
    font-size: 14px;
}

.homestay-info {
    display: flex;
    gap: 20px;
}

.homestay-image {
    width: 120px;
    height: 90px;
    border-radius: 8px;
    overflow: hidden;
}

.homestay-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.homestay-details h3 {
    margin: 0 0 8px 0;
    font-size: 18px;
}

.homestay-details p {
    margin: 4px 0;
    color: #666;
}

.status-notice {
    margin-bottom: 32px;
}

.pay-button-container {
    margin-top: 16px;
    text-align: center;
}

.reject-reason {
    background-color: #fef0f0;
    border-radius: 8px;
    padding: 16px;
    margin-top: 16px;
}

.reject-reason h4 {
    margin: 0 0 8px 0;
    color: #f56c6c;
}

.reject-reason p {
    margin: 0;
}

.guest-info-section,
.price-details-section {
    margin-bottom: 32px;
}

h2 {
    font-size: 18px;
    margin-bottom: 16px;
}

.info-item {
    display: flex;
    margin-bottom: 8px;
}

.info-item .label {
    width: 100px;
    color: #666;
}

.price-item {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
}

.price-total {
    display: flex;
    justify-content: space-between;
    font-weight: bold;
    padding-top: 12px;
    border-top: 1px solid #eee;
    margin-top: 12px;
}

.action-buttons {
    display: flex;
    gap: 16px;
    justify-content: center;
    margin-top: 40px;
}
</style>