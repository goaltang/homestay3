<template>
    <div class="notifications-page">
        <!-- 页面头部 -->
        <div class="page-header">
            <el-card shadow="never">
                <div class="header-content">
                    <div class="header-left">
                        <h2>通知中心</h2>
                        <p>管理审核通知、系统消息和重要提醒</p>
                    </div>
                    <div class="header-right">
                        <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="badge">
                            <el-button type="primary" @click="markAllAsRead" :disabled="unreadCount === 0">
                                <el-icon>
                                    <Check />
                                </el-icon>
                                全部已读
                            </el-button>
                        </el-badge>
                        <el-button @click="refreshNotifications">
                            <el-icon>
                                <Refresh />
                            </el-icon>
                            刷新
                        </el-button>
                    </div>
                </div>
            </el-card>
        </div>

        <!-- 筛选条件 -->
        <div class="filter-section">
            <el-card shadow="never">
                <el-form :inline="true" :model="filterForm">
                    <el-form-item label="通知类型">
                        <el-select v-model="filterForm.type" placeholder="全部类型" clearable style="width: 200px;">
                            <el-option label="房源提交审核" value="HOMESTAY_SUBMITTED" />
                            <el-option label="房源审核通过" value="HOMESTAY_APPROVED" />
                            <el-option label="房源审核拒绝" value="HOMESTAY_REJECTED" />
                            <el-option label="房源重新提交" value="HOMESTAY_RESUBMITTED" />
                            <el-option label="审核员分配" value="REVIEWER_ASSIGNED" />
                            <el-option label="房源审核中" value="HOMESTAY_UNDER_REVIEW" />
                            <el-option label="系统公告" value="SYSTEM_ANNOUNCEMENT" />
                            <el-option label="欢迎消息" value="WELCOME_MESSAGE" />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="状态">
                        <el-select v-model="filterForm.read" placeholder="全部状态" clearable style="width: 120px;">
                            <el-option label="未读" :value="false" />
                            <el-option label="已读" :value="true" />
                        </el-select>
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" @click="handleSearch">
                            <el-icon>
                                <Search />
                            </el-icon>
                            搜索
                        </el-button>
                        <el-button @click="resetSearch">重置</el-button>
                    </el-form-item>
                </el-form>
            </el-card>
        </div>

        <!-- 通知列表 -->
        <div class="notifications-list">
            <el-card shadow="never">
                <div v-loading="loading">
                    <div v-if="notifications.length === 0" class="empty-state">
                        <el-empty description="暂无通知" />
                    </div>
                    <div v-else class="notification-items">
                        <div v-for="notification in notifications" :key="notification.id" class="notification-item"
                            :class="{ 'unread': !notification.read }" @click="handleNotificationClick(notification)">
                            <div class="notification-icon">
                                <el-icon :class="getNotificationIconClass(notification.type)">
                                    <component :is="getNotificationIcon(notification.type)" />
                                </el-icon>
                            </div>
                            <div class="notification-content">
                                <div class="notification-header">
                                    <h4 class="notification-title">{{ getNotificationTitle(notification.type) }}</h4>
                                    <span class="notification-time">{{ formatTime(notification.createdAt) }}</span>
                                </div>
                                <p class="notification-message">{{ notification.content }}</p>
                                <div class="notification-meta" v-if="notification.entityType === 'HOMESTAY'">
                                    <el-tag size="small" type="info">房源ID: {{ notification.entityId }}</el-tag>
                                </div>
                            </div>
                            <div class="notification-actions">
                                <el-button v-if="!notification.read" type="primary" size="small" text
                                    @click.stop="markAsRead(notification.id)">
                                    标记已读
                                </el-button>
                                <el-button type="danger" size="small" text
                                    @click.stop="deleteNotification(notification.id)">
                                    删除
                                </el-button>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 分页 -->
                <div class="pagination" v-if="total > 0">
                    <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize"
                        :page-sizes="[10, 20, 50]" :total="total" layout="total, sizes, prev, pager, next, jumper"
                        @size-change="handleSizeChange" @current-change="handleCurrentChange" />
                </div>
            </el-card>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
    Check,
    Refresh,
    Bell,
    Document,
    Warning,
    CircleCheck,
    InfoFilled,
    House,
    Search
} from '@element-plus/icons-vue'
import {
    getNotifications,
    getUnreadCount,
    markAsRead as markNotificationAsRead,
    markAllAsRead as markAllNotificationsAsRead,
    deleteNotification as removeNotification,
    type NotificationDto
} from '@/api/notification'
import { useRouter } from 'vue-router'

const router = useRouter()

// 响应式数据
const loading = ref(false)
const notifications = ref<NotificationDto[]>([])
const unreadCount = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 筛选表单
const filterForm = reactive({
    type: '',
    read: null as boolean | null
})

