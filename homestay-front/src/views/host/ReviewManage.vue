<template>
    <div class="review-container">
        <h1>评价管理</h1>

        <!-- 统计数据 -->
        <el-row :gutter="20" class="stat-row">
            <el-col :span="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-content">
                        <div class="stat-title">总评价数</div>
                        <div class="stat-value">{{ reviewStats.total }}</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-content">
                        <div class="stat-title">好评数 (4-5星)</div>
                        <div class="stat-value">{{ reviewStats.good }}</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-content">
                        <div class="stat-title">中评数 (3星)</div>
                        <div class="stat-value">{{ reviewStats.neutral }}</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card shadow="hover" class="stat-card">
                    <div class="stat-content">
                        <div class="stat-title">差评数 (1-2星)</div>
                        <div class="stat-value">{{ reviewStats.bad }}</div>
                    </div>
                </el-card>
            </el-col>
        </el-row>

        <!-- 筛选器 -->
        <el-card class="filter-card">
            <el-form :inline="true" :model="filterForm" class="filter-form">
                <el-form-item label="房源">
                    <el-select v-model="filterForm.homestayId" placeholder="全部房源" clearable>
                        <el-option v-for="item in homestayOptions" :key="item.value" :label="item.label"
                            :value="item.value" />
                    </el-select>
                </el-form-item>
                <el-form-item label="评分">
                    <el-select v-model="filterForm.rating" placeholder="全部评分" clearable>
                        <el-option label="5星" value="5" />
                        <el-option label="4星" value="4" />
                        <el-option label="3星" value="3" />
                        <el-option label="2星" value="2" />
                        <el-option label="1星" value="1" />
                    </el-select>
                </el-form-item>
                <el-form-item label="回复状态">
                    <el-select v-model="filterForm.replyStatus" placeholder="全部状态" clearable>
                        <el-option label="已回复" value="REPLIED" />
                        <el-option label="未回复" value="PENDING" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleFilter">筛选</el-button>
                    <el-button @click="resetFilter">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <!-- 评价列表 -->
        <el-card class="review-list-card" v-loading="loading">
            <div v-if="reviews.length === 0" class="empty-data">
                暂无评价数据
            </div>

            <div v-for="review in reviews" :key="review.id" class="review-item">
                <div class="review-header">
                    <div class="review-info">
                        <div class="review-homestay">{{ review.homestayName }}</div>
                        <div class="review-user">{{ review.userName }} · {{ review.date }}</div>
                    </div>
                    <div class="review-rating">
                        <el-rate v-model="review.rating" disabled text-color="#ff9900" />
                    </div>
                </div>

                <div class="review-content">{{ review.content }}</div>

                <div class="review-reply" v-if="review.reply">
                    <div class="reply-header">
                        <span class="reply-title">我的回复:</span>
                        <span class="reply-date">{{ review.replyDate }}</span>
                    </div>
                    <div class="reply-content">{{ review.reply }}</div>
                </div>

                <div class="review-actions">
                    <el-button v-if="!review.reply" type="primary" size="small" @click="showReplyDialog(review)">
                        回复
                    </el-button>
                    <el-button v-else type="primary" plain size="small" @click="showReplyDialog(review, true)">
                        修改回复
                    </el-button>
                </div>
            </div>

            <!-- 分页 -->
            <div class="pagination">
                <el-pagination v-model:currentPage="currentPage" v-model:page-size="pageSize"
                    :page-sizes="[5, 10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" :total="total"
                    @size-change="handleSizeChange" @current-change="handleCurrentChange" />
            </div>
        </el-card>

        <!-- 回复对话框 -->
        <el-dialog v-model="replyDialogVisible" :title="currentReview.reply ? '修改回复' : '回复评价'" width="50%">
            <el-form :model="replyForm" ref="replyFormRef">
                <el-form-item label="回复内容" prop="content"
                    :rules="[{ required: true, message: '请输入回复内容', trigger: 'blur' }]">
                    <el-input v-model="replyForm.content" type="textarea" :rows="4" placeholder="请输入回复内容" />
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="replyDialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="submitReply" :loading="submitting">
                        {{ currentReview.reply ? '更新' : '提交' }}
                    </el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'

// 统计数据
const reviewStats = reactive({
    total: 5,
    good: 3,
    neutral: 1,
    bad: 1
})

// 筛选表单
const filterForm = reactive({
    homestayId: null,
    rating: null,
    replyStatus: null
})

// 房源选项
const homestayOptions = ref([
    { value: 1, label: '海景度假别墅' },
    { value: 2, label: '城市中心公寓' },
    { value: 3, label: '山间小木屋' }
])

