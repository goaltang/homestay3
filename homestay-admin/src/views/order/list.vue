<template>
    <div class="order-list app-container">
        <!-- 搜索区域 -->
        <div class="search-box">
            <el-form :inline="true" :model="searchForm" ref="searchFormRef">
                <el-form-item label="订单号" prop="orderNumber">
                    <el-input v-model="searchForm.orderNumber" placeholder="请输入订单号" clearable />
                </el-form-item>
                <el-form-item label="住客名称" prop="guestName">
                    <el-input v-model="searchForm.guestName" placeholder="请输入住客名称" clearable />
                </el-form-item>
                <el-form-item label="房源标题" prop="homestayTitle">
                    <el-input v-model="searchForm.homestayTitle" placeholder="请输入房源标题" clearable />
                </el-form-item>
                <el-form-item label="房东名称" prop="hostName">
                    <el-input v-model="searchForm.hostName" placeholder="请输入房东名称" clearable />
                </el-form-item>
                <el-form-item label="订单状态" prop="status">
                    <el-select v-model="searchForm.status" placeholder="订单状态" clearable>
                        <el-option v-for="(item, key) in orderStatusMap" :key="key" :label="item.text" :value="key" />
                    </el-select>
                </el-form-item>
                <el-form-item label="支付状态" prop="paymentStatus">
                    <el-select v-model="searchForm.paymentStatus" placeholder="支付状态" clearable>
                        <el-option v-for="(item, key) in paymentStatusMap" :key="key" :label="item.text" :value="key" />
                    </el-select>
                </el-form-item>
                <el-form-item label="支付方式" prop="paymentMethod">
                    <el-input v-model="searchForm.paymentMethod" placeholder="支付方式 (e.g., alipay)" clearable />
                    <!-- 或者使用 Select，如果支付方式固定 -->
                    <!-- <el-select v-model="searchForm.paymentMethod" placeholder="支付方式" clearable> ... </el-select> -->
                </el-form-item>
                <el-form-item label="入住日期" prop="checkInDateRange">
                    <el-date-picker v-model="searchForm.checkInDateRange" type="daterange" range-separator="至"
                        start-placeholder="开始日期" end-placeholder="结束日期" clearable />
                </el-form-item>
                <el-form-item label="创建日期" prop="createTimeRange">
                    <el-date-picker v-model="searchForm.createTimeRange" type="daterange" range-separator="至"
                        start-placeholder="开始日期" end-placeholder="结束日期" clearable />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch" :icon="Search">搜索</el-button>
                    <el-button @click="resetSearch" :icon="Refresh">重置</el-button>
                    <el-button type="success" @click="handleExport" :icon="Download">导出订单</el-button>
                </el-form-item>
            </el-form>
        </div>

        <!-- 批量操作区域 -->
        <div class="batch-operation" v-if="selectedRows.length > 0">
            <el-alert title="批量操作" type="info" :closable="false" show-icon>
                <template #default>
                    已选择 <strong>{{ selectedRows.length }}</strong> 项
                    <div class="batch-buttons">
                        <el-button size="small" type="success" @click="handleBatchComplete">批量完成</el-button>
                        <el-button size="small" type="danger" @click="handleBatchCancel">批量取消</el-button>
                        <el-button size="small" type="primary" @click="handleBatchExport">批量导出</el-button>
                    </div>
                </template>
            </el-alert>
        </div>

        <el-table :data="tableData" border style="width: 100%" v-loading="loading"
            @selection-change="handleSelectionChange" @sort-change="handleSortChange"
            :default-sort="{ prop: 'createTime', order: 'descending' }">
            <el-table-column type="selection" width="50" align="center" />
            <el-table-column prop="orderNumber" label="订单号" width="190" sortable="custom" />
            <el-table-column prop="homestayTitle" label="房源名称" min-width="150" show-overflow-tooltip />
            <el-table-column prop="hostName" label="房东名称" width="120" sortable="custom" show-overflow-tooltip />
            <el-table-column prop="guestName" label="住客名称" width="120" sortable="custom" show-overflow-tooltip />
            <el-table-column prop="totalAmount" label="总金额" width="110" sortable="custom">
                <template #default="scope">
                    ¥{{ scope.row.totalAmount?.toFixed(2) }}
                </template>
            </el-table-column>
            <el-table-column prop="status" label="订单状态" width="110" sortable="custom">
                <template #default="scope">
                    <el-tag :type="getOrderStatusType(scope.row.status)" size="small">
                        {{ getOrderStatusText(scope.row.status) }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="paymentStatus" label="支付状态" width="110" sortable="custom">
                <template #default="scope">
                    <el-tag :type="getPaymentStatusType(scope.row.paymentStatus)" size="small">
                        {{ getPaymentStatusText(scope.row.paymentStatus) }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="paymentMethod" label="支付方式" width="120" sortable="custom" show-overflow-tooltip />
            <el-table-column prop="checkInDate" label="入住日期" width="120" sortable="custom" />
            <el-table-column prop="checkOutDate" label="退房日期" width="120" sortable="custom" />
            <el-table-column prop="createTime" label="创建时间" width="170" sortable="custom">
                <template #default="scope">
                    {{ format(new Date(scope.row.createTime), 'yyyy-MM-dd HH:mm:ss') }}
                </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作" width="180" fixed="right" align="center">
                <template #default="scope">
                    <el-button type="primary" link size="small" @click="handleDetail(scope.row)">详情</el-button>
                    <el-button type="success" link size="small" @click="handleConfirmPayment(scope.row)"
                        v-if="scope.row.paymentStatus === 'UNPAID'">
                        确认支付
                    </el-button>
                    <el-button type="warning" link size="small" @click="handleRefund(scope.row)"
                        v-if="scope.row.paymentStatus === 'PAID'">
                        发起退款
                    </el-button>
                    <el-button type="primary" link size="small" @click="handleComplete(scope.row)"
                        v-if="scope.row.status === 'PAID' || scope.row.status === 'CHECKED_IN'">
                        完成
                    </el-button>
                    <el-button type="danger" link size="small" @click="handleCancel(scope.row)"
                        v-if="['PENDING', 'CONFIRMED', 'PAID'].includes(scope.row.status) && scope.row.paymentStatus !== 'REFUNDED'">
                        取消
                    </el-button>
                </template>
            </el-table-column>
        </el-table>

        <div class="pagination">
            <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize"
                :page-sizes="[10, 20, 50, 100]" :total="total" layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSizeChange" @current-change="handleCurrentChange" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { format } from 'date-fns'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import {
    getAdminOrders,
    updateOrderStatus,
    exportOrders,
    batchUpdateOrderStatus,
    batchExportOrders,
    deleteOrder,
    batchDeleteOrders,
    confirmPayment,
    initiateRefund
} from '@/api/order'

// 定义后端返回的 Order 数据类型 (需要与 OrderDTO.java 对应)
interface AdminOrder {
    id: number;
    orderNumber: string;
    homestayId: number;
    homestayTitle: string;
    guestId: number;
    guestName: string;
    guestPhone: string;
    hostId: number;
    hostName: string;
    checkInDate: string; // LocalDate -> string
    checkOutDate: string; // LocalDate -> string
    nights: number;
    guestCount: number;
    price: number;
    totalAmount: number;
    status: string; // 订单状态 (e.g., PENDING, PAID, CANCELLED)
    paymentStatus: string | null; // 支付状态 (e.g., UNPAID, PAID, REFUNDED)
    paymentMethod: string | null; // 支付方式
    remark: string | null;
    createTime: string; // LocalDateTime -> string
    updateTime: string; // LocalDateTime -> string
}

// 搜索表单的类型 (根据后端 AdminOrderController 的 getOrders 参数调整)
interface OrderSearchForm {
    orderNumber: string;
    guestName: string;
    homestayTitle: string;
    status: string;
    paymentStatus: string;
    paymentMethod: string;
    hostName: string;
    checkInDateRange: [string, string] | null; // [start, end]
    createTimeRange: [string, string] | null; // [start, end]
}

// 搜索表单
const searchForm = reactive<OrderSearchForm>({
    orderNumber: '',
    guestName: '',
    homestayTitle: '',
    status: '',
    paymentStatus: '',
    paymentMethod: '',
    hostName: '',
    checkInDateRange: null,
    createTimeRange: null
})

// 表格数据
const tableData = ref<AdminOrder[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedRows = ref<AdminOrder[]>([])
const sortInfo = ref({ prop: 'createTime', order: 'descending' }) // 默认排序

// 状态映射
const orderStatusMap = {
    PENDING: { text: '待确认', type: 'warning' },
    CONFIRMED: { text: '已确认', type: 'primary' },
    PAID: { text: '已支付', type: 'success' },
    CHECKED_IN: { text: '已入住', type: '' },
    COMPLETED: { text: '已完成', type: 'info' },
    CANCELLED: { text: '已取消', type: 'info' },
    CANCELLED_BY_USER: { text: '用户取消', type: 'info' },
    CANCELLED_BY_HOST: { text: '房东取消', type: 'info' },
    CANCELLED_SYSTEM: { text: '系统取消', type: 'info' },
    PAYMENT_PENDING: { text: '支付中', type: 'warning' }, // 可能不需要，PAID更常用
    PAYMENT_FAILED: { text: '支付失败', type: 'danger' },
    REFUND_PENDING: { text: '退款中', type: 'warning' },
    REFUNDED: { text: '已退款', type: 'info' },
    REFUND_FAILED: { text: '退款失败', type: 'danger' },
    READY_FOR_CHECKIN: { text: '待入住', type: '' },
    REJECTED: { text: '已拒绝', type: 'danger' }
}

const paymentStatusMap = {
    UNPAID: { text: '未支付', type: 'warning' },
    PAID: { text: '已支付', type: 'success' },
    PAYMENT_FAILED: { text: '支付失败', type: 'danger' },
    REFUND_PENDING: { text: '退款中', type: 'warning' },
    REFUNDED: { text: '已退款', type: 'info' },
    PARTIALLY_REFUNDED: { text: '部分退款', type: 'primary' },
    REFUND_FAILED: { text: '退款失败', type: 'danger' }
}

const getOrderStatusText = (status: string) => {
    return orderStatusMap[status as keyof typeof orderStatusMap]?.text || status || '未知'
}

const getOrderStatusType = (status: string) => {
    return orderStatusMap[status as keyof typeof orderStatusMap]?.type || ''
}

const getPaymentStatusText = (status: string | null) => {
    if (!status) return 'N/A';
    return paymentStatusMap[status as keyof typeof paymentStatusMap]?.text || status || '未知'
}

const getPaymentStatusType = (status: string | null) => {
    if (!status) return 'info';
    return paymentStatusMap[status as keyof typeof paymentStatusMap]?.type || ''
}

// 获取数据 - 重写以适配新API和筛选
const fetchData = async () => {
    loading.value = true
    const params = {
        page: currentPage.value - 1, // 后端需要 0-based page
        size: pageSize.value,
        orderNumber: searchForm.orderNumber || undefined,
        guestName: searchForm.guestName || undefined,
        homestayTitle: searchForm.homestayTitle || undefined,
        status: searchForm.status || undefined,
        paymentStatus: searchForm.paymentStatus || undefined,
        paymentMethod: searchForm.paymentMethod || undefined,
        hostName: searchForm.hostName || undefined,
        checkInDateStart: searchForm.checkInDateRange?.[0] ? format(new Date(searchForm.checkInDateRange[0]), 'yyyy-MM-dd') : undefined,
        checkInDateEnd: searchForm.checkInDateRange?.[1] ? format(new Date(searchForm.checkInDateRange[1]), 'yyyy-MM-dd') : undefined,
        createTimeStart: searchForm.createTimeRange?.[0] ? format(new Date(searchForm.createTimeRange[0]), 'yyyy-MM-dd') : undefined,
        createTimeEnd: searchForm.createTimeRange?.[1] ? format(new Date(searchForm.createTimeRange[1]), 'yyyy-MM-dd') : undefined,
        sort: `${sortInfo.value.prop},${sortInfo.value.order === 'ascending' ? 'asc' : 'desc'}`
    }
    console.log('Fetching orders with params:', params);
    try {
        const res = await getAdminOrders(params) // 调用新的API函数
        tableData.value = res.content
        total.value = res.totalElements
    } catch (error) {
        console.error('获取订单列表失败:', error)
        ElMessage.error('获取订单列表失败')
    } finally {
        loading.value = false
    }
}

// 表格排序变化
const handleSortChange = ({ prop, order }: { prop: string; order: string | null }) => {
    sortInfo.value.prop = prop;
    sortInfo.value.order = order || 'descending'; // 默认为降序
    fetchData();
};

// 搜索方法
const handleSearch = () => {
    currentPage.value = 1
    fetchData()
}

// 重置搜索
const resetSearch = () => {
    searchForm.orderNumber = ''
    searchForm.guestName = ''
    searchForm.homestayTitle = ''
    searchForm.status = ''
    searchForm.paymentStatus = ''
    searchForm.paymentMethod = ''
    searchForm.hostName = ''
    searchForm.checkInDateRange = null
    searchForm.createTimeRange = null
    handleSearch()
}

// 分页方法
const handleSizeChange = (val: number) => {
    pageSize.value = val
    fetchData()
}

const handleCurrentChange = (val: number) => {
    currentPage.value = val
    fetchData()
}

// 查看详情
const handleDetail = (row: any) => {
    // TODO: 跳转到订单详情页面
    ElMessage.info('订单详情功能开发中')
}

// 完成订单
const handleComplete = async (row: AdminOrder) => {
    try {
        await ElMessageBox.confirm(
            `确认要完成订单 ${row.orderNumber} 吗？`,
            '完成订单',
            {
                confirmButtonText: '确定完成',
                cancelButtonText: '取消',
                type: 'success'
            }
        );
        loading.value = true; // 开始加载
        await updateOrderStatus(row.id, 'COMPLETED'); // 使用后端状态 'COMPLETED'
        ElMessage.success('订单已成功标记为完成');
        // 更新本地数据或重新获取
        // fetchData(); // 简单起见，重新获取数据
        // 或者更精细地更新本地数据
        const index = tableData.value.findIndex(item => item.id === row.id);
        if (index !== -1) {
            tableData.value[index].status = 'COMPLETED';
        }
    } catch (error) { // 捕获包括取消在内的所有错误
        if (error !== 'cancel') { // 如果不是用户取消操作
            console.error('完成订单失败:', error);
            // 尝试从 error 对象获取后端返回的错误信息
            const message = (error as any)?.response?.data?.message || (error as Error)?.message || '操作失败，请重试';
            ElMessage.error(`完成订单失败: ${message}`);
        }
    } finally {
        loading.value = false; // 结束加载
    }
}

// 取消订单
const handleCancel = async (row: any) => {
    try {
        await ElMessageBox.confirm('确认要取消该订单吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })
        await updateOrderStatus(row.id || parseInt(row.orderNumber.substring(1)), '2')
        ElMessage.success('操作成功')
        row.status = '2'
    } catch (error) {
        console.error('操作失败:', error)
    }
}

// 导出订单
const handleExport = async () => {
    try {
        loading.value = true
        const blob = await exportOrders({
            ...searchForm,
            page: currentPage.value,
            pageSize: pageSize.value
        })

        // 创建下载链接
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `订单数据_${new Date().toISOString().slice(0, 10)}.xlsx`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)

        ElMessage.success('导出成功')
    } catch (error) {
        console.error('导出失败:', error)
        ElMessage.error('导出失败')
    } finally {
        loading.value = false
    }
}

// 批量完成订单
const handleBatchComplete = async () => {
    if (selectedRows.value.length === 0) {
        ElMessage.warning('请至少选择一项')
        return
    }

    // 筛选出可以完成的订单（已支付状态）
    const validOrders = selectedRows.value.filter(item => item.status === '1')
    if (validOrders.length === 0) {
        ElMessage.warning('所选订单中没有可以完成的订单（已支付状态）')
        return
    }

    try {
        await ElMessageBox.confirm(`确认要批量完成选中的 ${validOrders.length} 个订单吗？`, '批量操作', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })

        const ids = validOrders.map(item => item.id)
        await batchUpdateOrderStatus(ids, '3')
        ElMessage.success('批量完成成功')
        fetchData()
    } catch (error) {
        console.error('批量完成失败:', error)
    }
}

// 批量取消订单
const handleBatchCancel = async () => {
    if (selectedRows.value.length === 0) {
        ElMessage.warning('请至少选择一项')
        return
    }

    // 筛选出可以取消的订单（待支付状态）
    const validOrders = selectedRows.value.filter(item => item.status === '0')
    if (validOrders.length === 0) {
        ElMessage.warning('所选订单中没有可以取消的订单（待支付状态）')
        return
    }

    try {
        await ElMessageBox.confirm(`确认要批量取消选中的 ${validOrders.length} 个订单吗？`, '批量操作', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })

        const ids = validOrders.map(item => item.id)
        await batchUpdateOrderStatus(ids, '2')
        ElMessage.success('批量取消成功')
        fetchData()
    } catch (error) {
        console.error('批量取消失败:', error)
    }
}

