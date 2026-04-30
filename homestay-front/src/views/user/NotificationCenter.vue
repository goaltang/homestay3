<template>
    <div class="notification-center">
        <h1 class="text-2xl font-semibold mb-6">通知中心</h1>

        <!-- 操作与筛选 -->
        <div class="flex justify-between items-center mb-4">
            <el-radio-group v-model="filterStatus" @change="handleFilterChange">
                <el-radio-button label="all">全部</el-radio-button>
                <el-radio-button label="unread">未读</el-radio-button>
                <el-radio-button label="read">已读</el-radio-button>
            </el-radio-group>
            <el-button type="primary" plain @click="handleMarkAllRead"
                :disabled="loading || userStore.unreadNotificationCount === 0">
                全部标记为已读
            </el-button>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
            <el-skeleton :rows="4" animated v-for="i in 3" :key="i" />
        </div>

        <!-- 空状态 -->
        <div v-else-if="notifications.length === 0" class="empty-container">
            <el-empty :description="emptyText" />
        </div>

        <!-- 通知卡片列表 -->
        <div v-else class="notification-list">
            <div v-for="notification in notifications" :key="notification.id"
                class="notification-card"
                :class="{ 'notification-unread': !notification.read }"
                @click="handleNotificationClick(notification)">

                <div class="notification-icon" :class="`icon-${getCategory(notification.type)}`">
                    <el-icon :size="22">
                        <component :is="getIcon(notification.type)" />
                    </el-icon>
                </div>

                <div class="notification-body">
                    <div class="notification-header">
                        <el-tag size="small" :type="getTagType(notification.type)" effect="plain">
                            {{ formatType(notification.type) }}
                        </el-tag>
                        <span class="notification-time">{{ formatDate(notification.createdAt, 'YYYY-MM-DD HH:mm') }}</span>
                    </div>
                    <div class="notification-content" :class="{ 'font-semibold': !notification.read }">
                        {{ notification.content }}
                    </div>
                </div>

                <div class="notification-actions" @click.stop>
                    <el-button v-if="!notification.read" type="success" link size="small"
                        @click="handleMarkRead(notification.id)" :loading="markingIds.has(notification.id)">
                        标记已读
                    </el-button>
                    <el-button type="danger" link size="small" @click="handleDelete(notification.id)"
                        :loading="deletingIds.has(notification.id)">
                        删除
                    </el-button>
                </div>
            </div>
        </div>

        <!-- 分页 -->
        <div class="mt-6 flex justify-center" v-if="totalNotifications > pageSize">
            <el-pagination background layout="prev, pager, next" :total="totalNotifications" :page-size="pageSize"
                :current-page="currentPage" @current-change="handlePageChange" />
        </div>

    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import {
    ElRadioGroup, ElRadioButton, ElTag, ElButton, ElPagination,
    ElMessage, ElMessageBox, ElSkeleton, ElEmpty, ElIcon,
} from 'element-plus';
import {
    Bell, ChatDotRound, Goods, House, Star, Tickets
} from '@element-plus/icons-vue';
import { useUserStore } from '@/stores/user';
import { formatDate } from '@/utils/format';
import {
    getNotifications, markAsRead, markAllAsRead, deleteNotification,
} from '@/services/notificationService';
import type { NotificationDto } from '@/types/notification';

const router = useRouter();
const userStore = useUserStore();

const notifications = ref<NotificationDto[]>([]);
const loading = ref(true);
const currentPage = ref(1);
const pageSize = ref(10);
const totalNotifications = ref(0);
const filterStatus = ref<'all' | 'read' | 'unread'>('all');
const markingIds = ref<Set<number>>(new Set());
const deletingIds = ref<Set<number>>(new Set());

const emptyText = computed(() => {
    const map = { all: '暂无通知', unread: '暂无未读通知', read: '暂无已读通知' };
    return map[filterStatus.value];
});

// --- 通知分类与图标映射 ---
const getCategory = (type?: string) => {
    if (!type) return 'system';
    if (type.startsWith('BOOKING') || type.startsWith('ORDER') || type.startsWith('REFUND') || type.startsWith('PAYMENT')) return 'order';
    if (type.startsWith('NEW_MESSAGE')) return 'message';
    if (type.startsWith('NEW_REVIEW') || type.startsWith('REVIEW')) return 'review';
    if (type.startsWith('HOMESTAY')) return 'homestay';
    if (type.startsWith('COUPON')) return 'coupon';
    return 'system';
};

