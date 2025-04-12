<template>
    <div class="order-pay-container">
        <div v-if="loading" class="loading-container">
            <el-skeleton :rows="5" animated />
        </div>
        <div v-else-if="!orderData" class="error-container">
            <el-empty description="订单不存在">
                <template #description>
                    <p>未找到订单信息，请返回订单列表</p>
                </template>
                <el-button type="primary" @click="goToOrders">查看我的订单</el-button>
            </el-empty>
        </div>
        <div v-else class="pay-content">
            <div class="pay-header">
                <h1>订单支付</h1>
                <div class="order-info">
                    <p class="amount">支付金额: <span class="price">¥{{ orderData.totalAmount }}</span></p>
                    <p class="order-number">订单号: {{ orderData.orderNumber }}</p>
                    <el-tag :type="orderData.status === 'PENDING_PAYMENT' ? 'warning' : 'success'">
                        {{ getStatusText(orderData.status) }}
                    </el-tag>
                </div>
            </div>

            <div class="payment-section">
                <div class="qr-code-container" v-if="qrCode">
                    <img :src="qrCode" alt="支付二维码" class="qr-code">
                    <p class="qr-tip">请使用{{ getPaymentMethodText(paymentMethod) }}扫码支付</p>
                    <div class="countdown" v-if="countdown > 0">
                        二维码有效期: {{ formatCountdown(countdown) }}
                    </div>
                </div>
                <div v-else class="payment-methods">
                    <h3>选择支付方式</h3>
                    <el-radio-group v-model="paymentMethod" @change="generateQRCode">
                        <el-radio-button label="wechat">
                            <i class="fab fa-weixin"></i> 微信支付
                        </el-radio-button>
                        <el-radio-button label="alipay">
                            <i class="fab fa-alipay"></i> 支付宝
                        </el-radio-button>
                    </el-radio-group>
                    <el-button type="primary" class="generate-qr" @click="generateQRCode" :disabled="!paymentMethod">
                        生成支付二维码
                    </el-button>
                </div>
            </div>

            <div class="order-summary">
                <h3>订单信息</h3>
                <div class="summary-item">
                    <span>房源名称</span>
                    <span>{{ orderData.homestayTitle }}</span>
                </div>
                <div class="summary-item">
                    <span>入住日期</span>
                    <span>{{ formatDate(orderData.checkInDate) }}</span>
                </div>
                <div class="summary-item">
                    <span>退房日期</span>
                    <span>{{ formatDate(orderData.checkOutDate) }}</span>
                </div>
                <div class="summary-item">
                    <span>入住人数</span>
                    <span>{{ orderData.guestCount }}人</span>
                </div>
            </div>

            <div class="actions">
                <el-button @click="cancelOrder" plain>取消订单</el-button>
                <el-button type="primary" @click="checkPaymentStatus">
                    我已完成支付
                </el-button>
                <el-button type="success" @click="mockPaymentSuccess">
                    模拟支付成功 (测试用)
                </el-button>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, generatePaymentQRCode, checkPayment, cancelOrder as apiCancelOrder, mockPayment } from '../../api/order'

interface OrderData {
    id: number
    orderNumber: string
    homestayTitle: string
    checkInDate: string
    checkOutDate: string
    guestCount: number
    totalAmount: number
    status: string
}

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const orderData = ref<OrderData | null>(null)
const paymentMethod = ref(route.query.method?.toString() || '')
const qrCode = ref('')
const countdown = ref(900) // 15分钟倒计时
let timer: NodeJS.Timeout | null = null

// 获取订单详情
const fetchOrderDetail = async () => {
    try {
        const orderId = Number(route.params.id)
        const response = await getOrderDetail(orderId)
        orderData.value = response.data
    } catch (error: any) {
        console.error('获取订单详情失败:', error)
        ElMessage.error(error.response?.data?.message || '获取订单详情失败')
    } finally {
        loading.value = false
    }
}

// 生成支付二维码
const generateQRCode = async () => {
    if (!orderData.value || !paymentMethod.value) return

    try {
        const response = await generatePaymentQRCode({
            orderId: orderData.value.id,
            method: paymentMethod.value
        })
        qrCode.value = response.data.qrCode
        startCountdown()
    } catch (error: any) {
        ElMessage.error('生成支付二维码失败，请重试')
    }
}

