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
                    <div class="header-right">
                        <span class="order-number">订单号: {{ orderData.orderNumber }}</span>
                        <el-tag :type="getStatusType(orderData.status)" size="large" class="status-tag">
                            {{ getStatusText(orderData.status, orderData.paymentStatus, orderData.refundType) }}
                        </el-tag>
                    </div>
                </div>
                <div class="homestay-info">
                    <div class="homestay-image">
                        <img :src="processImageUrl(orderData.imageUrl)" alt="房源图片" @error="handleImageErrorEvent">
                    </div>
                    <div class="homestay-details">
                        <h3>{{ orderData.homestayTitle }}</h3>
                        <p><i class="el-icon-location"></i> {{ orderData.address }}</p>
                        <p><i class="el-icon-date"></i> {{ formatDateRange(orderData.checkInDate,
                            orderData.checkOutDate) }}</p>
                        <p><i class="el-icon-user"></i> {{ orderData.guestCount }}位房客 · {{ orderData.nights }}晚</p>
                    </div>
                </div>

                <!-- 旅客信息整合到订单信息中 -->
                <div class="guest-info-inline">
                    <div class="info-row">
                        <div class="info-item">
                            <span class="label">联系人:</span>
                            <span>{{ orderData.guestName }}</span>
                        </div>
                        <div class="info-item">
                            <span class="label">联系电话:</span>
                            <span>{{ orderData.guestPhone }}</span>
                        </div>
                    </div>
                    <div class="info-item" v-if="orderData.remark && orderData.status !== 'REJECTED'">
                        <span class="label">备注:</span>
                        <span>{{ orderData.remark }}</span>
                    </div>
                </div>
            </div>

            <!-- 预订状态提示 -->
            <div class="status-notice" v-if="orderData.status === 'PENDING'">
                <el-alert title="等待房东确认" type="warning" description="您的预订申请已提交" show-icon :closable="false" />
                <OrderTimeoutIndicator :order-id="orderData.id" :order-status="orderData.status as OrderStatus"
                    :create-time="orderData.createTime" :confirm-time="orderData.updateTime"
                    :update-time="orderData.updateTime" @timeout="handleOrderTimeout" @warning="handleOrderWarning" />
            </div>

            <div class="status-notice" v-else-if="orderData.status === 'CONFIRMED'">
                <el-alert title="房东已确认预订" type="success" description="请及时完成支付" show-icon :closable="false" />
                <OrderTimeoutIndicator :order-id="orderData.id" :order-status="orderData.status as OrderStatus"
                    :create-time="orderData.createTime" :confirm-time="orderData.updateTime"
                    :update-time="orderData.updateTime" @timeout="handleOrderTimeout" @warning="handleOrderWarning" />
            </div>

            <div class="status-notice" v-else-if="orderData.status === 'REJECTED'">
                <el-alert title="预订被拒绝" type="error" description="很抱歉，房东拒绝了您的预订申请。" show-icon :closable="false" />
                <div class="reject-reason" v-if="orderData.remark">
                    <h4>拒绝原因:</h4>
                    <p>{{ extractRejectReason(orderData.remark) }}</p>
                </div>
            </div>

            <!-- 退款状态提示 -->
            <div class="status-notice" v-else-if="isRefundStatus">
                <el-alert :title="getRefundStatusTitle" :type="getRefundStatusType" :description="getRefundStatusDesc"
                    show-icon :closable="false" />
            </div>



            <!-- 价格详情 -->
            <div class="price-details-section">
                <h2>{{ isRefundStatus ? '费用明细' : '价格详情' }}</h2>
                <div class="price-item">
                    <span>每晚价格 x {{ orderData.nights }}晚</span>
                    <span>¥{{ orderData.price * orderData.nights }}</span>
                </div>
                <div class="price-total">
                    <span>原始总价</span>
                    <span>¥{{ orderData.totalAmount }}</span>
                </div>

                <!-- 退款状态下显示退款信息 -->
                <template v-if="isRefundStatus && orderData.refundAmount">
                    <div class="price-item refund-item">
                        <span>退款金额</span>
                        <span class="refund-amount">-¥{{ orderData.refundAmount }}</span>
                    </div>
                    <div class="price-total final-amount" v-if="orderData.paymentStatus === 'REFUNDED'">
                        <span>实际扣费</span>
                        <span>¥{{ orderData.totalAmount - orderData.refundAmount }}</span>
                    </div>
                </template>
            </div>

            <!-- 退款信息 -->
            <div v-if="hasRefundInfo" class="refund-details-section">
                <h2>退款信息</h2>
                <div class="info-item">
                    <span class="label">退款类型:</span>
                    <span>{{ getRefundTypeText(orderData.refundType).replace(/[（）]/g, '') }}</span>
                </div>
                <div v-if="orderData.refundInitiatedByName" class="info-item">
                    <span class="label">发起人:</span>
                    <span>{{ orderData.refundInitiatedByName }}</span>
                </div>
                <div v-if="orderData.refundInitiatedAt" class="info-item">
                    <span class="label">申请时间:</span>
                    <span>{{ formatDateTime(orderData.refundInitiatedAt) }}</span>
                </div>
                <div v-if="orderData.refundReason" class="info-item">
                    <span class="label">退款原因:</span>
                    <span>{{ orderData.refundReason }}</span>
                </div>
                <div v-if="orderData.refundAmount" class="info-item">
                    <span class="label">退款金额:</span>
                    <span class="refund-amount">¥{{ orderData.refundAmount }}</span>
                </div>
                <div v-if="orderData.refundProcessedByName" class="info-item">
                    <span class="label">处理人:</span>
                    <span>{{ orderData.refundProcessedByName }}</span>
                </div>
                <div v-if="orderData.refundProcessedAt" class="info-item">
                    <span class="label">处理时间:</span>
                    <span>{{ formatDateTime(orderData.refundProcessedAt) }}</span>
                </div>
                <div v-if="orderData.refundTransactionId" class="info-item">
                    <span class="label">退款交易号:</span>
                    <span>{{ orderData.refundTransactionId }}</span>
                </div>
            </div>

            <!-- 评价详情 (如果已完成且已评价) -->
            <div class="review-details-section" v-if="orderData?.status === 'COMPLETED' && orderData.review">
                <div class="review-header">
                    <h2>我的评价</h2>
                    <div class="review-actions">
                        <el-button v-if="canEditReview(orderData.review)" type="primary" plain size="small"
                            @click="openEditModal(orderData.review)">
                            修改评价
                        </el-button>
                        <el-button v-if="canDeleteReview(orderData.review)" type="danger" plain size="small"
                            @click="handleDeleteReview(orderData.review.id)" :loading="isDeletingReview"
                            class="delete-review-btn">
                            删除评价
                        </el-button>
                    </div>
                </div>
                <div class="review-content">
                    <div class="rating-line">
                        <span class="label">评分:</span>
                        <el-rate :model-value="orderData.review.rating" disabled size="small" text-color="#ff9900" />
                    </div>
                    <div class="info-item">
                        <span class="label">评价内容:</span>
                        <p>{{ orderData.review.content }}</p>
                    </div>
                    <div class="info-item">
                        <span class="label">评价时间:</span>
                        <span>{{ formatDate(orderData.review.createTime) }}</span>
                    </div>
                    <div v-if="orderData.review.response" class="host-response-detail">
                        <el-divider direction="horizontal" />
                        <h4>房东回复:</h4>
                        <p>{{ orderData.review.response }}</p>
                        <div class="response-time" v-if="orderData.review.responseTime">
                            回复于: {{ formatDateTime(orderData.review.responseTime) }}
                        </div>
                    </div>
                </div>
            </div>

            <!-- 按钮操作区 -->
            <div class="action-buttons">
                <el-button @click="goToOrders">返回订单列表</el-button>

                <!-- 非退款状态下的正常操作 -->
                <template v-if="!isRefundStatus">
                    <el-button type="danger" plain v-if="canCancel" @click="confirmCancel">
                        取消订单
                    </el-button>
                    <el-button type="primary" v-if="orderData.status === 'CONFIRMED'" @click="goToPayment">
                        立即支付
                    </el-button>
                    <el-button type="warning" plain v-if="canRequestRefund" @click="confirmRequestRefund">
                        申请退款
                    </el-button>
                </template>

                <!-- 退款状态下的简化操作 -->
                <template v-else>
                    <el-button type="info" plain @click="contactCustomerService"
                        v-if="orderData.paymentStatus === 'REFUND_FAILED'">
                        联系客服
                    </el-button>
                </template>
            </div>
        </div>

        <!-- Add Edit Modal -->
        <ReviewEditModal v-model:visible="isEditModalVisible" :review-data="currentEditingReview"
            @submitted="handleReviewUpdated" />
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, cancelOrder } from '../../api/order'
import { getHomestayById } from '../../api/homestay'
import { getHomestayImageUrl, handleImageError } from '../../utils/image'
import dayjs from 'dayjs'
import { deleteReview } from '@/api/review'
import { requestRefund } from '@/api/refund'
import { useUserStore } from '@/stores/user'
import ReviewEditModal from '@/components/ReviewEditModal.vue'
import OrderTimeoutIndicator from '@/components/order/OrderTimeoutIndicator.vue'
import { OrderStatus } from '@/types/order'

