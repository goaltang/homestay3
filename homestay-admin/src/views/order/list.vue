<template>
    <div class="order-list">
        <div class="search-box">
            <el-form :inline="true" :model="searchForm">
                <el-form-item label="订单号">
                    <el-input v-model="searchForm.orderNo" placeholder="请输入订单号" clearable />
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
                        <el-option label="待支付" value="0" />
                        <el-option label="已支付" value="1" />
                        <el-option label="已取消" value="2" />
                        <el-option label="已完成" value="3" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">搜索</el-button>
                    <el-button @click="resetSearch">重置</el-button>
                    <el-button type="success" @click="handleExport">导出订单</el-button>
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
            @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" />
            <el-table-column prop="orderNo" label="订单号" width="180" />
            <el-table-column prop="homestayName" label="房源名称" />
            <el-table-column prop="userName" label="用户名" width="120" />
            <el-table-column prop="amount" label="金额" width="120">
                <template #default="scope">
                    ¥{{ scope.row.amount }}
                </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                    <el-tag :type="getStatusType(scope.row.status)">
                        {{ getStatusText(scope.row.status) }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作" width="150" fixed="right">
                <template #default="scope">
                    <el-button type="primary" link @click="handleDetail(scope.row)">详情</el-button>
                    <el-button type="success" link @click="handleComplete(scope.row)" v-if="scope.row.status === '1'">
                        完成
                    </el-button>
                    <el-button type="danger" link @click="handleCancel(scope.row)" v-if="scope.row.status === '0'">
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
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Order, OrderSearchParams, StatusMap } from '@/types'
import {
    getOrderList,
    updateOrderStatus,
    exportOrders,
    batchUpdateOrderStatus,
    batchExportOrders,
    deleteOrder,
    batchDeleteOrders
} from '@/api/order'

// 接口扩展，确保Order类型包含id字段
interface OrderWithId extends Order {
    id: number;
}

// 搜索表单
const searchForm = reactive<OrderSearchParams>({
    page: 1,
    pageSize: 10,
    orderNo: '',
    status: ''
})

// 表格数据
const tableData = ref<OrderWithId[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedRows = ref<OrderWithId[]>([])

// 处理表格选择变化
const handleSelectionChange = (selection: OrderWithId[]) => {
    selectedRows.value = selection
}

// 状态映射
const statusMap: StatusMap = {
    '0': { text: '待支付', type: 'warning' },
    '1': { text: '已支付', type: 'success' },
    '2': { text: '已取消', type: 'info' },
    '3': { text: '已完成', type: 'primary' }
}

const getStatusText = (status: string) => {
    return statusMap[status]?.text || '未知状态'
}

const getStatusType = (status: string) => {
    return statusMap[status]?.type || ''
}

// 搜索方法
const handleSearch = () => {
    currentPage.value = 1
    fetchData()
}

// 重置搜索
const resetSearch = () => {
    searchForm.orderNo = ''
    searchForm.status = ''
    handleSearch()
}

// 获取数据
const fetchData = async () => {
    loading.value = true
    try {
        const res = await getOrderList({
            page: currentPage.value,
            pageSize: pageSize.value,
            orderNo: searchForm.orderNo,
            status: searchForm.status
        })
        tableData.value = res.list
        total.value = res.total
    } catch (error) {
        console.error('获取订单列表失败:', error)
        ElMessage.error('获取订单列表失败')
    } finally {
        loading.value = false
    }
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
const handleComplete = async (row: any) => {
    try {
        await ElMessageBox.confirm('确认要完成该订单吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })
        await updateOrderStatus(row.id || parseInt(row.orderNo.substring(1)), '3')
        ElMessage.success('操作成功')
        row.status = '3'
    } catch (error) {
        console.error('操作失败:', error)
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
        await updateOrderStatus(row.id || parseInt(row.orderNo.substring(1)), '2')
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

// 初始化
fetchData()
</script>

<style scoped lang="scss">
.order-list {
    .search-box {
        margin-bottom: 20px;
        padding: 20px;
        background-color: #fff;
        border-radius: 4px;
    }

    .batch-operation {
        margin-bottom: 20px;

        .batch-buttons {
            display: inline-block;
            margin-left: 15px;
        }
    }

    .pagination {
        margin-top: 20px;
        display: flex;
        justify-content: flex-end;
    }
}
</style>