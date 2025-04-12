<template>
    <div class="bookings-container">
        <div class="bookings-header">
            <h1>我的预订</h1>
        </div>

        <div v-if="loading" class="loading-container">
            <el-skeleton :rows="3" animated />
            <el-skeleton :rows="3" animated />
            <el-skeleton :rows="3" animated />
        </div>

        <div v-else-if="bookings.length === 0" class="empty-result">
            <el-empty description="您还没有预订记录">
                <el-button type="primary" @click="$router.push('/')">去预订民宿</el-button>
            </el-empty>
        </div>

        <div v-else>
            <el-tabs v-model="activeTab" class="booking-tabs">
                <el-tab-pane label="全部预订" name="all"></el-tab-pane>
                <el-tab-pane label="待支付" name="PENDING"></el-tab-pane>
                <el-tab-pane label="待入住" name="CONFIRMED"></el-tab-pane>
                <el-tab-pane label="已完成" name="COMPLETED"></el-tab-pane>
                <el-tab-pane label="已取消" name="CANCELLED"></el-tab-pane>
            </el-tabs>

            <div class="booking-list">
                <el-card v-for="booking in filteredBookings" :key="booking.id" class="booking-card"
                    @click="viewOrderDetail(booking.id)">
                    <div class="booking-info">
                        <div class="booking-image">
                            <img :src="getImageUrl(booking.imageUrl)" :alt="booking.homestayTitle"
                                @error="handleImageError">
                        </div>
                        <div class="booking-details">
                            <h3 class="booking-title" @click="viewOrderDetail(booking.id)" style="cursor: pointer;">
                                {{ booking.homestayTitle || '未命名民宿' }}
                            </h3>
                            <div class="booking-location" v-if="booking.location">{{ booking.location }}</div>
                            <div class="booking-dates">
                                {{ formatDate(booking.checkInDate) }} 至 {{ formatDate(booking.checkOutDate) }}
                                <span class="booking-nights">({{ booking.nights }}晚)</span>
                            </div>
                            <div class="booking-guests">{{ booking.guestCount }}位房客</div>
                            <div :class="['booking-status', getStatusClass(booking.status)]">
                                <span>{{ formatStatus(booking.status) }}</span>
                                <span class="status-time" v-if="booking.createTime">
                                    {{ formatTime(booking.createTime) }}
                                </span>
                            </div>
                        </div>
                        <div class="booking-price">
                            <div class="price-total">¥{{ booking.totalAmount }}</div>
                            <div class="price-detail" v-if="booking.cleaningFee || booking.serviceFee">
                                <div>¥{{ booking.price || 0 }} x {{ booking.nights }}晚</div>
                                <div v-if="booking.cleaningFee">清洁费: ¥{{ booking.cleaningFee }}</div>
                                <div v-if="booking.serviceFee">服务费: ¥{{ booking.serviceFee }}</div>
                            </div>
                        </div>
                    </div>
                    <div class="booking-actions">
                        <el-button type="primary" size="small" @click="viewOrderDetail(booking.id)">
                            查看详情
                        </el-button>
                        <el-button type="warning" size="small" v-if="canBeCancelled(booking.status)"
                            @click="cancelBooking(booking.id)">
                            取消订单
                        </el-button>
                        <el-button type="success" size="small"
                            v-if="booking.status === 'CONFIRMED' || booking.status === 'PENDING_PAYMENT'"
                            @click="payBooking(booking.id)">
                            去支付
                        </el-button>
                        <el-button size="small" v-if="booking.status === 'COMPLETED'"
                            @click="reviewBooking(booking.id)">
                            评价
                        </el-button>
                    </div>
                </el-card>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElEmpty, ElLoading } from 'element-plus'
import dayjs from 'dayjs'
import { getUserOrders, cancelOrder as apiCancelOrder } from '@/api/order'