// 批量导出订单
const handleBatchExport = async () => {
    if (selectedRows.value.length === 0) {
        ElMessage.warning('请至少选择一项')
        return
    }

    try {
        loading.value = true
        const ids = selectedRows.value.map(item => item.id)
        const blob = await batchExportOrders(ids)

        // 创建下载链接
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `订单数据_${selectedRows.value.length}条_${new Date().toISOString().slice(0, 10)}.xlsx`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)

        ElMessage.success('批量导出成功')
    } catch (error) {
        console.error('批量导出失败:', error)
        ElMessage.error('批量导出失败')
    } finally {
        loading.value = false
    }
}

// 确认支付
const handleConfirmPayment = async (row: AdminOrder) => {
    try {
        await ElMessageBox.confirm(
            `确认要将订单 ${row.orderNumber} 标记为已支付吗？`,
            '确认支付',
            { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
        )
        loading.value = true;
        await confirmPayment(row.id);
        ElMessage.success('订单支付状态已确认');
        fetchData(); // 重新加载数据以更新状态
    } catch (error) {
        if (error !== 'cancel') { // 用户取消操作时通常返回 'cancel'
            console.error('确认支付失败:', error);
            // 尝试从 error 对象获取后端返回的错误信息
            const message = (error as any)?.response?.data?.error || (error as Error)?.message || '操作失败';
            ElMessage.error(`确认支付失败: ${message}`);
        }
    } finally {
        loading.value = false;
    }
}

// 发起退款
const handleRefund = async (row: AdminOrder) => {
    try {
        await ElMessageBox.confirm(
            `确认要为订单 ${row.orderNumber} 发起退款吗？<br/><small>(此操作仅标记状态为退款中，实际退款需后续处理)</small>`,
            '发起退款',
            {
                confirmButtonText: '确定发起',
                cancelButtonText: '取消',
                type: 'warning',
                dangerouslyUseHTMLString: true // 允许 HTML 字符串
            }
        )
        loading.value = true;
        await initiateRefund(row.id);
        ElMessage.success('已发起退款流程，订单状态更新为退款中');
        fetchData(); // 重新加载数据
    } catch (error) {
        if (error !== 'cancel') {
            console.error('发起退款失败:', error);
            const message = (error as any)?.response?.data?.error || (error as Error)?.message || '操作失败';
            ElMessage.error(`发起退款失败: ${message}`);
        }
    } finally {
        loading.value = false;
    }
}

// 处理表格选择变化
const handleSelectionChange = (selection: AdminOrder[]) => {
    selectedRows.value = selection
}

// 初始化
onMounted(() => {
    fetchData()
})
</script>

<style scoped lang="scss">
.order-list {
    padding: 20px;

    .search-box {
        margin-bottom: 20px;
    }

    .pagination {
        margin-top: 20px;
        display: flex;
        justify-content: flex-end;
    }

    .batch-operation {
        margin-bottom: 15px;
    }

    .batch-buttons {
        margin-top: 8px;
        display: flex;
        gap: 10px;
    }

    /* 确保 tooltip 生效 */
    .el-table .el-table__cell .cell {
        white-space: nowrap;
    }
}
</style>