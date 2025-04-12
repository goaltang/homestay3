<template>
    <div class="review-container">
        <div class="filter-container">
            <el-form :inline="true" :model="filterForm">
                <el-form-item label="评分">
                    <el-select v-model="filterForm.rating" placeholder="全部评分" clearable>
                        <el-option v-for="i in 5" :key="i" :label="`${i}星`" :value="i" />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="filterForm.status" placeholder="全部状态" clearable>
                        <el-option label="已回复" value="RESPONDED" />
                        <el-option label="未回复" value="UNREPLIED" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleFilter">筛选</el-button>
                    <el-button @click="resetFilter">重置</el-button>
                </el-form-item>
            </el-form>
        </div>

        <el-table :data="reviewList" border style="width: 100%" v-loading="loading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="userName" label="用户" width="120">
                <template #default="scope">
                    <div class="user-info">
                        <el-avatar :size="30" :src="scope.row.userAvatar" />
                        <span>{{ scope.row.userName }}</span>
                    </div>
                </template>
            </el-table-column>
            <el-table-column prop="homestayTitle" label="民宿" width="180" />
            <el-table-column prop="rating" label="评分" width="100">
                <template #default="scope">
                    <el-rate v-model="scope.row.rating" disabled />
                </template>
            </el-table-column>
            <el-table-column prop="content" label="评价内容" show-overflow-tooltip />
            <el-table-column prop="createTime" label="评价时间" width="180" />
            <el-table-column label="操作" width="180">
                <template #default="scope">
                    <el-button size="small" @click="handleDetail(scope.row)">查看</el-button>
                    <el-button size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
            @current-change="handleCurrentChange" />

        <!-- 评价详情弹窗 -->
        <el-dialog title="评价详情" v-model="dialogVisible" width="600px">
            <div class="review-detail" v-if="currentReview">
                <div class="review-header">
                    <div class="user-info">
                        <el-avatar :size="40" :src="currentReview.userAvatar" />
                        <div>
                            <div class="username">{{ currentReview.userName }}</div>
                            <div class="review-time">{{ currentReview.createTime }}</div>
                        </div>
                    </div>
                    <div class="rating">
                        <el-rate v-model="currentReview.rating" disabled />
                    </div>
                </div>

                <div class="review-content">{{ currentReview.content }}</div>

                <div class="review-images" v-if="currentReview.images && currentReview.images.length > 0">
                    <el-image v-for="(img, index) in currentReview.images" :key="index" :src="img"
                        :preview-src-list="currentReview.images" fit="cover" class="review-image" />
                </div>

                <div class="review-reply" v-if="currentReview.response">
                    <div class="reply-header">房东回复</div>
                    <div class="reply-content">{{ currentReview.response }}</div>
                    <div class="reply-time">{{ currentReview.responseTime }}</div>
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getReviewList, deleteReview } from '@/api/review'
import { ElMessage, ElMessageBox } from 'element-plus'

interface ReviewFilterForm {
    rating: string | number;
    status: string;
    [key: string]: string | number;
}

interface Review {
    id: number;
    userName: string;
    userAvatar: string;
    homestayTitle: string;
    rating: number;
    content: string;
    createTime: string;
    images?: string[];
    response?: string;
    responseTime?: string;
}

// 筛选表单
const filterForm = reactive<ReviewFilterForm>({
    rating: '',
    status: ''
})

// 分页参数
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

// 评价列表
const reviewList = ref<Review[]>([])

// 详情弹窗
const dialogVisible = ref(false)
const currentReview = ref<Review | null>(null)

// 获取评价列表
const fetchReviews = async () => {
    loading.value = true
    try {
        const params = {
            page: currentPage.value - 1,
            size: pageSize.value,
            ...filterForm
        }
        const { data } = await getReviewList(params)
        reviewList.value = data.content || []
        total.value = data.totalElements || 0
    } catch (error) {
        console.error('获取评价列表失败:', error)
        ElMessage.error('获取评价列表失败')
    } finally {
        loading.value = false
    }
}

// 筛选处理
const handleFilter = () => {
    currentPage.value = 1
    fetchReviews()
}

// 重置筛选
const resetFilter = () => {
    filterForm.rating = ''
    filterForm.status = ''
    handleFilter()
}

// 页码变化
const handleCurrentChange = (val: number) => {
    fetchReviews()
}

// 每页条数变化
const handleSizeChange = (val: number) => {
    fetchReviews()
}

// 查看详情
const handleDetail = (row: Review) => {
    currentReview.value = row
    dialogVisible.value = true
}

// 删除评价
const handleDelete = (id: number) => {
    ElMessageBox.confirm('确定要删除该评价吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        try {
            await deleteReview(id)
            ElMessage.success('删除成功')
            fetchReviews()
        } catch (error) {
            console.error('删除评价失败:', error)
            ElMessage.error('删除评价失败')
        }
    }).catch(() => { })
}

onMounted(() => {
    fetchReviews()
})
</script>

<style scoped lang="scss">
.review-container {
    padding: 20px;

    .filter-container {
        margin-bottom: 20px;
    }

    .user-info {
        display: flex;
        align-items: center;

        .el-avatar {
            margin-right: 10px;
        }
    }

    .review-detail {
        .review-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 15px;

            .user-info {
                display: flex;

                .username {
                    font-weight: bold;
                }

                .review-time {
                    font-size: 12px;
                    color: #999;
                }
            }
        }

        .review-content {
            margin-bottom: 15px;
            white-space: pre-wrap;
        }

        .review-images {
            display: flex;
            flex-wrap: wrap;
            margin-bottom: 15px;

            .review-image {
                width: 100px;
                height: 100px;
                margin-right: 10px;
                margin-bottom: 10px;
            }
        }

        .review-reply {
            background: #f5f7fa;
            padding: 15px;
            border-radius: 4px;

            .reply-header {
                font-weight: bold;
                margin-bottom: 10px;
            }

            .reply-time {
                font-size: 12px;
                color: #999;
                margin-top: 5px;
            }
        }
    }
}
</style>