// 定义评价数据接口
interface ReviewItem {
    id: number;
    userId?: number; // 后端DTO可能没有，但前端获取用户评价列表时可能有
    rating: number;
    content: string;
    response?: string;
    createTime: string;
    responseTime?: string;
    // 添加其他需要的字段...
}

// --- Add type for editable data ---
interface EditableReviewData {
    id: number;
    rating: number;
    content: string;
}
// --- End type ---

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
    paymentStatus?: string
    remark?: string
    createTime: string
    updateTime: string
    reviewed?: boolean; // 保留 reviewed 字段
    review?: ReviewItem | null; // 添加 review 字段
    // 退款相关字段
    refundType?: string
    refundReason?: string
    refundAmount?: number
    refundInitiatedBy?: number
    refundInitiatedByName?: string
    refundInitiatedAt?: string
    refundProcessedBy?: number
    refundProcessedByName?: string
    refundProcessedAt?: string
    refundTransactionId?: string
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const orderData = ref<OrderData | null>(null)
const isDeletingReview = ref(false)

// --- Add state for edit modal ---
const isEditModalVisible = ref(false);
const currentEditingReview = ref<EditableReviewData | null>(null);
// --- End state ---

// 获取当前登录用户ID
const currentUserId = userStore.userInfo?.id

