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
            </div>

            <!-- 预订状态提示 -->
            <div class="status-notice" v-if="orderData.status === 'PENDING'">
                <el-alert title="等待房东确认" type="warning" description="您的预订申请已提交，正在等待房东确认。房东将在24小时内确认您的预订。" show-icon
                    :closable="false" />
                <div class="countdown-container">
                    <div class="countdown-title">订单确认倒计时</div>
                    <div class="countdown-timer" :class="{ 'urgent': isTimeUrgent(orderData.createTime, 24) }">
                        {{ getCountdownTime(orderData.createTime, 24) }}
                    </div>
                    <div class="countdown-desc">超过24小时未确认，订单将自动取消</div>
                </div>
            </div>

            <div class="status-notice" v-else-if="orderData.status === 'CONFIRMED'">
                <el-alert title="房东已确认预订" type="success" description="房东已确认您的预订，请尽快完成支付以确保您的住宿。" show-icon
                    :closable="false" />
                <div class="countdown-container">
                    <div class="countdown-title">支付倒计时</div>
                    <div class="countdown-timer" :class="{ 'urgent': isTimeUrgent(orderData.updateTime, 2) }">
                        {{ getCountdownTime(orderData.updateTime, 2) }}
                    </div>
                    <div class="countdown-desc">超过2小时未支付，订单将自动取消</div>
                </div>
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

            <!-- 评价详情 (如果已完成且已评价) -->
            <div class="review-details-section" v-if="orderData?.status === 'COMPLETED' && orderData.review">
                <div class="review-header">
                    <h2>我的评价</h2>
                    <div class="review-actions">
                        <el-button v-if="canEditReview(orderData.review)"
                            type="primary" plain size="small"
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
                <el-button type="danger" plain v-if="canCancel" @click="confirmCancel">
                    取消订单
                </el-button>
                <el-button type="primary" v-if="orderData.status === 'CONFIRMED'" @click="goToPayment">
                    立即支付
                </el-button>
            </div>
        </div>

        <!-- Add Edit Modal -->
        <ReviewEditModal 
            v-model:visible="isEditModalVisible"
            :review-data="currentEditingReview"
            @submitted="handleReviewUpdated"
        />
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
import { useUserStore } from '@/stores/user'
import ReviewEditModal from '@/components/ReviewEditModal.vue'

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
    remark?: string
    createTime: string
    updateTime: string
    reviewed?: boolean; // 保留 reviewed 字段
    review?: ReviewItem | null; // 添加 review 字段
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
const getStatusText = (status: string) => {
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
                router.push('/orders'); // 返回订单列表
            }
        } catch (error: any) {
            // 关闭加载消息
            ElMessage.closeAll();

            if (error === 'cancel' || error === 'close') {
                // 用户选择了返回订单列表
                router.push('/orders');
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
    router.push('/user/orders')
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

    // 计算剩余小时、分钟和秒数
    const hours = Math.floor(remainingTime / (60 * 60 * 1000));
    const minutes = Math.floor((remainingTime % (60 * 60 * 1000)) / (60 * 1000));
    const seconds = Math.floor((remainingTime % (60 * 1000)) / 1000);

    return `${hours}小时${minutes}分钟${seconds}秒`;
}

// 判断时间是否紧急（剩余不到30分钟）
const isTimeUrgent = (startTimeStr: string, hoursLimit: number) => {
    const startTime = new Date(startTimeStr).getTime();
    const currentTime = new Date().getTime();
    const limitTime = startTime + hoursLimit * 60 * 60 * 1000;
    const remainingTime = limitTime - currentTime;

    // 如果剩余时间小于30分钟，显示紧急样式
    return remainingTime < 30 * 60 * 1000 && remainingTime > 0;
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
    background-color: #fafafa;
    border-radius: 12px;
    padding: 24px;
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

.action-buttons {
    display: flex;
    gap: 16px;
    justify-content: center;
    margin-top: 40px;
}

.action-buttons .el-button {
    min-width: 120px;
}

.countdown-container {
    margin-top: 15px;
    padding: 20px;
    border-radius: 8px;
    background-color: #f8f9fa;
    text-align: center;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.countdown-title {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 12px;
    color: #333;
}

.countdown-timer {
    font-size: 28px;
    font-weight: bold;
    color: #409eff;
    margin-bottom: 12px;
}

.countdown-timer.urgent {
    color: #f56c6c;
    animation: blink 1s infinite;
}

.countdown-desc {
    font-size: 14px;
    color: #606266;
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