const getIcon = (type?: string) => {
    const category = getCategory(type);
    const iconMap: Record<string, any> = {
        order: Tickets,
        message: ChatDotRound,
        review: Star,
        homestay: House,
        coupon: Goods,
        system: Bell,
    };
    return iconMap[category] || Bell;
};

const getTagType = (type?: string): any => {
    const category = getCategory(type);
    const typeMap: Record<string, any> = {
        order: 'primary',
        message: 'success',
        review: 'warning',
        homestay: 'info',
        coupon: 'danger',
        system: '',
    };
    return typeMap[category] || '';
};

const formatType = (type?: string) => {
    if (!type) return '系统';
    const map: Record<string, string> = {
        BOOKING_REQUEST: '预订请求',
        BOOKING_ACCEPTED: '预订确认',
        BOOKING_REJECTED: '预订被拒',
        BOOKING_CANCELLED: '预订取消',
        BOOKING_REMINDER: '入住提醒',
        REVIEW_REMINDER: '评价提醒',
        ORDER_CONFIRMED: '订单确认',
        PAYMENT_RECEIVED: '收款通知',
        ORDER_CANCELLED_BY_HOST: '订单取消',
        ORDER_CANCELLED_BY_GUEST: '订单取消',
        ORDER_COMPLETED: '订单完成',
        ORDER_STATUS_CHANGED: '订单状态',
        REFUND_REQUESTED: '退款申请',
        REFUND_APPROVED: '退款通过',
        REFUND_REJECTED: '退款被拒',
        REFUND_COMPLETED: '退款完成',
        NEW_MESSAGE: '新消息',
        NEW_REVIEW: '新评价',
        REVIEW_REPLIED: '评价回复',
        HOMESTAY_APPROVED: '房源通过',
        HOMESTAY_REJECTED: '房源被拒',
        HOMESTAY_SUBMITTED: '房源审核',
        PASSWORD_CHANGED: '账号安全',
        EMAIL_VERIFIED: '账号安全',
        SYSTEM_ANNOUNCEMENT: '系统公告',
        WELCOME_MESSAGE: '欢迎',
        COUPON_EXPIRING: '优惠券',
        COUPON_ISSUED: '优惠券',
    };
    return map[type] || '系统';
};

// --- API 调用与逻辑 ---
const fetchNotifications = async () => {
    if (!userStore.isAuthenticated) {
        loading.value = false;
        return;
    }
    loading.value = true;
    try {
        const params: { page: number; size: number; isRead?: boolean } = {
            page: currentPage.value,
            size: pageSize.value,
        };
        if (filterStatus.value !== 'all') {
            params.isRead = filterStatus.value === 'read';
        }
        const response = await getNotifications(params);
        notifications.value = response.data.content;
        totalNotifications.value = response.data.totalElements;
    } catch (error) {
        console.error('获取通知失败:', error);
        ElMessage.error('加载通知失败，请稍后重试');
        notifications.value = [];
        totalNotifications.value = 0;
    } finally {
        loading.value = false;
    }
};

const handleMarkRead = async (id: number) => {
    markingIds.value.add(id);
    try {
        await markAsRead(id);
        const idx = notifications.value.findIndex(n => n.id === id);
        if (idx !== -1) {
            notifications.value[idx].read = true;
        }
        await userStore.fetchUnreadCount();
        ElMessage.success('已标记为已读');
    } catch (error) {
        console.error('标记已读失败:', error);
        ElMessage.error('操作失败');
    } finally {
        markingIds.value.delete(id);
    }
};

const handleMarkAllRead = async () => {
    try {
        const response = await markAllAsRead();
        ElMessage.success(`成功标记 ${response.markedCount} 条通知为已读`);
        notifications.value.forEach(n => n.read = true);
        await userStore.fetchUnreadCount();
    } catch (error) {
        console.error('全部标记已读失败:', error);
        ElMessage.error('操作失败');
    }
};