// 定义类型
interface Booking {
    id: number;
    orderNumber: string;
    homestayId: number;
    homestayTitle: string;
    imageUrl?: string;
    location?: string;
    checkInDate: string;
    checkOutDate: string;
    nights: number;
    guestCount: number;
    totalAmount: number;
    price?: number;
    cleaningFee?: number;
    serviceFee?: number;
    status: string;
    createTime: string;
}

const router = useRouter()
const loading = ref(false)
const bookings = ref<Booking[]>([])
const activeTab = ref('all')

const filteredBookings = computed(() => {
    if (activeTab.value === 'all') {
        return bookings.value
    }
    return bookings.value.filter(booking => booking.status === activeTab.value)
})

onMounted(async () => {
    await fetchBookings()
})

const fetchBookings = async () => {
    loading.value = true
    try {
        // 从API获取订单数据
        const response = await getUserOrders()
        console.log('订单数据原始响应:', response.data)

        // 处理数据格式转换
        let ordersData = []

        // 处理不同的响应格式
        if (response.data && response.data.data && response.data.data.content) {
            // 标准分页格式
            ordersData = response.data.data.content
        } else if (response.data && Array.isArray(response.data)) {
            // 直接是数组的情况
            ordersData = response.data
        } else if (response.data && response.data.content) {
            // 简化的分页格式
            ordersData = response.data.content
        } else if (Array.isArray(response.data.data)) {
            // 数据嵌套在data字段中的情况
            ordersData = response.data.data
        }

        // 转换为前端需要的格式
        bookings.value = ordersData.map((order: any) => {
            // 计算入住晚数
            const checkIn = new Date(order.checkInDate)
            const checkOut = new Date(order.checkOutDate)
            const nights = Math.ceil((checkOut.getTime() - checkIn.getTime()) / (1000 * 60 * 60 * 24))

            return {
                id: order.id,
                orderNumber: order.orderNumber || `ORDER-${order.id}`,
                homestayId: order.homestayId,
                homestayTitle: order.homestayTitle || '未命名民宿',
                imageUrl: order.imageUrl || null,
                location: order.location || order.address || '',
                checkInDate: order.checkInDate,
                checkOutDate: order.checkOutDate,
                nights: nights || 1,
                guestCount: order.guestCount || 1,
                totalAmount: Number(order.totalAmount) || 0,
                price: Number(order.price) || 0,
                cleaningFee: Number(order.cleaningFee) || 0,
                serviceFee: Number(order.serviceFee) || 0,
                status: order.status || 'PENDING',
                createTime: order.createTime || new Date().toISOString()
            }
        })

        console.log('处理后的订单数据:', bookings.value)
    } catch (error) {
        console.error('获取订单数据失败:', error)
        ElMessage.error('获取订单数据失败，请稍后重试')
        bookings.value = []
    } finally {
        loading.value = false
    }
}

const getImageUrl = (imageUrl: string | undefined) => {
    if (!imageUrl) {
        return 'https://picsum.photos/400/300'
    }

    // 处理不同格式的图片URL
    if (imageUrl.startsWith('http')) {
        return imageUrl
    } else if (imageUrl.startsWith('/uploads/')) {
        return `/api${imageUrl}`
    } else if (imageUrl.startsWith('/homestays/')) {
        return `/api${imageUrl}`
    } else if (!imageUrl.startsWith('/')) {
        return `/api/uploads/homestays/${imageUrl}`
    }

    return `/api${imageUrl}`
}

const formatDate = (dateString: string) => {
    if (!dateString) return ''
    const date = new Date(dateString)
    return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

const getStatusText = (status: string) => {
    const statusMap: Record<string, string> = {
        PENDING: '待确认',
        CONFIRMED: '已确认',
        PAID: '已支付',
        COMPLETED: '已完成',
        CANCELLED: '已取消'
    }
    return statusMap[status] || status
}

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

const formatDateTime = (dateString: string) => {
    if (!dateString) return ''
    const date = new Date(dateString)
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    })
}

const viewOrderDetail = (id: number) => {
    router.push(`/orders/${id}`)
}

const payBooking = (id: number) => {
    router.push(`/orders/${id}/pay`)
}

