<template>
    <div class="homestay-manage">
        <div class="header">
            <h2>我的房源管理</h2>
            <el-button type="primary" @click="handleCreateHomestay">添加新房源</el-button>
        </div>

        <el-card shadow="never" class="filter-card">
            <el-form :inline="true" :model="filterForm" class="filter-form">
                <el-form-item label="房源状态">
                    <el-select v-model="filterForm.status" placeholder="全部状态" clearable>
                        <el-option label="已上线" value="ACTIVE" />
                        <el-option label="待审核" value="PENDING" />
                        <el-option label="已下架" value="INACTIVE" />
                        <el-option label="审核拒绝" value="REJECTED" />
                    </el-select>
                </el-form-item>
                <el-form-item label="房源类型">
                    <el-select v-model="filterForm.type" placeholder="全部类型" clearable>
                        <el-option label="整套" value="ENTIRE" />
                        <el-option label="独立房间" value="PRIVATE" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleFilter">筛选</el-button>
                    <el-button @click="resetFilter">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <!-- 批量操作区域 -->
        <div class="batch-operation" v-if="selectedRows.length > 0">
            <el-alert title="批量操作" type="info" :closable="false" show-icon>
                <template #default>
                    已选择 <strong>{{ selectedRows.length }}</strong> 项
                    <div class="batch-buttons">
                        <el-button size="small" type="success" @click="handleBatchActivate">批量上线</el-button>
                        <el-button size="small" type="warning" @click="handleBatchDeactivate">批量下架</el-button>
                        <el-button size="small" type="danger" @click="handleBatchDelete">批量删除</el-button>
                    </div>
                </template>
            </el-alert>
        </div>

        <el-table :data="homestays" stripe style="width: 100%" v-loading="loading" :empty-text="emptyText"
            @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" />
            <el-table-column prop="coverImage" label="房源图片" width="180">
                <template #default="{ row }">
                    <el-image :src="row.coverImage || '/img/no-image.png'" style="width: 120px; height: 80px"
                        fit="cover" :preview-src-list="row.coverImage ? [row.coverImage] : []" />
                </template>
            </el-table-column>
            <el-table-column prop="title" label="房源名称" min-width="180" />
            <el-table-column prop="type" label="类型" width="100">
                <template #default="{ row }">
                    <el-tag>{{ row.type === 'ENTIRE' ? '整套' : '独立房间' }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="price" label="价格" width="120">
                <template #default="{ row }">
                    ¥{{ row.price }}/晚
                </template>
            </el-table-column>
            <el-table-column prop="maxGuests" label="最大入住" width="100">
                <template #default="{ row }">
                    {{ row.maxGuests }}人
                </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                    <el-tag :type="getStatusType(row.status)">
                        {{ getStatusText(row.status) }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column width="220" label="操作" fixed="right">
                <template #default="{ row }">
                    <div class="action-buttons">
                        <el-button v-if="row.status === 'ACTIVE'" size="small" type="warning"
                            @click="handleDeactivate(row.id)">
                            下架
                        </el-button>
                        <el-button v-if="row.status === 'INACTIVE' || row.status === 'PENDING'" size="small"
                            type="success" @click="handleActivate(row.id)">
                            上线
                        </el-button>
                        <el-button size="small" type="primary" @click="handleEdit(row.id)">
                            编辑
                        </el-button>
                        <el-dropdown trigger="click">
                            <el-button size="small">
                                更多<el-icon class="el-icon--right">
                                    <ArrowDown />
                                </el-icon>
                            </el-button>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <el-dropdown-item @click="handleViewOrders(row.id)">
                                        <el-icon>
                                            <Document />
                                        </el-icon> 查看订单
                                    </el-dropdown-item>
                                    <el-dropdown-item @click="handleDelete(row.id)" divided>
                                        <el-icon>
                                            <Delete />
                                        </el-icon> 删除
                                    </el-dropdown-item>
                                </el-dropdown-menu>
                            </template>
                        </el-dropdown>
                    </div>
                </template>
            </el-table-column>
        </el-table>

        <div class="empty-state" v-if="!loading && homestays.length === 0">
            <el-empty description="暂无房源数据">
                <el-button type="primary" @click="handleCreateHomestay">添加新房源</el-button>
            </el-empty>
        </div>

        <div class="pagination" v-if="total > 0">
            <el-pagination v-model:currentPage="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 30, 50]"
                layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
                @current-change="handleCurrentChange" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import {
    getOwnerHomestays,
    activateHomestay,
    deactivateHomestay,
    deleteHomestay,
    batchActivateHomestays,
    batchDeactivateHomestays,
    batchDeleteHomestays
} from '@/api/homestay';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { HomestayStatus } from '@/types';
import { ArrowDown, Document, Delete } from '@element-plus/icons-vue';

interface Homestay {
    id: number;
    title: string;
    coverImage: string;
    type: 'ENTIRE' | 'PRIVATE';
    price: number;
    maxGuests: number;
    status: HomestayStatus;
}

const router = useRouter();
const loading = ref(false);
const homestays = ref<Homestay[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const emptyText = ref('加载中...');
const selectedRows = ref<Homestay[]>([]);

const filterForm = ref({
    status: '',
    type: '',
});

// 获取房源列表
const fetchHomestays = async () => {
    try {
        loading.value = true;
        emptyText.value = '正在加载...';

        const response = await getOwnerHomestays({
            page: currentPage.value - 1,
            size: pageSize.value,
            status: filterForm.value.status,
            type: filterForm.value.type
        });

        if (response.data && Array.isArray(response.data.data)) {
            homestays.value = response.data.data;
            total.value = response.data.total || 0;

            if (homestays.value.length === 0) {
                if (filterForm.value.status || filterForm.value.type) {
                    emptyText.value = '没有符合筛选条件的房源';
                } else {
                    emptyText.value = '您还没有添加任何房源';
                }
            }
        } else {
            homestays.value = [];
            total.value = 0;
            emptyText.value = '数据格式错误，请联系管理员';
            console.error('返回的数据格式不正确', response);
        }
    } catch (error: any) {
        console.error('获取房源列表失败', error);
        homestays.value = [];
        total.value = 0;

        if (error.response && error.response.status === 403) {
            emptyText.value = '没有访问权限，请确认您已经注册为房东';
            ElMessage.error('没有访问权限，请确认您已经注册为房东');
        } else {
            emptyText.value = '加载失败，请刷新重试';
            ElMessage.error('获取房源列表失败: ' + (error.message || '未知错误'));
        }
    } finally {
        loading.value = false;
    }
};

// 页码变化
const handleCurrentChange = (page: number) => {
    currentPage.value = page;
    fetchHomestays();
};

// 每页数量变化
const handleSizeChange = (size: number) => {
    pageSize.value = size;
    fetchHomestays();
};

// 筛选
const handleFilter = () => {
    currentPage.value = 1;
    fetchHomestays();
};

// 重置筛选
const resetFilter = () => {
    filterForm.value = {
        status: '',
        type: '',
    };
    handleFilter();
};

// 创建新房源
const handleCreateHomestay = () => {
    router.push('/host/homestay/create');
};

// 编辑房源
const handleEdit = (id: number) => {
    router.push(`/host/homestay/edit/${id}`);
};

// 上线房源
const handleActivate = async (id: number) => {
    try {
        await activateHomestay(id);
        ElMessage.success('房源已上线');
        fetchHomestays();
    } catch (error) {
        console.error('上线房源失败', error);
        ElMessage.error('上线房源失败');
    }
};

// 下架房源
const handleDeactivate = async (id: number) => {
    try {
        await deactivateHomestay(id);
        ElMessage.success('房源已下架');
        fetchHomestays();
    } catch (error) {
        console.error('下架房源失败', error);
        ElMessage.error('下架房源失败');
    }
};

// 查看房源订单
const handleViewOrders = (id: number) => {
    router.push(`/host/orders?homestayId=${id}`);
};

// 删除房源
const handleDelete = async (id: number) => {
    try {
        // 添加确认对话框
        await ElMessageBox.confirm(
            '删除房源后将无法恢复，确定要删除吗？',
            '删除提示',
            {
                confirmButtonText: '确定删除',
                cancelButtonText: '取消',
                type: 'warning',
                confirmButtonClass: 'el-button--danger'
            }
        );

        // 用户确认后执行删除
        const res = await deleteHomestay(id);
        ElMessage.success('房源已删除');
        fetchHomestays();
    } catch (error: any) {
        // 用户取消不显示错误
        if (error === 'cancel' || error.toString().includes('cancel')) {
            return;
        }
        console.error('删除房源失败', error);
        ElMessage.error('删除房源失败: ' + (error.message || '未知错误'));
    }
};

// 房源状态对应的标签类型
const getStatusType = (status: HomestayStatus): string => {
    const types: Record<HomestayStatus, string> = {
        ACTIVE: 'success',
        PENDING: 'warning',
        INACTIVE: 'info',
        REJECTED: 'danger'
    };
    return types[status] || 'info';
};

// 房源状态对应的文本
const getStatusText = (status: HomestayStatus): string => {
    const texts: Record<HomestayStatus, string> = {
        ACTIVE: '已上线',
        PENDING: '待审核',
        INACTIVE: '已下架',
        REJECTED: '已拒绝'
    };
    return texts[status] || '未知状态';
};

// 处理表格选择变化
const handleSelectionChange = (selection: Homestay[]) => {
    selectedRows.value = selection;
};

// 批量上线房源
const handleBatchActivate = async () => {
    if (selectedRows.value.length === 0) {
        ElMessage.warning('请至少选择一项');
        return;
    }

    try {
        await ElMessageBox.confirm('确认要批量上线选中的房源吗？', '批量操作', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        });

        const ids = selectedRows.value.map(item => item.id);
        await batchActivateHomestays(ids);
        ElMessage.success('批量上线成功');
        fetchHomestays();
    } catch (error) {
        console.error('批量上线失败:', error);
    }
};

// 批量下架房源
const handleBatchDeactivate = async () => {
    if (selectedRows.value.length === 0) {
        ElMessage.warning('请至少选择一项');
        return;
    }

    try {
        await ElMessageBox.confirm('确认要批量下架选中的房源吗？', '批量操作', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        });

        const ids = selectedRows.value.map(item => item.id);
        await batchDeactivateHomestays(ids);
        ElMessage.success('批量下架成功');
        fetchHomestays();
    } catch (error) {
        console.error('批量下架失败:', error);
    }
};

// 批量删除房源
const handleBatchDelete = async () => {
    if (selectedRows.value.length === 0) {
        ElMessage.warning('请至少选择一项');
        return;
    }

    try {
        await ElMessageBox.confirm(`确认要删除选中的 ${selectedRows.value.length} 个房源吗？此操作不可恢复！`, '批量操作', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        });

        const ids = selectedRows.value.map(item => item.id);
        await batchDeleteHomestays(ids);
        ElMessage.success('批量删除成功');
        fetchHomestays();
    } catch (error) {
        console.error('批量删除失败:', error);
    }
};

onMounted(() => {
    fetchHomestays();
});
</script>

<style scoped>
.homestay-manage {
    padding: 20px;
}

.header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
}

.filter-card {
    margin-bottom: 20px;
}

.filter-form {
    display: flex;
    flex-wrap: wrap;
}

.empty-state {
    padding: 40px 0;
    text-align: center;
}

.pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
}

/* 添加按钮布局样式 */
.action-buttons {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.batch-operation {
    margin-bottom: 20px;
}

.batch-buttons {
    display: inline-block;
    margin-left: 15px;
}
</style>