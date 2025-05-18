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
            <!-- 新增：支付失败提示 -->
            <el-alert v-if="orderData.status === 'PAYMENT_FAILED' || orderData.paymentStatus === 'FAILED'" title="支付提醒"
                type="warning" description="您之前的支付尝试未成功，请重新选择支付方式并完成支付。" show-icon :closable="false"
                style="margin-bottom: 20px;">
            </el-alert>

            <div class="pay-header">
                <h1>订单支付</h1>
                <div class="order-info">
                    <p class="amount">支付金额: <span class="price">¥{{ orderData.totalAmount }}</span></p>
                    <p class="order-number">订单号: {{ orderData.orderNumber }}</p>
                    <el-tag
                        :type="orderData.status === 'PENDING_PAYMENT' || orderData.status === 'CONFIRMED' || orderData.status === 'PAYMENT_PENDING' ? 'warning' : 'success'">
                        {{ getStatusText(orderData.status) }}
                    </el-tag>
                </div>
            </div>

            <!-- 添加订单超时倒计时提醒 -->
            <div class="order-timeout-alert" :class="{ 'urgent': isTimeUrgent(orderData.updateTime, 2) }">
                <el-alert title="请在限定时间内完成支付" type="warning" description="超过2小时未完成支付，订单将被自动取消" show-icon
                    :closable="false">
                    <template #default>
                        <div class="timeout-countdown">
                            <span>支付倒计时：</span>
                            <span class="countdown-time">{{ getCountdownTime(orderData.updateTime, 2) }}</span>
                        </div>
                    </template>
                </el-alert>
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
                <el-button type="success" plain @click="handleMockPaymentSuccess" v-if="orderData.status !== 'PAID'">
                    【测试】模拟支付成功
                </el-button>
                <el-button type="primary" @click="checkPaymentStatus" :disabled="!qrCodeGenerated">
                    我已完成支付
                </el-button>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, generatePaymentQRCode, checkPayment, cancelOrder as apiCancelOrder, mockPaymentSuccess } from '../../api/order'

interface OrderData {
    id: number
    orderNumber: string
    homestayTitle: string
    checkInDate: string
    checkOutDate: string
    guestCount: number
    totalAmount: number
    status: string
    paymentStatus: string
    updateTime: string
}

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const orderData = ref<OrderData | null>(null)
const paymentMethod = ref(route.query.method?.toString() || '')
const qrCode = ref('')
const countdown = ref(900) // 15分钟倒计时
const qrCodeGenerated = ref(false) // 新增状态变量，标记二维码是否已生成
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
        qrCodeGenerated.value = true // 生成成功后，更新状态为 true
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

    const loadingInstance = ElMessage({ type: 'info', message: '正在确认支付状态...', duration: 0 });
    try {
        const response = await checkPayment(orderData.value.id)
        loadingInstance.close(); // 关闭加载提示

        if (response.data.paid) {
            ElMessage.success('支付成功！')
            // 跳转到订单详情页
            router.push(`/orders/${orderData.value.id}`)
        } else {
            // 检查是否有错误消息从后端传来
            if (response.data.error) {
                ElMessage.error(response.data.error);
            } else {
                ElMessage.warning('支付状态尚未确认，请稍后再试或联系客服');
            }
        }
    } catch (error: any) {
        loadingInstance.close(); // 关闭加载提示
        console.error("检查支付状态接口调用失败:", error);
        ElMessage.error(error.response?.data?.error || '检查支付状态失败，请重试');
    }
}

