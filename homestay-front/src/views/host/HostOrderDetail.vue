<template>
    <div class="host-order-detail-container">
        <div v-if="loading" class="loading-container">
            <el-skeleton :rows="10" animated />
        </div>
        <div v-else-if="!orderData" class="error-container">
            <el-empty description="订单不存在">
                <template #description>
                    <p>未找到订单信息，请返回订单列表</p>
                </template>
                <el-button type="primary" @click="goToOrders">返回订单列表</el-button>
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

            <!-- 快速操作面板 - 仅待确认订单显示 -->
            <div class="action-panel" v-if="orderData.status === 'PENDING'">
                <div class="action-card">
                    <div class="action-info">
                        <div class="action-title">订单需要您的确认</div>
                        <div class="action-desc">租客已提交预订申请，请尽快确认或拒绝。</div>
                    </div>
                    <div class="action-buttons">
                        <el-button type="danger" @click="showRejectDialog">拒绝预订</el-button>
                        <el-button type="primary" @click="confirmOrder">接受预订</el-button>
                    </div>
                </div>
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
                        <p>{{ formatDateRange(orderData.checkInDate, orderData.checkOutDate) }}</p>
                        <p>{{ orderData.guestCount }}位房客 · {{ orderData.nights }}晚</p>
                    </div>
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
                <div class="info-item" v-if="orderData.remark && !orderData.remark.includes('拒绝原因')">
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

            <!-- 拒绝原因 - 如果订单被拒绝 -->
            <div class="reject-reason-section" v-if="orderData.status === 'REJECTED' && orderData.remark">
                <h2>拒绝原因</h2>
                <div class="reject-reason">
                    <p>{{ extractRejectReason(orderData.remark) }}</p>
                </div>
            </div>

            <!-- 订单时间线 -->
            <div class="order-timeline-section">
                <h2>订单状态记录</h2>
                <el-timeline>
                    <el-timeline-item :timestamp="formatDateTime(orderData.createTime)" placement="top" type="primary">
                        <h4>订单创建</h4>
                        <p>租客提交了预订申请</p>
                    </el-timeline-item>

                    <el-timeline-item v-if="orderData.status !== 'PENDING'"
                        :timestamp="formatDateTime(orderData.updateTime)" placement="top"
                        :type="orderData.status === 'REJECTED' || orderData.status === 'CANCELLED' ? 'danger' : 'success'">
                        <h4>{{ getStatusChangeTitle(orderData.status) }}</h4>
                        <p>{{ getStatusChangeDesc(orderData.status) }}</p>
                    </el-timeline-item>
                </el-timeline>
            </div>

            <!-- 底部操作栏 -->
            <div class="bottom-actions">
                <el-button @click="goToOrders">返回订单列表</el-button>
                <el-button type="success" v-if="orderData.status === 'CONFIRMED'">
                    联系租客
                </el-button>
            </div>
        </div>

        <!-- 拒绝订单对话框 -->
        <el-dialog v-model="rejectDialogVisible" title="拒绝预订" width="500px">
            <div class="reject-dialog-content">
                <p>请输入拒绝预订的原因，这将帮助租客了解您的决定。</p>
                <el-form :model="rejectForm" :rules="rejectRules" ref="rejectFormRef">
                    <el-form-item prop="reason">
                        <el-input v-model="rejectForm.reason" type="textarea" rows="4"
                            placeholder="例如：该日期我已有其他安排，无法接待..."></el-input>
                    </el-form-item>
                </el-form>
            </div>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="rejectDialogVisible = false">取消</el-button>
                    <el-button type="danger" @click="rejectOrder" :loading="submitting">
                        确认拒绝
                    </el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail } from '@/api/order'
import { updateOrderStatus, rejectOrder as rejectOrderApi } from '@/api/hostOrder'

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
const submitting = ref(false)
const orderData = ref<OrderData | null>(null)
const rejectDialogVisible = ref(false)
const rejectFormRef = ref()

const rejectForm = reactive({
    reason: ''
})

const rejectRules = {
    reason: [
        { required: true, message: '请输入拒绝原因', trigger: 'blur' },
        { min: 5, max: 200, message: '拒绝原因长度在5到200个字符之间', trigger: 'blur' }
    ]
}

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

