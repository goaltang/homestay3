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

            <!-- 提醒信息 -->
            <div class="order-notice">
                <el-alert title="请在2小时内完成支付" type="warning" description="超时未支付订单将被自动取消" show-icon :closable="false" />
            </div>

            <!-- 订单信息卡片 -->
            <div class="order-summary-card">
                <div class="card-header">
                    <h2>房源信息</h2>
                    <span class="homestay-id">房源编号: {{ orderData.homestayId }}</span>
                </div>
                <div class="homestay-info">
                    <div class="homestay-image">
                        <img :src="processImageUrl(orderData.imageUrl)" alt="房源图片">
                        <div class="image-overlay" @click="showFullImage">
                            <i class="el-icon-zoom-in"></i> 查看更多图片
                        </div>
                    </div>
                    <div class="homestay-details">
                        <h3>{{ orderData.homestayTitle }}</h3>
                        <p class="homestay-address"><i class="el-icon-location-outline"></i> {{ orderData.address }}</p>
                        <p class="date-highlight"><i class="el-icon-date"></i> {{ formatDateRange(orderData.checkInDate,
                            orderData.checkOutDate) }}
                        </p>
                        <p><i class="el-icon-user"></i> {{ orderData.guestCount }}位房客 · {{ orderData.nights }}晚</p>
                        <div class="host-info" v-if="hostInfo">
                            <img :src="hostInfo.avatar || 'https://picsum.photos/100/100'" class="host-avatar"
                                alt="房东头像">
                            <div class="host-details">
                                <div class="host-name">房东: {{ hostInfo.name || '房东' }}</div>
                                <div class="host-rating">
                                    <el-rate v-model="hostInfo.rating" disabled show-score text-color="#ff9900" />
                                </div>
                                <div class="host-contact">
                                    <el-button type="text" size="small">
                                        <i class="el-icon-message"></i> 联系房东
                                    </el-button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 入住退房信息 -->
            <div class="checkin-checkout-section">
                <div class="checkin-info">
                    <h3>入住信息</h3>
                    <div class="time-info">
                        <i class="el-icon-time"></i> 入住时间: 14:00后
                    </div>
                    <div class="date-info">
                        <i class="el-icon-date"></i> 入住日期: {{ formatDate(orderData.checkInDate) }}
                    </div>
                </div>
                <div class="checkout-info">
                    <h3>退房信息</h3>
                    <div class="time-info">
                        <i class="el-icon-time"></i> 退房时间: 12:00前
                    </div>
                    <div class="date-info">
                        <i class="el-icon-date"></i> 退房日期: {{ formatDate(orderData.checkOutDate) }}
                    </div>
                </div>
            </div>

            <!-- 订单规则 -->
            <div class="order-rules-section">
                <h2>订单规则</h2>
                <div class="rules-content">
                    <div class="rule-item">
                        <i class="el-icon-warning"></i>
                        <span>2小时内未支付，订单将被自动取消</span>
                    </div>
                    <div class="rule-item">
                        <i class="el-icon-warning"></i>
                        <span>入住前24小时可免费取消</span>
                    </div>
                    <div class="rule-item">
                        <i class="el-icon-warning"></i>
                        <span>入住当天取消将收取首晚房费</span>
                    </div>
                </div>
            </div>

            <!-- 旅客信息 -->
            <div class="guest-info-section">
                <h2>旅客信息</h2>
                <div class="guest-info-content">
                    <div class="info-display" v-if="!editingUserInfo">
                        <div class="info-item">
                            <span class="label">联系人:</span>
                            <span>{{ guestInfo.name || '未设置' }}</span>
                        </div>
                        <div class="info-item">
                            <span class="label">联系电话:</span>
                            <span>{{ guestInfo.phone || '未设置' }}</span>
                        </div>
                        <div class="info-item">
                            <span class="label">留言:</span>
                            <span>{{ guestInfo.message || '无' }}</span>
                        </div>
                        <div class="edit-button">
                            <el-button type="primary" plain size="small" @click="editingUserInfo = true">
                                <i class="el-icon-edit"></i> 修改信息
                            </el-button>
                        </div>
                    </div>
                    <el-form v-else :model="guestInfo" label-position="top">
                        <el-form-item label="联系人姓名">
                            <el-input v-model="guestInfo.name" placeholder="请输入联系人姓名" />
                        </el-form-item>
                        <el-form-item label="联系电话">
                            <el-input v-model="guestInfo.phone" placeholder="请输入联系电话" />
                        </el-form-item>
                        <el-form-item label="给房东的留言">
                            <el-input v-model="guestInfo.message" type="textarea" placeholder="告诉房东您的到达时间或特殊需求"
                                rows="3" />
                        </el-form-item>
                        <div class="form-actions">
                            <el-button @click="cancelEdit">取消</el-button>
                            <el-button type="primary" @click="saveUserInfo">保存</el-button>
                        </div>
                    </el-form>
                </div>
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
                <!-- 添加价格保障说明 -->
                <div class="price-guarantee">
                    <i class="el-icon-check"></i>
                    <span>价格透明保障：无任何隐藏费用，最终支付金额即为上述总价</span>
                </div>
            </div>

            <!-- 用户协议 -->
            <div class="agreement-section">
                <el-checkbox v-model="agreementChecked">我已阅读并同意</el-checkbox>
                <a href="javascript:void(0)" @click="showAgreement" class="agreement-link">《民宿预订服务协议》</a>
                <a href="javascript:void(0)" @click="showCancellationPolicy" class="agreement-link">《取消政策》</a>
            </div>

            <!-- 提交按钮 -->
            <div class="submit-section">
                <div class="total-price-display">
                    <span class="total-price-label">总价:</span>
                    <span class="total-price-value">¥{{ orderData.totalAmount }}</span>
                </div>
                <el-button type="primary" :loading="submitting" :disabled="!agreementChecked" @click="submitOrder">
                    提交订单
                </el-button>
                <el-button @click="goBack">返回</el-button>
            </div>

            <!-- 协议弹窗 -->
            <el-dialog v-model="agreementDialogVisible" title="民宿预订服务协议" width="60%" :before-close="handleCloseDialog">
                <div class="agreement-content">
                    <p>本协议是您与民宿平台之间关于民宿预订的相关事宜的约定。</p>
                    <h3>一、订单规则</h3>
                    <p>1. 订单确认后，需在2小时内完成支付，否则订单将被自动取消。</p>
                    <p>2. 入住前24小时可免费取消订单，入住当天取消将收取首晚房费。</p>
                    <h3>二、入住规则</h3>
                    <p>1. 入住时间为14:00后，退房时间为12:00前。</p>
                    <p>2. 请爱护房屋内设施，如有损坏需照价赔偿。</p>
                    <h3>三、其他事项</h3>
                    <p>平台将对您的信息进行保密，未经您的同意不会向任何第三方提供您的个人信息。</p>
                </div>
                <template #footer>
                    <span class="dialog-footer">
                        <el-button @click="agreementDialogVisible = false">关闭</el-button>
                        <el-button type="primary" @click="agreeToTerms">
                            我已阅读并同意
                        </el-button>
                    </span>
                </template>
            </el-dialog>

            <!-- 取消政策弹窗 -->
            <el-dialog v-model="cancellationDialogVisible" title="取消政策" width="60%" :before-close="handleCloseDialog">
                <div class="cancellation-content">
                    <h3>灵活取消政策</h3>
                    <p>入住前24小时可全额退款。</p>
                    <p>入住当天取消，收取首晚房费。</p>
                    <p>如需取消订单，请在"我的订单"中操作。</p>
                </div>
                <template #footer>
                    <span class="dialog-footer">
                        <el-button @click="cancellationDialogVisible = false">关闭</el-button>
                    </span>
                </template>
            </el-dialog>
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
import { getHomestayById } from '@/api/homestay'

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
    hostId: number
    hostName: string
}

