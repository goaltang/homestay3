<template>
  <div class="campaign-list-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>营销活动管理</span>
          <el-button type="primary" @click="handleCreate">创建活动</el-button>
        </div>
      </template>

      <!-- 搜索筛选 -->
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="名称">
          <el-input v-model="searchForm.name" placeholder="活动名称" clearable @keyup.enter="fetchData" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="草稿" value="DRAFT" />
            <el-option label="进行中" value="ACTIVE" />
            <el-option label="已暂停" value="PAUSED" />
            <el-option label="已结束" value="ENDED" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.campaignType" placeholder="全部" clearable>
            <el-option label="限时折扣" value="FLASH_SALE" />
            <el-option label="满减活动" value="FULL_REDUCTION" />
            <el-option label="房源折扣" value="HOMESTAY_DISCOUNT" />
          </el-select>
        </el-form-item>
        <el-form-item label="承担方">
          <el-select v-model="searchForm.subsidyBearer" placeholder="全部" clearable>
            <el-option label="平台" value="PLATFORM" />
            <el-option label="房东" value="HOST" />
            <el-option label="混合" value="MIXED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="campaignList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="活动名称" min-width="150" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ formatType(row.campaignType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="承担方" width="100">
          <template #default="{ row }">
            <el-tag :type="row.subsidyBearer === 'HOST' ? 'warning' : 'info'">
              {{ formatBearer(row.subsidyBearer) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="formatStatusType(row.status)">{{ formatStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预算使用" width="150">
          <template #default="{ row }">
            <div v-if="row.budgetTotal">
              <el-progress :percentage="calculateBudgetPercent(row)" :status="getBudgetStatus(row)" :stroke-width="6" />
              <span class="budget-text">{{ row.budgetUsed || 0 }} / {{ row.budgetTotal }}</span>
            </div>
            <span v-else class="budget-text">无限制</span>
          </template>
        </el-table-column>
        <el-table-column label="创建人" width="120">
          <template #default="{ row }">
            {{ row.hostId ? '房东#' + row.hostId : row.createdBy || '平台' }}
          </template>
        </el-table-column>
        <el-table-column prop="startAt" label="开始时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.startAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'DRAFT'" type="primary" size="small" @click="handlePublish(row.id)">发布</el-button>
            <el-button v-if="row.status === 'ACTIVE'" type="warning" size="small" @click="handlePause(row.id)">暂停</el-button>
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'DRAFT'" type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchData"
        class="pagination"
      />
    </el-card>

    <!-- 创建/编辑活动对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑活动' : '创建活动'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="活动名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="活动类型">
          <el-select v-model="form.campaignType">
            <el-option label="限时折扣" value="FLASH_SALE" />
            <el-option label="满减活动" value="FULL_REDUCTION" />
            <el-option label="房源折扣" value="HOMESTAY_DISCOUNT" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker v-model="dateRange" type="datetimerange" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" />
        </el-form-item>
        <el-form-item label="预算">
          <el-input-number v-model="form.budgetTotal" :min="0" :precision="2" placeholder="0表示无限制" />
        </el-form-item>
        <el-form-item label="承担方">
          <el-select v-model="form.subsidyBearer">
            <el-option label="平台" value="PLATFORM" />
            <el-option label="房东" value="HOST" />
            <el-option label="混合" value="MIXED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getCampaigns, createCampaign, updateCampaign,
  publishCampaign, pauseCampaign, deleteCampaign
} from '@/api/marketing';

const loading = ref(false);
const campaignList = ref<any[]>([]);
const page = ref(1);
const size = ref(20);
const total = ref(0);

const searchForm = reactive({
  name: '',
  status: '',
  campaignType: '',
  subsidyBearer: '',
});

const dialogVisible = ref(false);
const isEdit = ref(false);
const editId = ref<number | null>(null);
const dateRange = ref<[Date, Date] | null>(null);
const form = reactive({
  name: '',
  campaignType: 'FULL_REDUCTION',
  budgetTotal: 0,
  subsidyBearer: 'PLATFORM',
  status: 'DRAFT',
  startAt: '',
  endAt: '',
});

const resetSearch = () => {
  searchForm.name = '';
  searchForm.status = '';
  searchForm.campaignType = '';
  searchForm.subsidyBearer = '';
  fetchData();
};

const fetchData = async () => {
  loading.value = true;
  try {
    const params: any = { page: page.value - 1, size: size.value };
    if (searchForm.name) params.name = searchForm.name;
    if (searchForm.status) params.status = searchForm.status;
    if (searchForm.campaignType) params.campaignType = searchForm.campaignType;
    if (searchForm.subsidyBearer) params.subsidyBearer = searchForm.subsidyBearer;

    const res: any = await getCampaigns(params);
    const data = res.data || res;
    campaignList.value = data.content || data || [];
    total.value = data.totalElements || data.length || 0;
  } catch (e) {
    ElMessage.error('获取活动列表失败');
  } finally {
    loading.value = false;
  }
};

const handleCreate = () => {
  isEdit.value = false;
  editId.value = null;
  form.name = '';
  form.campaignType = 'FULL_REDUCTION';
  form.budgetTotal = 0;
  form.subsidyBearer = 'PLATFORM';
  form.status = 'DRAFT';
  form.startAt = '';
  form.endAt = '';
  dateRange.value = null;
  dialogVisible.value = true;
};

const handleEdit = (row: any) => {
  isEdit.value = true;
  editId.value = row.id;
  form.name = row.name;
  form.campaignType = row.campaignType;
  form.budgetTotal = row.budgetTotal || 0;
  form.subsidyBearer = row.subsidyBearer || 'PLATFORM';
  form.status = row.status || 'DRAFT';
  if (row.startAt && row.endAt) {
    dateRange.value = [new Date(row.startAt), new Date(row.endAt)];
    form.startAt = row.startAt;
    form.endAt = row.endAt;
  } else {
    dateRange.value = null;
    form.startAt = '';
    form.endAt = '';
  }
  dialogVisible.value = true;
};

const submitForm = async () => {
  try {
    const payload = { ...form };
    if (dateRange.value && dateRange.value.length === 2) {
      payload.startAt = dateRange.value[0].toISOString();
      payload.endAt = dateRange.value[1].toISOString();
    }
    if (isEdit.value && editId.value) {
      await updateCampaign(editId.value, payload);
      ElMessage.success('更新成功');
    } else {
      await createCampaign(payload);
      ElMessage.success('创建成功');
    }
    dialogVisible.value = false;
    fetchData();
  } catch (e) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败');
  }
};

const handlePublish = async (id: number) => {
  try {
    await publishCampaign(id);
    ElMessage.success('发布成功');
    fetchData();
  } catch (e) {
    ElMessage.error('发布失败');
  }
};

const handlePause = async (id: number) => {
  try {
    await pauseCampaign(id);
    ElMessage.success('暂停成功');
    fetchData();
  } catch (e) {
    ElMessage.error('暂停失败');
  }
};

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该活动吗？', '提示', { type: 'warning' });
    await deleteCampaign(id);
    ElMessage.success('删除成功');
    fetchData();
  } catch (e) {
    // 取消
  }
};

const formatType = (type: string) => {
  const map: Record<string, string> = {
    FLASH_SALE: '限时折扣',
    FULL_REDUCTION: '满减',
    HOMESTAY_DISCOUNT: '房源折扣',
  };
  return map[type] || type;
};

const formatBearer = (bearer: string) => {
  const map: Record<string, string> = {
    PLATFORM: '平台',
    HOST: '房东',
    MIXED: '混合',
  };
  return map[bearer] || bearer;
};

const formatStatus = (status: string) => {
  const map: Record<string, string> = {
    ACTIVE: '进行中',
    PAUSED: '已暂停',
    DRAFT: '草稿',
    ENDED: '已结束',
  };
  return map[status] || status;
};

const formatStatusType = (status: string) => {
  const map: Record<string, any> = {
    ACTIVE: 'success',
    PAUSED: 'warning',
    DRAFT: 'info',
    ENDED: 'danger',
  };
  return map[status] || 'info';
};

const formatDate = (date: string) => {
  if (!date) return '-';
  return new Date(date).toLocaleString();
};

const calculateBudgetPercent = (row: any) => {
  if (!row.budgetTotal || row.budgetTotal <= 0) return 0;
  const percent = Math.round(((row.budgetUsed || 0) / row.budgetTotal) * 100);
  return Math.min(percent, 100);
};

const getBudgetStatus = (row: any) => {
  if (!row.budgetTotal) return '';
  const percent = ((row.budgetUsed || 0) / row.budgetTotal) * 100;
  if (percent >= 100) return 'exception';
  if (row.budgetAlertTriggered) return 'warning';
  return '';
};

onMounted(fetchData);
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.search-form {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}
.pagination {
  margin-top: 20px;
  text-align: right;
}
.budget-text {
  font-size: 12px;
  color: #666;
}
</style>