// 确认订单
const confirmOrder = async () => {
    if (!orderData.value) return

    try {
        await ElMessageBox.confirm(
            '确定接受此预订吗？确认后将通知租客支付订单。',
            '接受预订',
            {
                confirmButtonText: '确认接受',
                cancelButtonText: '取消',
                type: 'success'
            }
        )

        submitting.value = true
        await updateOrderStatus(orderData.value.id, 'CONFIRMED')
        ElMessage.success('已接受预订')
        fetchOrderDetail()
    } catch (error: any) {
        if (error !== 'cancel') {
            console.error('接受预订失败:', error)
            ElMessage.error(error.response?.data?.error || '接受预订失败，请重试')
        }
    } finally {
        submitting.value = false
    }
}

// 显示拒绝对话框
const showRejectDialog = () => {
    rejectForm.reason = ''
    rejectDialogVisible.value = true
}

// 拒绝订单
const rejectOrder = async () => {
    if (!rejectFormRef.value || !orderData.value) return

    try {
        await rejectFormRef.value.validate()

        submitting.value = true
        await rejectOrderApi(orderData.value.id, rejectForm.reason)

        ElMessage.success('已拒绝预订')
        rejectDialogVisible.value = false
        fetchOrderDetail()
    } catch (error: any) {
        if (error !== false) {
            console.error('拒绝预订失败:', error)
            ElMessage.error(error.response?.data?.error || '拒绝预订失败，请重试')
        }
    } finally {
        submitting.value = false
    }
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

// 获取状态变更标题
const getStatusChangeTitle = (status: string) => {
    const titleMap: Record<string, string> = {
        'CONFIRMED': '预订已确认',
        'REJECTED': '预订已拒绝',
        'CANCELLED': '预订已取消',
        'PAID': '订单已支付',
        'CHECKED_IN': '租客已入住',
        'COMPLETED': '订单已完成'
    }
    return titleMap[status] || '状态已更新'
}

// 获取状态变更描述
const getStatusChangeDesc = (status: string) => {
    const descMap: Record<string, string> = {
        'CONFIRMED': '您已接受租客的预订申请',
        'REJECTED': '您已拒绝租客的预订申请',
        'CANCELLED': '此订单已被取消',
        'PAID': '租客已完成支付',
        'CHECKED_IN': '租客已成功入住',
        'COMPLETED': '订单已完成，感谢您的服务'
    }
    return descMap[status] || ''
}

// 格式化日期范围
const formatDateRange = (checkIn: string, checkOut: string) => {
    if (!checkIn || !checkOut) return ''

    const checkInDate = new Date(checkIn)
    const checkOutDate = new Date(checkOut)

    return `${checkInDate.getMonth() + 1}月${checkInDate.getDate()}日 - ${checkOutDate.getMonth() + 1}月${checkOutDate.getDate()}日`
}

// 格式化日期时间
const formatDateTime = (dateTime: string) => {
    if (!dateTime) return ''

    const date = new Date(dateTime)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 提取拒绝原因
const extractRejectReason = (remark: string) => {
    if (!remark) return ''

    const reasonMatch = remark.match(/拒绝原因: (.+)/)
    return reasonMatch ? reasonMatch[1] : remark
}

// 返回订单列表
const goToOrders = () => {
    router.push('/host/orders')
}

onMounted(() => {
    fetchOrderDetail()
})
</script>

<style scoped>
.host-order-detail-container {
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

.action-panel {
    margin-bottom: 24px;
}

.action-card {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background-color: #ecf5ff;
    border-radius: 8px;
    padding: 20px;
    border-left: 4px solid #409eff;
}

.action-title {
    font-size: 16px;
    font-weight: bold;
    margin-bottom: 8px;
}

.action-desc {
    font-size: 14px;
    color: #606266;
}

.action-buttons {
    display: flex;
    gap: 12px;
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

.guest-info-section,
.price-details-section,
.reject-reason-section,
.order-timeline-section {
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

.reject-reason {
    background-color: #fef0f0;
    border-radius: 8px;
    padding: 16px;
}

.bottom-actions {
    display: flex;
    gap: 16px;
    justify-content: center;
    margin-top: 40px;
}

.reject-dialog-content p {
    margin-top: 0;
    margin-bottom: 16px;
}
</style>