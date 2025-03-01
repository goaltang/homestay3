<template>
    <div class="profile-container">
        <el-card class="profile-card">
            <template #header>
                <div class="card-header">
                    <h2>个人中心</h2>
                </div>
            </template>

            <div class="avatar-container">
                <div class="avatar-wrapper">
                    <el-avatar :size="100" :src="avatarUrl" @error="handleAvatarError">
                        <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=fallback" />
                    </el-avatar>
                    <div class="avatar-upload">
                        <el-upload class="avatar-uploader" :show-file-list="false" :before-upload="beforeAvatarUpload"
                            :http-request="customUpload" accept="image/jpeg,image/png,image/gif">
                            <el-button size="small" type="primary">更换头像</el-button>
                        </el-upload>
                    </div>
                </div>
            </div>

            <el-tabs v-model="activeTab" class="profile-tabs">
                <el-tab-pane label="基本信息" name="basic">
                    <div class="info-section">
                        <el-descriptions :column="1" border>
                            <el-descriptions-item label="用户名">
                                {{ userStore.user?.username }}
                            </el-descriptions-item>
                            <el-descriptions-item label="邮箱">
                                {{ userStore.user?.email }}
                            </el-descriptions-item>
                            <el-descriptions-item label="手机号码">
                                {{ userStore.user?.phone || '未设置' }}
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
                                    <template v-if="userStore.user?.realName">
                                        {{ maskedRealName }}
                                        <el-tag size="small" type="success" style="margin-left: 8px">已验证</el-tag>
                                    </template>
                                    <template v-else>
                                        未设置
                                    </template>
                                </el-descriptions-item>
                                <el-descriptions-item label="身份证号">
                                    <template v-if="userStore.user?.idCard">
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
                                <span>{{ userStore.user?.phone || '未绑定' }}</span>
                                <el-button type="primary" link @click="handleChangePhone">
                                    {{ userStore.user?.phone ? '修改' : '绑定' }}
                                </el-button>
                            </el-descriptions-item>
                            <el-descriptions-item label="邮箱绑定">
                                <span>{{ userStore.user?.email }}</span>
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
import type { UploadRequestOptions } from 'element-plus';
import { Loading } from '@element-plus/icons-vue';

const userStore = useUserStore();
const activeTab = ref('basic');
const loading = ref(false);
const mode = ref(import.meta.env.MODE || 'development');
const isDev = ref(mode.value === 'development');

// 获取API基础URL
const apiBaseUrl = computed(() => import.meta.env.VITE_API_BASE_URL || '');

// 表单数据
const form = ref({
    username: userStore.user?.username || "",
    email: userStore.user?.email || "",
    phone: userStore.user?.phone || "",
    realName: userStore.user?.realName || "",
    idCard: userStore.user?.idCard || "",
});

// 计算属性：头像URL
const avatarUrl = computed(() => {
    if (!userStore.user?.avatar) {
        // 如果没有头像，返回默认头像
        const seed = userStore.user?.username || "default" + Date.now();
        return `https://api.dicebear.com/7.x/avataaars/svg?seed=${seed}`;
    }

    // 如果是完整URL，直接返回
    if (userStore.user.avatar.startsWith("http")) {
        return userStore.user.avatar;
    }

    // 获取基础URL
    const baseUrl = apiBaseUrl.value;

    // 提取文件名
    const filename = userStore.user.avatar.split("/").pop();
    if (filename) {
        // 使用相对路径访问头像，避免CORS问题
        const apiUrl = `/api/files/avatar/${filename}`;
        console.log("使用相对路径访问头像:", apiUrl);
        return apiUrl;
    }

    // 如果无法提取文件名，使用默认头像
    const seed = userStore.user?.username || "fallback" + Date.now();
    return `https://api.dicebear.com/7.x/avataaars/svg?seed=${seed}`;
});

// 认证状态相关
const isVerified = computed(() => {
    // 如果有真实姓名和身份证号，则认为已认证
    return !!(userStore.user?.realName && userStore.user?.idCard);
});

