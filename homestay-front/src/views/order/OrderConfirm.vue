<template>
    <div class="order-confirm-container">
        <div v-if="loading" class="loading-container">
            <el-skeleton :rows="10" animated />
        </div>
        <div v-else-if="!orderData" class="error-container">
            <el-empty description="订单数据不存在">
                <template #description>
                    <p>未找到订单数据，请返回房源详情页重新预订</p>
                </template>
                <el-button type="primary" @click="goBack">返回房源详情</el-button>
            </el-empty>
        </div>
        <div v-else class="order-content">
            <h1>确认并支付</h1>

            <!-- 订单信息卡片 -->
            <div class="order-summary-card">
                <div class="homestay-info">
                    <div class="homestay-image">
                        <img :src="orderData.imageUrl || 'https://picsum.photos/400/300'" alt="房源图片">
                    </div>
                    <div class="homestay-details">
                        <h3>{{ orderData.homestayTitle }}</h3>
                        <p>{{ orderData.address }}</p>
                        <p>{{ formatDateRange(orderData.checkInDate, orderData.checkOutDate) }}</p>
                        <p>{{ orderData.guestCount }}位房客</p>
                    </div>
                </div>
            </div>

            <!-- 旅客信息 -->
            <div class="guest-info-section">
                <h2>旅客信息</h2>
                <el-form :model="guestInfo" label-position="top">
                    <el-form-item label="联系人姓名">
                        <el-input v-model="guestInfo.name" placeholder="请输入联系人姓名" />
                    </el-form-item>
                    <el-form-item label="联系电话">
                        <el-input v-model="guestInfo.phone" placeholder="请输入联系电话" />
                    </el-form-item>
                    <el-form-item label="给房东的留言">
                        <el-input v-model="guestInfo.message" type="textarea" placeholder="告诉房东您的到达时间或特殊需求" rows="3" />
                    </el-form-item>
                </el-form>
            </div>

            <!-- 价格详情 -->
            <div class="price-details-section">
                <h2>价格详情</h2>
                <div class="price-breakdown">
                    <div class="price-row">
                        <span>{{ orderData.price }}元 x {{ orderData.nights }}晚</span>
                        <span>{{ orderData.baseAmount }}元</span>
                    </div>
                    <div class="price-row" v-if="orderData.cleaningFee">
                        <span>清洁费</span>
                        <span>{{ orderData.cleaningFee }}元</span>
                    </div>
                    <div class="price-row" v-if="orderData.serviceFee">
                        <span>服务费</span>
                        <span>{{ orderData.serviceFee }}元</span>
                    </div>
                    <div class="price-row total">
                        <span>总价</span>
                        <span>{{ orderData.totalAmount }}元</span>
                    </div>
                </div>
            </div>

            <!-- 支付方式 -->
            <div class="payment-method-section">
                <h2>支付方式</h2>
                <div class="payment-options">
                    <el-radio-group v-model="paymentMethod">
                        <el-radio label="alipay">支付宝</el-radio>
                        <el-radio label="wechat">微信支付</el-radio>
                        <el-radio label="credit">信用卡支付</el-radio>
                    </el-radio-group>
                </div>
            </div>

            <!-- 提交按钮 -->
            <div class="submit-section">
                <el-button type="primary" :loading="submitting" @click="submitOrder">提交订单</el-button>
                <el-button @click="goBack">返回</el-button>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createOrder } from '../../api/order'
import { useAuthStore } from '../../stores/auth'
import { useUserStore } from '../../stores/user'
import { getUserInfo } from '../../api/user'

// 定义订单数据类型
interface OrderData {
    homestayId: number
    homestayTitle: string
    imageUrl: string
    address: string
    checkInDate: string
    checkOutDate: string
    nights: number
    guestCount: number
    price: number
    baseAmount: number
    cleaningFee?: number
    serviceFee?: number
    totalAmount: number
}

