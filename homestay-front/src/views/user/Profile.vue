<template>
    <div class="profile-container">
        <el-card class="profile-card">
            <div class="profile-header">
                <div class="avatar-section">
                    <el-upload class="avatar-uploader" :show-file-list="false" :before-upload="beforeAvatarUpload"
                        :http-request="customUpload">
                        <el-avatar :size="100"
                            :src="userStore.userInfo?.avatar ? `${baseUrl}${userStore.userInfo.avatar}` : ''">
                            {{ userStore.userInfo?.username?.charAt(0)?.toUpperCase() }}
                        </el-avatar>
                        <div class="avatar-hover-text">点击更换头像</div>
                    </el-upload>
                    <h2>{{ userStore.userInfo?.username }}</h2>
                    <div class="verification-status" :class="verificationStatusClass">
                        <el-tag :type="verificationStatusType">
                            {{ verificationStatusText }}
                        </el-tag>
                    </div>
                </div>
            </div>

            <el-tabs v-model="activeTab" class="profile-tabs">
                <el-tab-pane label="基本信息" name="basic">
                    <div class="info-section">
                        <el-descriptions :column="1" border>
                            <el-descriptions-item label="用户名">
                                {{ userStore.userInfo?.username }}
                            </el-descriptions-item>
                            <el-descriptions-item label="邮箱">
                                {{ userStore.userInfo?.email }}
                            </el-descriptions-item>
                            <el-descriptions-item label="手机号码">
                                {{ userStore.userInfo?.phone || '未设置' }}
                            </el-descriptions-item>
                        </el-descriptions>

                        <div class="verification-section">
                            <div class="section-header">
                                <h3>实名认证信息</h3>
                                <el-tag v-if="isVerified" type="success" size="small">已认证</el-tag>
                                <el-button v-else type="primary" size="small" @click="handleVerification">
                                    去认证
                                </el-button>
                            </div>
                            <el-descriptions :column="1" border>
                                <el-descriptions-item label="真实姓名">
                                    <template v-if="userStore.userInfo?.realName">
                                        {{ maskedRealName }}
                                        <el-tag size="small" type="success" style="margin-left: 8px">已验证</el-tag>
                                    </template>
                                    <template v-else>
                                        未设置
                                    </template>
                                </el-descriptions-item>
                                <el-descriptions-item label="身份证号">
                                    <template v-if="userStore.userInfo?.idCard">
                                        {{ maskedIdCard }}
                                        <el-tag size="small" type="success" style="margin-left: 8px">已验证</el-tag>
                                    </template>
                                    <template v-else>
                                        未设置
                                    </template>
                                </el-descriptions-item>
                            </el-descriptions>
                        </div>
                    </div>
                </el-tab-pane>

                <el-tab-pane label="账号安全" name="security">
                    <div class="security-section">
                        <el-descriptions :column="1" border>
                            <el-descriptions-item label="登录密码">
                                <span>已设置</span>
                                <el-button type="primary" link @click="handleChangePassword">
                                    修改密码
                                </el-button>
                            </el-descriptions-item>
                            <el-descriptions-item label="手机绑定">
                                <span>{{ userStore.userInfo?.phone || '未绑定' }}</span>
                                <el-button type="primary" link @click="handleChangePhone">
                                    {{ userStore.userInfo?.phone ? '修改' : '绑定' }}
                                </el-button>
                            </el-descriptions-item>
                            <el-descriptions-item label="邮箱绑定">
                                <span>{{ userStore.userInfo?.email }}</span>
                                <el-button type="primary" link @click="handleChangeEmail">
                                    修改
                                </el-button>
                            </el-descriptions-item>
                        </el-descriptions>
                    </div>
                </el-tab-pane>
            </el-tabs>
        </el-card>

        <!-- 修改密码对话框 -->
        <el-dialog v-model="passwordDialogVisible" title="修改密码" width="400px">
            <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px">
                <el-form-item label="原密码" prop="oldPassword">
                    <el-input v-model="passwordForm.oldPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                    <el-input v-model="passwordForm.newPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="确认新密码" prop="confirmPassword">
                    <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="passwordDialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="submitPasswordChange">
                        确认
                    </el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useUserStore } from '@/stores/user';
import { ElMessage } from 'element-plus';
import type { UploadProps } from 'element-plus';

const userStore = useUserStore();
const baseUrl = 'http://localhost:8080';
const activeTab = ref('basic');

// 认证状态相关
const isVerified = computed(() => {
    // 如果有真实姓名和身份证号，则认为已认证
    return !!(userStore.userInfo?.realName && userStore.userInfo?.idCard);
});

const verificationStatusType = computed(() => {
    if (userStore.userInfo?.realName && userStore.userInfo?.idCard) {
        return 'success';
    }
    return 'info';
});

const verificationStatusText = computed(() => {
    if (userStore.userInfo?.realName && userStore.userInfo?.idCard) {
        return '已认证';
    }
    return '未认证';
});

const verificationStatusClass = computed(() => {
    return {
        'verified': !!(userStore.userInfo?.realName && userStore.userInfo?.idCard),
        'unverified': !(userStore.userInfo?.realName && userStore.userInfo?.idCard)
    };
});

// 信息脱敏处理
const maskedRealName = computed(() => {
    const realName = userStore.userInfo?.realName;
    if (!realName) return '';
    if (realName.length <= 2) {
        return realName.charAt(0) + '*';
    }
    return realName.charAt(0) + '*'.repeat(realName.length - 2) + realName.charAt(realName.length - 1);
});

