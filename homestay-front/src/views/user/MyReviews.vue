<template>
    <div class="my-reviews-container">
        <div class="page-header">
            <h1>我的评价</h1>
        </div>

        <div v-if="loading" class="loading-container">
            <el-skeleton :rows="5" animated />
        </div>

        <div v-else-if="reviews.length === 0" class="empty-container">
            <el-empty description="您还没有发表过评价">
                <el-button type="primary" @click="goToHomepage">去逛逛房源</el-button>
            </el-empty>
        </div>

        <div v-else class="review-list">
            <el-card v-for="(review, index) in reviews" :key="review.id" class="review-card">
                <div class="review-card-header">
                    <div class="homestay-info">
                        <span class="homestay-title">{{ review.homestayTitle || '民宿信息加载中...' }}</span>
                        <!-- 可以考虑添加民宿图片或链接 -->
                    </div>
                    <div class="review-date">
                        评价于: {{ formatDate(review.createTime) }}
                    </div>
                </div>
                <div class="review-card-content">
                    <div class="rating-line">
                        <span class="rating-label">评分:</span>
                        <el-rate :model-value="review.rating" disabled size="small" text-color="#ff9900" />
                    </div>
                    <div class="review-text">
                        <p>{{ review.content }}</p>
                    </div>
                    <div class="host-response" v-if="review.response">
                        <el-divider direction="horizontal" />
                        <div class="response-header">房东回复:</div>
                        <p class="response-content">{{ review.response }}</p>
                        <div class="response-time" v-if="review.responseTime">({{ formatDateTime(review.responseTime)
                            }})</div>
                    </div>
                </div>
                <div class="review-card-footer">
                    <!-- Edit Button -->
                    <el-button
                        v-if="canEditReview(review)" 
                        type="primary" 
                        plain 
                        size="small" 
                        @click="openEditModal(review)"
                    >
                        修改评价
                    </el-button>
                    <!-- Delete Button -->
                    <el-button
                        v-if="canDeleteReview(review)"
                        type="danger"
                        plain
                        size="small"
                        @click="handleDeleteReview(review.id)"
                        :loading="deleting[String(review.id)]"
                        style="margin-left: 10px;"
                    >
                        删除评价
                    </el-button>
                    <!-- 添加一些间距 -->
                </div>
            </el-card>

            <div class="pagination-container">
                <el-pagination background layout="prev, pager, next" :total="total" :page-size="pageSize"
                    :current-page="currentPage" @current-change="handlePageChange" />
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
import { ref, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getUserReviews, deleteReview } from '@/api/review'; // 导入 deleteReview API
import { useUserStore } from '@/stores/user'; // 导入用户 store
import dayjs from 'dayjs';
// --- Add import for modal ---
import ReviewEditModal from '@/components/ReviewEditModal.vue';
// --- End import ---

// 定义评价类型 (确保包含 userId)
interface ReviewItem {
    id: number; // 改回 number
    userId: number; // 用户ID 是 number
    homestayId: number;
    homestayTitle?: string;
    rating: number;
    content: string;
    response?: string;
    createTime: string;
    responseTime?: string;
    // 根据 DTO 添加其他字段
}

// --- Add type for editable data ---
interface EditableReviewData {
  id: number;
  rating: number;
  content: string;
}
// --- End type ---