const cancelBooking = async (id: number) => {
    try {
        await ElMessageBox.confirm('确定要取消此预订吗？取消后不可恢复。', '取消预订', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })

        // 使用PUT方法更新订单状态为取消，而不是使用特定的取消API
        try {
            const response = await fetch(`/api/orders/${id}/status`, {
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
                // 刷新订单列表
                await fetchBookings();
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
            ElMessage.error('取消订单失败，请稍后重试')
        }
    }
}

const reviewBooking = (id: number) => {
    ElMessage.info('评价功能开发中')
}

const getStatusClass = (status: string) => {
    const statusClasses: Record<string, string> = {
        PENDING: 'booking-status pending',
        CONFIRMED: 'booking-status confirmed',
        PAID: 'booking-status paid',
        COMPLETED: 'booking-status completed',
        CANCELLED: 'booking-status cancelled',
        REJECTED: 'booking-status rejected'
    }
    return statusClasses[status] || 'booking-status info'
}

const formatStatus = (status: string) => {
    const statusText = getStatusText(status)
    return statusText.charAt(0).toUpperCase() + statusText.slice(1)
}

const formatTime = (dateString: string) => {
    if (!dateString) return ''
    const date = new Date(dateString)
    return date.toLocaleTimeString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit'
    })
}

const calculateNights = (checkInDate: string, checkOutDate: string) => {
    const checkIn = new Date(checkInDate)
    const checkOut = new Date(checkOutDate)
    const nights = Math.ceil((checkOut.getTime() - checkIn.getTime()) / (1000 * 60 * 60 * 24))
    return nights > 0 ? nights : 1
}

const formatPrice = (amount: number) => {
    return amount.toLocaleString('zh-CN', {
        style: 'currency',
        currency: 'CNY'
    })
}

const handleImageError = (event: Event) => {
    const target = event.target as HTMLImageElement
    if (target) {
        target.src = 'https://picsum.photos/400/300'
    }
}

const canBeCancelled = (status: string) => {
    return ['PENDING', 'CONFIRMED'].includes(status)
}
</script>

<style scoped>
.bookings-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px;
}

.bookings-header {
    margin-bottom: 24px;
}

.loading-container {
    padding: 24px;
}

.empty-result {
    padding: 48px 0;
    text-align: center;
}

.booking-tabs {
    margin-bottom: 24px;
}

.booking-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.booking-card {
    margin-bottom: 16px;
}

.booking-info {
    display: flex;
    gap: 16px;
}

.booking-image {
    width: 200px;
    height: 150px;
    overflow: hidden;
    border-radius: 8px;
}

.booking-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.booking-details {
    flex: 1;
}

.booking-title {
    margin: 0 0 8px 0;
    font-size: 18px;
}

.booking-location,
.booking-dates,
.booking-guests {
    margin-bottom: 4px;
    color: #606266;
}

.booking-nights {
    color: #909399;
}

.booking-status {
    margin-top: 8px;
    display: flex;
    align-items: center;
    gap: 8px;
}

.status-time {
    font-size: 12px;
    color: #909399;
}

/* 订单状态样式 */
.booking-status.pending {
    color: #e6a23c;
}

.booking-status.confirmed,
.booking-status.paid {
    color: #67c23a;
}

.booking-status.cancelled,
.booking-status.rejected {
    color: #909399;
}

.booking-status.completed {
    color: #409eff;
}

.booking-price {
    text-align: right;
    min-width: 150px;
}

.price-total {
    font-size: 20px;
    font-weight: 600;
    margin-bottom: 8px;
}

.price-detail {
    font-size: 14px;
    color: #606266;
}

.booking-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid #ebeef5;
}

@media (max-width: 768px) {
    .booking-info {
        flex-direction: column;
    }

    .booking-image {
        width: 100%;
        height: 200px;
    }

    .booking-price {
        text-align: left;
        margin-top: 16px;
    }

    .booking-actions {
        flex-wrap: wrap;
    }
}
</style>