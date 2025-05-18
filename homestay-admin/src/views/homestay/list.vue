<template>
    <div class="homestay-list">
        <div class="search-box">
            <el-form :inline="true" :model="searchForm">
                <el-form-item label="房源名称">
                    <el-input v-model="searchForm.name" placeholder="请输入房源名称" clearable />
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
                        <el-option label="待审核" value="PENDING" />
                        <el-option label="已上架" value="ACTIVE" />
                        <el-option label="已下架" value="INACTIVE" />
                        <el-option label="已拒绝" value="REJECTED" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">搜索</el-button>
                    <el-button @click="resetSearch">重置</el-button>
                    <el-button type="success" @click="handleAdd">新增房源</el-button>
                </el-form-item>
            </el-form>
        </div>

        <!-- 批量操作区域 -->
        <div class="batch-operation" v-if="selectedRows.length > 0">
            <el-alert title="批量操作" type="info" :closable="false" show-icon>
                <template #default>
                    已选择 <strong>{{ selectedRows.length }}</strong> 项
                    <div class="batch-buttons">
                        <el-button size="small" type="success" @click="handleBatchActive">批量上架</el-button>
                        <el-button size="small" type="info" @click="handleBatchInactive">批量下架</el-button>
                        <el-button size="small" type="danger" @click="handleBatchDelete">批量删除</el-button>
                    </div>
                </template>
            </el-alert>
        </div>

        <el-table :data="tableData" border style="width: 100%" v-loading="loading"
            @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" />
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="房源名称" />
            <el-table-column prop="price" label="价格" width="120">
                <template #default="scope">
                    ¥{{ scope.row.price || scope.row.pricePerNight || 'N/A' }}
                </template>
            </el-table-column>
            <el-table-column label="地址" width="250">
                <template #default="scope">
                    {{ getLocationText(scope.row) }}
                </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                    <el-tag :type="getStatusTagType(scope.row.status)">
                        {{ formatStatus(scope.row.status) }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
                <template #default="scope">
                    <el-button v-if="scope.row.status === 'PENDING'" type="warning" link
                        @click="handleReview(scope.row)">审核</el-button>

                    <template v-if="scope.row.status === 'ACTIVE'">
                        <el-button type="info" link @click="handleToggleStatus(scope.row)">下架</el-button>
                        <el-button type="primary" link @click="handleEdit(scope.row)">编辑</el-button>
                    </template>

                    <template v-if="scope.row.status === 'INACTIVE'">
                        <el-button type="success" link @click="handleToggleStatus(scope.row)">上架</el-button>
                        <el-button type="primary" link @click="handleEdit(scope.row)">编辑</el-button>
                    </template>

                    <template v-if="scope.row.status === 'REJECTED'">
                        <el-button type="primary" link @click="handleViewDetail(scope.row)">查看详情</el-button>
                        <el-button type="danger" link @click="handleDelete(scope.row)">删除</el-button>
                    </template>
                </template>
            </el-table-column>
        </el-table>

        <div class="pagination">
            <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize"
                :page-sizes="[10, 20, 50, 100]" :total="total" layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSizeChange" @current-change="handleCurrentChange" />
        </div>

        <!-- Review Dialog -->
        <el-dialog v-model="reviewDialogVisible" title="审核房源" width="75%" top="5vh">
            <div v-if="currentReviewItem" class="review-details">
                <el-row :gutter="20">
                    <el-col :span="14">
                        <el-card shadow="never" class="info-card">
                            <template #header>
                                <div class="card-header">房源概览</div>
                            </template>
                            <el-descriptions :column="2" border>
                                <el-descriptions-item label="ID" min-width="80px">{{ currentReviewItem.id
                                }}</el-descriptions-item>
                                <el-descriptions-item label="当前状态">
                                    <el-tag :type="getStatusTagType(currentReviewItem.status)">
                                        {{ formatStatus(currentReviewItem.status) }}
                                    </el-tag>
                                </el-descriptions-item>
                                <el-descriptions-item label="房源名称" :span="2"><strong>{{ currentReviewItem.name ||
                                    currentReviewItem.title }}</strong></el-descriptions-item>
                                <el-descriptions-item label="类型">{{ currentReviewItem.type || 'N/A'
                                }}</el-descriptions-item>
                                <el-descriptions-item label="价格"><strong>¥{{ currentReviewItem.price
                                }}</strong></el-descriptions-item>
                                <el-descriptions-item label="最大入住">{{ currentReviewItem.maxGuests || 'N/A' }}
                                    人</el-descriptions-item>
                                <el-descriptions-item label="最少入住">{{ currentReviewItem.minNights || 'N/A' }}
                                    晚</el-descriptions-item>
                            </el-descriptions>
                        </el-card>

                        <el-card shadow="never" class="info-card">
                            <template #header>
                                <div class="card-header">位置与详情</div>
                            </template>
                            <el-descriptions :column="1" border>
                                <el-descriptions-item label="地址">{{ getLocationText(currentReviewItem)
                                }}</el-descriptions-item>
                                <el-descriptions-item label="描述">{{ currentReviewItem.description || '暂无描述'
                                }}</el-descriptions-item>
                            </el-descriptions>
                        </el-card>

                        <el-card shadow="never" class="info-card">
                            <template #header>
                                <div class="card-header">房东信息</div>
                            </template>
                            <el-descriptions :column="1" border>
                                <el-descriptions-item label="姓名">{{ currentReviewItem.ownerName || 'N/A'
                                }}</el-descriptions-item>
                                <el-descriptions-item label="用户名">{{ currentReviewItem.ownerUsername || 'N/A'
                                }}</el-descriptions-item>
                            </el-descriptions>
                        </el-card>

                    </el-col>
                    <el-col :span="10">
                        <el-card shadow="never" class="info-card">
                            <template #header>
                                <div class="card-header">设施与服务</div>
                            </template>
                            <div v-if="currentReviewItem.amenities && currentReviewItem.amenities.length > 0"
                                class="amenities-list">
                                <el-tag v-for="amenity in currentReviewItem.amenities" :key="amenity.value"
                                    effect="plain" size="small" style="margin-right: 5px; margin-bottom: 5px;">
                                    {{ amenity.label }}
                                </el-tag>
                            </div>
                            <span v-else>暂无设施信息</span>
                        </el-card>

                        <el-card shadow="never" class="info-card">
                            <template #header>
                                <div class="card-header">房源图片</div>
                            </template>
                            <div v-if="currentReviewItem.images && currentReviewItem.images.length > 0"
                                class="image-gallery">
                                <el-image v-for="(imgUrl, index) in currentReviewItem.images" :key="index" :src="imgUrl"
                                    :preview-src-list="currentReviewItem.images" :initial-index="index" fit="cover"
                                    :class="{ 'cover-image-highlight': index === 0 && currentReviewItem.images.length > 1 }"
                                    style="width: 90px; height: 90px; margin-right: 8px; margin-bottom: 8px; border-radius: 4px; border: 1px solid #ebeef5; cursor: pointer; position: relative;"
                                    lazy>
                                    <template #placeholder>
                                        <div class="image-slot">加载中...</div>
                                    </template>
                                    <template #error>
                                        <div class="image-slot">
                                            <el-icon>
                                                <Picture />
                                            </el-icon>
                                        </div>
                                    </template>
                                    <span v-if="index === 0" class="cover-image-label">封面</span>
                                </el-image>
                            </div>
                            <span v-else>暂无图片信息</span>
                        </el-card>
                    </el-col>
                </el-row>
            </div>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="reviewDialogVisible = false">取消</el-button>
                    <el-button type="danger" @click="handleReject">拒绝</el-button>
                    <el-button type="success" @click="handleApprove">批准</el-button>
                </span>
            </template>
        </el-dialog>

    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Homestay, HomestaySearchParams } from '@/types'