const router = useRouter();
const userStore = useUserStore(); // 使用用户 store
const loading = ref(true);
const reviews = ref<ReviewItem[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const deleting = reactive<Record<string, boolean>>({}); // 用于跟踪删除状态

// --- Add state for edit modal ---
const isEditModalVisible = ref(false);
const currentEditingReview = ref<EditableReviewData | null>(null);
// --- End state ---

// 获取当前登录用户ID
const currentUserId = userStore.userInfo?.id; // 使用 userInfo

// 获取评价列表
const fetchReviews = async () => {
    loading.value = true;
    try {
        const params = {
            page: currentPage.value - 1,
            size: pageSize.value,
        };
        // 注意：getUserReviews 可能需要传递用户名或其他标识符，根据你的API调整
        // 这里假设 getUserReviews 不需要额外参数，或者它内部知道当前用户
        const response = await getUserReviews(params); // 如果需要用户名: await getUserReviews(userStore.user.username, params);
        console.log("获取到用户评价数据:", response);

        if (response.data && response.data.content) {
            reviews.value = response.data.content;
            total.value = response.data.totalElements;
        } else {
            console.warn("未识别的用户评价数据格式", response);
            reviews.value = [];
            total.value = 0;
        }

    } catch (error) {
        console.error('获取我的评价列表失败:', error);
        ElMessage.error('获取评价列表失败，请稍后重试');
        reviews.value = [];
        total.value = 0;
    } finally {
        loading.value = false;
    }
};

// 判断是否可以删除评价 (当前用户是评价者)
const canDeleteReview = (review: ReviewItem): boolean => {
    // 需要确保 currentUserId 和 review.userId 都存在且类型匹配
    return !!currentUserId && review.userId === currentUserId;
};

// --- Add function to check if review can be edited ---
// 通常和删除权限一样，但可以分开写以备将来扩展
const canEditReview = (review: ReviewItem): boolean => {
    return !!currentUserId && review.userId === currentUserId;
};
// --- End function ---

// 处理删除评价
const handleDeleteReview = async (reviewId: number) => { // reviewId 是 number
    try {
        await ElMessageBox.confirm('确定要删除这条评价吗？删除后不可恢复。', '删除确认', {
            confirmButtonText: '确定删除',
            cancelButtonText: '取消',
            type: 'warning',
        });

        deleting[String(reviewId)] = true; // reactive key 必须是 string
        await deleteReview(reviewId); // 直接传递 number
        ElMessage.success('评价删除成功');
        // 刷新列表或直接移除
        reviews.value = reviews.value.filter(r => r.id !== reviewId);

    } catch (error: any) {
        if (error !== 'cancel') { // 用户取消操作不报错
            console.error('删除评价失败:', error);
            const errMsg = error?.response?.data?.message || '删除评价失败，请稍后重试';
            ElMessage.error(errMsg);
        }
    } finally {
        deleting[String(reviewId)] = false; // reactive key 必须是 string
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
    const index = reviews.value.findIndex(r => r.id === updatedReviewData.id);
    if (index !== -1) {
        // 只更新 rating 和 content
        reviews.value[index].rating = updatedReviewData.rating;
        reviews.value[index].content = updatedReviewData.content;
        // 如果需要，可以更新 updateTime，但这通常由后端处理
    }
    isEditModalVisible.value = false; // 关闭弹窗
};
// --- End functions ---

// 处理分页变化
const handlePageChange = (page: number) => {
    currentPage.value = page;
    fetchReviews();
};

// 格式化日期
const formatDate = (dateString: string) => {
    if (!dateString) return '';
    return dayjs(dateString).format('YYYY-MM-DD');
};

// 格式化日期时间
const formatDateTime = (dateString: string) => {
    if (!dateString) return '';
    return dayjs(dateString).format('YYYY-MM-DD HH:mm');
};

// 跳转到首页
const goToHomepage = () => {
    router.push('/');
};

// 初始化加载
onMounted(() => {
    // 确保用户信息加载后再获取评价
    if (userStore.userInfo) { // 检查 userInfo
        fetchReviews();
    } else {
        // 可以添加一个 watcher 或者等待用户信息加载完成的逻辑
        console.warn("用户信息尚未加载，无法获取评价列表");
        loading.value = false; // 停止加载状态
        // ElMessage.warning("请先登录"); // 或者提示登录
    }
});
</script>

<style scoped>
.my-reviews-container {
    max-width: 900px;
    margin: 20px auto;
    padding: 30px;
    background-color: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.page-header {
    margin-bottom: 24px;
    text-align: center;
}

.page-header h1 {
    font-size: 24px;
    font-weight: 600;
}

.loading-container,
.empty-container {
    padding: 40px;
    text-align: center;
}

.review-list {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.review-card {
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    overflow: hidden;
}

.review-card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background-color: #f9fafb;
    border-bottom: 1px solid #e4e7ed;
}

.homestay-title {
    font-weight: 500;
    color: #303133;
}

.review-date {
    font-size: 13px;
    color: #909399;
}

.review-card-content {
    padding: 16px;
}

.rating-line {
    display: flex;
    align-items: center;
    margin-bottom: 10px;
}

.rating-label {
    margin-right: 8px;
    color: #606266;
}

.review-text p {
    margin: 0;
    line-height: 1.6;
    color: #303133;
}

.host-response {
    margin-top: 15px;
    padding-top: 15px;
    /* border-top: 1px dashed #dcdfe6; */
}

.response-header {
    font-weight: 500;
    color: #409EFF;
    /* Use a different color */
    margin-bottom: 5px;
}

.response-content {
    color: #606266;
    font-size: 14px;
    line-height: 1.5;
}

.response-time {
    font-size: 12px;
    color: #909399;
    text-align: right;
    margin-top: 5px;
}

.review-card-footer {
    display: flex;
    justify-content: flex-end; /* 将按钮对齐到右侧 */
    padding: 12px 16px;
    border-top: 1px solid #e4e7ed;
    margin-top: 10px;
}

.pagination-container {
    margin-top: 24px;
    display: flex;
    justify-content: center;
}

.el-rate {
    height: auto;
    /* Override default height if needed */
    line-height: normal;
}

/* 微调按钮样式 */
.review-card-footer .el-button {
    padding: 6px 12px;
}
</style>