const handleDelete = async (id: number) => {
    try {
        await ElMessageBox.confirm('确定要删除这条通知吗？此操作不可恢复。', '确认删除', {
            confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
        });
        deletingIds.value.add(id);
        await deleteNotification(id);
        notifications.value = notifications.value.filter(n => n.id !== id);
        totalNotifications.value = Math.max(0, totalNotifications.value - 1);
        await userStore.fetchUnreadCount();
        ElMessage.success('通知已删除');
    } catch (error: any) {
        if (error !== 'cancel') {
            console.error('删除通知失败:', error);
            ElMessage.error('删除失败');
        }
    } finally {
        deletingIds.value.delete(id);
    }
};

const handleNotificationClick = async (notification: NotificationDto) => {
    // 标记已读
    if (!notification.read) {
        try {
            await markAsRead(notification.id);
            notification.read = true;
            await userStore.fetchUnreadCount();
        } catch (e) { /* ignore */ }
    }

    // 跳转
    const { entityType, entityId, type } = notification;
    if (!entityType || !entityId) {
        // 无跳转目标的类型，根据 type 推断
        if (type?.startsWith('NEW_MESSAGE')) {
            router.push('/user/notifications'); // 暂留在通知中心
            return;
        }
        return;
    }

    let path = '';
    try {
        const id = parseInt(entityId, 10);
        if (isNaN(id)) throw new Error('Invalid entity ID');

        switch (entityType) {
            case 'BOOKING':
            case 'ORDER':
                path = `/orders/${id}`;
                break;
            case 'HOMESTAY':
                path = `/homestays/${id}`;
                break;
            case 'REVIEW':
                path = `/homestays/${id}`; // 跳到房源详情，用户自行看评价
                break;
            case 'USER':
                path = '/user/profile';
                break;
            case 'MESSAGE':
                // 如果有消息中心页面，跳过去
                path = '/user/notifications';
                break;
            default:
                console.log(`未知实体类型 ${entityType}，不跳转`);
        }

        if (path) router.push(path);
    } catch (e) {
        console.error('无法解析跳转链接:', e);
    }
};

const handlePageChange = (newPage: number) => {
    currentPage.value = newPage;
    fetchNotifications();
};

const handleFilterChange = () => {
    currentPage.value = 1;
    fetchNotifications();
};

onMounted(() => {
    fetchNotifications();
    userStore.fetchUnreadCount();
});
</script>

<style scoped>
.notification-center {
    max-width: 900px;
    margin: 0 auto;
    padding: 20px;
}

.loading-container {
    padding: 24px;
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.empty-container {
    padding: 48px 0;
    text-align: center;
}

.notification-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.notification-card {
    display: flex;
    align-items: flex-start;
    gap: 16px;
    padding: 16px 20px;
    background: #fff;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.2s ease;
}

.notification-card:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
    transform: translateY(-1px);
}

.notification-unread {
    background: linear-gradient(135deg, #fff8f0 0%, #fff 100%);
    border-color: #ffe4c4;
}

.notification-icon {
    flex-shrink: 0;
    width: 44px;
    height: 44px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
}

.icon-order { background: linear-gradient(135deg, #409eff 0%, #79bbff 100%); }
.icon-message { background: linear-gradient(135deg, #67c23a 0%, #95d475 100%); }
.icon-review { background: linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%); }
.icon-homestay { background: linear-gradient(135deg, #909399 0%, #b1b3b8 100%); }
.icon-coupon { background: linear-gradient(135deg, #f56c6c 0%, #fab6b6 100%); }
.icon-system { background: linear-gradient(135deg, #a0cfff 0%, #c6e2ff 100%); }

.notification-body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.notification-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
}

.notification-time {
    font-size: 12px;
    color: #c0c4cc;
    flex-shrink: 0;
}

.notification-content {
    font-size: 14px;
    color: #606266;
    line-height: 1.6;
    word-break: break-all;
}

.notification-actions {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 4px;
    flex-shrink: 0;
}

.notification-actions :deep(.el-button) {
    padding: 4px 0;
}

@media (max-width: 768px) {
    .notification-card {
        flex-direction: column;
        gap: 12px;
    }

    .notification-actions {
        flex-direction: row;
        width: 100%;
        justify-content: flex-end;
    }
}
</style>
