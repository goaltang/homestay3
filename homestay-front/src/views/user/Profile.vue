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

                    <!-- 增加图片调试信息 -->
                    <div class="avatar-debug" v-if="isDev">
                        <small>{{ userStore.userInfo?.avatar }}</small>
                    </div>

                    <div class="avatar-upload">
                        <el-upload class="avatar-uploader" :action="`${apiBaseUrl}/api/files/upload`" :headers="{
                            ...uploadHeaders,
                            'Accept': '*/*',
                        }" :data="{ type: 'avatar' }" :on-success="handleAvatarSuccess"
                            :before-upload="beforeAvatarUpload" :show-file-list="false" :on-error="handleAvatarError"
                            :with-credentials="true" :multiple="false" accept="image/jpeg,image/png,image/gif"
                            name="file">
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
import type { UploadRequestOptions } from 'element-plus';
import { Loading } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import request from '@/utils/request';
import { getAvatarUrl, handleImageError } from '@/utils/image';
import axios from 'axios';

const userStore = useUserStore();
const router = useRouter();
const activeTab = ref('basic');
const loading = ref(false);
const mode = ref(import.meta.env.MODE || 'development');
const isDev = ref(mode.value === 'development');

// 获取API基础URL
const apiBaseUrl = computed(() => import.meta.env.VITE_API_BASE_URL || '');

// 表单数据
const form = ref({
    username: userStore.userInfo?.username || "",
    email: userStore.userInfo?.email || "",
    phone: userStore.userInfo?.phone || "",
    realName: userStore.userInfo?.realName || "",
    idCard: userStore.userInfo?.idCard || "",
});

// 计算属性：头像URL
const avatarUrl = computed(() => {
    if (!userStore.userInfo?.avatar) {
        return getAvatarUrl('');
    }
    // 添加时间戳参数确保浏览器不使用缓存
    const baseUrl = getAvatarUrl(userStore.userInfo.avatar);
    return `${baseUrl}?t=${new Date().getTime()}`;
});

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

// 头像上传前的验证
const beforeAvatarUpload = (file: File) => {
    console.log('开始前置验证: 文件名=', file.name, '大小=', Math.round(file.size / 1024) + 'KB', '类型=', file.type);

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

    console.log('文件验证通过，准备上传...');
    ElMessage.info({
        message: '正在上传头像，请稍候...',
        duration: 2000
    });

    return true;
};

// 上传参数头部
const uploadHeaders = computed(() => {
    return {
        Authorization: `Bearer ${localStorage.getItem('token')}`
    };
});

// 头像上传成功处理
const handleAvatarSuccess = (response: any, file: File) => {
    console.log('头像上传响应数据:', response);
    console.log('头像文件信息:', file);

    try {
        if (!response) {
            console.error('服务器响应为空');
            ElMessage.error('头像上传失败：服务器未返回数据');
            setTimeout(() => window.location.reload(), 2000);
            return;
        }

        // 根据响应结构获取文件名
        let fileName = '';
        
        if (response.fileName) {
            // 直接使用返回的文件名
            fileName = response.fileName;
            console.log('使用服务器返回的fileName:', fileName);
        } else if (response.status === 'success' && response.downloadUrl) {
            // 从URL中提取文件名
            const urlParts = response.downloadUrl.split('/');
            fileName = urlParts[urlParts.length - 1];
            console.log('从downloadUrl提取的fileName:', fileName);
        } else if (typeof response === 'string') {
            // 如果响应是字符串，可能就是文件名
            fileName = response;
            console.log('将字符串响应作为fileName:', fileName);
        } else if (response.data && response.data.fileName) {
            // 如果响应中有data.fileName
            fileName = response.data.fileName;
            console.log('使用response.data.fileName:', fileName);
        }

        if (!fileName) {
            console.error('无法从响应中提取文件名:', response);
            throw new Error('无法获取有效的头像文件名');
        }

        // 使用工具函数构建头像URL
        const avatarUrl = getAvatarUrl(fileName);
        console.log('构建的头像URL:', avatarUrl);
        
        // 更新用户信息
        updateUserAvatar(avatarUrl);
    } catch (error) {
        console.error('处理头像上传响应时出错:', error);
        ElMessage.error('头像更新失败，请重试');
        // 如果出错，2秒后刷新页面
        setTimeout(() => window.location.reload(), 2000);
    }
};