// 开始倒计时
const startCountdown = () => {
    countdown.value = 900
    if (timer) clearInterval(timer)
    timer = setInterval(() => {
        if (countdown.value > 0) {
            countdown.value--
        } else {
            qrCode.value = ''
            if (timer) clearInterval(timer)
        }
    }, 1000)
}

// 格式化倒计时
const formatCountdown = (seconds: number) => {
    const minutes = Math.floor(seconds / 60)
    const remainingSeconds = seconds % 60
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`
}

// 检查支付状态
const checkPaymentStatus = async () => {
    if (!orderData.value) return

    try {
        const response = await checkPayment(orderData.value.id)
        if (response.data.paid) {
            ElMessage.success('支付成功！')
            router.push(`/order/${orderData.value.id}`)
        } else {
            ElMessage.warning('未检测到支付完成，请确认支付状态')
        }
    } catch (error: any) {
        ElMessage.error('检查支付状态失败，请重试')
    }
}

// 模拟支付成功（仅用于测试）
const mockPaymentSuccess = async () => {
    if (!orderData.value) return

    try {
        const response = await mockPayment(orderData.value.id)
        if (response.data.paid) {
            ElMessage.success('模拟支付成功！')
            router.push(`/order/${orderData.value.id}`)
        }
    } catch (error: any) {
        console.error('模拟支付失败:', error)
        ElMessage.error(error.response?.data?.message || '模拟支付失败')
    }
}

// 取消订单
const cancelOrder = async () => {
    if (!orderData.value) return

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

        await apiCancelOrder(orderData.value.id)
        ElMessage.success('订单已取消')
        router.push('/orders')
    } catch (error: any) {
        if (error !== 'cancel') {
            ElMessage.error('取消订单失败，请重试')
        }
    }
}

// 获取支付方式文本
const getPaymentMethodText = (method: string) => {
    const methods = {
        wechat: '微信',
        alipay: '支付宝'
    }
    return methods[method as keyof typeof methods] || ''
}

// 获取订单状态文本
const getStatusText = (status: string) => {
    const statusMap = {
        PENDING_PAYMENT: '待支付',
        PAID: '已支付',
        CANCELLED: '已取消'
    }
    return statusMap[status as keyof typeof statusMap] || status
}

// 格式化日期
const formatDate = (date: string) => {
    return new Date(date).toLocaleDateString('zh-CN', {
        month: 'long',
        day: 'numeric'
    })
}

// 跳转到订单列表
const goToOrders = () => {
    router.push('/orders')
}

onMounted(() => {
    fetchOrderDetail()
})

onUnmounted(() => {
    if (timer) clearInterval(timer)
})
</script>

<style scoped>
.order-pay-container {
    max-width: 800px;
    margin: 0 auto;
    padding: 40px 20px;
}

.loading-container,
.error-container {
    padding: 40px;
    text-align: center;
}

.pay-header {
    margin-bottom: 40px;
    text-align: center;
}

h1 {
    font-size: 28px;
    margin-bottom: 20px;
}

.order-info {
    text-align: center;
}

.amount {
    font-size: 20px;
    margin-bottom: 10px;
}

.price {
    color: #f56c6c;
    font-size: 24px;
    font-weight: bold;
}

.order-number {
    color: #666;
    margin-bottom: 10px;
}

.payment-section {
    background-color: #f8f9fa;
    border-radius: 12px;
    padding: 32px;
    margin-bottom: 32px;
    text-align: center;
}

.qr-code-container {
    display: flex;
    flex-direction: column;
    align-items: center;
}

.qr-code {
    width: 200px;
    height: 200px;
    margin-bottom: 16px;
}

.qr-tip {
    color: #666;
    margin-bottom: 8px;
}

.countdown {
    color: #f56c6c;
    font-size: 14px;
}

.payment-methods {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20px;
}

.generate-qr {
    margin-top: 20px;
}

.order-summary {
    background-color: #fff;
    border: 1px solid #eee;
    border-radius: 12px;
    padding: 24px;
    margin-bottom: 32px;
}

.summary-item {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
    color: #666;
}

.actions {
    display: flex;
    justify-content: center;
    gap: 16px;
    margin-top: 32px;
}

:deep(.el-radio-button__inner) {
    display: flex;
    align-items: center;
    gap: 8px;
}

.fab {
    font-size: 18px;
}
</style>