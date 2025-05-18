<template>
    <el-dialog v-model="dialogVisible" :title="'评价订单: ' + homestayTitle" width="600px" :before-close="handleClose"
        @open="onDialogOpen">
        <el-form ref="reviewFormRef" :model="reviewForm" :rules="rules" label-width="80px">
            <el-form-item label="总体评分" prop="rating">
                <el-rate v-model="reviewForm.rating" :max="5" show-score text-color="#ff9900" />
            </el-form-item>

            <!-- 可选：添加各项细分评分 -->
            <el-collapse v-model="activeCollapse">
                <el-collapse-item title="详细评分 (可选)" name="detailedRatings">
                    <el-row :gutter="20">
                        <el-col :span="12">
                            <el-form-item label="清洁度">
                                <el-rate v-model="reviewForm.cleanlinessRating" :max="5" />
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item label="准确性">
                                <el-rate v-model="reviewForm.accuracyRating" :max="5" />
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item label="沟通">
                                <el-rate v-model="reviewForm.communicationRating" :max="5" />
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item label="位置">
                                <el-rate v-model="reviewForm.locationRating" :max="5" />
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item label="入住">
                                <el-rate v-model="reviewForm.checkInRating" :max="5" />
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item label="性价比">
                                <el-rate v-model="reviewForm.valueRating" :max="5" />
                            </el-form-item>
                        </el-col>
                    </el-row>
                </el-collapse-item>
            </el-collapse>

            <el-form-item label="评论内容" prop="content">
                <el-input v-model="reviewForm.content" type="textarea" :rows="4" placeholder="分享您的入住体验..."
                    maxlength="500" show-word-limit />
            </el-form-item>
        </el-form>
        <template #footer>
            <span class="dialog-footer">
                <el-button @click="handleClose">取消</el-button>
                <el-button type="primary" @click="submitForm" :loading="loading">提交评价</el-button>
            </span>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, defineProps, defineEmits } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';

// --- Props --- 
const props = defineProps({
    visible: {
        type: Boolean,
        required: true,
    },
    orderId: {
        type: Number,
        required: true,
    },
    homestayId: {
        type: Number,
        required: true,
    },
    homestayTitle: {
        type: String,
        default: '民宿'
    }
});

// --- Emits --- 
const emit = defineEmits(['update:visible', 'submit']);

// --- Refs and Reactive Variables --- 
const dialogVisible = ref(props.visible);
const reviewFormRef = ref<FormInstance>();
const loading = ref(false);
const activeCollapse = ref<string[]>([]); // 控制折叠面板默认不展开

const initialFormState = () => ({
    orderId: props.orderId,
    homestayId: props.homestayId,
    rating: 0,
    content: '',
    cleanlinessRating: 0,
    accuracyRating: 0,
    communicationRating: 0,
    locationRating: 0,
    checkInRating: 0,
    valueRating: 0,
});

const reviewForm = reactive(initialFormState());

// --- Rules --- 
const rules = reactive<FormRules>({
    rating: [
        { required: true, message: '请给出总体评分', trigger: 'change' },
        { type: 'number', min: 1, message: '评分不能低于1星', trigger: 'change' }
    ],
    content: [
        { required: true, message: '请输入评论内容', trigger: 'blur' },
        { min: 10, message: '评论内容至少需要10个字符', trigger: 'blur' },
    ],
});

// --- Watchers --- 
watch(() => props.visible, (newVal) => {
    dialogVisible.value = newVal;
    if (newVal) {
        // Reset form when dialog opens, using props passed potentially at this moment
        Object.assign(reviewForm, initialFormState());
        reviewForm.orderId = props.orderId; // Ensure IDs are set from props
        reviewForm.homestayId = props.homestayId;
        reviewFormRef.value?.resetFields(); // Also reset validation state
        activeCollapse.value = []; // Ensure collapse is closed
    }
});

watch(() => props.orderId, (newVal) => {
    if (dialogVisible.value) { // Only update if dialog is visible
        reviewForm.orderId = newVal;
    }
});

watch(() => props.homestayId, (newVal) => {
    if (dialogVisible.value) { // Only update if dialog is visible
        reviewForm.homestayId = newVal;
    }
});

// --- Methods --- 
const onDialogOpen = () => {
    // Reset form state explicitly when dialog opens
    Object.assign(reviewForm, initialFormState());
    reviewForm.orderId = props.orderId;
    reviewForm.homestayId = props.homestayId;
    reviewFormRef.value?.resetFields(); // Reset validation
    activeCollapse.value = []; // Ensure collapse is closed
};

const handleClose = () => {
    emit('update:visible', false);
};

const submitForm = async () => {
    if (!reviewFormRef.value) return;
    await reviewFormRef.value.validate(async (valid) => {
        if (valid) {
            loading.value = true;
            try {
                console.log('Submitting review form:', reviewForm);
                // Emit the form data including all ratings
                emit('submit', { ...reviewForm });
                // Optionally close dialog here or let parent decide after API call
                // handleClose(); 
            } catch (error) {
                console.error('Error preparing review submission:', error);
                ElMessage.error('提交评价准备失败，请稍后重试');
            } finally {
                loading.value = false;
            }
        } else {
            console.log('error submit!');
            ElMessage.error('请检查表单必填项');
            return false;
        }
    });
};

</script>

<style scoped>
.dialog-footer {
    text-align: right;
}

.el-rate {
    /* Align rate component better with label */
    height: var(--el-form-item-height);
    line-height: var(--el-form-item-height);
}

.el-collapse {
    margin-bottom: 18px;
    /* Add some space before content */
    border-top: none;
    border-bottom: none;
}

.el-collapse-item__header {
    border-bottom: none !important;
}

.el-collapse-item__wrap {
    border-bottom: none !important;
}

/* Remove border around collapse content */
:deep(.el-collapse-item__wrap) {
    border-bottom: none;
}

:deep(.el-collapse-item__header) {
    border-bottom: none;
}
</style>