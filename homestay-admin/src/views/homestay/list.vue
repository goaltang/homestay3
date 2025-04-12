<template>
    <div class="homestay-list">
        <div class="search-box">
            <el-form :inline="true" :model="searchForm">
                <el-form-item label="房源名称">
                    <el-input v-model="searchForm.name" placeholder="请输入房源名称" clearable />
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
                        <el-option label="上架" value="1" />
                        <el-option label="下架" value="0" />
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
            <el-table-column prop="name" label="房源名称" />
            <el-table-column prop="price" label="价格" width="120">
                <template #default="scope">
                    ¥{{ scope.row.price }}
                </template>
            </el-table-column>
            <el-table-column prop="address" label="地址" />
            <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                    <el-tag :type="scope.row.status === '1' ? 'success' : 'info'">
                        {{ scope.row.status === '1' ? '上架' : '下架' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
                <template #default="scope">
                    <el-button type="primary" link @click="handleEdit(scope.row)">编辑</el-button>
                    <el-button type="success" link @click="handleToggleStatus(scope.row)"
                        v-if="scope.row.status === '0'">
                        上架
                    </el-button>
                    <el-button type="info" link @click="handleToggleStatus(scope.row)" v-else>
                        下架
                    </el-button>
                    <el-button type="danger" link @click="handleDelete(scope.row)">删除</el-button>
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
import type { Homestay, HomestaySearchParams } from '@/types'
import {
    getHomestayList,
    updateHomestayStatus,
    deleteHomestay,
    batchUpdateHomestayStatus,
    batchDeleteHomestays
} from '@/api/homestay'

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
        })
        tableData.value = res.list
        total.value = res.total
    } catch (error) {
        console.error('获取房源列表失败:', error)
        ElMessage.error('获取房源列表失败')
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

// 新增房源
const handleAdd = () => {
    // TODO: 跳转到新增房源页面
    ElMessage.info('新增房源功能开发中')
}

// 编辑房源
const handleEdit = (row: any) => {
    // TODO: 跳转到编辑房源页面
    ElMessage.info('编辑房源功能开发中')
}

// 切换状态
const handleToggleStatus = async (row: any) => {
    try {
        await ElMessageBox.confirm(
            `确认要${row.status === '1' ? '下架' : '上架'}该房源吗？`,
            '提示',
            {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }
        )
        await updateHomestayStatus(row.id, row.status === '1' ? '0' : '1')
        ElMessage.success(`${row.status === '1' ? '下架' : '上架'}成功`)
        row.status = row.status === '1' ? '0' : '1'
    } catch (error) {
        console.error('操作失败:', error)
    }
}

// 删除房源
const handleDelete = async (row: any) => {
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
        console.error('删除失败:', error)
    }
}

// 批量上架
const handleBatchActive = async () => {
    if (selectedRows.value.length === 0) {
        ElMessage.warning('请至少选择一项')
        return
    }

    try {
        await ElMessageBox.confirm('确认要批量上架选中的房源吗？', '批量操作', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })

        const ids = selectedRows.value.map(item => item.id)
        await batchUpdateHomestayStatus(ids, '1')
        ElMessage.success('批量上架成功')
        fetchData()
    } catch (error) {
        console.error('批量上架失败:', error)
    }
}

// 批量下架
const handleBatchInactive = async () => {
    if (selectedRows.value.length === 0) {
        ElMessage.warning('请至少选择一项')
        return
    }

    try {
        await ElMessageBox.confirm('确认要批量下架选中的房源吗？', '批量操作', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })

        const ids = selectedRows.value.map(item => item.id)
        await batchUpdateHomestayStatus(ids, '0')
        ElMessage.success('批量下架成功')
        fetchData()
    } catch (error) {
        console.error('批量下架失败:', error)
    }
}

// 批量删除
const handleBatchDelete = async () => {
    if (selectedRows.value.length === 0) {
        ElMessage.warning('请至少选择一项')
        return
    }

    try {
        await ElMessageBox.confirm(`确认要删除选中的 ${selectedRows.value.length} 个房源吗？此操作不可恢复！`, '批量操作', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })

        const ids = selectedRows.value.map(item => item.id)
        await batchDeleteHomestays(ids)
        ElMessage.success('批量删除成功')
        fetchData()
    } catch (error) {
        console.error('批量删除失败:', error)
    }
}

// 初始化
fetchData()
</script>

<style scoped lang="scss">
.homestay-list {
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