// 获取订单详情
const fetchOrderDetail = async () => {
    loading.value = true; // 开始时设置加载状态
    try {
        const orderId = Number(route.params.id)
        if (isNaN(orderId)) {
            ElMessage.error('无效的订单ID')
            return;
        }

        const response = await getOrderDetail(orderId)

        // 直接将后端返回的 data 赋值给 orderData
        // 假设 response.data 的结构与 OrderData 接口匹配 (包括 review)
        orderData.value = response.data;
        console.log('获取到的订单详情数据:', orderData.value);

        // 获取房源详情，更新房源图片 (这部分逻辑保持不变)
        if (orderData.value && orderData.value.homestayId && !orderData.value.imageUrl) {
            try {
                const homestayResponse = await getHomestayById(orderData.value.homestayId)
                if (homestayResponse && homestayResponse.data) {
                    const coverImage = homestayResponse.data.coverImage ||
                        (homestayResponse.data.images && homestayResponse.data.images.length > 0 ?
                            homestayResponse.data.images[0] : null);

                    if (coverImage && orderData.value) { // Check orderData again
                        orderData.value.imageUrl = coverImage;
                    }
                }
            } catch (error) {
                console.error('获取房源详情失败:', error);
            }
        }
    } catch (error: any) {
        console.error('获取订单详情失败:', error);
        ElMessage.error('获取订单详情失败，请重试');
        orderData.value = null; // 清空数据
    } finally {
        loading.value = false;
    }
}

// 处理图片URL
const processImageUrl = (url?: string) => {
    return getHomestayImageUrl(url);
}

// 处理图片加载错误事件处理器
const handleImageErrorEvent = (event: Event) => {
    handleImageError(event, 'homestay');
}

