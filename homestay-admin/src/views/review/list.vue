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
                    <el-button type="primary" @click="handleFilter" :icon="Search">筛选</el-button>
                    <el-button @click="resetFilter" :icon="Refresh">重置</el-button>
                </el-form-item>
            </el-form>
        </div>

        <el-table :data="reviewList" border style="width: 100%" v-loading="loading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="userName" label="用户" width="120">
                <template #default="scope">
                    <div class="user-info">
                        <el-avatar :size="30" :src="scope.row.userAvatar" />
                        <span style="margin-left: 5px;">{{ scope.row.userName }}</span>
                    </div>
                </template>
            </el-table-column>
            <el-table-column prop="homestayTitle" label="民宿" width="180" show-overflow-tooltip />
            <el-table-column prop="rating" label="评分" width="100">
                <template #default="scope">
                    <el-rate :model-value="scope.row.rating" disabled size="small" />
                </template>
            </el-table-column>
            <el-table-column prop="content" label="评价内容" show-overflow-tooltip />
            <el-table-column prop="createTime" label="评价时间" width="160">
                <template #default="scope">{{ formatDateTime(scope.row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="可见性" width="100" align="center">
                <template #default="scope">
                    <el-switch v-model="scope.row.isPublic" :active-value="true" :inactive-value="false"
                        :loading="scope.row.visibilityLoading" @change="handleVisibilityChange(scope.row)" />
                </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right" align="center">
                <template #default="scope">
                    <el-button size="small" @click="handleDetail(scope.row)">查看</el-button>
                    <el-button size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <div class="pagination-container">
            <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize"
                :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" :total="total"
                @size-change="handleSizeChange" @current-change="handleCurrentChange" />
        </div>

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
import { getAdminReviewList, deleteReview, setReviewVisibility } from '@/api/review'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

interface ReviewFilterForm {
    rating: number | null;
    status: string | null;
}

interface ReviewItem {
    id: number;
    userName: string;
    userAvatar?: string;
    homestayTitle: string;
    rating: number;
    content: string;
    createTime: string;
    response?: string | null;
    responseTime?: string | null;
    isPublic: boolean;
    visibilityLoading?: boolean;
    images?: string[];
}

const filterForm = reactive<ReviewFilterForm>({
    rating: null,
    status: null,
})

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(true)

const reviewList = ref<ReviewItem[]>([])

const dialogVisible = ref(false)
const currentReview = ref<ReviewItem | null>(null)

const fetchReviews = async () => {
    loading.value = true
    try {
        const params = {
            page: currentPage.value - 1,
            size: pageSize.value,
            rating: filterForm.rating,
            status: filterForm.status,
            sort: 'createTime,desc'
        }
        const response = await getAdminReviewList(params)

        console.log("[DEBUG] Received Data (response variable):", response);
        console.log("[DEBUG] Type of response:", typeof response);

        if (response && response.content) {
            reviewList.value = response.content.map((r: ReviewItem) => ({ ...r, visibilityLoading: false }))
            total.value = response.totalElements || 0
        } else {
            reviewList.value = []
            total.value = 0
            console.warn("获取管理员评价列表数据无效或为空")
        }
    } catch (error) {
        console.error('获取评价列表失败:', error)
        ElMessage.error('获取评价列表失败')
        reviewList.value = []
        total.value = 0
    } finally {
        loading.value = false
    }
}

const handleVisibilityChange = async (review: ReviewItem) => {
    const newVisibility = review.isPublic
    const actionText = newVisibility ? '显示' : '隐藏'
    review.visibilityLoading = true

    try {
        await setReviewVisibility(review.id, newVisibility)
        ElMessage.success(`评价 ${actionText} 成功`)
    } catch (error: any) {
        console.error(`设置评价可见性失败 (ID: ${review.id}):`, error)
        ElMessage.error(`设置评价可见性失败: ${error?.response?.data?.message || error.message}`)
        review.isPublic = !newVisibility
    } finally {
        review.visibilityLoading = false
    }
}

const handleFilter = () => {
    currentPage.value = 1
    fetchReviews()
}

const resetFilter = () => {
    filterForm.rating = null
    filterForm.status = null
    handleFilter()
}

const handleCurrentChange = (val: number) => {
    currentPage.value = val
    fetchReviews()
}

const handleSizeChange = (val: number) => {
    pageSize.value = val
    currentPage.value = 1
    fetchReviews()
}

const handleDetail = (row: ReviewItem) => {
    currentReview.value = row
    dialogVisible.value = true
}

const handleDelete = (id: number) => {
    ElMessageBox.confirm('确定要删除该评价吗？删除后不可恢复。', '警告', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        loading.value = true
        try {
            await deleteReview(id)
            ElMessage.success('删除成功')
            const remainingItems = reviewList.value.length - 1
            if (remainingItems === 0 && currentPage.value > 1) {
                currentPage.value -= 1
            }
            fetchReviews()
        } catch (error: any) {
            console.error('删除评价失败:', error)
            ElMessage.error(`删除评价失败: ${error?.response?.data?.message || error.message}`)
        } finally {
            loading.value = false
        }
    }).catch(() => {
        ElMessage.info('已取消删除')
    })
}

const formatDateTime = (dateString?: string | null) => {
    if (!dateString) return ''
    return dayjs(dateString).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
    fetchReviews()
})
</script>

<style scoped lang="scss">
.review-container {
    padding: 20px;

    .filter-container {
        background-color: #f5f7fa;
        padding: 15px;
        border-radius: 4px;
        margin-bottom: 20px;
    }

    .user-info {
        display: flex;
        align-items: center;
    }

    .el-pagination {
        margin-top: 20px;
        justify-content: flex-end;
    }

    .pagination-container {
        margin-top: 20px;
        display: flex;
        justify-content: flex-end;
    }

    .review-detail {
        .review-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 15px;
            padding-bottom: 15px;
            border-bottom: 1px solid #eee;

            .user-info {
                display: flex;
                align-items: center;
                gap: 10px;

                .username {
                    font-weight: bold;
                }

                .review-time {
                    font-size: 12px;
                    color: #999;
                }
            }

            .rating .el-rate {
                height: auto;
            }
        }

        .review-content {
            margin-bottom: 15px;
            white-space: pre-wrap;
            line-height: 1.6;
        }

        .review-images {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            margin-bottom: 15px;

            .review-image {
                width: 100px;
                height: 100px;
                border-radius: 4px;
                cursor: pointer;
            }
        }

        .review-reply {
            background-color: #f8f8f8;
            border-radius: 4px;
            padding: 15px;
            margin-top: 15px;

            .reply-header {
                font-weight: bold;
                margin-bottom: 8px;
                color: #333;
            }

            .reply-content {
                white-space: pre-wrap;
                line-height: 1.6;
                color: #555;
                margin-bottom: 8px;
            }

            .reply-time {
                font-size: 12px;
                color: #999;
                text-align: right;
            }
        }
    }
}
</style>