// 定义旅客信息类型
interface GuestInfo {
    name: string
    phone: string
    message: string
}

// HostInfo类型定义，移除不再需要的id属性
interface HostInfo {
    name: string;
    avatar: string;
    rating: number;
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const userStore = useUserStore()

const loading = ref(true)
const submitting = ref(false)
const orderData = ref<OrderData | null>(null)

// 初始化旅客信息
const guestInfo = reactive<GuestInfo>({
    name: '',
    phone: '',
    message: ''
})

// 房东信息
const hostInfo = ref<HostInfo>({
    name: '房东名称',
    avatar: '',
    rating: 4.5
})

// 用户协议相关
const agreementChecked = ref(false)
const agreementDialogVisible = ref(false)
const cancellationDialogVisible = ref(false)

const editingUserInfo = ref(false)

// 格式化单个日期
const formatDate = (dateString: string) => {
    if (!dateString) return ''
    const date = new Date(dateString)
    return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
}

// 格式化日期范围 - 优化现有函数
const formatDateRange = (checkIn: string, checkOut: string) => {
    if (!checkIn || !checkOut) return ''

    const checkInDate = new Date(checkIn)
    const checkOutDate = new Date(checkOut)

    const options: Intl.DateTimeFormatOptions = { month: 'long', day: 'numeric' }
    return `${checkInDate.toLocaleDateString('zh-CN', options)} - ${checkOutDate.toLocaleDateString('zh-CN', options)}`
}

// 显示完整图片
const showFullImage = () => {
    ElMessage.info('查看更多房源图片功能将在未来版本开放')
}

// 取消编辑用户信息
const cancelEdit = () => {
    // 恢复原来的信息
    populateUserInfo()
    editingUserInfo.value = false
}

// 保存用户信息
const saveUserInfo = () => {
    if (!guestInfo.name || !guestInfo.phone) {
        ElMessage.warning('联系人姓名和电话不能为空')
        return
    }
    editingUserInfo.value = false
    ElMessage.success('旅客信息已更新')
}

// 显示协议
const showAgreement = () => {
    agreementDialogVisible.value = true
}

// 显示取消政策
const showCancellationPolicy = () => {
    cancellationDialogVisible.value = true
}

// 关闭弹窗
const handleCloseDialog = () => {
    agreementDialogVisible.value = false
    cancellationDialogVisible.value = false
}

// 同意条款
const agreeToTerms = () => {
    agreementChecked.value = true
    agreementDialogVisible.value = false
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

    if (!agreementChecked.value) {
        ElMessage.warning('请阅读并同意相关协议')
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
            // 先从userStore尝试获取缓存的用户信息
            if (userStore.userInfo) {
                guestInfo.name = userStore.userInfo.realName || userStore.userInfo.username || ''
                guestInfo.phone = userStore.userInfo.phone || ''
                return
            }

            // 如果没有缓存的用户信息，则从API获取
            const userId = authStore.user?.id
            if (userId) {
                const response = await getUserInfo(userId)
                if (response && response.data) {
                    guestInfo.name = response.data.realName || response.data.username || ''
                    guestInfo.phone = response.data.phone || ''
                }
            } else {
                // 尝试获取当前用户信息
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

// 更新fetchHostInfo函数来获取真实的房东信息
const fetchHostInfo = async () => {
    if (!orderData.value?.homestayId) return;

    try {
        // 获取房源详情，现在应包含完整的房东信息
        const response = await getHomestayById(orderData.value.homestayId);
        if (response && response.data) {
            // 更新房源详细信息 (保留原有逻辑，但现在应有更全数据)
            if (orderData.value) {
                orderData.value = {
                    ...orderData.value,
                    address: response.data.address || orderData.value.address,
                    imageUrl: response.data.coverImage || response.data.images?.[0] || orderData.value.imageUrl,
                    homestayTitle: response.data.title || orderData.value.homestayTitle,
                };
                console.log('获取到房源详情:', response.data);
            }

            // 直接使用getHomestayById返回的房东信息
            if (response.data.ownerUsername) { // 检查是否有房东信息
                hostInfo.value = {
                    name: response.data.ownerName || response.data.ownerUsername || '房东',
                    avatar: processImageUrl(response.data.ownerAvatar) || '', // 使用processImageUrl处理头像
                    rating: response.data.ownerRating || 4.5 // 使用返回的评分，提供默认值
                };
                console.log('已更新房东信息:', hostInfo.value);
            } else {
                console.warn('房源详情中未找到房东信息');
            }
        }
    } catch (error) {
        console.error('获取房源和房东信息失败:', error);
        // 可以保留一个简单的备用逻辑，但避免错误的API调用
        // 例如，如果API调用失败，显示默认信息
        hostInfo.value = {
            name: '房东信息加载失败',
            avatar: '',
            rating: 0
        };
    }
}

// 修改initOrderData函数，添加价格计算逻辑
const initOrderData = () => {
    // 从URL参数获取订单数据
    const query = route.query
    if (!query.homestayId || !query.checkIn || !query.checkOut || !query.guests) {
        ElMessage.error('参数不完整，无法创建订单')
        return null
    }

    // 确保homestayId被正确转换为数字
    const homestayId = Number(query.homestayId)
    if (isNaN(homestayId) || homestayId <= 0) {
        ElMessage.error('无效的房源ID')
        return null
    }

    // 从查询参数中获取基础数据
    const price = Number(query.price) || 0
    const nights = Number(query.nights) || 1
    const cleaningFee = Number(query.cleaningFee) || 0
    const serviceFee = Number(query.serviceFee) || 0

    // 计算基础金额和总金额
    const calculatedBaseAmount = price * nights
    const calculatedTotalAmount = calculatedBaseAmount + cleaningFee + serviceFee

    // 构建订单数据对象，使用计算后的金额
    return {
        homestayId: homestayId,
        homestayTitle: query.title as string || '未命名房源',
        imageUrl: query.image as string || '',
        address: query.address as string || '地址未知',
        checkInDate: query.checkIn as string,
        checkOutDate: query.checkOut as string,
        nights: nights,
        guestCount: Number(query.guests) || 1,
        price: price,
        baseAmount: calculatedBaseAmount, // 使用计算值
        cleaningFee: cleaningFee,
        serviceFee: serviceFee,
        totalAmount: calculatedTotalAmount, // 使用计算值
        hostId: Number(query.hostId) || 0,
        hostName: query.hostName as string || ''
    } as OrderData
}

// 添加一个函数来处理图片URL
const processImageUrl = (url: string | undefined): string => {
    if (!url) {
        return 'https://picsum.photos/400/300'; // 默认占位图片
    }

    // 如果是完整URL，直接返回
    if (url.startsWith('http')) {
        return url;
    }

    // 如果是相对路径，添加API前缀
    if (url.startsWith('/uploads/') || url.startsWith('/homestays/') || url.startsWith('/images/')) {
        return `/api${url}`;
    }

    // 如果是其他格式，假设是文件名，添加完整路径
    return `/api/uploads/homestays/${url}`;
}

onMounted(async () => {
    try {
        // 初始化订单数据
        console.log('初始化订单数据，查询参数:', route.query)
        const initialData = initOrderData()
        if (initialData) {
            orderData.value = initialData
            console.log('订单数据已初始化:', orderData.value)

            // 获取用户信息和房东信息
            await Promise.all([populateUserInfo(), fetchHostInfo()]);
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
    position: relative;
    width: 180px;
    height: 140px;
    border-radius: 8px;
    overflow: hidden;
}

.homestay-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.3s ease;
}

.homestay-image:hover img {
    transform: scale(1.05);
}

.image-overlay {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    background: rgba(0, 0, 0, 0.5);
    color: white;
    padding: 4px 8px;
    font-size: 12px;
    text-align: center;
    cursor: pointer;
}

.homestay-details h3 {
    font-size: 18px;
    margin-bottom: 8px;
}

.homestay-details p {
    color: #666;
    margin-bottom: 4px;
}

.date-highlight {
    font-weight: bold;
    color: #409EFF;
}

.host-info {
    display: flex;
    align-items: center;
    margin-top: 12px;
    padding-top: 8px;
    border-top: 1px dashed #eee;
}

.host-avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    margin-right: 10px;
}

.host-name {
    font-weight: bold;
    margin-bottom: 2px;
}

.checkin-checkout-section {
    display: flex;
    gap: 20px;
    margin: 20px 0;
    padding: 15px;
    background-color: #f9f9f9;
    border-radius: 8px;
}

.checkin-info,
.checkout-info {
    flex: 1;
    padding: 10px;
    border-radius: 5px;
    background-color: white;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.checkin-info h3,
.checkout-info h3 {
    margin-top: 0;
    color: #303133;
    font-size: 16px;
    margin-bottom: 10px;
}

.time-info,
.date-info {
    margin: 5px 0;
    display: flex;
    align-items: center;
}

.time-info i,
.date-info i {
    margin-right: 5px;
    color: #909399;
}

.order-rules-section {
    margin: 20px 0;
    padding: 15px;
    background-color: #fff;
    border: 1px solid #e6e6e6;
    border-radius: 8px;
}

.rules-content {
    margin-top: 10px;
}

.rule-item {
    display: flex;
    align-items: flex-start;
    margin-bottom: 10px;
}

.rule-item i {
    color: #E6A23C;
    margin-right: 10px;
    margin-top: 3px;
}

.guest-info-section,
.price-details-section,
.payment-method-section {
    margin: 20px 0;
    padding: 20px;
    background-color: white;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.payment-option-content {
    display: flex;
    align-items: center;
    gap: 8px;
}

.payment-icon {
    width: 20px;
    height: 20px;
}

.agreement-section {
    margin: 20px 0;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
}

.agreement-link {
    color: #409EFF;
    margin: 0 8px;
    text-decoration: none;
}

.agreement-link:hover {
    text-decoration: underline;
}

.agreement-content,
.cancellation-content {
    max-height: 400px;
    overflow-y: auto;
}

.agreement-content h3,
.cancellation-content h3 {
    margin-top: 20px;
    margin-bottom: 10px;
    font-size: 16px;
}

.agreement-content p,
.cancellation-content p {
    margin-bottom: 8px;
    line-height: 1.5;
}

.total-price-display {
    margin-bottom: 15px;
    font-size: 18px;
}

.total-price-label {
    font-weight: normal;
    margin-right: 10px;
}

.total-price-value {
    font-weight: bold;
    color: #f56c6c;
    font-size: 24px;
}

.submit-section {
    margin-top: 30px;
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
}

.submit-section .el-button {
    padding: 12px 24px;
    font-size: 16px;
}

.submit-section .el-button--primary {
    width: 200px;
}

.order-notice {
    margin-bottom: 20px;
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

.price-guarantee {
    margin-top: 12px;
    padding: 8px;
    background-color: #f0f9eb;
    border-radius: 4px;
    display: flex;
    align-items: center;
    font-size: 13px;
    color: #67c23a;
}

.price-guarantee i {
    margin-right: 8px;
}

.payment-options {
    margin-top: 16px;
}

/* 新增和修改的样式 */
.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}

.homestay-id {
    color: #909399;
    font-size: 14px;
}

.homestay-address {
    display: flex;
    align-items: center;
    gap: 5px;
    color: #606266;
}

.host-contact {
    margin-top: 5px;
}

.info-display {
    background-color: #f9f9f9;
    padding: 15px;
    border-radius: 8px;
}

.info-item {
    display: flex;
    margin-bottom: 10px;
}

.info-item .label {
    width: 100px;
    color: #606266;
    font-weight: 500;
}

.edit-button {
    text-align: right;
    margin-top: 10px;
}

.form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 15px;
}

.guest-info-content {
    margin-top: 15px;
}

/* 响应式调整 */
@media (max-width: 768px) {
    .info-item {
        flex-direction: column;
    }

    .info-item .label {
        width: 100%;
        margin-bottom: 5px;
    }
}
</style>