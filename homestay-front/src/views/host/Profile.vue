<template>
    <div class="host-profile">
        <div class="header">
            <h2>房东资料</h2>
        </div>

        <el-row :gutter="20">
            <el-col :span="8">
                <el-card class="profile-card" shadow="hover">
                    <div class="profile-avatar">
                        <el-avatar :size="120"
                            :src="hostInfo.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
                        <div class="upload-avatar">
                            <el-upload class="avatar-uploader" :action="`${API_URL}/api/v1/users/upload/avatar`"
                                :headers="uploadHeaders" :show-file-list="false" :on-success="handleAvatarSuccess"
                                :before-upload="beforeAvatarUpload">
                                <el-button size="small">更换头像</el-button>
                            </el-upload>
                        </div>
                    </div>
                    <div class="profile-info">
                        <h3>{{ hostInfo.nickname || hostInfo.username }}</h3>
                        <p>{{ hostInfo.email }}</p>
                        <div class="profile-stats">
                            <div class="stat-item">
                                <div class="stat-number">{{ hostInfo.homestayCount || 0 }}</div>
                                <div class="stat-label">房源数</div>
                            </div>
                            <div class="stat-item">
                                <div class="stat-number">{{ hostInfo.orderCount || 0 }}</div>
                                <div class="stat-label">订单数</div>
                            </div>
                            <div class="stat-item">
                                <div class="stat-number">{{ hostInfo.reviewCount || 0 }}</div>
                                <div class="stat-label">评价数</div>
                            </div>
                        </div>
                        <div class="host-since">
                            <span class="label">成为房东时间:</span>
                            <span>{{ hostInfo.hostSince || '暂无数据' }}</span>
                        </div>
                    </div>
                </el-card>
            </el-col>

            <el-col :span="16">
                <el-tabs type="border-card">
                    <el-tab-pane label="基本信息">
                        <el-form ref="basicFormRef" :model="basicForm" :rules="basicRules" label-width="100px"
                            class="profile-form">
                            <el-form-item label="用户名" prop="username">
                                <el-input v-model="basicForm.username" disabled />
                            </el-form-item>
                            <el-form-item label="昵称" prop="nickname">
                                <el-input v-model="basicForm.nickname" placeholder="请输入昵称" />
                            </el-form-item>
                            <el-form-item label="手机号" prop="phone">
                                <el-input v-model="basicForm.phone" placeholder="请输入手机号" />
                            </el-form-item>
                            <el-form-item label="电子邮箱" prop="email">
                                <el-input v-model="basicForm.email" placeholder="请输入邮箱" />
                            </el-form-item>
                            <el-form-item label="性别" prop="gender">
                                <el-select v-model="basicForm.gender" placeholder="请选择性别">
                                    <el-option label="男" value="MALE" />
                                    <el-option label="女" value="FEMALE" />
                                    <el-option label="保密" value="OTHER" />
                                </el-select>
                            </el-form-item>
                            <el-form-item label="生日" prop="birthday">
                                <el-date-picker v-model="basicForm.birthday" type="date" placeholder="请选择生日"
                                    format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
                            </el-form-item>
                            <el-form-item>
                                <el-button type="primary" @click="saveBasicInfo">保存</el-button>
                            </el-form-item>
                        </el-form>
                    </el-tab-pane>

                    <el-tab-pane label="房东介绍">
                        <el-form ref="introFormRef" :model="introForm" :rules="introRules" label-width="100px"
                            class="profile-form">
                            <el-form-item label="职业" prop="occupation">
                                <el-input v-model="introForm.occupation" placeholder="请输入您的职业" />
                            </el-form-item>
                            <el-form-item label="语言" prop="languages">
                                <el-select v-model="introForm.languages" multiple placeholder="请选择您会的语言">
                                    <el-option label="中文" value="CHINESE" />
                                    <el-option label="英语" value="ENGLISH" />
                                    <el-option label="日语" value="JAPANESE" />
                                    <el-option label="韩语" value="KOREAN" />
                                    <el-option label="法语" value="FRENCH" />
                                    <el-option label="德语" value="GERMAN" />
                                    <el-option label="西班牙语" value="SPANISH" />
                                    <el-option label="俄语" value="RUSSIAN" />
                                    <el-option label="其他" value="OTHER" />
                                </el-select>
                            </el-form-item>
                            <el-form-item label="自我介绍" prop="introduction">
                                <el-input v-model="introForm.introduction" type="textarea" :rows="6"
                                    placeholder="请输入自我介绍，向客人展示您的特点和为什么适合作为房东" maxlength="1000" show-word-limit />
                            </el-form-item>
                            <el-form-item>
                                <el-button type="primary" @click="saveIntroInfo">保存</el-button>
                            </el-form-item>
                        </el-form>
                    </el-tab-pane>

                    <el-tab-pane label="安全设置">
                        <el-form ref="securityFormRef" :model="securityForm" :rules="securityRules" label-width="120px"
                            class="profile-form">
                            <el-form-item label="旧密码" prop="oldPassword">
                                <el-input v-model="securityForm.oldPassword" type="password" placeholder="请输入旧密码"
                                    show-password />
                            </el-form-item>
                            <el-form-item label="新密码" prop="newPassword">
                                <el-input v-model="securityForm.newPassword" type="password" placeholder="请输入新密码"
                                    show-password />
                            </el-form-item>
                            <el-form-item label="确认新密码" prop="confirmPassword">
                                <el-input v-model="securityForm.confirmPassword" type="password" placeholder="请再次输入新密码"
                                    show-password />
                            </el-form-item>
                            <el-form-item>
                                <el-button type="primary" @click="changePassword">修改密码</el-button>
                            </el-form-item>
                        </el-form>
                    </el-tab-pane>
                </el-tabs>
            </el-col>
        </el-row>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import {
    getUserInfo,
    updateUserInfo,
    updateHostInfo,
    changePassword as updatePassword
} from '@/api/user';
import { useAuthStore } from '@/stores/auth';
import { ElMessage } from 'element-plus';
import type { FormInstance, UploadProps } from 'element-plus';
import { HostInfo } from '@/types/host';

