<template>
    <div class="user-list">
        <div class="search-box">
            <el-form :inline="true" :model="searchForm">
                <el-form-item label="用户名">
                    <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
                </el-form-item>
                <el-form-item label="手机号">
                    <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable />
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
                        <el-option label="正常" value="1" />
                        <el-option label="禁用" value="0" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">搜索</el-button>
                    <el-button @click="resetSearch">重置</el-button>
                    <el-button type="success" @click="handleAdd">新增用户</el-button>
                </el-form-item>
            </el-form>
        </div>

        <!-- 批量操作区域 -->
        <div class="batch-operation" v-if="selectedRows.length > 0">
            <el-alert title="批量操作" type="info" :closable="false" show-icon>
                <template #default>
                    已选择 <strong>{{ selectedRows.length }}</strong> 项
                    <div class="batch-buttons">
                        <el-button size="small" type="success" @click="handleBatchEnable">批量启用</el-button>
                        <el-button size="small" type="danger" @click="handleBatchDisable">批量禁用</el-button>
                        <el-button size="small" type="warning" @click="handleBatchResetPassword">批量重置密码</el-button>
                    </div>
                </template>
            </el-alert>
        </div>

        <el-table :data="tableData" border style="width: 100%" v-loading="loading"
            @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" />
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" />
            <el-table-column prop="nickname" label="昵称" />
            <el-table-column prop="phone" label="手机号" width="120" />
            <el-table-column prop="email" label="邮箱" width="180" />
            <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                    <el-tag :type="scope.row.status === '1' ? 'success' : 'info'">
                        {{ scope.row.status === '1' ? '正常' : '禁用' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="createTime" label="注册时间" width="180" />
            <el-table-column label="操作" width="200" fixed="right">
                <template #default="scope">
                    <el-button type="primary" link @click="handleEdit(scope.row)">编辑</el-button>
                    <el-button :type="scope.row.status === '1' ? 'danger' : 'success'" link
                        @click="handleToggleStatus(scope.row)">
                        {{ scope.row.status === '1' ? '禁用' : '启用' }}
                    </el-button>
                    <el-button type="warning" link @click="handleResetPassword(scope.row)">重置密码</el-button>
                </template>
            </el-table-column>
        </el-table>

        <div class="pagination">
            <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize"
                :page-sizes="[10, 20, 50, 100]" :total="total" layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSizeChange" @current-change="handleCurrentChange" />
        </div>

        <!-- 编辑用户对话框 -->
        <el-dialog v-model="dialogVisible" title="编辑用户" width="500px" :close-on-click-modal="false">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="form.username" disabled />
                </el-form-item>
                <el-form-item label="昵称" prop="nickname">
                    <el-input v-model="form.nickname" />
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                    <el-input v-model="form.phone" />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                    <el-input v-model="form.email" />
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="dialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="handleSubmit">
                        确定
                    </el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 重置密码结果对话框 -->
        <el-dialog v-model="passwordDialogVisible" title="重置密码结果" width="500px">
            <div v-if="resetPasswordResults.length === 0">
                无可重置的密码
            </div>
            <div v-else>
                <p>已重置以下用户的密码：</p>
                <el-table :data="resetPasswordResults" border style="width: 100%">
                    <el-table-column prop="username" label="用户名" />
                    <el-table-column prop="password" label="新密码" />
                </el-table>
                <div class="password-tips">
                    <el-alert title="请妥善保管新密码，关闭此窗口后将无法再次查看！" type="warning" :closable="false" />
                </div>
            </div>
            <template #footer>
                <span class="dialog-footer">
                    <el-button type="primary" @click="passwordDialogVisible = false">
                        确定
                    </el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { User, UserSearchParams } from '@/types'
import {
    getUserList,
    updateUser,
    updateUserStatus,
    resetUserPassword,
    batchUpdateUserStatus,
    batchResetUserPasswords,
    createUser
} from '@/api/user'

// 搜索表单
const searchForm = reactive<UserSearchParams>({
    page: 1,
    pageSize: 10,
    username: '',
    phone: '',
    status: ''
})

// 表格数据
const tableData = ref<User[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedRows = ref<User[]>([])

// 处理表格选择变化
const handleSelectionChange = (selection: User[]) => {
    selectedRows.value = selection
}

// 编辑表单
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Omit<User, 'status' | 'createTime'>>({
    id: 0,
    username: '',
    nickname: '',
    phone: '',
    email: ''
})

// 重置密码结果
const passwordDialogVisible = ref(false)
const resetPasswordResults = ref<{ username: string, password: string }[]>([])

// 表单校验规则
const rules: FormRules = {
    nickname: [
        { required: true, message: '请输入昵称', trigger: 'blur' }
    ],
    phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
    ],
    email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
    ]
}

// 搜索方法
const handleSearch = () => {
    currentPage.value = 1
    fetchData()
}

// 重置搜索
const resetSearch = () => {
    searchForm.username = ''
    searchForm.phone = ''
    searchForm.status = ''
    handleSearch()
}

// 获取数据
const fetchData = async () => {
    loading.value = true
    try {
        const res = await getUserList({
            page: currentPage.value,
            pageSize: pageSize.value,
            username: searchForm.username,
            phone: searchForm.phone,
            status: searchForm.status
        })
        tableData.value = res.list
        total.value = res.total
    } catch (error) {
        console.error('获取用户列表失败:', error)
        ElMessage.error('获取用户列表失败')
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

// 新增用户
const handleAdd = () => {
    form.id = 0
    form.username = ''
    form.nickname = ''
    form.phone = ''
    form.email = ''
    dialogVisible.value = true
}

// 编辑用户
const handleEdit = (row: User) => {
    form.id = row.id
    form.username = row.username
    form.nickname = row.nickname
    form.phone = row.phone
    form.email = row.email
    dialogVisible.value = true
}

// 切换用户状态
const handleToggleStatus = async (row: User) => {
    try {
        await ElMessageBox.confirm(
            `确定要${row.status === '1' ? '禁用' : '启用'}该用户吗？`,
            '提示',
            {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }
        )
        await updateUserStatus(row.id, row.status === '1' ? false : true)
        ElMessage.success(`${row.status === '1' ? '禁用' : '启用'}成功`)
        row.status = row.status === '1' ? '0' : '1'
    } catch (error) {
        console.error('操作失败:', error)
    }
}

// 重置用户密码
const handleResetPassword = async (row: User) => {
    try {
        await ElMessageBox.confirm(
            '确定要重置该用户的密码吗？',
            '提示',
            {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }
        )

        const result = await resetUserPassword(row.id)
        resetPasswordResults.value = [{
            username: row.username,
            password: result.newPassword
        }]

        passwordDialogVisible.value = true
    } catch (error) {
        console.error('重置密码失败:', error)
        ElMessage.error('重置密码失败')
    }
}

// 批量启用用户
const handleBatchEnable = async () => {
    if (selectedRows.value.length === 0) {
        ElMessage.warning('请至少选择一项')
        return
    }

    try {
        await ElMessageBox.confirm('确认要批量启用选中的用户吗？', '批量操作', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })

        const ids = selectedRows.value.map(item => item.id)
        await batchUpdateUserStatus(ids, '1')
        ElMessage.success('批量启用成功')
        fetchData()
    } catch (error) {
        console.error('批量启用失败:', error)
    }
}

// 批量禁用用户
const handleBatchDisable = async () => {
    if (selectedRows.value.length === 0) {
        ElMessage.warning('请至少选择一项')
        return
    }

    try {
        await ElMessageBox.confirm('确认要批量禁用选中的用户吗？', '批量操作', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })

        const ids = selectedRows.value.map(item => item.id)
        await batchUpdateUserStatus(ids, '0')
        ElMessage.success('批量禁用成功')
        fetchData()
    } catch (error) {
        console.error('批量禁用失败:', error)
    }
}

// 批量重置密码
const handleBatchResetPassword = async () => {
    if (selectedRows.value.length === 0) {
        ElMessage.warning('请至少选择一项')
        return
    }

    try {
        await ElMessageBox.confirm(`确认要重置选中的 ${selectedRows.value.length} 个用户的密码吗？`, '批量操作', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })

        const ids = selectedRows.value.map(item => item.id)
        const result = await batchResetUserPasswords(ids)

        // 处理返回结果
        resetPasswordResults.value = []
        for (const id in result) {
            const user = selectedRows.value.find(item => item.id.toString() === id)
            if (user) {
                resetPasswordResults.value.push({
                    username: user.username,
                    password: result[id]
                })
            }
        }

        passwordDialogVisible.value = true
    } catch (error) {
        console.error('批量重置密码失败:', error)
        ElMessage.error('批量重置密码失败')
    }
}

// 提交表单
const handleSubmit = async () => {
    if (!formRef.value) return

    try {
        await formRef.value.validate()

        if (form.id === 0) {
            // 新增用户
            await createUser({
                username: form.username,
                nickname: form.nickname,
                phone: form.phone,
                email: form.email,
                status: '1', // 默认启用
                createTime: new Date().toISOString()
            })
            ElMessage.success('创建成功')
        } else {
            // 更新用户
            await updateUser(form.id, {
                nickname: form.nickname,
                phone: form.phone,
                email: form.email
            })
            ElMessage.success('更新成功')
        }

        dialogVisible.value = false
        fetchData() // 刷新数据
    } catch (error) {
        console.error('提交失败:', error)
    }
}

// 初始化
fetchData()
</script>

<style scoped lang="scss">
.user-list {
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

    .password-tips {
        margin-top: 15px;
    }
}
</style>