<template>
  <el-dialog
    :model-value="visible"
    title="修改评价"
    width="500px"
    @close="handleClose"
    :close-on-click-modal="false"
  >
    <el-form
      ref="editFormRef"
      :model="formData"
      :rules="formRules"
      label-width="80px"
      v-loading="loading"
    >
      <el-form-item label="评分" prop="rating">
        <el-rate v-model="formData.rating" :colors="['#99A9BF', '#F7BA2A', '#FF9900']" />
      </el-form-item>
      <el-form-item label="内容" prop="content">
        <el-input
          type="textarea"
          v-model="formData.content"
          :rows="5"
          maxlength="1000"
          show-word-limit
          placeholder="请输入评价内容"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确认修改
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, nextTick } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import { updateReview } from '@/api/review'; // 导入更新 API

// 定义评价数据接口
interface ReviewData {
  id: number;
  rating: number;
  content: string;
}

// 定义 props
const props = defineProps<{
  visible: boolean;
  reviewData: ReviewData | null; // 接收要编辑的评价数据
}>();

// 定义 emits
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'submitted', updatedReview: ReviewData): void; // 提交成功后传递更新后的数据
}>();

const editFormRef = ref<FormInstance>();
const loading = ref(false); // 可以添加加载状态，比如初始化数据时
const submitting = ref(false);

// 表单数据
const formData = reactive<ReviewData>({
  id: 0,
  rating: 0,
  content: '',
});

// 表单验证规则
const formRules = reactive<FormRules>({
  rating: [{ required: true, message: '请选择评分', trigger: 'change' }],
  content: [{ required: true, message: '请输入评价内容', trigger: 'blur' }],
});

// 监听 props.reviewData 变化，填充表单
watch(() => props.reviewData, (newVal) => {
  if (newVal && props.visible) {
      nextTick(() => { // 确保 DOM 更新后再访问 ref
          editFormRef.value?.resetFields(); // 重置验证状态
          formData.id = newVal.id;
          formData.rating = newVal.rating;
          formData.content = newVal.content;
      });
  } else {
      // 如果弹窗关闭或数据为空，也重置表单
      nextTick(() => {
          editFormRef.value?.resetFields();
          formData.id = 0;
          formData.rating = 0;
          formData.content = '';
      });
  }
}, { immediate: true, deep: true }); // 立即执行并在 visible 变为 true 时填充


// 关闭弹窗
const handleClose = () => {
  emit('update:visible', false);
};

// 提交表单
const handleSubmit = async () => {
  if (!editFormRef.value) return;
  await editFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true;
      try {
        // 后端需要的数据格式 { rating: number, content: string }
        const payload = {
            rating: formData.rating,
            content: formData.content,
        };
        const response = await updateReview(formData.id, payload);
        ElMessage.success('评价修改成功');
        // 传递更新后的数据给父组件
        // 后端返回的可能是完整的 ReviewDTO，我们只需要更新 rating 和 content
        // 或者直接使用 formData，因为它们已经是更新后的值
        emit('submitted', { ...formData });
        handleClose();
      } catch (error: any) {
        console.error('修改评价失败:', error);
        const errMsg = error?.response?.data?.message || '修改评价失败，请稍后重试';
        ElMessage.error(errMsg);
      } finally {
        submitting.value = false;
      }
    } else {
      console.log('表单验证失败');
    }
  });
};
</script>

<style scoped>
.dialog-footer {
  text-align: right;
}
/* 可以添加其他样式 */
</style> 