// 方法
const loadNotifications = async () => {
    try {
        loading.value = true
        const params = {
            page: currentPage.value - 1, // 后端从0开始计数
            size: pageSize.value,
            type: filterForm.type || undefined,
            isRead: filterForm.read !== null ? filterForm.read : undefined
        }

        const response = await getNotifications(params)
        notifications.value = response.content
        total.value = response.totalElements
    } catch (error) {
        console.error('获取通知列表失败:', error)
        ElMessage.error('获取通知列表失败')
    } finally {
        loading.value = false
    }
}

const loadUnreadCount = async () => {
    try {
        const response = await getUnreadCount()
        unreadCount.value = response.unreadCount
    } catch (error) {
        console.error('获取未读数量失败:', error)
    }
}

const refreshNotifications = async () => {
    await Promise.all([
        loadNotifications(),
        loadUnreadCount()
    ])
    ElMessage.success('刷新成功')
}

const handleSearch = () => {
    currentPage.value = 1
    loadNotifications()
}

const resetSearch = () => {
    filterForm.type = ''
    filterForm.read = null
    currentPage.value = 1
    loadNotifications()
}

const handleSizeChange = (val: number) => {
    pageSize.value = val
    currentPage.value = 1
    loadNotifications()
}

const handleCurrentChange = (val: number) => {
    currentPage.value = val
    loadNotifications()
}

const markAsRead = async (notificationId: number) => {
    try {
        await markNotificationAsRead(notificationId)
        // 更新本地状态
        const notification = notifications.value.find(n => n.id === notificationId)
        if (notification) {
            notification.read = true
            unreadCount.value = Math.max(0, unreadCount.value - 1)
        }
        ElMessage.success('已标记为已读')
    } catch (error) {
        console.error('标记已读失败:', error)
        ElMessage.error('标记已读失败')
    }
}

const markAllAsRead = async () => {
    if (unreadCount.value === 0) return

    try {
        await ElMessageBox.confirm('确认将所有通知标记为已读？', '确认操作', {
            confirmButtonText: '确认',
            cancelButtonText: '取消',
            type: 'info'
        })

        await markAllNotificationsAsRead()

        // 更新本地状态
        notifications.value.forEach(n => n.read = true)
        unreadCount.value = 0

        ElMessage.success('已全部标记为已读')
    } catch (error) {
        if (error !== 'cancel') {
            console.error('标记全部已读失败:', error)
            ElMessage.error('标记全部已读失败')
        }
    }
}

const deleteNotification = async (notificationId: number) => {
    try {
        await ElMessageBox.confirm('确认删除该通知？', '确认删除', {
            confirmButtonText: '确认',
            cancelButtonText: '取消',
            type: 'warning'
        })

        await removeNotification(notificationId)

        // 更新本地状态
        const index = notifications.value.findIndex(n => n.id === notificationId)
        if (index !== -1) {
            const notification = notifications.value[index]
            if (!notification.read) {
                unreadCount.value = Math.max(0, unreadCount.value - 1)
            }
            notifications.value.splice(index, 1)
            total.value = Math.max(0, total.value - 1)
        }

        ElMessage.success('删除成功')
    } catch (error) {
        if (error !== 'cancel') {
            console.error('删除通知失败:', error)
            ElMessage.error('删除通知失败')
        }
    }
}

const handleNotificationClick = (notification: NotificationDto) => {
    // 如果未读，先标记为已读
    if (!notification.read) {
        markAsRead(notification.id)
    }

    // 根据通知类型和实体类型进行导航
    if (notification.entityType === 'HOMESTAY' && notification.entityId) {
        switch (notification.type) {
            case 'HOMESTAY_SUBMITTED':
            case 'HOMESTAY_RESUBMITTED':
            case 'HOMESTAY_UNDER_REVIEW':
                // 提交相关通知跳转到审核工作台
                router.push('/audit/workbench')
                break
            case 'HOMESTAY_APPROVED':
            case 'HOMESTAY_REJECTED':
                // 审核结果通知跳转到审核历史
                router.push('/audit/history')
                break
            case 'REVIEWER_ASSIGNED':
                // 审核员分配通知跳转到工作台
                router.push('/audit/workbench')
                break
            default:
                // 其他房源相关通知跳转到房源列表
                router.push('/homestays')
        }
    } else if (notification.type === 'SYSTEM_ANNOUNCEMENT' || notification.type === 'WELCOME_MESSAGE') {
        // 系统公告类通知不进行跳转，只标记已读
        ElMessage.info('已查看通知')
    } else {
        // 其他类型通知的默认处理
        ElMessage.info('通知已查看')
    }
}

// 工具方法
const getNotificationIcon = (type: string) => {
    const iconMap: Record<string, any> = {
        'HOMESTAY_SUBMITTED': Document,
        'HOMESTAY_APPROVED': CircleCheck,
        'HOMESTAY_REJECTED': Warning,
        'HOMESTAY_RESUBMITTED': Document,
        'REVIEWER_ASSIGNED': InfoFilled,
        'HOMESTAY_UNDER_REVIEW': House,
        'SYSTEM_ANNOUNCEMENT': Bell,
        'WELCOME_MESSAGE': Bell
    }
    return iconMap[type] || Bell
}

