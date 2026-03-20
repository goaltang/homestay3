<template>
  <div>
    <div class="container">
      <div class="handle-box">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-select v-model="query.status" placeholder="公告状态" clearable class="handle-select mr10" @change="handleSearch">
              <el-option label="全部状态" value=""></el-option>
              <el-option label="草稿" value="DRAFT"></el-option>
              <el-option label="已发布" value="PUBLISHED"></el-option>
              <el-option label="已下线" value="OFFLINE"></el-option>
            </el-select>
            <el-button :icon="Plus" type="primary" @click="handleAdd">新增公告</el-button>
            <el-button :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" circle @click="clearSearch"></el-button>
          </el-col>
        </el-row>
      </div>

      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <span>公告管理</span>
            <span class="log-count" v-if="pageTotal > 0">共 {{ pageTotal }} 条记录</span>
          </div>
        </template>

        <el-table v-loading="loading" :data="tableData" border stripe highlight-current-row class="table">
          <el-table-column prop="id" label="ID" width="80"></el-table-column>
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip></el-table-column>
          <el-table-column prop="category" label="分类" width="150">
            <template #default="scope">
              <el-tag size="small" :type="scope.row.category === 'SYSTEM_NOTIFICATION' ? 'primary' : 'success'">
                {{ scope.row.category === 'SYSTEM_NOTIFICATION' ? '系统通知' : '活动公告' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag size="small" :type="getStatusTag(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="priority" label="优先级" width="80" align="center">
            <template #default="scope">
              {{ scope.row.priority || 0 }}
            </template>
          </el-table-column>
          <el-table-column prop="publisherName" label="发布人" width="120"></el-table-column>
          <el-table-column prop="publishedAt" label="发布时间" width="180">
            <template #default="scope">
              {{ scope.row.publishedAt ? formatDate(scope.row.publishedAt) : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180">
            <template #default="scope">
              {{ formatDate(scope.row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right" align="center">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="handleEdit(scope.row)">编辑</el-button>
              <el-button
                v-if="scope.row.status === 'DRAFT'"
                type="success"
                link
                size="small"
                @click="handlePublish(scope.row)"
              >发布</el-button>
              <el-button
                v-if="scope.row.status === 'PUBLISHED'"
                type="warning"
                link
                size="small"
                @click="handleOffline(scope.row)"
              >下线</el-button>
              <el-button type="danger" link size="small" @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无公告" />
          </template>
        </el-table>

        <div class="pagination">
          <el-pagination
            background
            layout="total, sizes, prev, pager, next, jumper"
            :current-page="query.pageIndex"
            :page-size="query.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="pageTotal"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </el-card>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editMode ? '编辑公告' : '新增公告'"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="200" show-word-limit></el-input>
        </el-form-item>
        <el-form-item label="公告分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="系统通知" value="SYSTEM_NOTIFICATION"></el-option>
            <el-option label="活动公告" value="ACTIVITY_ANNOUNCEMENT"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="form.priority" :min="0" :max="999" placeholder="数值越大越靠前"></el-input-number>
        </el-form-item>
        <el-form-item label="展示时间">
          <el-date-picker
            v-model="form.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="请输入公告内容"
            maxlength="5000"
            show-word-limit
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAnnouncement" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus';
import { Search, Refresh, Plus } from '@element-plus/icons-vue';
import {
  getAdminAnnouncementsApi,
  getAdminAnnouncementByIdApi,
  createAnnouncementApi,
  updateAnnouncementApi,
  deleteAnnouncementApi,
  publishAnnouncementApi,
  offlineAnnouncementApi,
  Announcement
} from '@/api/announcement';

const tableData = ref<Announcement[]>([]);
const loading = ref(false);
const pageTotal = ref(0);
const dialogVisible = ref(false);
const editMode = ref(false);
const submitLoading = ref(false);
const formRef = ref<FormInstance>();
const currentUsername = ref(localStorage.getItem('username') || 'admin');
const currentUserId = ref<number>(Number(localStorage.getItem('userId')) || 1);

const query = reactive<{
  pageIndex: number;
  pageSize: number;
  status: string;
}>({
  pageIndex: 1,
  pageSize: 20,
  status: ''
});

const form = reactive<{
  id?: number;
  title: string;
  content: string;
  category: string;
  status: string;
  priority: number;
  dateRange: string[];
}>({
  title: '',
  content: '',
  category: 'SYSTEM_NOTIFICATION',
  status: 'DRAFT',
  priority: 0,
  dateRange: []
});

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
};

onMounted(() => {
  getAnnouncements();
});

const getAnnouncements = () => {
  loading.value = true;
  const params = {
    page: query.pageIndex - 1,
    size: query.pageSize,
    status: query.status || undefined
  };

  getAdminAnnouncementsApi(params)
    .then(response => {
      if (response.success) {
        tableData.value = response.data || [];
        pageTotal.value = response.total || 0;
      } else {
        ElMessage.error(response.message || '获取公告失败');
        tableData.value = [];
        pageTotal.value = 0;
      }
    })
    .catch(error => {
      console.error('获取公告出错:', error);
      ElMessage.error('获取公告出错');
      tableData.value = [];
      pageTotal.value = 0;
    })
    .finally(() => {
      loading.value = false;
    });
};

const handleSearch = () => {
  query.pageIndex = 1;
  getAnnouncements();
};

const clearSearch = () => {
  query.status = '';
  query.pageIndex = 1;
  getAnnouncements();
};

const handlePageChange = (val: number) => {
  query.pageIndex = val;
  getAnnouncements();
};

const handleSizeChange = (val: number) => {
  query.pageSize = val;
  query.pageIndex = 1;
  getAnnouncements();
};

const handleAdd = () => {
  editMode.value = false;
  resetForm();
  dialogVisible.value = true;
};

const handleEdit = (row: Announcement) => {
  editMode.value = true;
  getAdminAnnouncementByIdApi(row.id!).then(response => {
    if (response.success && response.data) {
      const announcement = response.data;
      form.id = announcement.id;
      form.title = announcement.title;
      form.content = announcement.content;
      form.category = announcement.category;
      form.status = announcement.status;
      form.priority = announcement.priority || 0;
      form.dateRange = [];
      if (announcement.startTime) {
        form.dateRange[0] = announcement.startTime;
      }
      if (announcement.endTime) {
        form.dateRange[1] = announcement.endTime;
      }
      dialogVisible.value = true;
    } else {
      ElMessage.error(response.message || '获取公告详情失败');
    }
  });
};

const handleDialogClose = () => {
  resetForm();
};

const resetForm = () => {
  form.id = undefined;
  form.title = '';
  form.content = '';
  form.category = 'SYSTEM_NOTIFICATION';
  form.status = 'DRAFT';
  form.priority = 0;
  form.dateRange = [];
  formRef.value?.resetFields();
};

const saveAnnouncement = async () => {
  if (!formRef.value) return;

  await formRef.value.validate((valid) => {
    if (valid) {
      submitLoading.value = true;
      const announcement: Announcement = {
        title: form.title,
        content: form.content,
        category: form.category,
        status: form.status,
        priority: form.priority,
        startTime: form.dateRange && form.dateRange.length === 2 ? form.dateRange[0] : undefined,
        endTime: form.dateRange && form.dateRange.length === 2 ? form.dateRange[1] : undefined
      };

      const promise = editMode.value
        ? updateAnnouncementApi(form.id!, announcement, currentUsername.value)
        : createAnnouncementApi(announcement, currentUserId.value, currentUsername.value);

      promise
        .then(response => {
          if (response.success) {
            ElMessage.success(response.message || (editMode.value ? '更新成功' : '创建成功'));
            dialogVisible.value = false;
            getAnnouncements();
          } else {
            ElMessage.error(response.message || (editMode.value ? '更新失败' : '创建失败'));
          }
        })
        .catch(error => {
          console.error('保存公告出错:', error);
          ElMessage.error('保存公告出错');
        })
        .finally(() => {
          submitLoading.value = false;
        });
    }
  });
};

const handlePublish = (row: Announcement) => {
  ElMessageBox.confirm('确定要发布此公告吗？', '发布确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    publishAnnouncementApi(row.id!, currentUserId.value, currentUsername.value)
      .then(response => {
        if (response.success) {
          ElMessage.success('发布成功');
          getAnnouncements();
        } else {
          ElMessage.error(response.message || '发布失败');
        }
      })
      .catch(() => {
        ElMessage.error('发布失败');
      });
  }).catch(() => {});
};

const handleOffline = (row: Announcement) => {
  ElMessageBox.confirm('确定要下线此公告吗？', '下线确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    offlineAnnouncementApi(row.id!, currentUsername.value)
      .then(response => {
        if (response.success) {
          ElMessage.success('下线成功');
          getAnnouncements();
        } else {
          ElMessage.error(response.message || '下线失败');
        }
      })
      .catch(() => {
        ElMessage.error('下线失败');
      });
  }).catch(() => {});
};

const handleDelete = (row: Announcement) => {
  ElMessageBox.confirm('确定要删除此公告吗？删除后无法恢复。', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteAnnouncementApi(row.id!, currentUsername.value)
      .then(response => {
        if (response.success) {
          ElMessage.success('删除成功');
          getAnnouncements();
        } else {
          ElMessage.error(response.message || '删除失败');
        }
      })
      .catch(() => {
        ElMessage.error('删除失败');
      });
  }).catch(() => {});
};

const getStatusTag = (status?: string) => {
  const map: Record<string, string> = {
    DRAFT: 'info',
    PUBLISHED: 'success',
    OFFLINE: 'warning'
  };
  return map[status || ''] || 'info';
};

const getStatusText = (status?: string) => {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    OFFLINE: '已下线'
  };
  return map[status || ''] || status;
};

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
};
</script>

<style scoped>
.handle-box { margin-bottom: 20px; }
.handle-input { width: 150px; display: inline-block; }
.handle-select { width: 130px; display: inline-block; }
.mr10 { margin-right: 10px; }
.table { width: 100%; font-size: 14px; }
.pagination { margin: 20px 0; text-align: right; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.log-count { font-size: 12px; color: #909399; }
</style>
