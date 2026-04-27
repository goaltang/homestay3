<template>
  <div class="container">
    <div class="handle-box">
      <el-row :gutter="20">
        <el-col :span="24" class="mb10">
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增节假日</el-button>
          <el-input v-model="query.year" placeholder="年份" class="handle-input mr10" style="width:120px" @keyup.enter="getList">
            <template #append>
              <el-button :icon="Search" @click="getList"></el-button>
            </template>
          </el-input>
        </el-col>
      </el-row>
    </div>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>节假日日历</span>
          <el-tag type="info">共 {{ tableData.length }} 项</el-tag>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="date" label="日期" width="120"></el-table-column>
        <el-table-column prop="name" label="名称" width="150"></el-table-column>
        <el-table-column prop="type" label="类型" width="120"></el-table-column>
        <el-table-column prop="regionCode" label="地区" width="100"></el-table-column>
        <el-table-column prop="isHoliday" label="是否节假日" width="110">
          <template #default="scope">
            <el-tag :type="scope.row.isHoliday ? 'danger' : 'info'">{{ scope.row.isHoliday ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isMakeupWorkday" label="是否调休" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.isMakeupWorkday ? 'warning' : 'info'">{{ scope.row.isMakeupWorkday ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" :icon="Edit" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" :icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="editMode ? '编辑节假日' : '新增节假日'" v-model="dialogVisible" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="日期" prop="date">
          <el-date-picker v-model="form.date" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width:100%"></el-date-picker>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如：国庆节"></el-input>
        </el-form-item>
        <el-form-item label="类型">
          <el-input v-model="form.type" placeholder="如：PUBLIC_HOLIDAY"></el-input>
        </el-form-item>
        <el-form-item label="地区">
          <el-input v-model="form.regionCode" placeholder="默认 CN"></el-input>
        </el-form-item>
        <el-form-item label="节假日">
          <el-switch v-model="form.isHoliday" :active-value="true" :inactive-value="false"></el-switch>
        </el-form-item>
        <el-form-item label="调休补班">
          <el-switch v-model="form.isMakeupWorkday" :active-value="true" :inactive-value="false"></el-switch>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Edit, Delete } from '@element-plus/icons-vue'
import { getHolidays, createHoliday, updateHoliday, deleteHoliday } from '@/api/pricing'

const loading = ref(false)
const tableData = ref<any[]>([])
const query = reactive({ year: new Date().getFullYear() })
const dialogVisible = ref(false)
const editMode = ref(false)
const formRef = ref()
const form = reactive<any>({
  date: '',
  name: '',
  type: 'PUBLIC_HOLIDAY',
  regionCode: 'CN',
  isHoliday: true,
  isMakeupWorkday: false,
})
const currentId = ref<number | null>(null)

const rules = {
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
}

const getList = async () => {
  loading.value = true
  try {
    const res: any = await getHolidays({ year: query.year })
    tableData.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  editMode.value = false
  currentId.value = null
  Object.assign(form, {
    date: '',
    name: '',
    type: 'PUBLIC_HOLIDAY',
    regionCode: 'CN',
    isHoliday: true,
    isMakeupWorkday: false,
  })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  editMode.value = true
  currentId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  try {
    if (editMode.value && currentId.value) {
      await updateHoliday(currentId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createHoliday(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    getList()
  } catch (e: any) {
    ElMessage.error(e.response?.data || '操作失败')
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除 ${row.date} ${row.name} 吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteHoliday(row.id)
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

onMounted(getList)
</script>

<style scoped>
.container { padding: 20px; }
.handle-box { margin-bottom: 20px; }
.handle-input { width: 200px; }
.mr10 { margin-right: 10px; }
.mb10 { margin-bottom: 10px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