const API_URL = import.meta.env.VITE_API_URL;
const authStore = useAuthStore();

const hostInfo = ref<HostInfo>({
    username: '',
    nickname: '',
    email: '',
    avatar: '',
    phone: '',
    gender: '',
    birthday: '',
    occupation: '',
    languages: [],
    introduction: '',
    homestayCount: 0,
    orderCount: 0,
    reviewCount: 0,
    hostSince: ''
});

const basicForm = ref({
    username: '',
    nickname: '',
    email: '',
    phone: '',
    gender: '',
    birthday: ''
});

const introForm = ref({
    occupation: '',
    languages: [] as string[],
    introduction: ''
});

const securityForm = ref({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
});

const basicRules = {
    nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
    phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
    ],
    email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
    ]
};

const introRules = {
    introduction: [{ max: 1000, message: '自我介绍不能超过1000个字符', trigger: 'blur' }]
};

const securityRules = {
    oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
    newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
    ],
    confirmPassword: [
        { required: true, message: '请再次输入新密码', trigger: 'blur' },
        {
            validator: (rule: any, value: string, callback: any) => {
                if (value !== securityForm.value.newPassword) {
                    callback(new Error('两次输入的密码不一致'));
                } else {
                    callback();
                }
            },
            trigger: 'blur'
        }
    ]
};

const uploadHeaders = {
    Authorization: `Bearer ${authStore.token}`
};

const basicFormRef = ref<FormInstance | null>(null);
const introFormRef = ref<FormInstance | null>(null);
const securityFormRef = ref<FormInstance | null>(null);

// 获取用户信息
const fetchUserInfo = async () => {
    try {
        const response = await getUserInfo();
        hostInfo.value = response.data;

        // 填充表单数据
        basicForm.value = {
            username: response.data.username,
            nickname: response.data.nickname || '',
            email: response.data.email || '',
            phone: response.data.phone || '',
            gender: response.data.gender || '',
            birthday: response.data.birthday || ''
        };

        introForm.value = {
            occupation: response.data.occupation || '',
            languages: response.data.languages || [],
            introduction: response.data.introduction || ''
        };
    } catch (error) {
        console.error('获取用户信息失败', error);
        ElMessage.error('获取用户信息失败');
    }
};

// 上传头像成功
const handleAvatarSuccess: UploadProps['onSuccess'] = (response) => {
    if (response && response.data) {
        hostInfo.value.avatar = response.data.url;
        ElMessage.success('头像上传成功');
    }
};

// 上传头像前检查
const beforeAvatarUpload: UploadProps['beforeUpload'] = (file) => {
    const isJPG = file.type === 'image/jpeg';
    const isPNG = file.type === 'image/png';
    const isLt2M = file.size / 1024 / 1024 < 2;

    if (!isJPG && !isPNG) {
        ElMessage.error('头像只能是JPG或PNG格式!');
        return false;
    }
    if (!isLt2M) {
        ElMessage.error('头像大小不能超过2MB!');
        return false;
    }
    return true;
};

// 保存基本信息
const saveBasicInfo = async () => {
    if (!basicFormRef.value) return;

    try {
        await basicFormRef.value.validate();

        await updateUserInfo(basicForm.value);
        ElMessage.success('基本信息保存成功');
        fetchUserInfo();
    } catch (error) {
        console.error('保存基本信息失败', error);
        ElMessage.error('保存基本信息失败');
    }
};

// 保存房东介绍
const saveIntroInfo = async () => {
    if (!introFormRef.value) return;

    try {
        await introFormRef.value.validate();

        await updateHostInfo(introForm.value);
        ElMessage.success('房东介绍保存成功');
        fetchUserInfo();
    } catch (error) {
        console.error('保存房东介绍失败', error);
        ElMessage.error('保存房东介绍失败');
    }
};

// 修改密码
const changePassword = async () => {
    if (!securityFormRef.value) return;

    try {
        await securityFormRef.value.validate();

        await updatePassword({
            oldPassword: securityForm.value.oldPassword,
            newPassword: securityForm.value.newPassword
        });

        ElMessage.success('密码修改成功');
        securityForm.value = {
            oldPassword: '',
            newPassword: '',
            confirmPassword: ''
        };
    } catch (error) {
        console.error('修改密码失败', error);
        ElMessage.error('修改密码失败');
    }
};

onMounted(() => {
    fetchUserInfo();
});
</script>

<style scoped>
.host-profile {
    padding: 20px;
}

.header {
    margin-bottom: 20px;
}

.profile-card {
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
}

.profile-avatar {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-bottom: 20px;
}

.upload-avatar {
    margin-top: 10px;
}

.profile-info {
    width: 100%;
    text-align: center;
}

.profile-info h3 {
    margin-bottom: 5px;
    font-size: 20px;
}

.profile-info p {
    color: #606266;
    margin-bottom: 20px;
}

.profile-stats {
    display: flex;
    justify-content: space-around;
    margin: 20px 0;
}

.stat-item {
    text-align: center;
}

.stat-number {
    font-size: 24px;
    font-weight: bold;
    color: #409EFF;
}

.stat-label {
    font-size: 14px;
    color: #606266;
}

.host-since {
    margin-top: 20px;
    color: #606266;
}

.label {
    color: #909399;
}

.profile-form {
    max-width: 500px;
    margin: 0 auto;
    padding: 20px 0;
}
</style>