// 页码相关
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 评价列表
const reviews = ref([
    {
        id: 1,
        homestayId: 1,
        homestayName: '海景度假别墅',
        userName: '张三',
        date: '2023-07-15',
        rating: 5,
        content: '非常棒的住宿体验，房间干净整洁，视野开阔，服务周到，下次还会再来！',
        reply: '感谢您的好评！期待您的再次光临。',
        replyDate: '2023-07-16',
        replyStatus: 'REPLIED'
    },
    {
        id: 2,
        homestayId: 1,
        homestayName: '海景度假别墅',
        userName: '李四',
        date: '2023-07-18',
        rating: 4,
        content: '住宿环境很好，设施齐全，就是位置有点偏，不太好找。',
        reply: null,
        replyDate: null,
        replyStatus: 'PENDING'
    },
    {
        id: 3,
        homestayId: 2,
        homestayName: '城市中心公寓',
        userName: '王五',
        date: '2023-07-20',
        rating: 3,
        content: '房间还可以，但是周围有点吵，晚上休息不好。',
        reply: '非常抱歉给您带来的不便，我们会考虑加强隔音设施。谢谢您的反馈！',
        replyDate: '2023-07-21',
        replyStatus: 'REPLIED'
    },
    {
        id: 4,
        homestayId: 3,
        homestayName: '山间小木屋',
        userName: '赵六',
        date: '2023-07-25',
        rating: 5,
        content: '风景太美了，空气清新，小木屋也很有特色，非常喜欢！',
        reply: '谢谢您的好评！很高兴您喜欢我们的小木屋，欢迎再次光临。',
        replyDate: '2023-07-26',
        replyStatus: 'REPLIED'
    },
    {
        id: 5,
        homestayId: 2,
        homestayName: '城市中心公寓',
        userName: '钱七',
        date: '2023-07-28',
        rating: 2,
        content: '房间比图片上看起来要小，设施也比较陈旧，性价比不高。',
        reply: null,
        replyDate: null,
        replyStatus: 'PENDING'
    }
])

// 回复相关
const replyDialogVisible = ref(false)
const replyFormRef = ref<FormInstance>()
const submitting = ref(false)
const currentReview = ref<any>({})
const replyForm = reactive({
    content: ''
})

// 处理筛选
const handleFilter = () => {
    fetchReviews()
}

// 重置筛选
const resetFilter = () => {
    filterForm.homestayId = null
    filterForm.rating = null
    filterForm.replyStatus = null
    fetchReviews()
}

// 显示回复对话框
const showReplyDialog = (review: any, isEdit = false) => {
    currentReview.value = review
    replyForm.content = isEdit ? review.reply : ''
    replyDialogVisible.value = true
}

// 提交回复
const submitReply = async () => {
    if (!replyFormRef.value) return

    await replyFormRef.value.validate(async (valid) => {
        if (valid) {
            submitting.value = true

            try {
                // 模拟API调用
                await new Promise(resolve => setTimeout(resolve, 800))

                // 更新本地数据
                const index = reviews.value.findIndex(item => item.id === currentReview.value.id)
                if (index !== -1) {
                    reviews.value[index].reply = replyForm.content
                    reviews.value[index].replyDate = new Date().toLocaleDateString()
                    reviews.value[index].replyStatus = 'REPLIED'
                }

                ElMessage.success(currentReview.value.reply ? '回复已更新' : '回复已提交')
                replyDialogVisible.value = false
            } catch (error) {
                ElMessage.error('操作失败，请重试')
            } finally {
                submitting.value = false
            }
        }
    })
}

// 页码变化
const handleCurrentChange = (page: number) => {
    currentPage.value = page
    fetchReviews()
}

// 每页数量变化
const handleSizeChange = (size: number) => {
    pageSize.value = size
    fetchReviews()
}

// 获取评价列表
const fetchReviews = () => {
    loading.value = true

    // 模拟API调用
    setTimeout(() => {
        // 应用筛选
        let filteredReviews = [...reviews.value]

        if (filterForm.homestayId) {
            filteredReviews = filteredReviews.filter(item => item.homestayId === filterForm.homestayId)
        }

        if (filterForm.rating) {
            filteredReviews = filteredReviews.filter(item => item.rating === Number(filterForm.rating))
        }

        if (filterForm.replyStatus) {
            filteredReviews = filteredReviews.filter(item => item.replyStatus === filterForm.replyStatus)
        }

        reviews.value = filteredReviews
        total.value = filteredReviews.length
        loading.value = false
    }, 500)
}

onMounted(() => {
    fetchReviews()
})
</script>

<style scoped>
.review-container {
    padding: 20px;
}

.stat-row {
    margin-bottom: 20px;
}

.stat-card {
    height: 100px;
}

.stat-content {
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
}

.stat-title {
    font-size: 14px;
    color: #606266;
    margin-bottom: 10px;
}

.stat-value {
    font-size: 24px;
    font-weight: bold;
    color: #409EFF;
}

.filter-card {
    margin-bottom: 20px;
}

.review-list-card {
    min-height: 400px;
}

.review-item {
    padding: 15px 0;
    border-bottom: 1px solid #EBEEF5;
}

.review-item:last-child {
    border-bottom: none;
}

.review-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 10px;
}

.review-homestay {
    font-weight: bold;
    font-size: 16px;
    margin-bottom: 5px;
}

.review-user {
    color: #909399;
    font-size: 14px;
}

.review-content {
    line-height: 1.6;
    margin-bottom: 15px;
}

.review-reply {
    background-color: #F5F7FA;
    padding: 10px 15px;
    border-radius: 4px;
    margin-bottom: 15px;
}

.reply-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 5px;
}

.reply-title {
    font-weight: bold;
}

.reply-date {
    color: #909399;
    font-size: 14px;
}

.review-actions {
    display: flex;
    justify-content: flex-end;
}

.pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
}

.empty-data {
    padding: 40px;
    text-align: center;
    color: #909399;
}
</style>