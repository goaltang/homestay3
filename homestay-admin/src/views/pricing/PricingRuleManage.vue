<template>
  <div class="container">
    <div class="handle-box">
      <el-row :gutter="20">
        <el-col :span="24" class="mb10">
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增规则</el-button>
          <el-select v-model="query.ruleType" placeholder="规则类型" clearable class="mr10" style="width:150px" @change="getList">
            <el-option label="全部" value=""></el-option>
            <el-option label="周末" value="WEEKEND"></el-option>
            <el-option label="节假日" value="HOLIDAY"></el-option>
            <el-option label="日期范围" value="DATE_RANGE"></el-option>
            <el-option label="早鸟" value="EARLY_BIRD"></el-option>
            <el-option label="连住" value="LONG_STAY"></el-option>
          </el-select>
        </el-col>
      </el-row>
    </div>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>价格规则</span>
          <el-tag type="info">共 {{ total }} 项</el-tag>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="name" label="规则名称" width="180"></el-table-column>
        <el-table-column prop="scopeType" label="作用域" width="100"></el-table-column>
        <el-table-column prop="ruleType" label="类型" width="100">
          <template #default="scope">
            <el-tag size="small">{{ scope.row.ruleType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="adjustmentType" label="调价方式" width="100"></el-table-column>
        <el-table-column prop="adjustmentValue" label="数值" width="100"></el-table-column>
        <el-table-column prop="priority" label="优先级" width="80"></el-table-column>
        <el-table-column prop="stackable" label="可叠加" width="80">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.stackable ? 'success' : 'info'">{{ scope.row.stackable ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="80">
          <template #default="scope">
            <el-switch v-model="scope.row.enabled" @change="(val: any) => handleToggle(scope.row)"></el-switch>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" :icon="Edit" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" :icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.size"
        v-model:current-page="query.page"
        @current-change="getList"
      />
    </el-card>

    <el-dialog :title="editMode ? '编辑规则' : '新增规则'" v-model="dialogVisible" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="form.name" placeholder="如：全局周末浮动"></el-input>
        </el-form-item>
        <el-form-item label="作用域" prop="scopeType">
          <el-select v-model="form.scopeType" placeholder="选择作用域" style="width:100%">
            <el-option label="全局" value="GLOBAL"></el-option>
            <el-option label="城市" value="CITY"></el-option>
            <el-option label="房东" value="HOST"></el-option>
            <el-option label="分组" value="GROUP"></el-option>
            <el-option label="房源" value="HOMESTAY"></el-option>
            <el-option label="类型" value="TYPE"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="作用域值(JSON)">
          <el-input v-model="form.scopeValueJson" type="textarea" :rows="2" placeholder='如：{"cityText":"杭州"} 或 {"homestayId":123}'></el-input>
        </el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="form.ruleType" placeholder="选择类型" style="width:100%">
            <el-option label="周末" value="WEEKEND"></el-option>
            <el-option label="节假日" value="HOLIDAY"></el-option>
            <el-option label="日期范围" value="DATE_RANGE"></el-option>
            <el-option label="早鸟" value="EARLY_BIRD"></el-option>
            <el-option label="连住" value="LONG_STAY"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="调价方式" prop="adjustmentType">
          <el-select v-model="form.adjustmentType" placeholder="选择方式" style="width:100%">
            <el-option label="乘数" value="MULTIPLY"></el-option>
            <el-option label="折扣率" value="DISCOUNT_RATE"></el-option>
            <el-option label="固定减免" value="AMOUNT_OFF"></el-option>
            <el-option label="固定价格" value="FIXED_PRICE"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="调价数值" prop="adjustmentValue">
          <el-input-number v-model="form.adjustmentValue" :precision="4" :step="0.01" style="width:100%"></el-input-number>
          <div class="form-tip">MULTIPLY 用 1.20；DISCOUNT_RATE 用 0.90</div>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="0" style="width:100%"></el-input-number>
        </el-form-item>
        <el-form-item label="可叠加">
          <el-switch v-model="form.stackable" :active-value="true" :inactive-value="false"></el-switch>
        </el-form-item>
        <el-form-item label="生效日期">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" style="width:100%"></el-date-picker>
        </el-form-item>
        <el-form-item label="最少晚数">
          <el-input-number v-model="form.minNights" :min="0" style="width:100%"></el-input-number>
        </el-form-item>
        <el-form-item label="最多晚数">
          <el-input-number v-model="form.maxNights" :min="0" style="width:100%"></el-input-number>
        </el-form-item>
        <el-form-item label="最少提前天数">
          <el-input-number v-model="form.minAdvanceDays" :min="0" style="width:100%"></el-input-number>
        </el-form-item>
        <el-form-item label="最多提前天数">
          <el-input-number v-model="form.maxAdvanceDays" :min="0" style="width:100%"></el-input-number>
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
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Edit, Delete } from '@element-plus/icons-vue'
import { getPricingRules, createPricingRule, updatePricingRule, deletePricingRule, togglePricingRule } from '@/api/pricing'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 0, size: 20, ruleType: '' })
const dialogVisible = ref(false)
const editMode = ref(false)
const formRef = ref()
const dateRange = ref<string[]>([])
const form = reactive<any>({
  name: '',
  scopeType: 'GLOBAL',
  scopeValueJson: '',
  ruleType: 'WEEKEND',
  adjustmentType: 'MULTIPLY',
  adjustmentValue: 1.0,
  priority: 0,
  stackable: true,
  startDate: null,
  endDate: null,
  minNights: null,
  maxNights: null,
  minAdvanceDays: null,
  maxAdvanceDays: null,
})
const currentId = ref<number | null>(null)