// 计算是否可以取消订单
const canCancel = computed(() => {
    if (!orderData.value) return false

    // 只有待确认、已确认的订单可以取消
    return ['PENDING', 'CONFIRMED'].includes(orderData.value.status)
})

// 计算是否有退款信息
const hasRefundInfo = computed(() => {
    if (!orderData.value) return false

    return orderData.value.refundType ||
        orderData.value.refundReason ||
        orderData.value.refundAmount ||
        orderData.value.refundInitiatedByName ||
        orderData.value.refundProcessedByName
})

// 计算是否可以申请退款
const canRequestRefund = computed(() => {
    if (!orderData.value) return false

    // 只有已支付且未退款的订单可以申请退款
    return (orderData.value.status === 'PAID' || orderData.value.paymentStatus === 'PAID') &&
        orderData.value.paymentStatus !== 'REFUND_PENDING' &&
        orderData.value.paymentStatus !== 'REFUNDED' &&
        orderData.value.status !== 'COMPLETED' &&
        orderData.value.status !== 'CANCELLED'
})

// 计算是否为退款状态
const isRefundStatus = computed(() => {
    if (!orderData.value) return false
    return ['REFUND_PENDING', 'REFUNDED', 'REFUND_FAILED'].includes(orderData.value.paymentStatus || '')
})

// 获取退款状态标题
const getRefundStatusTitle = computed(() => {
    if (!orderData.value) return ''

    const refundTypeText = getRefundTypeText(orderData.value.refundType)

    switch (orderData.value.paymentStatus) {
        case 'REFUND_PENDING':
            return `退款申请处理中${refundTypeText}`
        case 'REFUNDED':
            return `退款已完成${refundTypeText}`
        case 'REFUND_FAILED':
            return `退款处理失败${refundTypeText}`
        default:
            return ''
    }
})

// 获取退款状态类型
const getRefundStatusType = computed(() => {
    if (!orderData.value) return 'info'

    switch (orderData.value.paymentStatus) {
        case 'REFUND_PENDING':
            return 'warning'
        case 'REFUNDED':
            return 'success'
        case 'REFUND_FAILED':
            return 'error'
        default:
            return 'info'
    }
})

// 获取退款状态描述
const getRefundStatusDesc = computed(() => {
    if (!orderData.value) return ''

    switch (orderData.value.paymentStatus) {
        case 'REFUND_PENDING':
            return '您的退款申请正在处理中，请耐心等待'
        case 'REFUNDED':
            return `退款金额 ¥${orderData.value.refundAmount || orderData.value.totalAmount} 将在1-3个工作日内到账`
        case 'REFUND_FAILED':
            return '退款处理失败，如有疑问请联系客服'
        default:
            return ''
    }
})

// 获取订单状态的步骤
const getStatusStep = (status: string) => {
    const statusSteps: Record<string, number> = {
        'PENDING': 1,       // 预订申请(步骤1)
        'CONFIRMED': 2,     // 房东确认(步骤2)
        'REJECTED': 1,      // 被拒绝(保持在步骤1)
        'CANCELLED': 1,     // 已取消(保持在步骤1)
        'CANCELLED_SYSTEM': 1, // 系统取消(保持在步骤1)
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
        'CANCELLED_SYSTEM': 'info',
        'CANCELLED_BY_USER': 'info',
        'CANCELLED_BY_HOST': 'info',
        'PAID': 'success',
        'CHECKED_IN': 'success',
        'COMPLETED': 'success'
    }
    return statusTypes[status] || 'info'
}

// 获取状态显示文本
const getStatusText = (status: string, paymentStatus?: string, refundType?: string) => {
    // 优先处理退款相关状态
    if (paymentStatus === 'REFUND_PENDING') {
        const refundTypeText = getRefundTypeText(refundType);
        return `退款中${refundTypeText}`;
    }
    if (paymentStatus === 'REFUNDED') {
        const refundTypeText = getRefundTypeText(refundType);
        return `已退款${refundTypeText}`;
    }
    if (paymentStatus === 'REFUND_FAILED') {
        const refundTypeText = getRefundTypeText(refundType);
        return `退款失败${refundTypeText}`;
    }

    const statusTexts: Record<string, string> = {
        'PENDING': '待确认',
        'CONFIRMED': '已确认',
        'REJECTED': '已拒绝',
        'CANCELLED': '已取消',
        'CANCELLED_SYSTEM': '系统已取消',
        'CANCELLED_BY_USER': '已取消',
        'CANCELLED_BY_HOST': '已取消',
        'PAID': '已支付',
        'CHECKED_IN': '已入住',
        'COMPLETED': '已完成',
        'PAYMENT_FAILED': '支付失败',
        'REFUND_PENDING': '退款中',
        'REFUNDED': '已退款',
        'REFUND_FAILED': '退款失败'
    }
    return statusTexts[status] || status
}