const getNotificationIconClass = (type: string) => {
    const classMap: Record<string, string> = {
        'HOMESTAY_SUBMITTED': 'icon-primary',
        'HOMESTAY_APPROVED': 'icon-success',
        'HOMESTAY_REJECTED': 'icon-danger',
        'HOMESTAY_RESUBMITTED': 'icon-warning',
        'REVIEWER_ASSIGNED': 'icon-info',
        'HOMESTAY_UNDER_REVIEW': 'icon-primary',
        'SYSTEM_ANNOUNCEMENT': 'icon-primary',
        'WELCOME_MESSAGE': 'icon-success'
    }
    return classMap[type] || 'icon-primary'
}

const getNotificationTitle = (type: string) => {
    const titleMap: Record<string, string> = {
        'HOMESTAY_SUBMITTED': '新房源提交审核',
        'HOMESTAY_APPROVED': '房源审核通过',
        'HOMESTAY_REJECTED': '房源审核拒绝',
        'HOMESTAY_RESUBMITTED': '房源重新提交',
        'REVIEWER_ASSIGNED': '审核员分配',
        'HOMESTAY_UNDER_REVIEW': '房源审核中',
        'SYSTEM_ANNOUNCEMENT': '系统公告',
        'WELCOME_MESSAGE': '欢迎消息'
    }
    return titleMap[type] || '通知'
}

const formatTime = (dateTime: string) => {
    const now = new Date()
    const time = new Date(dateTime)
    const diff = now.getTime() - time.getTime()

    if (diff < 60 * 1000) {
        return '刚刚'
    } else if (diff < 60 * 60 * 1000) {
        return `${Math.floor(diff / (60 * 1000))}分钟前`
    } else if (diff < 24 * 60 * 60 * 1000) {
        return `${Math.floor(diff / (60 * 60 * 1000))}小时前`
    } else if (diff < 7 * 24 * 60 * 60 * 1000) {
        return `${Math.floor(diff / (24 * 60 * 60 * 1000))}天前`
    } else {
        return time.toLocaleDateString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        })
    }
}

// 初始化
onMounted(() => {
    loadNotifications()
    loadUnreadCount()
})
</script>

<style scoped lang="scss">
.notifications-page {
    padding: 20px;
    background-color: #f5f7fa;
    min-height: 100vh;

    .page-header {
        margin-bottom: 20px;

        .header-content {
            display: flex;
            justify-content: space-between;
            align-items: center;

            .header-left {
                h2 {
                    margin: 0 0 8px 0;
                    color: #303133;
                    font-size: 24px;
                    font-weight: 600;
                }

                p {
                    margin: 0;
                    color: #909399;
                    font-size: 14px;
                }
            }

            .header-right {
                display: flex;
                align-items: center;
                gap: 12px;

                .badge {
                    .el-button {
                        padding: 8px 16px;
                    }
                }
            }
        }
    }

    .filter-section {
        margin-bottom: 20px;
    }

    .notifications-list {
        .empty-state {
            padding: 60px 0;
            text-align: center;
        }

        .notification-items {
            .notification-item {
                display: flex;
                align-items: flex-start;
                padding: 16px;
                border-bottom: 1px solid #ebeef5;
                cursor: pointer;
                transition: all 0.3s ease;

                &:hover {
                    background-color: #f5f7fa;
                }

                &.unread {
                    background-color: #f0f9ff;
                    border-left: 4px solid #409eff;

                    .notification-title {
                        font-weight: 600;
                    }
                }

                .notification-icon {
                    margin-right: 16px;
                    padding-top: 4px;

                    .el-icon {
                        font-size: 24px;

                        &.icon-primary {
                            color: #409eff;
                        }

                        &.icon-success {
                            color: #67c23a;
                        }

                        &.icon-warning {
                            color: #e6a23c;
                        }

                        &.icon-danger {
                            color: #f56c6c;
                        }

                        &.icon-info {
                            color: #909399;
                        }
                    }
                }

                .notification-content {
                    flex: 1;
                    min-width: 0;

                    .notification-header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        margin-bottom: 8px;

                        .notification-title {
                            margin: 0;
                            font-size: 16px;
                            color: #303133;
                            font-weight: 500;
                        }

                        .notification-time {
                            font-size: 12px;
                            color: #909399;
                            white-space: nowrap;
                        }
                    }

                    .notification-message {
                        margin: 0 0 8px 0;
                        font-size: 14px;
                        color: #606266;
                        line-height: 1.6;
                        word-break: break-word;
                    }

                    .notification-meta {
                        .el-tag {
                            margin-right: 8px;
                        }
                    }
                }

                .notification-actions {
                    margin-left: 16px;
                    display: flex;
                    flex-direction: column;
                    gap: 4px;
                    align-items: flex-end;

                    .el-button {
                        margin: 0;
                        padding: 4px 8px;
                        font-size: 12px;
                    }
                }
            }
        }

        .pagination {
            margin-top: 20px;
            display: flex;
            justify-content: center;
        }
    }
}
</style>