const rules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  scopeType: [{ required: true, message: '请选择作用域', trigger: 'change' }],
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  adjustmentType: [{ required: true, message: '请选择调价方式', trigger: 'change' }],
  adjustmentValue: [{ required: true, message: '请输入调价数值', trigger: 'blur' }],
}

watch(dateRange, (val) => {
  if (val && val.length === 2) {
    form.startDate = val[0]
    form.endDate = val[1]
  } else {
    form.startDate = null
    form.endDate = null
  }
})

const getList = async () => {
  loading.value = true
  try {
    const res: any = await getPricingRules({ page: query.page, size: query.size, ruleType: query.ruleType })
    const pageData = res.data
    tableData.value = pageData?.content || pageData || []
    total.value = pageData?.totalElements || pageData?.length || 0
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
    name: '',
    scopeType: 'GLOBAL',
    scopeValueJson: '',
    ruleType: 'WEEKEND',
    adjustmentType: 'MULTIPLY',
    adjustmentValue: 1.0,
    priority: 0,
    stackable: true,
    startDate: null,
    endDate: null,
    minNights: null,
    maxNights: null,
    minAdvanceDays: null,
    maxAdvanceDays: null,
  })
  dateRange.value = []
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  editMode.value = true
  currentId.value = row.id
  Object.assign(form, row)
  if (row.startDate && row.endDate) {
    dateRange.value = [row.startDate, row.endDate]
  } else {
    dateRange.value = []
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  try {
    if (editMode.value && currentId.value) {
      await updatePricingRule(currentId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createPricingRule(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    getList()
  } catch (e: any) {
    ElMessage.error(e.response?.data || '操作失败')
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除规则「${row.name}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deletePricingRule(row.id)
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

const handleToggle = async (row: any) => {
  try {
    await togglePricingRule(row.id)
    ElMessage.success('状态已更新')
  } catch (e) {
    row.enabled = !row.enabled
  }
}

onMounted(getList)
</script>

<style scoped>
.container { padding: 20px; }
.handle-box { margin-bottom: 20px; }
.mr10 { margin-right: 10px; }
.mb10 { margin-bottom: 10px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 20px; justify-content: flex-end; }
.form-tip { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
