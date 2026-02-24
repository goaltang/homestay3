<template>
    <div class="homestay-list">
        <div class="search-box">
            <el-form :inline="true" :model="searchForm">
                <el-form-item label="房源名称">
                    <el-input v-model="searchForm.name" placeholder="请输入房源名称" clearable />
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
                        <el-option label="草稿" value="DRAFT" />
                        <el-option label="待审核" value="PENDING" />
                        <el-option label="已上架" value="ACTIVE" />
                        <el-option label="已下架" value="INACTIVE" />
                        <el-option label="已拒绝" value="REJECTED" />
                        <el-option label="已暂停" value="SUSPENDED" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">搜索</el-button>
                    <el-button @click="resetSearch">重置</el-button>
                    <el-button type="success" @click="handleAdd">新增房源</el-button>
                    <el-button type="warning" @click="goToAuditWorkbench" v-if="hasPendingItems">去审核中心</el-button>
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
            <el-table-column prop="status" label="状态" width="120">
                <template #default="scope">
                    <el-tag :type="getStatusTagType(scope.row.status)" size="small">
                        {{ formatStatus(scope.row.status) }}
                    </el-tag>
                </template>
            </el-table-column>

            <el-table-column label="操作" width="300" fixed="right">
                <template #default="scope">
                    <!-- 待审核状态 - 引导到审核工作台 -->
                    <template v-if="scope.row.status === 'PENDING'">
                        <el-button type="warning" link @click="goToAuditWorkbenchWithId(scope.row.id)">
                            <el-icon>
                                <Document />
                            </el-icon>
                            去审核
                        </el-button>
                        <el-button type="primary" link @click="handleViewDetail(scope.row)">查看详情</el-button>
                        <el-button type="info" link @click="handleViewAuditLogs(scope.row)">审核记录</el-button>
                    </template>

                    <!-- 已上架状态 -->
                    <template v-if="scope.row.status === 'ACTIVE'">
                        <el-button type="info" link @click="handleToggleStatus(scope.row)">下架</el-button>
                        <el-button type="primary" link @click="handleEdit(scope.row)">编辑</el-button>
                        <el-button type="success" link @click="handleViewAuditLogs(scope.row)">审核记录</el-button>
                    </template>

                    <!-- 已下架状态 -->
                    <template v-if="scope.row.status === 'INACTIVE'">
                        <el-button type="success" link @click="handleToggleStatus(scope.row)">上架</el-button>
                        <el-button type="primary" link @click="handleEdit(scope.row)">编辑</el-button>
                        <el-button type="info" link @click="handleViewAuditLogs(scope.row)">审核记录</el-button>
                    </template>

                    <!-- 已拒绝状态 -->
                    <template v-if="scope.row.status === 'REJECTED'">
                        <el-button type="primary" link @click="handleViewDetail(scope.row)">查看详情</el-button>
                        <el-button type="warning" link @click="handleViewAuditLogs(scope.row)">审核记录</el-button>
                        <el-button type="danger" link @click="handleDelete(scope.row)">删除</el-button>
                    </template>

                    <!-- 其他状态 -->
                    <template v-if="['DRAFT', 'SUSPENDED'].includes(scope.row.status)">
                        <el-button type="primary" link @click="handleEdit(scope.row)">编辑</el-button>
                        <el-button type="info" link @click="handleViewAuditLogs(scope.row)">审核记录</el-button>
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

        <!-- 房源详情对话框 -->
        <el-dialog v-model="detailDialogVisible" title="房源详情" width="80%" top="5vh">
            <div v-if="currentDetailItem" class="detail-content">
                <el-descriptions :column="2" border>
                    <el-descriptions-item label="房源ID">{{ currentDetailItem.id }}</el-descriptions-item>
                    <el-descriptions-item label="状态">
                        <el-tag :type="getStatusTagType(currentDetailItem.status)" size="small">
                            {{ formatStatus(currentDetailItem.status) }}
                        </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="房源名称" :span="2">
                        <strong>{{ currentDetailItem.title }}</strong>
                    </el-descriptions-item>
                    <el-descriptions-item label="价格">¥{{ currentDetailItem.price }}/晚</el-descriptions-item>
                    <el-descriptions-item label="房源类型">{{ currentDetailItem.type || 'N/A' }}</el-descriptions-item>
                    <el-descriptions-item label="地址" :span="2">
                        {{ getLocationText(currentDetailItem) }}
                    </el-descriptions-item>
                    <el-descriptions-item label="描述" :span="2">
                        {{ currentDetailItem.description || '暂无描述' }}
                    </el-descriptions-item>
                </el-descriptions>
            </div>
            <template #footer>
                <el-button @click="detailDialogVisible = false">关闭</el-button>
            </template>
        </el-dialog>

        <!-- 审核记录查看对话框 -->
        <el-dialog v-model="auditLogsDialogVisible" title="审核记录" width="70%">
            <div v-if="selectedHomestay">
                <h4>{{ selectedHomestay.title }} - 审核历史</h4>
                <el-table :data="auditLogsData" border style="width: 100%;" v-loading="auditLogsLoading">
                    <el-table-column prop="createdAt" label="操作时间" width="160">
                        <template #default="scope">
                            {{ formatDateTime(scope.row.createdAt) }}
                        </template>
                    </el-table-column>
                    <el-table-column prop="actionType" label="操作类型" width="100">
                        <template #default="scope">
                            <el-tag :type="getAuditLogType(scope.row.actionType)" size="small">
                                {{ getActionTypeText(scope.row.actionType) }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="状态变更" width="150">
                        <template #default="scope">
                            <span>{{ formatStatus(scope.row.oldStatus) }}</span>
                            <el-icon style="margin: 0 5px;">
                                <ArrowRight />
                            </el-icon>
                            <span>{{ formatStatus(scope.row.newStatus) }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column prop="reviewerName" label="操作人" width="120" />
                    <el-table-column prop="reviewReason" label="原因" />
                </el-table>

                <div class="pagination" style="margin-top: 20px;">
                    <el-pagination v-model:current-page="auditLogsPage" v-model:page-size="auditLogsPageSize"
                        :page-sizes="[5, 10, 20]" :total="auditLogsTotal" layout="total, sizes, prev, pager, next"
                        @size-change="handleAuditLogsPageSizeChange" @current-change="handleAuditLogsPageChange" />
                </div>
            </div>
            <template #footer>
                <el-button @click="auditLogsDialogVisible = false">关闭</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, ArrowRight } from '@element-plus/icons-vue'
import {
    getHomestayList,
    deleteHomestay,
    updateHomestayStatus,
    batchDeleteHomestays,
    batchUpdateHomestayStatus,
    getHomestayDetail,
    getHomestayAuditLogs
} from '@/api/homestay'
import type { Homestay } from '@/types'
// 简化的区域代码映射
const codeToText: Record<string, string> = {
    '11': '北京市',
    '12': '天津市',
    '31': '上海市',
    '50': '重庆市',
    // 可以根据需要添加更多地区代码
}

// 定义 AuditLog 类型
interface AuditLog {
    id: number;
    homestayId: number;
    homestayTitle: string;
    reviewerId: number;
    reviewerName: string;
    oldStatus: string;
    newStatus: string;
    actionType: string;
    reviewReason?: string;
    reviewNotes?: string;
    createdAt: string;
    ipAddress?: string;
}

const router = useRouter()

// 搜索表单
const searchForm = reactive({
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

// 详情对话框
const detailDialogVisible = ref(false)
const currentDetailItem = ref<Homestay | null>(null)

// 审核记录对话框
const auditLogsDialogVisible = ref(false)
const selectedHomestay = ref<Homestay | null>(null)
const auditLogsData = ref<AuditLog[]>([])
const auditLogsLoading = ref(false)
const auditLogsPage = ref(1)
const auditLogsPageSize = ref(10)
const auditLogsTotal = ref(0)

// 计算属性
const hasPendingItems = computed(() => {
    return tableData.value.some(item => item.status === 'PENDING')
})

// 跳转到审核工作台
const goToAuditWorkbench = () => {
    router.push('/audit/workbench')
}

const goToAuditWorkbenchWithId = (id: number) => {
    router.push('/audit/workbench?selectedId=' + id)
}

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
        const res = await getHomestayList({
            page: currentPage.value,
            pageSize: pageSize.value,
            name: searchForm.name,
            status: searchForm.status
        });

        if (res && res.list) {
            tableData.value = res.list;
            total.value = res.total;
        } else {
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

// 查看详情
const handleViewDetail = async (row: Homestay) => {
    try {
        const homestayDetail = await getHomestayDetail(row.id);
        currentDetailItem.value = homestayDetail;
        detailDialogVisible.value = true;
    } catch (error) {
        console.error('获取房源详情失败:', error);
        ElMessage.error('获取房源详情失败');
    }
}

// 切换状态
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
        fetchData();
    } catch (error) {
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

// 批量操作
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
        selectedRows.value = [];
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
        ElMessage.info('选中的房源都已是下架状态');
        return;
    }
    const ids = rowsToDeactivate.map(item => item.id);
    try {
        await ElMessageBox.confirm(`确认要批量下架选中的 ${ids.length} 个房源吗？`, '提示', { type: 'warning' });
        await batchUpdateHomestayStatus(ids, 'INACTIVE');
        ElMessage.success('批量下架成功');
        fetchData();
        selectedRows.value = [];
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
        selectedRows.value = [];
    } catch (error) {
        console.log('批量删除取消或失败');
    }
}

// 查看审核记录
const handleViewAuditLogs = async (row: Homestay) => {
    selectedHomestay.value = row;
    auditLogsPage.value = 1;
    auditLogsDialogVisible.value = true;
    await loadAuditLogsData();
}

// 加载审核记录数据
const loadAuditLogsData = async () => {
    if (!selectedHomestay.value) return;

    try {
        auditLogsLoading.value = true;
        const result = await getHomestayAuditLogs(
            selectedHomestay.value.id,
            auditLogsPage.value,
            auditLogsPageSize.value
        );
        auditLogsData.value = result.list;
        auditLogsTotal.value = result.total;
    } catch (error) {
        console.error('获取审核记录失败:', error);
        ElMessage.error('获取审核记录失败');
    } finally {
        auditLogsLoading.value = false;
    }
}

// 审核记录分页处理
const handleAuditLogsPageChange = (page: number) => {
    auditLogsPage.value = page;
    loadAuditLogsData();
}

const handleAuditLogsPageSizeChange = (size: number) => {
    auditLogsPageSize.value = size;
    auditLogsPage.value = 1;
    loadAuditLogsData();
}

// 工具方法
const formatStatus = (status: string): string => {
    switch (status) {
        case 'DRAFT': return '草稿';
        case 'PENDING': return '待审核';
        case 'ACTIVE': return '已上架';
        case 'INACTIVE': return '已下架';
        case 'REJECTED': return '已拒绝';
        case 'SUSPENDED': return '已暂停';
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

const formatDateTime = (dateTime: string | null | undefined): string => {
    if (!dateTime) return '未知时间';
    try {
        return new Date(dateTime).toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch (error) {
        return '时间格式错误';
    }
}

const getActionTypeText = (actionType: string): string => {
    switch (actionType) {
        case 'SUBMIT': return '提交';
        case 'APPROVE': return '批准';
        case 'REJECT': return '拒绝';
        case 'REVIEW': return '审核';
        case 'UPDATE': return '更新';
        case 'DELETE': return '删除';
        case 'RESUBMIT': return '重新提交';
        default: return actionType;
    }
}

const getAuditLogType = (actionType: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' => {
    switch (actionType) {
        case 'APPROVE': return 'success';
        case 'REJECT': return 'danger';
        case 'SUBMIT':
        case 'RESUBMIT': return 'primary';
        default: return 'warning';
    }
}

const getLocationText = (homestay: Homestay | null | undefined): string => {
    if (!homestay) return '地址信息不完整';
    const parts = [];
    try {
        if (homestay.provinceCode && codeToText[homestay.provinceCode]) {
            parts.push(codeToText[homestay.provinceCode]);
        }
        if (homestay.cityCode && codeToText[homestay.cityCode]) {
            const cityText = codeToText[homestay.cityCode];
            if (!parts.includes(cityText)) {
                parts.push(cityText);
            }
        }
        if (homestay.districtCode && codeToText[homestay.districtCode]) {
            const districtText = codeToText[homestay.districtCode];
            if (!parts.includes(districtText)) {
                parts.push(districtText);
            }
        }
        if (homestay.addressDetail) {
            parts.push(homestay.addressDetail);
        }
        return parts.length > 0 ? parts.join(' ') : '地址信息不完整';
    } catch (error) {
        console.error(`Error in getLocationText for homestay ID ${homestay.id}:`, error, homestay);
        return '地址处理错误';
    }
};

// 初始化
onMounted(() => {
    fetchData();
});
</script>

<style scoped lang="scss">
.homestay-list {
    padding: 20px;

    .search-box {
        margin-bottom: 20px;
        padding: 20px;
        background: #f5f7fa;
        border-radius: 8px;
    }

    .batch-operation {
        margin-bottom: 20px;

        .batch-buttons {
            margin-top: 10px;
        }
    }

    .pagination {
        margin-top: 20px;
        text-align: center;
    }

    .detail-content {
        padding: 20px 0;
    }
}
</style>