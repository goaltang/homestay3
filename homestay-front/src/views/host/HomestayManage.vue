<template>
    <div class="homestay-manage">
        <div class="header">
            <h2>我的房源管理</h2>
            <div class="header-buttons">
                <el-button v-if="isDev" type="success" @click="handleQuickAddHomestay" :loading="quickAddLoading">
                    一键添加测试房源
                </el-button>
                <el-button type="primary" @click="handleCreateHomestay">添加新房源</el-button>
            </div>
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
                        <el-option v-for="item in homestayTypeOptions" :key="item.value" :label="item.label"
                            :value="item.value" />
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
                    <el-tag>{{ getHomestayTypeLabel(row.type) }}</el-tag>
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
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import {
    getOwnerHomestays,
    activateHomestay,
    deactivateHomestay,
    deleteHomestay,
    batchActivateHomestays,
    batchDeactivateHomestays,
    batchDeleteHomestays,
    createHomestay,
    getHomestayTypesForFilter
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

const homestayTypeOptions = ref<{ label: string; value: string }[]>([]);

const fetchHomestayTypes = async () => {
    try {
        const response = await getHomestayTypesForFilter();
        if (response && response.data && response.data.success && Array.isArray(response.data.data)) {
            homestayTypeOptions.value = response.data.data;
        } else {
            console.error('获取房源类型数据格式错误:', response);
            homestayTypeOptions.value = [];
            ElMessage.warning('加载房源类型选项失败，请检查接口返回');
        }
    } catch (error) {
        console.error('获取房源类型接口请求失败:', error);
        homestayTypeOptions.value = [];
        ElMessage.error('加载房源类型选项时出错');
    }
};

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
    try {
        console.log(`准备编辑房源，ID: ${id}`);
        router.push(`/host/homestay/edit/${id}`);
    } catch (error) {
        console.error('编辑房源导航失败', error);
        ElMessage.error('无法跳转到编辑页面，请重试');
    }
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

        // 显示加载状态
        loading.value = true;

        // 用户确认后执行删除
        const res = await deleteHomestay(id);

        // 检查返回结果
        if (res && (res.data?.success || res.status === 200 || res.status === 204)) {
            ElMessage.success('房源已成功删除');
            fetchHomestays();
        } else {
            throw new Error('删除请求未返回成功状态');
        }
    } catch (error: any) {
        // 用户取消不显示错误
        if (error === 'cancel' || error.toString().includes('cancel')) {
            return;
        }

        let errorMessage = '未知错误';

        if (error.response) {
            // 根据HTTP状态码提供具体错误信息
            const status = error.response.status;

            if (status === 403) {
                errorMessage = '您没有权限删除此房源';
            } else if (status === 404) {
                errorMessage = '房源不存在或已被删除';
            } else if (status === 400) {
                errorMessage = '请求参数有误';
            } else if (status === 500) {
                errorMessage = '服务器内部错误，请稍后重试';
            } else {
                errorMessage = `服务器返回错误(${status})`;
            }

            // 如果有详细错误信息，优先使用
            if (error.response.data?.message) {
                errorMessage = error.response.data.message;
            } else if (error.response.data?.error) {
                errorMessage = error.response.data.error;
            }
        } else if (error.request) {
            // 请求发出但没有收到响应
            errorMessage = '服务器无响应，请检查网络连接';
        } else {
            // 请求设置触发的错误
            errorMessage = error.message || '删除请求发送失败';
        }

        console.error('删除房源失败', error);
        ElMessage.error('删除房源失败: ' + errorMessage);

        // 可选：询问用户是否重试
        try {
            const shouldRetry = await ElMessageBox.confirm(
                '删除操作失败，是否重试？',
                '重试确认',
                {
                    confirmButtonText: '重试',
                    cancelButtonText: '取消',
                    type: 'info'
                }
            );

            if (shouldRetry) {
                handleDelete(id);
            }
        } catch {
            // 用户取消重试，不做处理
        }
    } finally {
        loading.value = false;
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

// 检查是否为开发环境
const isDev = computed(() => {
    return import.meta.env.MODE === 'development' || window.location.hostname === 'localhost';
});

// 一键添加测试房源
const quickAddLoading = ref(false);
const handleQuickAddHomestay = async () => {
    if (!isDev.value) {
        ElMessage.warning('此功能仅在开发环境可用');
        return;
    }

    try {
        quickAddLoading.value = true;

        // 随机数，用于避免标题重复
        const randomNum = Math.floor(Math.random() * 10000);

        // 随机房源标题
        const titles = [
            '湖景豪华套房',
            '城市中心温馨公寓',
            '西湖边的家',
            '市中心江景公寓',
            '古城墙附近的民宿',
            '梦幻花园独栋别墅',
            '山景舒适度假屋',
            '现代loft公寓',
            '运河边的小屋',
            '临湖浪漫套房'
        ];

        // 随机设施列表
        const allAmenities = [
            'WIFI', 'AIR_CONDITIONING', 'KITCHEN', 'WASHER', 'PARKING', 'POOL',
            'TV', 'WORKSPACE', 'DRYER', 'HEATING', 'BREAKFAST',
            'GYM', 'HOT_TUB', 'BBQ', 'FIRST_AID', 'SMOKE_ALARM'
        ];

        // 随机选择3-8个设施
        const selectedAmenities: string[] = [];
        const amenityCount = Math.floor(Math.random() * 6) + 3; // 3-8个设施
        while (selectedAmenities.length < amenityCount) {
            const randomIndex = Math.floor(Math.random() * allAmenities.length);
            const amenity = allAmenities[randomIndex];
            if (!selectedAmenities.includes(amenity)) {
                selectedAmenities.push(amenity);
            }
        }

        // 随机地址
        const streets = ['翠苑路', '文三路', '莫干山路', '西湖大道', '教工路', '余杭塘路', '湖墅南路', '凤起路'];
        const randomStreet = streets[Math.floor(Math.random() * streets.length)];
        const randomNum1 = Math.floor(Math.random() * 500) + 1;
        const randomNum2 = Math.floor(Math.random() * 20) + 1;
        const randomAddress = `${randomStreet}${randomNum1}号${randomNum2}单元`;

        // 构建测试房源数据
        const testHomestay = {
            title: Math.random() > 0.5 ? `${titles[Math.floor(Math.random() * titles.length)]} #${randomNum}` : `测试房源 #${randomNum}`,
            type: Math.random() > 0.5 ? 'ENTIRE' : 'PRIVATE',
            price: String(Math.floor(Math.random() * 500) + 100), // 100-600元随机价格
            status: 'INACTIVE', // 默认不上线，避免干扰
            maxGuests: Math.floor(Math.random() * 6) + 1, // 1-6人
            minNights: Math.floor(Math.random() * 3) + 1, // 1-3晚
            province: 'zhejiang',
            city: 'hangzhou',
            district: 'xihu',
            address: randomAddress,
            amenities: [], // 设置为空数组，稍后通过API添加
            description: `这是一个自动生成的测试房源(#${randomNum})。位于美丽的西湖附近，交通便利，周边设施齐全。\n\n房源亮点：\n- 位置优越，靠近景点\n- 设施齐全，温馨舒适\n- 适合商务和休闲旅行\n\n温馨提示：此房源仅用于开发和测试目的。`,
            // 使用空字符串或相对路径代替外部URL
            coverImage: '',
            images: [],
            featured: false
        };

        // 调用创建API
        const response = await createHomestay(testHomestay);

        if (response && response.data) {
            const newHomestayId = response.data.id;

            // 添加设施
            if (selectedAmenities.length > 0) {
                try {
                    // 引入设施API
                    import('@/api/amenities').then(async ({ addAmenityToHomestayApi }) => {
                        let addedCount = 0;
                        for (const amenity of selectedAmenities) {
                            try {
                                const result = await addAmenityToHomestayApi(newHomestayId, amenity);
                                if (result.data && result.data.success) {
                                    addedCount++;
                                    console.log(`成功添加设施: ${amenity}`);
                                }
                            } catch (error) {
                                console.error(`添加设施${amenity}失败:`, error);
                            }
                        }
                        console.log(`设施添加成功，共添加${addedCount}个设施`);
                    });
                } catch (amenityError) {
                    console.error('设施添加失败:', amenityError);
                }
            }

            ElMessage.success(`测试房源"${testHomestay.title}"创建成功，请添加图片`);

            // 询问用户是否立即编辑添加图片
            ElMessageBox.confirm(
                '房源创建成功！是否立即前往编辑页面添加图片？',
                '添加图片',
                {
                    confirmButtonText: '立即添加图片',
                    cancelButtonText: '稍后再说',
                    type: 'info'
                }
            ).then(() => {
                // 跳转到编辑页面
                router.push(`/host/homestay/edit/${newHomestayId}`);
            }).catch(() => {
                // 用户选择稍后添加图片，只刷新列表
                fetchHomestays();
            });
        } else {
            throw new Error('创建失败，服务器未返回期望的响应');
        }
    } catch (error: any) {
        console.error('创建测试房源失败', error);

        // 添加详细的错误处理逻辑
        let errorMessage = '未知错误';
        if (error.response) {
            // 服务器返回了错误响应
            console.error('服务器响应错误：', error.response.status, error.response.data);
            if (error.response.data && error.response.data.error) {
                errorMessage = error.response.data.error;
            } else if (error.response.data && error.response.data.message) {
                errorMessage = error.response.data.message;
            } else {
                errorMessage = `服务器错误 (${error.response.status})`;
            }
        } else if (error.request) {
            // 请求已发出，但没有收到响应
            console.error('没有收到服务器响应');
            errorMessage = '服务器无响应，请检查网络连接';
        } else {
            // 请求配置出错
            errorMessage = error.message || '请求错误';
        }

        ElMessage.error('创建测试房源失败: ' + errorMessage);
    } finally {
        quickAddLoading.value = false;
    }
};

// --- 新增：根据类型代码获取显示标签 ---
const getHomestayTypeLabel = (typeCode: string): string => {
    const foundType = homestayTypeOptions.value.find(option => option.value === typeCode);
    return foundType ? foundType.label : typeCode; // 找不到时返回原始代码
};

onMounted(() => {
    fetchHomestays();
    fetchHomestayTypes();
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

.header-buttons {
    display: flex;
    gap: 10px;
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