<template>
    <div class="notification-center container mx-auto p-4 md:p-6">
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

        <!-- 通知列表表格 -->
        <el-table :data="notifications" v-loading="loading" style="width: 100%" empty-text="暂无通知" row-key="id">
            <el-table-column label="状态" width="100">
                <template #default="{ row }">
                    <el-tag :type="row.read ? 'info' : 'warning'" size="small">
                        {{ row.read ? '已读' : '未读' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="内容">
                <template #default="{ row }">
                    <span @click="handleNotificationClick(row)" class="cursor-pointer hover:text-blue-600"
                        :class="{ 'font-semibold': !row.read }">
                        {{ row.content }}
                    </span>
                </template>
            </el-table-column>
            <el-table-column label="时间" width="180">
                <template #default="{ row }">
                    {{ formatDate(row.createdAt, 'YYYY-MM-DD HH:mm') }}
                </template>
            </el-table-column>
            <el-table-column label="操作" width="150" align="center">
                <template #default="{ row }">
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
        <div class="mt-6 flex justify-center" v-if="totalNotifications > pageSize">
            <el-pagination background layout="prev, pager, next" :total="totalNotifications" :page-size="pageSize"
                :current-page="currentPage" @current-change="handlePageChange" />
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
    vLoading, // 导入 v-loading 指令
} from 'element-plus';
import { useRouter } from 'vue-router';
import {
    getNotifications,
    markAsRead,
    markAllAsRead,
    deleteNotification,
} from '@/services/notificationService';
import type { NotificationDto } from '@/types/notification';
import { useUserStore } from '@/stores/user';
import { formatDate } from '@/utils/format'; // 使用现有的 format.ts

const router = useRouter();
const userStore = useUserStore();

// --- 状态定义 ---
const notifications = ref<NotificationDto[]>([]);
const loading = ref(true);
const currentPage = ref(1);
const pageSize = ref(10);
const totalNotifications = ref(0);
const filterStatus = ref<'all' | 'read' | 'unread'>('all');

// --- 计算属性 ---
// (可以添加一个 getter 来访问 store 中的 unreadCount，如果模板中多处使用)
// const unreadCount = computed(() => userStore.unreadNotificationCount);

// --- API 调用与逻辑 ---
const fetchNotifications = async () => {
    if (!userStore.isAuthenticated) {
        console.warn('用户未登录，无法获取通知');
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
        console.log('[UserNotificationCenter] getNotifications API 响应:', response);

        // 修复 Linter 错误：从 response.data 获取数据
        notifications.value = response.data.content;
        totalNotifications.value = response.data.totalElements;

        console.log('[UserNotificationCenter] fetchNotifications 成功，赋值后的 notifications.value:', JSON.stringify(notifications.value));
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
    console.log(`[UserNotificationCenter] handleMarkRead called for id: ${id}`);
    loading.value = true; // 开始时设置 loading
    try {
        console.log(`[UserNotificationCenter] Calling markAsRead API for id: ${id}`);
        await markAsRead(id);
        console.log(`[UserNotificationCenter] markAsRead API success for id: ${id}`);
        ElMessage.success('已标记为已读');

        console.log(`[UserNotificationCenter] Calling fetchUnreadCount`);
        await userStore.fetchUnreadCount();
        console.log(`[UserNotificationCenter] fetchUnreadCount finished`);

        console.log(`[UserNotificationCenter] Reloading notifications after marking as read...`);
        // fetchNotifications 内部会管理自己的 loading 状态，但外层需要确保最后重置
        await fetchNotifications();
        console.log(`[UserNotificationCenter] Reload notifications finished`);

    } catch (error) {
        console.error('[UserNotificationCenter] 标记已读失败 (catch block):', error);
        ElMessage.error('操作失败，请稍后重试');
        // 错误时也需要重置 loading，由 finally 处理
    } finally {
        loading.value = false; // 确保无论成功或失败都重置 loading
        console.log(`[UserNotificationCenter] handleMarkRead finished for id: ${id}, loading set to false`); // 添加日志
    }
};

const handleMarkAllRead = async () => {
    loading.value = true;
    try {
        const response = await markAllAsRead();
        ElMessage.success(`成功标记 ${response.markedCount} 条通知为已读`);
        await fetchNotifications();
        await userStore.fetchUnreadCount(); // 更新 store
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

        loading.value = true; // 可以在 confirm 后再设置 loading
        await deleteNotification(id);
        ElMessage.success('通知已删除');
        await fetchNotifications();
        await userStore.fetchUnreadCount(); // 更新 store

    } catch (error: any) {
        if (error !== 'cancel') { // 用户点击取消时不显示错误
            console.error('删除通知失败:', error);
            ElMessage.error('删除失败，请稍后重试');
        }
        loading.value = false; // 确保 loading 状态被重置
    }
};

const handleNotificationClick = async (notification: NotificationDto) => {
    console.log('点击通知:', notification);

    if (!notification.read) {
        // 不直接调用 handleMarkRead，避免重复的 loading 和 message
        try {
            await markAsRead(notification.id);
            const index = notifications.value.findIndex(n => n.id === notification.id);
            if (index !== -1) {
                notifications.value[index].read = true;
            }
            await userStore.fetchUnreadCount(); // 更新 store
        } catch (error) {
            console.error('点击时标记已读失败:', error);
            // 这里可以不提示错误，避免干扰跳转
        }
    }

    // 根据类型和ID跳转
    const { entityType, entityId } = notification;
    if (!entityType || !entityId) return;

    let path = '';
    try {
        const id = parseInt(entityId, 10);
        if (isNaN(id)) throw new Error('Invalid entity ID');

        switch (entityType) {
            case 'BOOKING':
            case 'ORDER': // 兼容可能的类型名
                path = `/orders/${id}`; // 跳转到订单详情
                break;
            case 'HOMESTAY':
                path = `/homestays/${id}`; // 跳转到房源详情
                break;
            case 'REVIEW':
                // 可能跳转到房源详情的评论区，或者单独的评价页面
                // path = `/homestays/${relatedHomestayId}?tab=reviews`; // 示例，需要额外信息
                console.warn('评论通知的跳转链接暂未实现');
                break;
            case 'USER':
                // 可能跳转到用户资料页？
                // path = `/user/profile/${id}`; // 示例
                console.warn('用户相关通知的跳转链接暂未实现');
                break;
            case 'MESSAGE':
                // 可能跳转到聊天/消息页面
                // path = '/messages/${id}'; // 示例
                console.warn('消息通知的跳转链接暂未实现');
                break;
            default:
                console.log(`未知实体类型 ${entityType}，无法跳转`);
        }

        if (path) {
            router.push(path);
        }
    } catch (e) {
        console.error('无法解析跳转链接:', e);
    }
};

// --- 事件处理 ---
const handlePageChange = (newPage: number) => {
    currentPage.value = newPage;
    fetchNotifications();
};

const handleFilterChange = () => {
    currentPage.value = 1; // 切换筛选时回到第一页
    fetchNotifications();
};

// --- 生命周期钩子 ---
onMounted(() => {
    console.log('Notification Center mounted');
    fetchNotifications();
    userStore.fetchUnreadCount();
});

</script>

<style scoped>
/* 可以在这里添加更多自定义样式 */
.el-table .el-button+.el-button {
    margin-left: 8px;
}
</style>