const verificationStatusType = computed(() => {
    if (userStore.user?.realName && userStore.user?.idCard) {
        return 'success';
    }
    return 'info';
});

const verificationStatusText = computed(() => {
    if (userStore.user?.realName && userStore.user?.idCard) {
        return '已认证';
    }
    return '未认证';
});

const verificationStatusClass = computed(() => {
    return {
        'verified': !!(userStore.user?.realName && userStore.user?.idCard),
        'unverified': !(userStore.user?.realName && userStore.user?.idCard)
    };
});

// 信息脱敏处理
const maskedRealName = computed(() => {
    const realName = userStore.user?.realName;
    if (!realName) return '';
    if (realName.length <= 2) {
        return realName.charAt(0) + '*';
    }
    return realName.charAt(0) + '*'.repeat(realName.length - 2) + realName.charAt(realName.length - 1);
});

const maskedIdCard = computed(() => {
    const idCard = userStore.user?.idCard;
    if (!idCard) return '';
    return idCard.substring(0, 6) + '*'.repeat(8) + idCard.substring(14);
});

// 头像上传前的验证
const beforeAvatarUpload = (file: File) => {
    // 检查文件类型
    const isImage = file.type.startsWith('image/');
    if (!isImage) {
        ElMessage.error('只能上传图片文件!');
        return false;
    }

    // 检查文件大小 (10MB)
    const isLt10M = file.size / 1024 / 1024 < 10;
    if (!isLt10M) {
        ElMessage.error('图片大小不能超过10MB!');
        return false;
    }

    return true;
};

// 自定义上传方法
const customUpload = async (options: UploadRequestOptions) => {
    try {
        loading.value = true;
        const file = options.file as File;
        console.log("开始上传头像:", file.name);

        const avatarPath = await userStore.uploadAvatar(file);
        if (avatarPath) {
            ElMessage.success('头像上传成功');
            // 确保用户信息已保存到本地存储
            if (userStore.user) {
                localStorage.setItem("user", JSON.stringify(userStore.user));
            }
        }
    } catch (error: any) {
        console.error("头像上传失败:", error);
        // 提供更详细的错误信息
        if (error.response && error.response.status === 413) {
            ElMessage.error('头像文件太大，超出服务器允许的上传大小限制');
        } else if (error.message && error.message.includes('Maximum upload size exceeded')) {
            ElMessage.error('头像文件太大，超出服务器允许的上传大小限制');
        } else {
            ElMessage.error(error.message || '头像上传失败，请稍后重试');
        }
        // 确保不会丢失用户状态
        console.log("保持用户登录状态不变");
    } finally {
        loading.value = false;
    }
};

// 头像加载错误处理
const handleAvatarError = (e: Event) => {
    const target = e.target as HTMLImageElement;
    const failedUrl = target.src;

    console.error("头像加载失败:", failedUrl);
    console.log("头像加载错误详情:", {
        原始头像: userStore.user?.avatar,
        计算URL: avatarUrl.value,
        加载失败URL: failedUrl,
        用户信息: userStore.user
    });

    // 直接使用默认头像
    const seed = userStore.user?.username || "fallback" + Date.now();
    const defaultAvatar = `https://api.dicebear.com/7.x/avataaars/svg?seed=${seed}`;
    console.log("已切换到默认头像:", defaultAvatar);
    target.src = defaultAvatar;
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
    if (userStore.isLoggedIn && !userStore.user) {
        try {
            await userStore.fetchUserInfo();
        } catch (error) {
            console.error('获取用户信息失败:', error);
            ElMessage.error('获取用户信息失败，请刷新页面重试');
        }
    }

    // 打印环境信息
    console.log('环境信息:', {
        模式: mode.value,
        API基础URL: apiBaseUrl.value,
        开发模式: isDev.value
    });
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

.avatar-container {
    text-align: center;
    margin-bottom: 20px;
}

.avatar-wrapper {
    position: relative;
    display: inline-block;
}

.avatar-upload {
    position: absolute;
    bottom: 0;
    right: 0;
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