// 获取退款类型文本
const getRefundTypeText = (refundType?: string): string => {
    if (!refundType) return '';

    switch (refundType) {
        case 'USER_REQUESTED':
            return '（用户申请）';
        case 'HOST_CANCELLED':
            return '（房东取消）';
        case 'ADMIN_INITIATED':
            return '（管理员发起）';
        case 'SYSTEM_AUTOMATIC':
            return '（系统自动）';
        default:
            return '';
    }
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

        if (!orderData.value) return

        // 显示取消中的加载状态
        const loadingInstance = ElMessage({
            type: 'info',
            message: '订单取消中...',
            duration: 0
        })

        try {
            // 使用cancelOrder API方法，确保它使用正确的/status接口
            await cancelOrder(orderData.value.id);

            // 关闭加载消息
            ElMessage.closeAll();

            // 显示更详细的成功消息，并提供跳转链接
            const result = await ElMessageBox.alert(
                '订单已成功取消，您可以继续浏览其他房源或返回订单列表查看',
                '取消成功',
                {
                    confirmButtonText: '查看其他房源',
                    cancelButtonText: '返回订单列表',
                    distinguishCancelAndClose: true,
                    showCancelButton: true
                }
            );

            if (result === 'confirm') {
                router.push('/'); // 前往首页
            } else {
                router.push('/user/bookings'); // 返回订单列表
            }
        } catch (error: any) {
            // 关闭加载消息
            ElMessage.closeAll();

            if (error === 'cancel' || error === 'close') {
                // 用户选择了返回订单列表
                router.push('/user/bookings');
                return;
            }

            console.error('API调用错误:', error);
            ElMessage.error(`取消订单失败: ${error.message || '请稍后重试'}`);
            // 刷新订单数据
            fetchOrderDetail();
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
    router.push('/user/bookings')
}

// 联系客服
const contactCustomerService = () => {
    ElMessage.info('客服热线：400-123-4567，工作时间：9:00-21:00')
}

// 确认申请退款
const confirmRequestRefund = async () => {
    try {
        const result = await ElMessageBox.prompt(
            '请说明退款原因（必填）',
            '申请退款',
            {
                confirmButtonText: '提交申请',
                cancelButtonText: '取消',
                inputPattern: /\S/,
                inputErrorMessage: '退款原因不能为空',
                inputPlaceholder: '请输入退款原因...',
                type: 'warning'
            }
        )

        if (!result.value || !orderData.value) {
            ElMessage.warning('退款原因不能为空')
            return
        }

        const loadingInstance = ElMessage({
            type: 'info',
            message: '正在提交退款申请...',
            duration: 0
        })

        try {
            await requestRefund(orderData.value.id, result.value.trim())
            ElMessage.closeAll()
            ElMessage.success('退款申请已提交，请耐心等待处理')

            // 刷新订单详情
            fetchOrderDetail()
        } catch (error: any) {
            ElMessage.closeAll()
            console.error('申请退款失败:', error)

            let errorMessage = '申请退款失败，请稍后重试'
            if (error.response && error.response.data && error.response.data.message) {
                errorMessage = error.response.data.message
            }
            ElMessage.error(errorMessage)
        }
    } catch (error) {
        // 用户取消了对话框
        if (error !== 'cancel') {
            console.error('退款申请确认框错误:', error)
        }
    }
}



// 添加日期格式化函数
const formatDate = (dateString?: string) => {
    if (!dateString) return '';
    return dayjs(dateString).format('YYYY-MM-DD');
};

const formatDateTime = (dateString?: string) => {
    if (!dateString) return '';
    return dayjs(dateString).format('YYYY-MM-DD HH:mm');
};

// 判断是否可以删除评价 (当前用户是评价者)
const canDeleteReview = (review?: ReviewItem | null): boolean => {
    // 注意：orderData.review 可能没有 userId，如果后端 /api/orders/{id} 返回的ReviewDTO不包含userId
    // 需要确认 getOrderDetail 返回的数据中 Review 对象是否包含 userId
    // 如果不包含，需要调整逻辑，可能需要基于 orderData.guestId 判断
    // return !!currentUserId && orderData.value?.guestId === currentUserId && !!review;
    // 假设后端返回了 userId
    return !!currentUserId && !!review && review.userId === currentUserId;
};

// --- Add function to check if review can be edited ---
const canEditReview = (review?: ReviewItem | null): boolean => {
    // 权限同删除
    return canDeleteReview(review);
};
// --- End function ---

// 处理删除评价
const handleDeleteReview = async (reviewId: number) => {
    if (!reviewId) return;
    try {
        await ElMessageBox.confirm('确定要删除这条评价吗？删除后不可恢复。', '删除确认', {
            confirmButtonText: '确定删除',
            cancelButtonText: '取消',
            type: 'warning',
        });

        isDeletingReview.value = true;
        await deleteReview(reviewId);
        ElMessage.success('评价删除成功');
        // 从订单数据中移除评价信息或重新加载订单详情
        if (orderData.value) {
            orderData.value.review = null; // 直接置空
            // 或者重新获取订单详情: fetchOrderDetail();
        }

    } catch (error: any) {
        if (error !== 'cancel') {
            console.error('删除评价失败:', error);
            const errMsg = error?.response?.data?.message || '删除评价失败，请稍后重试';
            ElMessage.error(errMsg);
        }
    } finally {
        isDeletingReview.value = false;
    }
};

// 处理订单超时事件
const handleOrderTimeout = () => {
    ElMessage.warning('订单已超时，正在自动取消...')
    // 刷新订单状态
    fetchOrderDetail()
}

// 处理订单超时预警事件
const handleOrderWarning = (remainingTime: number) => {
    const minutes = Math.floor(remainingTime / (60 * 1000))
    if (minutes <= 30) {
        ElMessage.warning(`订单即将超时，剩余 ${minutes} 分钟`)
    }
}

// --- Add functions for edit modal ---
// 打开编辑弹窗
const openEditModal = (review: ReviewItem) => {
    currentEditingReview.value = {
        id: review.id,
        rating: review.rating,
        content: review.content,
    };
    isEditModalVisible.value = true;
};

// 处理评价更新事件
const handleReviewUpdated = (updatedReviewData: EditableReviewData) => {
    if (orderData.value && orderData.value.review) {
        orderData.value.review.rating = updatedReviewData.rating;
        orderData.value.review.content = updatedReviewData.content;
        // 可以考虑更新 updateTime 如果需要显示的话
    }
    isEditModalVisible.value = false; // 关闭弹窗
};
// --- End functions ---

onMounted(() => {
    fetchOrderDetail()
})
</script>

<style scoped>
.order-detail-container {
    max-width: 800px;
    margin: 0 auto;
    padding: 40px 20px;
    background-color: #f9f9f9;
}

.loading-container,
.error-container {
    padding: 40px;
    text-align: center;
    background-color: white;
    border-radius: 12px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
}

.order-content {
    background-color: white;
    border-radius: 12px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
    padding: 32px;
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
    font-weight: 600;
    color: #333;
}

.order-status-flow {
    margin-bottom: 32px;
    padding: 24px;
    background-color: #fafafa;
    border-radius: 8px;
}

.order-summary-card {
    background-color: #f8f9fa;
    border-radius: 12px;
    padding: 24px;
    margin-bottom: 32px;
    transition: transform 0.3s ease, box-shadow 0.3s ease;
    overflow: hidden;
}

.order-summary-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}

