<template>
    <div class="notification-center container mx-auto p-4 md:p-6">
        <h1 class="text-2xl font-semibold mb-6">通知中心 (房东)</h1> <!-- 标题区分 -->

        <!-- 操作与筛选 -->
        <div class="flex flex-wrap justify-between items-center gap-4 mb-4">
            <div class="flex items-center gap-4">
                <el-radio-group v-model="filterStatus" @change="handleFilterChange">
                    <el-radio-button label="all">全部状态</el-radio-button>
                    <el-radio-button label="unread">未读</el-radio-button>
                    <el-radio-button label="read">已读</el-radio-button>
                </el-radio-group>

                <el-select v-model="filterType" placeholder="筛选类型" clearable @change="handleFilterChange"
                    style="width: 180px;">
                    <el-option v-for="item in notificationTypes" :key="item.value" :label="item.label"
                        :value="item.value" />
                </el-select>
            </div>

            <el-button type="primary" plain @click="handleMarkAllRead"
                :disabled="loading || userStore.unreadNotificationCount === 0">
                全部标记为已读
            </el-button>
        </div>

        <!-- 恢复 el-table -->
        <el-table :data="notifications" v-loading="loading" style="width: 100%" empty-text="暂无通知" row-key="id">
            <el-table-column label="状态" width="100">
                <template #default="{ row }">
                    <!-- 使用 row.read 判断 -->
                    <el-tag :type="row.read ? 'info' : 'warning'" size="small">
                        {{ row.read ? '已读' : '未读' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="内容">
                <template #default="{ row }">
                    <!-- 修改内容展示方式 -->
                    <div @click="handleNotificationClick(row)" class="cursor-pointer hover:text-blue-600"
                        :class="{ 'font-semibold': !row.read }">
                        <component :is="renderNotificationContent(row)" />
                    </div>
                </template>
            </el-table-column>
            <el-table-column label="时间" width="180">
                <template #default="{ row }">
                    {{ formatDate(row.createdAt, 'YYYY-MM-DD HH:mm') }}
                </template>
            </el-table-column>
            <el-table-column label="操作" width="150" align="center">
                <template #default="{ row }">
                    <!-- 使用 row.read 判断 -->
                    <el-button v-if="!row.read" type="success" link size="small" @click="handleMarkRead(row.id)"
                        :disabled="loading">
                        标记已读
                    </el-button>
                    <el-button type="danger" link size="small" @click="handleDelete(row.id)" :disabled="loading">
                        删除
                    </el-button>
                </template>
            </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="mt-6 flex justify-center" v-if="!loading && totalNotifications > pageSize">
            <el-pagination background layout="prev, pager, next" :total="totalNotifications" :page-size="pageSize"
                :current-page="currentPage" @current-change="handlePageChange" />
        </div>

        <!-- 恢复加载和空状态 (如果需要用 ElEmpty) -->
        <div v-if="loading && notifications.length === 0" class="text-center py-10">
            <el-icon class="is-loading text-4xl">
                <Loading />
            </el-icon>
            <p>加载中...</p>
        </div>
        <div v-else-if="!loading && notifications.length === 0" class="text-center py-10 text-gray-500">
            <el-empty description="暂无通知"></el-empty>
        </div>

    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import {
    ElRadioGroup,
    ElRadioButton,
    ElTable,
    ElTableColumn,
    ElTag,
    ElButton,
    ElPagination,
    ElMessage,
    ElMessageBox,
    vLoading,
    ElIcon,
    ElEmpty,
    ElSelect,
    ElOption
} from 'element-plus';
import { Loading } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import {
    getNotifications,
    markAsRead,
    markAllAsRead,
    deleteNotification,
} from '@/services/notificationService'; // 复用 service
import type { NotificationDto } from '@/types/notification'; // 复用类型
import { useUserStore } from '@/stores/user';
import { formatDate } from '@/utils/format';
import { VNode, h } from 'vue';
import { RouterLink } from 'vue-router';

const router = useRouter();
const userStore = useUserStore();

// --- 状态定义 (与用户版一致) ---
const notifications = ref<NotificationDto[]>([]);
const loading = ref(true);
const currentPage = ref(1);
const pageSize = ref(10);
const totalNotifications = ref(0);
const filterStatus = ref<'all' | 'read' | 'unread'>('all');
const filterType = ref<string>('');

// 新增：通知类型选项
const notificationTypes = ref([
    { label: '所有类型', value: '' },
    { label: '预订请求', value: 'BOOKING_REQUEST' },
    { label: '预订已接受', value: 'BOOKING_ACCEPTED' },
    { label: '预订已拒绝', value: 'BOOKING_REJECTED' },
    { label: '预订已取消', value: 'BOOKING_CANCELLED' },
    { label: '订单已完成', value: 'ORDER_COMPLETED' },
    { label: '收到付款', value: 'PAYMENT_RECEIVED' }, // 根据实际情况调整 key
    { label: '新评价', value: 'NEW_REVIEW' },
    { label: '评价被回复', value: 'REVIEW_REPLIED' },
    { label: '系统通知', value: 'SYSTEM_ANNOUNCEMENT' }, // 假设有系统通知
    { label: '欢迎消息', value: 'WELCOME_MESSAGE' },
    // 根据后端 NotificationType 枚举添加更多类型...
]);

// --- 新增：渲染通知内容的函数 ---
const renderNotificationContent = (notification: NotificationDto): VNode => {
    const { type, actorUsername, entityTitle, content, entityType, entityId } = notification;
    const actorLink = actorUsername ? h(RouterLink, { class: 'font-medium text-blue-700 hover:underline', to: `/user/profile/${notification.actorId}` }, () => actorUsername) : '系统';
    let entityLink: VNode | string = entityTitle || '';
    let entityPath = '';

    // 生成实体链接 (根据房东视角)
    if (entityType && entityId) {
        try {
            const id = parseInt(entityId, 10);
            if (!isNaN(id)) {
                switch (entityType) {
                    case 'BOOKING':
                    case 'ORDER':
                        entityPath = `/host/orders?highlightOrderId=${id}`;
                        entityLink = entityTitle ? h(RouterLink, { class: 'font-medium text-blue-700 hover:underline', to: entityPath }, () => entityTitle) : `订单 #${id}`;
                        break;
                    case 'HOMESTAY':
                        entityPath = `/host/homestay/edit/${id}`;
                        entityLink = entityTitle ? h(RouterLink, { class: 'font-medium text-blue-700 hover:underline', to: entityPath }, () => entityTitle) : `房源 #${id}`;
                        break;
                    case 'REVIEW':
                        entityPath = `/host/reviews?highlightReviewId=${id}`;
                        // 对于评价，entityTitle 可能是房源名，链接指向评价管理
                        const reviewLinkText = entityTitle ? `对房源 '${entityTitle}' 的评价` : `评价 #${id}`;
                        entityLink = h(RouterLink, { class: 'font-medium text-blue-700 hover:underline', to: entityPath }, () => reviewLinkText);
                        break;
                    case 'USER': // 如果 entity 是用户，actor 可能是系统或其他用户
                        entityPath = `/user/profile/${id}`; // 假设可以查看用户资料
                        entityLink = entityTitle ? h(RouterLink, { class: 'font-medium text-blue-700 hover:underline', to: entityPath }, () => entityTitle) : `用户 #${id}`;
                        break;
                }
            }
        } catch (e) {
            console.error("Error creating entity link:", e);
        }
    }

    // 根据通知类型构建 VNode
    switch (type) {
        case 'BOOKING_REQUEST':
            return h('span', ['您收到了来自用户 ', actorLink, ' 的新预订请求 (', entityLink, ')，请及时处理。']);
        case 'BOOKING_ACCEPTED': // 需要后端支持并填充 entityTitle 为订单号
            return h('span', ['您接受了用户 ', actorLink, ' 的预订 (', entityLink, ').']);
        case 'BOOKING_REJECTED': // 需要后端支持并填充 entityTitle 为订单号
            return h('span', ['您拒绝了用户 ', actorLink, ' 的预订 (', entityLink, ').']);
        case 'BOOKING_CANCELLED': // entityTitle 可能是订单号
            return h('span', ['订单 ', entityLink, ' 已被用户 ', actorLink, ' 取消。']);
        case 'ORDER_COMPLETED': // entityTitle 可能是订单号
            return h('span', [entityLink, ' 已完成。请尽快结算相关收益。']);
        case 'PAYMENT_RECEIVED': // entityTitle 可能是订单号
            return h('span', ['用户 ', actorLink, ' 已支付 ', entityLink, '。']);
        case 'NEW_REVIEW': // entityTitle 可能是房源名
            return h('span', ['用户 ', actorLink, ' 提交了 ', entityLink, '。']);
        case 'REVIEW_REPLIED': // entityTitle 可能是房源名
            // 这个理论上是给房客的，但如果房东也收到...
            return h('span', ['您回复了用户 ', actorLink, ' 关于 ', entityLink, ' 的评价。']);
        case 'WELCOME_MESSAGE':
            return h('span', [content]); // 欢迎消息直接用原始内容
        case 'HOMESTAY_APPROVED': // 假设 entityTitle 是房源名
            return h('span', ['您的房源 ', entityLink, ' 已通过审核。']);
        case 'HOMESTAY_REJECTED': // 假设 entityTitle 是房源名
            return h('span', ['您的房源 ', entityLink, ' 未通过审核。', content ? ` 原因: ${content}` : '']); // 可选地显示拒绝原因
        // 添加其他通知类型的渲染逻辑...
        default:
            // 使用原始 content 作为后备
            return h('span', content || '通知内容缺失');
    }
};

// --- API 调用与逻辑 (大部分可复用) ---
const fetchNotifications = async () => {
    console.log('[HostNotificationManage] fetchNotifications called');

    if (!userStore.isAuthenticated) {
        console.warn('[HostNotificationManage] 用户未登录，无法获取通知');
        loading.value = false;
        return;
    }
    console.log('[HostNotificationManage] 用户已认证，继续获取通知');

    loading.value = true;
    try {
        const params: { page: number; size: number; isRead?: boolean; type?: string } = {
            page: currentPage.value,
            size: pageSize.value,
        };
        if (filterStatus.value !== 'all') {
            params.isRead = filterStatus.value === 'read';
        }
        if (filterType.value && filterType.value !== '') {
            params.type = filterType.value;
        }
        console.log('[HostNotificationManage] 调用 getNotifications API, params:', params);
        const response = await getNotifications(params);
        console.log('[HostNotificationManage] getNotifications API 响应:', response);

        notifications.value = response.data.content;
        totalNotifications.value = response.data.totalElements;
        console.log('[HostNotificationManage] fetchNotifications 成功，赋值后的 notifications.value:', JSON.stringify(notifications.value));

    } catch (error) {
        console.error('[HostNotificationManage] 获取通知失败 (catch block):', error);
        ElMessage.error('加载通知失败，请稍后重试');
        notifications.value = [];
        totalNotifications.value = 0;
    } finally {
        loading.value = false;
    }
};

const handleMarkRead = async (id: number) => {
    console.log(`[HostNotificationManage] handleMarkRead called for id: ${id}`);
    try {
        console.log(`[HostNotificationManage] Calling markAsRead API for id: ${id}`);
        await markAsRead(id);
        console.log(`[HostNotificationManage] markAsRead API success for id: ${id}`);
        ElMessage.success('已标记为已读');

        console.log(`[HostNotificationManage] Calling fetchUnreadCount`);
        await userStore.fetchUnreadCount();
        console.log(`[HostNotificationManage] fetchUnreadCount finished`);

        console.log(`[HostNotificationManage] Reloading notifications after marking as read...`);
        await fetchNotifications();
        console.log(`[HostNotificationManage] Reload notifications finished`);

    } catch (error) {
        console.error('[HostNotificationManage] 标记已读失败 (catch block):', error);
        ElMessage.error('操作失败，请稍后重试');
    }
};

const handleMarkAllRead = async () => {
    loading.value = true;
    try {
        const response = await markAllAsRead();
        ElMessage.success(`成功标记 ${response.markedCount} 条通知为已读`);
        await fetchNotifications();
        await userStore.fetchUnreadCount();
    } catch (error) {
        console.error('全部标记已读失败:', error);
        ElMessage.error('操作失败，请稍后重试');
        loading.value = false;
    }
};

const handleDelete = async (id: number) => {
    try {
        await ElMessageBox.confirm(
            '确定要删除这条通知吗？此操作不可恢复。',
            '确认删除',
            {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning',
            }
        );
        loading.value = true;
        await deleteNotification(id);
        ElMessage.success('通知已删除');
        await fetchNotifications();
        await userStore.fetchUnreadCount();
    } catch (error: any) {
        if (error !== 'cancel') {
            console.error('删除通知失败:', error);
            ElMessage.error('删除失败，请稍后重试');
        }
        loading.value = false;
    }
};

// --- 核心区别：点击跳转逻辑 --- 
const handleNotificationClick = async (notification: NotificationDto) => {
    console.log('(房东)点击通知:', notification);

    // 点击整行时仍然标记为已读
    if (!notification.read) {
        try {
            await markAsRead(notification.id);
            const index = notifications.value.findIndex(n => n.id === notification.id);
            if (index !== -1) {
                notifications.value[index].read = true;
            }
            await userStore.fetchUnreadCount();
        } catch (error) {
            console.error('点击时标记已读失败:', error);
            // 即使标记失败，也尝试跳转
        }
    }

    // 点击整行时的跳转逻辑保持不变 (作为后备或主要导航)
    const { entityType, entityId } = notification;
    if (!entityType || !entityId) return;

    let path = '';
    try {
        const id = parseInt(entityId, 10);
        if (isNaN(id)) throw new Error('Invalid entity ID');

        switch (entityType) {
            case 'BOOKING':
            case 'ORDER':
                path = `/host/orders?highlightOrderId=${id}`;
                break;
            case 'HOMESTAY':
                path = `/host/homestay/edit/${id}`;
                break;
            case 'REVIEW':
                path = `/host/reviews?highlightReviewId=${id}`;
                break;
            default:
                console.log(`房东视角：未知或无需跳转的实体类型 ${entityType}`);
        }

        if (path) {
            router.push(path);
        }
    } catch (e) {
        console.error('无法解析跳转链接:', e);
    }
};

// --- 事件处理 (分页和筛选不变) ---
const handlePageChange = (newPage: number) => {
    currentPage.value = newPage;
    fetchNotifications();
};

const handleFilterChange = () => {
    currentPage.value = 1;
    fetchNotifications();
};

// --- 生命周期钩子 ---
onMounted(() => {
    console.log('[HostNotificationManage] Component onMounted hook called');
    console.log('[HostNotificationManage] onMounted - isAuthenticated:', userStore.isAuthenticated);
    fetchNotifications();
    userStore.fetchUnreadCount();
});

</script>

<style scoped>
/* 恢复之前的样式 */
.el-table .el-button+.el-button {
    margin-left: 8px;
}

.flex-wrap {
    flex-wrap: wrap;
}

.gap-4 {
    gap: 1rem;
    /* 16px */
}
</style>