// 取消订单
const cancelOrder = async () => {
    if (!orderData.value) return

    try {
        await ElMessageBox.confirm(
            '确定要取消该订单吗？取消后将无法恢复',
            '取消订单',
            {
                confirmButtonText: '确定取消',
                cancelButtonText: '继续支付',
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
            await apiCancelOrder(orderData.value.id)

            // 关闭加载消息
            ElMessage.closeAll()

            // 显示更详细的成功消息，并提供跳转选项
            const result = await ElMessageBox.alert(
                '订单已成功取消，您可以继续浏览其他房源或返回订单列表',
                '取消成功',
                {
                    confirmButtonText: '查看其他房源',
                    cancelButtonText: '返回订单列表',
                    distinguishCancelAndClose: true,
                    showCancelButton: true
                }
            )

            if (result === 'confirm') {
                router.push('/') // 前往首页
            } else {
                router.push('/orders') // 返回订单列表
            }
        } catch (error: any) {
            // 关闭加载消息
            ElMessage.closeAll()

            console.error('取消订单失败:', error)
            ElMessage.error('取消订单失败，请重试')
        }
    } catch (error: any) {
        if (error !== 'cancel') {
            console.error('取消订单确认操作失败:', error)
        }
    }
}

// 新增：处理模拟支付成功的函数
const handleMockPaymentSuccess = async () => {
    if (!orderData.value) return;

    const loadingInstance = ElMessage({
        message: '正在模拟支付成功...',
        type: 'info',
        duration: 0,
    });
    try {
        await mockPaymentSuccess(orderData.value.id);
        loadingInstance.close();
        ElMessage.success('模拟支付成功！正在检查最终状态...');
        // 模拟成功后，立即检查支付状态，这会确认状态并可能导航离开
        await checkPaymentStatus();
    } catch (error) {
        loadingInstance.close();
        // 错误已在 api/order.ts 中处理并提示，这里可以不再重复提示
        console.error('模拟支付成功失败:', error);
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
        PAYMENT_PENDING: '待支付',
        CONFIRMED: '待支付',
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

// 计算倒计时
const getCountdownTime = (startTimeStr: string, hoursLimit: number) => {
    if (!startTimeStr) return "获取中...";

    const startTime = new Date(startTimeStr).getTime();
    const currentTime = new Date().getTime();
    const limitTime = startTime + hoursLimit * 60 * 60 * 1000;
    const remainingTime = limitTime - currentTime;

    if (remainingTime <= 0) {
        return "即将自动取消";
    }

    // 计算剩余小时、分钟和秒数
    const hours = Math.floor(remainingTime / (60 * 60 * 1000));
    const minutes = Math.floor((remainingTime % (60 * 60 * 1000)) / (60 * 1000));
    const seconds = Math.floor((remainingTime % (60 * 1000)) / 1000);

    return `${hours}小时${minutes}分钟${seconds}秒`;
}

// 判断时间是否紧急（剩余不到30分钟）
const isTimeUrgent = (startTimeStr: string, hoursLimit: number) => {
    if (!startTimeStr) return false;

    const startTime = new Date(startTimeStr).getTime();
    const currentTime = new Date().getTime();
    const limitTime = startTime + hoursLimit * 60 * 60 * 1000;
    const remainingTime = limitTime - currentTime;

    // 如果剩余时间小于30分钟，显示紧急样式
    return remainingTime < 30 * 60 * 1000 && remainingTime > 0;
}

// 自动刷新倒计时
let autoRefreshTimer: NodeJS.Timeout | null = null;

onMounted(() => {
    fetchOrderDetail();
    // 每秒刷新倒计时显示
    autoRefreshTimer = setInterval(() => {
        if (orderData.value?.updateTime) {
            // 强制更新页面
            orderData.value = { ...orderData.value };
        }
    }, 1000);
});

onUnmounted(() => {
    if (timer) clearInterval(timer);
    if (autoRefreshTimer) clearInterval(autoRefreshTimer);
});
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

.order-timeout-alert {
    margin: 20px 0;
}

.order-timeout-alert.urgent .el-alert {
    background-color: #fef0f0;
    border-color: #f56c6c;
}

.timeout-countdown {
    margin-top: 10px;
    text-align: center;
}

.countdown-time {
    font-size: 18px;
    font-weight: bold;
    color: #e6a23c;
}

.order-timeout-alert.urgent .countdown-time {
    color: #f56c6c;
    animation: blink 1s infinite;
}

@keyframes blink {
    0% {
        opacity: 1;
    }

    50% {
        opacity: 0.5;
    }

    100% {
        opacity: 1;
    }
}
</style>