.header-right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 8px;
}

.card-header h2 {
    margin: 0;
    font-size: 18px;
    color: #333;
    font-weight: 600;
}

.order-number {
    color: #666;
    font-size: 14px;
}

.status-tag {
    font-weight: 600;
}

.homestay-info {
    display: flex;
    gap: 20px;
}

.homestay-image {
    width: 160px;
    height: 120px;
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

.homestay-details h3 {
    margin: 0 0 12px 0;
    font-size: 18px;
    font-weight: 600;
    color: #333;
}

.homestay-details p {
    margin: 8px 0;
    color: #666;
    display: flex;
    align-items: center;
    gap: 6px;
}

.homestay-details i {
    color: #409EFF;
}

.guest-info-inline {
    margin-top: 20px;
    padding-top: 20px;
    border-top: 1px solid #eee;
}

.info-row {
    display: flex;
    gap: 32px;
    margin-bottom: 12px;
}

.guest-info-inline .info-item {
    display: flex;
    margin-bottom: 8px;
}

.guest-info-inline .label {
    width: 80px;
    color: #666;
    font-weight: 500;
}

.status-notice {
    margin-bottom: 32px;
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

.price-details-section {
    margin-bottom: 32px;
    background-color: #fafafa;
    border-radius: 12px;
    padding: 24px;
}

.refund-details-section {
    margin-bottom: 32px;
    background-color: #fff3e0;
    border-radius: 12px;
    padding: 24px;
    border: 1px solid #ffcc02;
}

h2 {
    font-size: 18px;
    margin-bottom: 16px;
    font-weight: 600;
    color: #333;
    position: relative;
    display: inline-block;
}

h2::after {
    content: '';
    position: absolute;
    bottom: -4px;
    left: 0;
    width: 30px;
    height: 2px;
    background-color: #409EFF;
    border-radius: 2px;
}

.info-item {
    display: flex;
    margin-bottom: 12px;
}

.info-item .label {
    width: 100px;
    color: #666;
    font-weight: 500;
}

.price-item {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
    padding: 8px 0;
}

.price-total {
    display: flex;
    justify-content: space-between;
    font-weight: bold;
    padding-top: 16px;
    border-top: 1px solid #eee;
    margin-top: 16px;
    font-size: 18px;
    color: #333;
}

.price-total span:last-child {
    color: #ff6b6b;
    font-size: 20px;
}

.refund-amount {
    color: #f39c12;
    font-weight: 600;
    font-size: 16px;
}

.refund-item {
    background-color: #fff9f0;
    border-radius: 6px;
    padding: 8px 12px;
    margin: 8px 0;
}

.refund-item .refund-amount {
    color: #e67e22;
    font-weight: 700;
}

.final-amount {
    background-color: #f0f9ff;
    border-radius: 8px;
    padding: 12px;
    border: 1px solid #3b82f6;
}

.final-amount span:last-child {
    color: #3b82f6;
}

.action-buttons {
    display: flex;
    gap: 16px;
    justify-content: center;
    margin-top: 40px;
}

.action-buttons .el-button {
    min-width: 120px;
}



@media (max-width: 768px) {
    .homestay-info {
        flex-direction: column;
    }

    .homestay-image {
        width: 100%;
        height: 180px;
        margin-bottom: 16px;
    }

    .action-buttons {
        flex-direction: column;
    }
}

.review-details-section {
    background-color: #fafafa;
    border-radius: 12px;
    padding: 24px;
    margin-bottom: 32px;
}

.review-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}

.review-header h2 {
    margin-bottom: 0;
}

.review-actions {
    display: flex;
    gap: 10px;
}

.delete-review-btn {
    margin-left: 16px;
}

.review-content .info-item {
    display: flex;
    margin-bottom: 12px;
}

.review-content .label {
    width: 100px;
    color: #666;
    font-weight: 500;
}

.review-content p {
    margin: 0;
    flex: 1;
}

.host-response-detail {
    margin-top: 15px;
    padding-top: 15px;
    border-top: 1px dashed #dcdfe6;
}

.host-response-detail h4 {
    margin: 0 0 8px 0;
    color: #409EFF;
}

.host-response-detail p {
    margin-bottom: 5px;
}

.host-response-detail .response-time {
    font-size: 12px;
    color: #909399;
    text-align: right;
}
</style>