// 定义旅客信息类型
interface GuestInfo {
    name: string
    phone: string
    message: string
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const userStore = useUserStore()

const loading = ref(true)
const submitting = ref(false)
const orderData = ref<OrderData | null>(null)
const paymentMethod = ref('alipay')

// 初始化旅客信息
const guestInfo = reactive<GuestInfo>({
    name: '',
    phone: '',
    message: ''
})

// 格式化日期范围
const formatDateRange = (checkIn: string, checkOut: string) => {
    if (!checkIn || !checkOut) return ''
    return `${checkIn} 至 ${checkOut}`
}

// 初始化订单数据
const initOrderData = () => {
    // 从URL参数获取订单数据
    const query = route.query
    if (!query.homestayId || !query.checkIn || !query.checkOut || !query.guests) {
        ElMessage.error('参数不完整，无法创建订单')
        return null
    }

    // 这里应该是从查询参数中构建订单数据
    return {
        homestayId: Number(query.homestayId),
        homestayTitle: query.title as string || '未命名房源',
        imageUrl: query.image as string || '',
        address: query.address as string || '地址未知',
        checkInDate: query.checkIn as string,
        checkOutDate: query.checkOut as string,
        nights: Number(query.nights) || 1,
        guestCount: Number(query.guests) || 1,
        price: Number(query.price) || 0,
        baseAmount: Number(query.baseAmount) || 0,
        cleaningFee: Number(query.cleaningFee) || 0,
        serviceFee: Number(query.serviceFee) || 0,
        totalAmount: Number(query.totalAmount) || 0
    } as OrderData
}

// 提交订单
const submitOrder = async () => {
    if (!orderData.value) {
        ElMessage.error('订单数据不存在')
        return
    }

    if (!guestInfo.name || !guestInfo.phone) {
        ElMessage.warning('请填写联系人信息')
        return
    }

    try {
        submitting.value = true
        const orderRequestData = {
            homestayId: orderData.value.homestayId,
            checkInDate: orderData.value.checkInDate,
            checkOutDate: orderData.value.checkOutDate,
            guestCount: orderData.value.guestCount,
            totalPrice: orderData.value.totalAmount,
            guestName: guestInfo.name,
            guestPhone: guestInfo.phone,
            message: guestInfo.message
        }

        const response = await createOrder(orderRequestData)
        if (response && response.data && response.data.id) {
            ElMessage.success('订单创建成功')
            router.push(`/orders/submit-success/${response.data.id}`)
        } else {
            ElMessage.error('订单创建失败')
        }
    } catch (error) {
        console.error('提交订单出错:', error)
        ElMessage.error('订单提交失败，请稍后重试')
    } finally {
        submitting.value = false
    }
}

// 返回上一页
const goBack = () => {
    router.back()
}

// 自动填充用户信息
const populateUserInfo = async () => {
    try {
        if (authStore.isAuthenticated && userStore.isAuthenticated) {
            const userId = userStore.userInfo?.id || authStore.user?.id
            if (userId) {
                const response = await getUserInfo(userId)
                if (response && response.data) {
                    guestInfo.name = response.data.realName || response.data.username || ''
                    guestInfo.phone = response.data.phone || ''
                }
            } else {
                // 如果没有userId，尝试获取当前用户信息
                const currentResponse = await getUserInfo()
                if (currentResponse && currentResponse.data) {
                    guestInfo.name = currentResponse.data.realName || currentResponse.data.username || ''
                    guestInfo.phone = currentResponse.data.phone || ''
                }
            }
        }
    } catch (error) {
        console.error('获取用户信息失败:', error)
    }
}

onMounted(async () => {
    try {
        // 初始化订单数据
        console.log('初始化订单数据，查询参数:', route.query)
        const initialData = initOrderData()
        if (initialData) {
            orderData.value = initialData
            console.log('订单数据已初始化:', orderData.value)
            await populateUserInfo()
        }
    } catch (error) {
        console.error('初始化订单数据失败:', error)
        ElMessage.error('加载订单数据失败')
    } finally {
        loading.value = false
    }
})
</script>

<style scoped>
.order-confirm-container {
    max-width: 1000px;
    margin: 0 auto;
    padding: 20px;
}

.loading-container,
.error-container {
    min-height: 400px;
    display: flex;
    justify-content: center;
    align-items: center;
}

.order-content {
    margin-top: 20px;
}

h1 {
    font-size: 28px;
    font-weight: 700;
    margin-bottom: 24px;
}

h2 {
    font-size: 20px;
    font-weight: 600;
    margin-bottom: 16px;
}

.order-summary-card,
.guest-info-section,
.price-details-section,
.payment-method-section {
    background-color: #fff;
    border-radius: 8px;
    padding: 24px;
    margin-bottom: 24px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.homestay-info {
    display: flex;
    gap: 20px;
}

.homestay-image {
    width: 160px;
    height: 120px;
    border-radius: 8px;
    overflow: hidden;
}

.homestay-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.homestay-details h3 {
    font-size: 18px;
    margin-bottom: 8px;
}

.homestay-details p {
    color: #666;
    margin-bottom: 4px;
}

.price-breakdown {
    margin-top: 16px;
}

.price-row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
}

.price-row.total {
    border-top: 1px solid #eee;
    padding-top: 12px;
    font-weight: bold;
    font-size: 18px;
}

.payment-options {
    margin-top: 16px;
}

.submit-section {
    margin-top: 32px;
    display: flex;
    justify-content: center;
    gap: 16px;
}

.submit-section .el-button {
    min-width: 120px;
}
</style>