// 更新用户头像URL
const updateUserAvatar = (avatarUrl: string) => {
    // 先确保URL不包含时间戳参数，清除可能已有的时间戳
    let cleanUrl = avatarUrl;
    if (cleanUrl.includes('?')) {
        cleanUrl = cleanUrl.split('?')[0];
    }
    
    // 为URL添加时间戳参数
    const urlWithTimestamp = `${cleanUrl}?t=${new Date().getTime()}`;
    
    // 更新用户信息中的头像URL
    if (userStore.userInfo) {
        // 使用不带时间戳的URL存储，避免累积参数
        userStore.userInfo.avatar = cleanUrl;

        // 更新本地存储的用户信息
        const userInfo = localStorage.getItem('userInfo');
        if (userInfo) {
            const parsedUserInfo = JSON.parse(userInfo);
            parsedUserInfo.avatar = cleanUrl;
            localStorage.setItem('userInfo', JSON.stringify(parsedUserInfo));
        }
    }

    // 通知用户
    ElMessage.success('头像更新成功');

    // 强制刷新页面上显示的头像
    refreshAvatar();
    
    // 延迟500ms后刷新整个页面，确保所有组件都能看到新头像
    setTimeout(() => {
        window.location.reload();
    }, 500);
};

// 刷新头像显示
const refreshAvatar = () => {
    // 生成唯一的时间戳，确保缓存失效
    const timestamp = new Date().getTime();
    
    // 更新本地头像URL以强制刷新
    if (userStore.userInfo && userStore.userInfo.avatar) {
        const baseUrl = userStore.userInfo.avatar.split('?')[0];
        userStore.userInfo.avatar = `${baseUrl}?t=${timestamp}&nocache=true`;
    }
    
    // 查找并更新所有头像图片元素
    const avatarElements = document.querySelectorAll('.el-avatar img');
    avatarElements.forEach(element => {
        const img = element as HTMLImageElement;
        if (img.src) {
            // 移除旧的时间戳参数(如果有)
            const baseUrl = img.src.split('?')[0];
            img.src = `${baseUrl}?t=${timestamp}&nocache=true`;
            
            // 添加图片加载完成监听，确保图片已更新
            img.onload = () => console.log('头像图片已成功重新加载');
        }
    });
};

