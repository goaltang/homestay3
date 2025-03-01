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
                <el-tab-pane label="待支付" name="pending"></el-tab-pane>
                <el-tab-pane label="待入住" name="upcoming"></el-tab-pane>
                <el-tab-pane label="已完成" name="completed"></el-tab-pane>
                <el-tab-pane label="已取消" name="cancelled"></el-tab-pane>
            </el-tabs>

            <div class="booking-list">
                <el-card v-for="booking in filteredBookings" :key="booking.id" class="booking-card">
                    <div class="booking-info">
                        <div class="booking-image">
                            <img :src="booking.homestay.images[0]" :alt="booking.homestay.title" />
                        </div>
                        <div class="booking-details">
                            <h3 class="booking-title">{{ booking.homestay.title }}</h3>
                            <div class="booking-location">{{ booking.homestay.city }}, {{ booking.homestay.country }}
                            </div>
                            <div class="booking-dates">
                                {{ formatDate(booking.checkIn) }} 至 {{ formatDate(booking.checkOut) }}
                                <span class="booking-nights">({{ booking.nights }}晚)</span>
                            </div>
                            <div class="booking-guests">{{ booking.guests }}位房客</div>
                            <div class="booking-status" :class="booking.status">
                                {{ getStatusText(booking.status) }}
                            </div>
                        </div>
                        <div class="booking-price">
                            <div class="price-total">¥{{ booking.totalPrice }}</div>
                            <div class="price-detail">
                                <div>¥{{ booking.homestay.pricePerNight }} x {{ booking.nights }}晚</div>
                                <div>清洁费: ¥{{ booking.cleaningFee }}</div>
                                <div>服务费: ¥{{ booking.serviceFee }}</div>
                            </div>
                        </div>
                    </div>
                    <div class="booking-actions">
                        <el-button v-if="booking.status === 'pending'" type="primary" @click="payBooking(booking.id)">
                            去支付
                        </el-button>
                        <el-button v-if="['pending', 'upcoming'].includes(booking.status)" type="danger"
                            @click="cancelBooking(booking.id)">
                            取消预订
                        </el-button>
                        <el-button v-if="booking.status === 'completed'" type="primary"
                            @click="reviewBooking(booking.id)">
                            评价
                        </el-button>
                        <el-button @click="viewHomestayDetails(booking.homestay.id)">
                            查看民宿
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
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

// 定义类型
interface Homestay {
    id: number;
    title: string;
    city: string;
    country: string;
    pricePerNight: number;
    images: string[];
}

interface Booking {
    id: number;
    homestay: Homestay;
    checkIn: string;
    checkOut: string;
    nights: number;
    guests: number;
    totalPrice: number;
    cleaningFee: number;
    serviceFee: number;
    status: 'pending' | 'upcoming' | 'completed' | 'cancelled';
    createdAt: string;
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
        // 尝试从后端获取预订记录
        // 如果后端没有实现，使用模拟数据
        try {
            const response = await request.get('/api/bookings')
            bookings.value = response.data
        } catch (error) {
            console.error('获取预订记录失败，使用模拟数据:', error)

            // 使用模拟数据
            bookings.value = [
                {
                    id: 1,
                    homestay: {
                        id: 1,
                        title: '大理古城树屋',
                        city: '大理',
                        country: '中国',
                        pricePerNight: 688,
                        images: ['https://picsum.photos/800/600?random=1']
                    },
                    checkIn: '2023-07-15',
                    checkOut: '2023-07-20',
                    nights: 5,
                    guests: 2,
                    totalPrice: 3440,
                    cleaningFee: 68,
                    serviceFee: 516,
                    status: 'completed',
                    createdAt: '2023-06-20T10:30:00'
                },
                {
                    id: 2,
                    homestay: {
                        id: 2,
                        title: '杭州山顶树屋',
                        city: '杭州',
                        country: '中国',
                        pricePerNight: 1288,
                        images: ['https://picsum.photos/800/600?random=2']
                    },
                    checkIn: '2023-12-25',
                    checkOut: '2023-12-30',
                    nights: 5,
                    guests: 4,
                    totalPrice: 6440,
                    cleaningFee: 128,
                    serviceFee: 966,
                    status: 'upcoming',
                    createdAt: '2023-11-15T14:20:00'
                },
                {
                    id: 3,
                    homestay: {
                        id: 3,
                        title: '三亚海景房',
                        city: '三亚',
                        country: '中国',
                        pricePerNight: 1688,
                        images: ['https://picsum.photos/800/600?random=3']
                    },
                    checkIn: '2023-08-01',
                    checkOut: '2023-08-05',
                    nights: 4,
                    guests: 6,
                    totalPrice: 6752,
                    cleaningFee: 168,
                    serviceFee: 1012,
                    status: 'cancelled',
                    createdAt: '2023-07-01T09:15:00'
                }
            ]
        }
    } catch (error) {
        console.error('获取预订记录失败:', error)
        ElMessage.error('获取预订记录失败')
    } finally {
        loading.value = false
    }
}

const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

const getStatusText = (status: string) => {
    const statusMap: Record<string, string> = {
        pending: '待支付',
        upcoming: '待入住',
        completed: '已完成',
        cancelled: '已取消'
    }
    return statusMap[status] || status
}

const viewHomestayDetails = (id: number) => {
    router.push(`/homestay/${id}`)
}

const payBooking = (id: number) => {
    ElMessage.info('支付功能正在开发中')
}

const cancelBooking = (id: number) => {
    ElMessageBox.confirm('确定要取消此预订吗？', '取消预订', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        // 模拟取消预订
        const index = bookings.value.findIndex(booking => booking.id === id)
        if (index !== -1) {
            bookings.value[index].status = 'cancelled'
            ElMessage.success('预订已取消')
        }
    }).catch(() => {
        // 用户取消操作
    })
}

const reviewBooking = (id: number) => {
    ElMessage.info('评价功能正在开发中')
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
    display: inline-block;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 14px;
    margin-top: 8px;
}

.booking-status.pending {
    background-color: #e6a23c;
    color: white;
}

.booking-status.upcoming {
    background-color: #409eff;
    color: white;
}

.booking-status.completed {
    background-color: #67c23a;
    color: white;
}

.booking-status.cancelled {
    background-color: #f56c6c;
    color: white;
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