import { Picture } from '@element-plus/icons-vue'
import { regionData, codeToText } from 'element-china-area-data'
import {
    getHomestayList,
    updateHomestayStatus,
    deleteHomestay,
    batchUpdateHomestayStatus,
    batchDeleteHomestays,
    getHomestayDetail
} from '@/api/homestay'
import { useRouter } from 'vue-router'

const router = useRouter()

// 搜索表单
const searchForm = reactive<HomestaySearchParams>({
    page: 1,
    pageSize: 10,
    name: '',
    status: ''
})

// 表格数据
const tableData = ref<Homestay[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedRows = ref<Homestay[]>([])

// Review Dialog State
const reviewDialogVisible = ref(false)
const currentReviewItem = ref<any | null>(null)

// 处理表格选择变化
const handleSelectionChange = (selection: Homestay[]) => {
    selectedRows.value = selection
}

// 搜索方法
const handleSearch = () => {
    currentPage.value = 1
    fetchData()
}

// 重置搜索
const resetSearch = () => {
    searchForm.name = ''
    searchForm.status = ''
    handleSearch()
}

// 获取数据
const fetchData = async () => {
    loading.value = true
    try {
        // Call the API - adaptPageResponse converts the response to { list, total }
        const res = await getHomestayList({
            page: currentPage.value,
            pageSize: pageSize.value,
            name: searchForm.name,
            status: searchForm.status
        });

        // Use the adapted response fields (list, total)
        if (res && res.list) {
            tableData.value = res.list;
            total.value = res.total;
            console.log('Fetched and adapted homestay list:', tableData.value);
        } else {
            console.error('Invalid adapted response structure received:', res);
            tableData.value = [];
            total.value = 0;
        }

    } catch (error) {
        console.error('获取房源列表失败:', error);
        ElMessage.error('获取房源列表失败');
        tableData.value = [];
        total.value = 0;
    } finally {
        loading.value = false;
    }
};

// 分页方法
const handleSizeChange = (val: number) => {
    pageSize.value = val
    fetchData()
}

const handleCurrentChange = (val: number) => {
    currentPage.value = val
    fetchData()
}

// 新增房源
const handleAdd = () => {
    router.push('/homestays/add')
}

// 编辑房源
const handleEdit = (row: Homestay) => {
    if (!row.id) {
        ElMessage.error('无效的房源ID');
        return;
    }
    router.push(`/homestays/edit/${row.id}`)
}

// 审核房源
const handleReview = async (row: Homestay) => {
    if (!row.id) {
        ElMessage.error('无效的房源ID');
        return;
    }
    try {
        loading.value = true; // Show loading while fetching details
        const homestayDetails = await getHomestayDetail(row.id);
        currentReviewItem.value = homestayDetails; // Store detailed data
        reviewDialogVisible.value = true;
        loading.value = false;
    } catch (error) {
        loading.value = false;
        console.error('获取房源详情失败:', error);
        ElMessage.error('获取房源详情失败，请稍后重试');
        // Fallback: use row data if detail fetch fails, or just don't open dialog
        // currentReviewItem.value = row; 
        // reviewDialogVisible.value = true; 
    }
}

// 查看详情
const handleViewDetail = (row: Homestay) => {
    // Can reuse the review logic/dialog for viewing details
    handleReview(row); // Or implement a separate detail view if needed
    // ElMessage.info(`准备查看已拒绝房源 ID: ${row.id}, 功能待实现`);
}

// 切换状态 (Approve/Reject handled separately now)
const handleToggleStatus = async (row: Homestay) => {
    const targetStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    const actionText = row.status === 'ACTIVE' ? '下架' : '上架';
    try {
        await ElMessageBox.confirm(
            `确认要${actionText}该房源吗？`,
            '提示',
            {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }
        )
        await updateHomestayStatus(row.id, targetStatus)
        ElMessage.success(`${actionText}成功`)
        // Refresh data to reflect the change
        fetchData();
        // Or update locally for better UX
        // const index = tableData.value.findIndex(item => item.id === row.id);
        // if (index !== -1) {
        //     tableData.value[index].status = targetStatus;
        // }
    } catch (error) {
        // User cancelled the action
        console.log('操作取消或失败');
    }
}

// 删除房源
const handleDelete = async (row: Homestay) => {
    try {
        await ElMessageBox.confirm('确认要删除该房源吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })
        await deleteHomestay(row.id)
        ElMessage.success('删除成功')
        fetchData()
    } catch (error) {
        console.log('删除操作取消或失败');
    }
}

// --- Review Dialog Actions --- 
const handleApprove = async () => {
    if (!currentReviewItem.value || !currentReviewItem.value.id) return;
    try {
        await updateHomestayStatus(currentReviewItem.value.id, 'ACTIVE');
        ElMessage.success('房源已批准');
        reviewDialogVisible.value = false;
        fetchData(); // Refresh list
    } catch (error) {
        console.error('批准房源失败:', error);
        ElMessage.error('批准操作失败');
    }
}

const handleReject = async () => {
    if (!currentReviewItem.value || !currentReviewItem.value.id) return;

    try {
        const { value } = await ElMessageBox.prompt('请输入拒绝理由', '拒绝房源', {
            confirmButtonText: '确定拒绝',
            cancelButtonText: '取消',
            inputPlaceholder: '拒绝理由不能为空',
            inputValidator: (val) => !!val, // Basic validation: not empty
            inputErrorMessage: '拒绝理由不能为空'
        });

        if (value) {
            console.log(`拒绝理由: ${value}`); // Log or send reason to backend
            await updateHomestayStatus(currentReviewItem.value.id, 'REJECTED');
            ElMessage.warning('房源已拒绝');
            reviewDialogVisible.value = false;
            fetchData(); // Refresh list
        }
    } catch (error) {
        if (error !== 'cancel') {
            console.error('拒绝房源失败:', error);
            ElMessage.error('拒绝操作失败');
        } else {
            console.log('用户取消拒绝操作');
        }
    }
}

// --- Batch Actions (Implementation needed) --- 
const handleBatchActive = async () => {
    if (selectedRows.value.length === 0) {
        ElMessage.warning('请至少选择一项')
        return
    }
    const rowsToActivate = selectedRows.value.filter(item => item.status !== 'ACTIVE');
    if (rowsToActivate.length === 0) {
        ElMessage.info('选中的房源都已是上架状态');
        return;
    }
    const ids = rowsToActivate.map(item => item.id);
    try {
        await ElMessageBox.confirm(`确认要批量上架选中的 ${ids.length} 个房源吗？`, '提示', { type: 'warning' });
        await batchUpdateHomestayStatus(ids, 'ACTIVE');
        ElMessage.success('批量上架成功');
        fetchData();
        selectedRows.value = []; // Clear selection
    } catch (error) {
        console.log('批量上架取消或失败');
    }
}

const handleBatchInactive = async () => {
    if (selectedRows.value.length === 0) {
        ElMessage.warning('请至少选择一项')
        return
    }
    const rowsToDeactivate = selectedRows.value.filter(item => item.status !== 'INACTIVE');
    if (rowsToDeactivate.length === 0) {
        ElMessage.info('选中的房源都已是下架状态或待审核/已拒绝');
        return;
    }
    const ids = rowsToDeactivate.map(item => item.id);
    try {
        await ElMessageBox.confirm(`确认要批量下架选中的 ${ids.length} 个房源吗？`, '提示', { type: 'warning' });
        await batchUpdateHomestayStatus(ids, 'INACTIVE');
        ElMessage.success('批量下架成功');
        fetchData();
        selectedRows.value = []; // Clear selection
    } catch (error) {
        console.log('批量下架取消或失败');
    }
}

const handleBatchDelete = async () => {
    if (selectedRows.value.length === 0) {
        ElMessage.warning('请至少选择一项')
        return
    }
    const ids = selectedRows.value.map(item => item.id);
    try {
        await ElMessageBox.confirm(`确认要批量删除选中的 ${ids.length} 个房源吗？此操作不可恢复！`, '危险操作确认', {
            confirmButtonText: '确定删除',
            cancelButtonText: '取消',
            type: 'error'
        });
        await batchDeleteHomestays(ids);
        ElMessage.success('批量删除成功');
        fetchData();
        selectedRows.value = []; // Clear selection
    } catch (error) {
        console.log('批量删除取消或失败');
    }
}

// Helper functions for display
const formatStatus = (status: string): string => {
    switch (status) {
        case 'PENDING': return '待审核';
        case 'ACTIVE': return '已上架';
        case 'INACTIVE': return '已下架';
        case 'REJECTED': return '已拒绝';
        default: return '未知';
    }
};

const getStatusTagType = (status: string): "" | "success" | "info" | "warning" | "danger" => {
    switch (status) {
        case 'PENDING': return 'warning';
        case 'ACTIVE': return 'success';
        case 'INACTIVE': return 'info';
        case 'REJECTED': return 'danger';
        default: return '';
    }
};

// --- 添加地址转换函数 ---
const getLocationText = (homestay: Homestay | null | undefined): string => {
    if (!homestay) return '地址信息不完整';
    const parts = [];
    try {
        if (homestay.provinceCode && codeToText[homestay.provinceCode]) {
            parts.push(codeToText[homestay.provinceCode]);
        } else if (homestay.provinceCode) {
            // console.warn(`Province code ${homestay.provinceCode} not found in codeToText`);
        }
        if (homestay.cityCode && codeToText[homestay.cityCode]) {
            const cityText = codeToText[homestay.cityCode];
            if (!parts.includes(cityText)) {
                parts.push(cityText);
            }
        } else if (homestay.cityCode) {
            // console.warn(`City code ${homestay.cityCode} not found in codeToText`);
        }
        if (homestay.districtCode && codeToText[homestay.districtCode]) {
            const districtText = codeToText[homestay.districtCode];
            if (!parts.includes(districtText)) {
                parts.push(districtText);
            }
        } else if (homestay.districtCode) {
            // console.warn(`District code ${homestay.districtCode} not found in codeToText`);
        }
        if (homestay.addressDetail) {
            parts.push(homestay.addressDetail);
        }

        const result = parts.length > 0 ? parts.join(' ') : '地址信息不完整';
        // --- 添加日志 --- 
        // console.log(`getLocationText for ID ${homestay.id}:`, result, 
        //    `Codes: P=${homestay.provinceCode}, C=${homestay.cityCode}, D=${homestay.districtCode}`, 
        //    `Detail: ${homestay.addressDetail}`);
        // --- 日志结束 ---
        return result;
    } catch (error) {
        console.error(`Error in getLocationText for homestay ID ${homestay.id}:`, error, homestay);
        return '地址处理错误';
    }
};
// --- 函数结束 ---

// Fetch initial data
onMounted(() => {
    fetchData();
});

</script>

<style scoped lang="scss">
.homestay-list {
    padding: 20px;
}

.search-box {
    margin-bottom: 20px;
}

.pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
}

.batch-operation {
    margin-bottom: 20px;

    .el-alert {
        align-items: center; // Vertically center content in alert
    }

    .batch-buttons {
        margin-left: 20px;
        display: inline-flex; // Keep buttons on the same line
        gap: 10px;
    }
}

.review-details {
    max-height: 75vh;
    overflow-y: auto;
    padding-right: 10px;
}

.info-card {
    margin-bottom: 15px;

    .card-header {
        font-weight: bold;
    }

    .el-descriptions__label {
        min-width: 80px;
    }

    .el-descriptions__content strong {
        font-weight: 600;
    }
}

.amenities-list {
    display: flex;
    flex-wrap: wrap;
}

.image-gallery {
    display: flex;
    flex-wrap: wrap;

    .image-slot {
        display: flex;
        justify-content: center;
        align-items: center;
        width: 100%;
        height: 100%;
        background: var(--el-fill-color-light);
        color: var(--el-text-color-secondary);
        font-size: 12px;
    }

    .image-slot .el-icon {
        font-size: 24px;
    }

    .cover-image-label {
        position: absolute;
        top: 0;
        left: 0;
        background-color: rgba(0, 0, 0, 0.5);
        color: white;
        font-size: 10px;
        padding: 1px 4px;
        border-radius: 4px 0 4px 0;
    }

    .cover-image-highlight {
        border: 2px solid var(--el-color-primary);
    }
}

.dialog-footer {
    display: flex;
    justify-content: flex-end;
    align-items: center;
}
</style>