// 头像加载/上传错误处理
const handleAvatarError = (e: Event | any, file?: any, fileList?: any) => {
    if (e instanceof Event) {
        // 头像加载错误
        console.error('头像加载错误:', e);
        handleImageError(e, 'avatar');
    } else {
        // 头像上传错误
        console.error('头像上传错误:', e);
        console.error('上传的文件:', file);

        // 检查是否是服务器返回了200但无法解析JSON
        if (e.name === 'SyntaxError' && e.message?.includes('JSON')) {
            console.warn('服务器返回了非JSON响应，但请求可能成功。尝试刷新页面获取最新头像');
            ElMessage.warning('上传可能已成功，将刷新页面获取最新头像');
            setTimeout(() => window.location.reload(), 1500);
            return;
        }

        // 检查是否网络错误或CORS问题
        if (e.name === 'UploadAjaxError' || e.status === 0) {
            console.error('上传网络错误或CORS问题:', e);
            ElMessage.error('网络错误：请检查网络连接或CORS配置');

            // 尝试查看后端日志，如果服务器实际上处理了请求，我们可以尝试刷新页面
            ElMessage.warning('正在检查上传是否实际成功...');
            setTimeout(() => window.location.reload(), 2000);
            return;
        }

        // 记录详细的错误信息
        if (e.response) {
            console.error('错误响应数据:', e.response);
        }

        // 解析错误消息
        let errorMsg = '头像上传失败';

        if (e.status === 401) {
            errorMsg = '登录已过期，请重新登录';
        } else if (e.status === 413) {
            errorMsg = '文件太大，无法上传';
        } else if (e.message) {
            errorMsg = e.message;
        } else if (e.response) {
            if (typeof e.response === 'string') {
                try {
                    const resp = JSON.parse(e.response);
                    errorMsg = resp.message || resp.error || '上传失败';
                } catch (_) {
                    errorMsg = e.response;
                }
            } else if (e.response.data) {
                errorMsg = e.response.data.message || e.response.data.error || `服务器错误 (${e.response.status})`;
            }
        }

        // 显示错误消息
        ElMessage.error(errorMsg);

        // 调试日志
        console.log('上传组件配置:', {
            action: `${apiBaseUrl.value}/api/files/upload`,
            headers: uploadHeaders.value,
            data: { type: 'avatar' }
        });
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

// 检查用户信息是否完整，如果不完整则自动获取
const checkUserInfo = async () => {
    if (userStore.token && !userStore.userInfo) {
        try {
            console.log("用户已登录但缺少用户信息，尝试获取用户信息");
            await userStore.fetchUserInfo();
            console.log("用户信息获取成功:", userStore.userInfo);
        } catch (error: any) {
            console.error("获取用户信息失败:", error);
            if (error.response && error.response.status === 401) {
                ElMessage.error("登录已过期，请重新登录");
                userStore.logout();
                router.push("/login");
            }
        }
    }
};

// 自定义上传处理
const handleCustomUpload = (options: any) => {
    const { file, headers, data, action, onSuccess, onError } = options;

    // 检查文件
    if (!beforeAvatarUpload(file)) {
        return;
    }

    console.log('开始自定义上传，参数:', {
        file: file.name,
        size: file.size,
        type: file.type,
        action
    });

    // 创建FormData
    const formData = new FormData();
    formData.append('file', file); // 确保字段名为"file"，与后端接收参数一致
    formData.append('type', 'avatar');

    // 添加其他数据
    if (data) {
        Object.keys(data).forEach(key => {
            formData.append(key, data[key]);
        });
    }

    // 构建完整URL
    let fullUrl = "";
    if (action.startsWith('http')) {
        fullUrl = action;
    } else {
        // 移除可能的重复路径
        const cleanAction = action.startsWith('/api') ? action : `/api${action}`;
        fullUrl = `${apiBaseUrl.value}${cleanAction}`;
    }

    console.log('上传完整URL:', fullUrl);

    // 使用 request 工具而不是直接使用 axios，确保正确应用拦截器和共享配置
    try {
        // 使用本地 fetch 而不是 axios，避免 CORS 问题
        fetch(fullUrl, {
            method: 'POST',
            headers: {
                ...headers
                // 不要手动设置 Content-Type，让浏览器自动设置 multipart/form-data 和 boundary
            },
            body: formData,
            credentials: 'include'  // 包含凭证
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`上传失败: ${response.status} ${response.statusText}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('上传成功:', data);
                onSuccess(data);
            })
            .catch(error => {
                console.error('上传失败:', error);
                onError(error);
            });
    } catch (error) {
        console.error('上传请求创建失败:', error);
        onError(error);
    }
};

// 初始化
onMounted(async () => {
    if (userStore.token && !userStore.userInfo) {
        try {
            await userStore.fetchUserInfo();
        } catch (error) {
            console.error("获取用户信息失败:", error);
        }
    }

    console.log("个人中心初始化信息:", {
        API基地址: apiBaseUrl.value,
        开发模式: isDev.value
    });

    await checkUserInfo();
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