const maskedIdCard = computed(() => {
    const idCard = userStore.userInfo?.idCard;
    if (!idCard) return '';
    return idCard.substring(0, 6) + '*'.repeat(8) + idCard.substring(14);
});

// 头像上传相关
const beforeAvatarUpload: UploadProps['beforeUpload'] = (file) => {
    const isImage = /^image\/(jpeg|png|gif)$/.test(file.type);
    const isLt2M = file.size / 1024 / 1024 < 2;

    if (!isImage) {
        ElMessage.error('头像必须是图片格式！');
        return false;
    }
    if (!isLt2M) {
        ElMessage.error('头像大小不能超过 2MB！');
        return false;
    }
    return true;
};

const customUpload = async (options: any) => {
    try {
        await userStore.uploadAvatar(options.file);
        ElMessage.success('头像上传成功');
    } catch (error) {
        ElMessage.error('头像上传失败');
    }
};

// 密码修改相关
const passwordDialogVisible = ref(false);
const passwordFormRef = ref();
const passwordForm = ref({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
});

const passwordRules = {
    oldPassword: [
        { required: true, message: '请输入原密码', trigger: 'blur' },
        { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
    ],
    newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
    ],
    confirmPassword: [
        { required: true, message: '请确认新密码', trigger: 'blur' },
        {
            validator: (rule: any, value: string, callback: Function) => {
                if (value !== passwordForm.value.newPassword) {
                    callback(new Error('两次输入的密码不一致'));
                } else {
                    callback();
                }
            },
            trigger: 'blur'
        }
    ]
};

// 各种操作处理函数
const handleVerification = () => {
    // TODO: 实现实名认证逻辑
    ElMessage.info('实名认证功能开发中');
};

const handleChangePassword = () => {
    passwordDialogVisible.value = true;
};

const handleChangePhone = () => {
    // TODO: 实现手机号修改逻辑
    ElMessage.info('手机号修改功能开发中');
};

const handleChangeEmail = () => {
    // TODO: 实现邮箱修改逻辑
    ElMessage.info('邮箱修改功能开发中');
};

const submitPasswordChange = async () => {
    if (!passwordFormRef.value) return;

    try {
        await passwordFormRef.value.validate();
        await userStore.changePassword({
            oldPassword: passwordForm.value.oldPassword,
            newPassword: passwordForm.value.newPassword
        });
        ElMessage.success('密码修改成功');
        passwordDialogVisible.value = false;
        passwordForm.value = {
            oldPassword: '',
            newPassword: '',
            confirmPassword: ''
        };
    } catch (error: any) {
        ElMessage.error(error.message || '密码修改失败');
    }
};

// 初始化
onMounted(async () => {
    if (userStore.isAuthenticated && !userStore.userInfo) {
        try {
            await userStore.getUserInfo();
        } catch (error) {
            console.error('获取用户信息失败:', error);
            ElMessage.error('获取用户信息失败，请刷新页面重试');
        }
    }
});
</script>

<style scoped>
.profile-container {
    max-width: 800px;
    margin: 20px auto;
    padding: 0 20px;
}

.profile-card {
    background-color: #fff;
    border-radius: 8px;
}

.profile-header {
    text-align: center;
    padding: 20px 0;
    border-bottom: 1px solid var(--el-border-color-light);
}

.avatar-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
}

.avatar-uploader {
    position: relative;
    cursor: pointer;
}

.avatar-uploader:hover .avatar-hover-text {
    opacity: 1;
}

.avatar-hover-text {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    background-color: rgba(0, 0, 0, 0.6);
    color: white;
    padding: 4px;
    font-size: 12px;
    opacity: 0;
    transition: opacity 0.3s;
    border-bottom-left-radius: 50%;
    border-bottom-right-radius: 50%;
}

.verification-status {
    margin-top: -8px;
}

.verification-status.verified .el-tag {
    background-color: var(--el-color-success-light-9);
    border-color: var(--el-color-success-light-5);
    color: var(--el-color-success);
}

.verification-status.pending .el-tag {
    background-color: var(--el-color-warning-light-9);
    border-color: var(--el-color-warning-light-5);
    color: var(--el-color-warning);
}

.verification-status.unverified .el-tag {
    background-color: var(--el-color-info-light-9);
    border-color: var(--el-color-info-light-5);
    color: var(--el-color-info);
}

.profile-tabs {
    margin-top: 20px;
}

.info-section,
.security-section {
    padding: 20px 0;
}

.verification-section {
    margin-top: 30px;
    background-color: var(--el-fill-color-blank);
    border-radius: 4px;
}

.section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding: 16px 24px;
    border-bottom: 1px solid var(--el-border-color-light);
}

.section-header h3 {
    margin: 0;
    color: var(--el-text-color-primary);
    font-size: 16px;
    font-weight: 500;
}

:deep(.el-descriptions) {
    margin-bottom: 0;
}

:deep(.el-descriptions__cell) {
    padding: 16px 24px;
}

:deep(.el-descriptions__label) {
    width: 120px;
    color: var(--el-text-color-regular);
    font-weight: normal;
}

:deep(.el-tag--small) {
    height: 20px;
    padding: 0 6px;
    font-size: 12px;
}

.verified-info {
    display: flex;
    align-items: center;
    gap: 8px;
}

.verified-info .el-tag {
    margin-left: 8px;
}

.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 20px;
}
</style>