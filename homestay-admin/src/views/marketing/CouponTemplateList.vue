<template>
  <div class="coupon-list-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>优惠券模板管理</span>
          <el-button type="primary" @click="handleCreate">创建模板</el-button>
        </div>
      </template>

      <!-- 搜索筛选 -->
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="名称">
          <el-input v-model="searchForm.name" placeholder="模板名称" clearable @keyup.enter="fetchData" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已激活" value="ACTIVE" />
            <el-option label="已暂停" value="PAUSED" />
            <el-option label="已过期" value="EXPIRED" />
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

      <el-table :data="templateList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="模板名称" min-width="150" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag>{{ formatType(row.couponType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="承担方" width="100">
          <template #default="{ row }">
            <el-tag :type="row.subsidyBearer === 'HOST' ? 'warning' : 'info'">
              {{ formatBearer(row.subsidyBearer) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="新人券" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.isNewUserCoupon" type="success">是</el-tag>
            <span v-else class="text-gray">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="faceValue" label="面值" width="100">
          <template #default="{ row }">
            {{ row.faceValue ? '¥' + row.faceValue : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="discountRate" label="折扣率" width="90">
          <template #default="{ row }">
            {{ row.discountRate ? (row.discountRate * 10) + '折' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="thresholdAmount" label="使用门槛" width="120">
          <template #default="{ row }">
            {{ row.thresholdAmount ? '满¥' + row.thresholdAmount : '无门槛' }}
          </template>
        </el-table-column>
        <el-table-column prop="totalStock" label="库存" width="100">
          <template #default="{ row }">
            {{ row.issuedCount || 0 }} / {{ row.totalStock || '∞' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="formatStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
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

    <!-- 创建/编辑模板对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑模板' : '创建优惠券模板'" width="600px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="模板名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="券类型">
          <el-select v-model="form.couponType">
            <el-option label="现金券" value="CASH" />
            <el-option label="折扣券" value="DISCOUNT" />
            <el-option label="满减券" value="FULL_REDUCTION" />
          </el-select>
        </el-form-item>
        <el-form-item label="承担方">
          <el-select v-model="form.subsidyBearer">
            <el-option label="平台" value="PLATFORM" />
            <el-option label="房东" value="HOST" />
            <el-option label="混合" value="MIXED" />
          </el-select>
        </el-form-item>
        <el-form-item label="新人专属">
          <el-switch v-model="form.isNewUserCoupon" active-text="注册自动发放" />
        </el-form-item>
        <el-form-item label="面值/折扣率">
          <el-input-number v-model="form.faceValue" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="折扣率(0-1)">
          <el-input-number v-model="form.discountRate" :min="0" :max="1" :precision="2" />
        </el-form-item>
        <el-form-item label="使用门槛">
          <el-input-number v-model="form.thresholdAmount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="最大优惠金额">
          <el-input-number v-model="form.maxDiscount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="总库存">
          <el-input-number v-model="form.totalStock" :min="0" :precision="0" />
        </el-form-item>
        <el-form-item label="每人限领">
          <el-input-number v-model="form.perUserLimit" :min="1" :precision="0" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker v-model="dateRange" type="datetimerange" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" />
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
  getCouponTemplates, createCouponTemplate,
  updateCouponTemplate, deleteCouponTemplate
} from '@/api/marketing';

const loading = ref(false);
const templateList = ref<any[]>([]);
const page = ref(1);
const size = ref(20);
const total = ref(0);

const searchForm = reactive({
  name: '',
  status: '',
  subsidyBearer: '',
});

const dialogVisible = ref(false);
const isEdit = ref(false);
const editId = ref<number | null>(null);
const dateRange = ref<[Date, Date] | null>(null);
const form = reactive({
  name: '',
  couponType: 'CASH',
  subsidyBearer: 'PLATFORM',
  isNewUserCoupon: false as boolean,
  faceValue: 0,
  discountRate: 0,
  thresholdAmount: 0,
  maxDiscount: 0,
  totalStock: 100,
  perUserLimit: 1,
  validType: 'FIXED_TIME',
  status: 'ACTIVE',
  validStartAt: '',
  validEndAt: '',
});

const resetSearch = () => {
  searchForm.name = '';
  searchForm.status = '';
  searchForm.subsidyBearer = '';
  fetchData();
};

const fetchData = async () => {
  loading.value = true;
  try {
    const params: any = { page: page.value - 1, size: size.value };
    if (searchForm.name) params.name = searchForm.name;
    if (searchForm.status) params.status = searchForm.status;
    if (searchForm.subsidyBearer) params.subsidyBearer = searchForm.subsidyBearer;

    const res: any = await getCouponTemplates(params);
    const data = res.data || res;
    templateList.value = data.content || data || [];
    total.value = data.totalElements || data.length || 0;
  } catch (e) {
    ElMessage.error('获取模板列表失败');
  } finally {
    loading.value = false;
  }
};

const handleCreate = () => {
  isEdit.value = false;
  editId.value = null;
  Object.assign(form, {
    name: '',
    couponType: 'CASH',
    subsidyBearer: 'PLATFORM',
    isNewUserCoupon: false,
    faceValue: 0,
    discountRate: 0,
    thresholdAmount: 0,
    maxDiscount: 0,
    totalStock: 100,
    perUserLimit: 1,
    validType: 'FIXED_TIME',
    status: 'ACTIVE',
    validStartAt: '',
    validEndAt: '',
  });
  dateRange.value = null;
  dialogVisible.value = true;
};

const handleEdit = (row: any) => {
  isEdit.value = true;
  editId.value = row.id;
  Object.assign(form, {
    name: row.name,
    couponType: row.couponType,
    subsidyBearer: row.subsidyBearer || 'PLATFORM',
    isNewUserCoupon: row.isNewUserCoupon || false,
    faceValue: row.faceValue || 0,
    discountRate: row.discountRate || 0,
    thresholdAmount: row.thresholdAmount || 0,
    maxDiscount: row.maxDiscount || 0,
    totalStock: row.totalStock || 100,
    perUserLimit: row.perUserLimit || 1,
    validType: row.validType || 'FIXED_TIME',
    status: row.status || 'ACTIVE',
  });
  if (row.validStartAt && row.validEndAt) {
    dateRange.value = [new Date(row.validStartAt), new Date(row.validEndAt)];
    form.validStartAt = row.validStartAt;
    form.validEndAt = row.validEndAt;
  } else {
    dateRange.value = null;
    form.validStartAt = '';
    form.validEndAt = '';
  }
  dialogVisible.value = true;
};

const submitForm = async () => {
  try {
    const payload = { ...form };
    if (dateRange.value && dateRange.value.length === 2) {
      payload.validStartAt = dateRange.value[0].toISOString();
      payload.validEndAt = dateRange.value[1].toISOString();
    }
    if (isEdit.value && editId.value) {
      await updateCouponTemplate(editId.value, payload);
      ElMessage.success('更新成功');
    } else {
      await createCouponTemplate(payload);
      ElMessage.success('创建成功');
    }
    dialogVisible.value = false;
    fetchData();
  } catch (e) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败');
  }
};

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该模板吗？', '提示', { type: 'warning' });
    await deleteCouponTemplate(id);
    ElMessage.success('删除成功');
    fetchData();
  } catch (e) {
    // 取消
  }
};

const formatType = (type: string) => {
  const map: Record<string, string> = {
    CASH: '现金券',
    DISCOUNT: '折扣券',
    FULL_REDUCTION: '满减券',
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

const formatStatusType = (status: string) => {
  const map: Record<string, any> = {
    ACTIVE: 'success',
    PAUSED: 'warning',
    DRAFT: 'info',
    EXPIRED: 'danger',
  };
  return map[status] || 'info';
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
</style>
