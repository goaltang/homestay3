<template>
    <div class="profile-container">
        <div class="profile-header">
            <h1>个人资料</h1>
            <el-button type="primary" @click="handleSubmit" :loading="loading">保存修改</el-button>
        </div>

        <div class="profile-layout">
            <!-- 左侧列 - 头像和统计信息 -->
            <div class="profile-left-column">
                <!-- 头像卡片 -->
                <el-card class="avatar-card">
                    <div class="avatar-wrapper">
                        <div class="avatar-container">
                            <img :src="formData.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                                class="avatar-image" alt="用户头像" />
                            <div class="avatar-overlay">
                                <el-upload class="avatar-uploader" action="/api/host/avatar" :show-file-list="false"
                                    :on-success="handleAvatarSuccess" :before-upload="beforeAvatarUpload">
                                    <el-button class="upload-btn" type="primary" circle>
                                        <el-icon>
                                            <Upload />
                                        </el-icon>
                                    </el-button>
                                </el-upload>
                            </div>
                        </div>
                    </div>

                    <div class="host-info">
                        <h2>{{ formData.nickname || formData.username }}</h2>
                        <div class="host-since">
                            <el-icon>
                                <Calendar />
                            </el-icon>
                            <span>加入时间: {{ hostSince }}</span>
                        </div>
                    </div>
                </el-card>

                <!-- 统计卡片 -->
                <el-card class="statistics-card">
                    <template #header>
                        <div class="card-header">
                            <h2>房东数据统计</h2>
                        </div>
                    </template>
                    <div class="stats-grid">
                        <div class="stat-item">
                            <div class="stat-icon homestay-icon">
                                <el-icon>
                                    <House />
                                </el-icon>
                            </div>
                            <div class="stat-content">
                                <div class="stat-value">{{ statistics.homestayCount || 0 }}</div>
                                <div class="stat-label">房源数量</div>
                            </div>
                        </div>
                        <div class="stat-item">
                            <div class="stat-icon order-icon">
                                <el-icon>
                                    <Document />
                                </el-icon>
                            </div>
                            <div class="stat-content">
                                <div class="stat-value">{{ statistics.orderCount || 0 }}</div>
                                <div class="stat-label">订单数量</div>
                            </div>
                        </div>
                        <div class="stat-item">
                            <div class="stat-icon review-icon">
                                <el-icon>
                                    <ChatDotRound />
                                </el-icon>
                            </div>
                            <div class="stat-content">
                                <div class="stat-value">{{ statistics.reviewCount || 0 }}</div>
                                <div class="stat-label">评价数量</div>
                            </div>
                        </div>
                        <div class="stat-item">
                            <div class="stat-icon rating-icon">
                                <el-icon>
                                    <Star />
                                </el-icon>
                            </div>
                            <div class="stat-content">
                                <div class="stat-value">{{ statistics.rating ? statistics.rating.toFixed(1) : '0.0' }}
                                </div>
                                <div class="stat-label">平均评分</div>
                            </div>
                        </div>
                    </div>
                </el-card>
            </div>

            <!-- 右侧列 - 表单和密码 -->
            <div class="profile-right-column">
                <el-tabs type="border-card" class="profile-tabs">
                    <el-tab-pane label="基本信息">
                        <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px"
                            class="profile-form">
                            <el-row :gutter="20">
                                <el-col :span="12">
                                    <el-form-item label="用户名" prop="username">
                                        <el-input v-model="formData.username" disabled />
                                    </el-form-item>
                                </el-col>
                                <el-col :span="12">
                                    <el-form-item label="昵称" prop="nickname">
                                        <el-input v-model="formData.nickname" />
                                    </el-form-item>
                                </el-col>
                            </el-row>

                            <el-row :gutter="20">
                                <el-col :span="12">
                                    <el-form-item label="电子邮箱" prop="email">
                                        <el-input v-model="formData.email" />
                                    </el-form-item>
                                </el-col>
                                <el-col :span="12">
                                    <el-form-item label="手机号码" prop="phone">
                                        <el-input v-model="formData.phone" />
                                    </el-form-item>
                                </el-col>
                            </el-row>

                            <el-row :gutter="20">
                                <el-col :span="12">
                                    <el-form-item label="真实姓名" prop="realName">
                                        <el-input v-model="formData.realName" />
                                    </el-form-item>
                                </el-col>
                                <el-col :span="12">
                                    <el-form-item label="身份证号" prop="idCard">
                                        <el-input v-model="formData.idCard" />
                                    </el-form-item>
                                </el-col>
                            </el-row>
                        </el-form>
                    </el-tab-pane>

                    <el-tab-pane label="房东信息">
                        <el-form ref="hostFormRef" :model="formData" label-width="100px" class="profile-form">
                            <el-form-item label="个人介绍" prop="introduction">
                                <el-input v-model="formData.introduction" type="textarea" :rows="4"
                                    placeholder="介绍一下自己，让房客更了解您..." />
                            </el-form-item>

                            <el-row :gutter="20">
                                <el-col :span="12">
                                    <el-form-item label="职业" prop="occupation">
                                        <el-input v-model="formData.occupation" placeholder="您的职业" />
                                    </el-form-item>
                                </el-col>
                                <el-col :span="12">
                                    <el-form-item label="语言能力" prop="languages">
                                        <el-select v-model="formData.languages" multiple placeholder="您会说的语言"
                                            style="width: 100%">
                                            <el-option label="中文" value="中文" />
                                            <el-option label="英语" value="英语" />
                                            <el-option label="日语" value="日语" />
                                            <el-option label="韩语" value="韩语" />
                                            <el-option label="法语" value="法语" />
                                            <el-option label="德语" value="德语" />
                                            <el-option label="西班牙语" value="西班牙语" />
                                            <el-option label="俄语" value="俄语" />
                                            <el-option label="阿拉伯语" value="阿拉伯语" />
                                        </el-select>
                                    </el-form-item>
                                </el-col>
                            </el-row>

                            <el-form-item label="接待伙伴" prop="companions">
                                <div class="companions-container">
                                    <div class="companions-list">
                                        <div v-for="(companion, index) in formData.companions" :key="index"
                                            class="companion-card">
                                            <div class="companion-header">
                                                <span class="companion-title">伙伴 {{ index + 1 }}</span>
                                                <el-button type="danger" circle size="small"
                                                    @click="removeCompanion(index)">
                                                    <el-icon>
                                                        <Delete />
                                                    </el-icon>
                                                </el-button>
                                            </div>
                                            <div class="companion-content">
                                                <el-input v-model="companion.name" placeholder="伙伴姓名" />
                                            </div>
                                        </div>
                                    </div>
                                    <el-button type="primary" plain @click="addCompanion" class="add-companion-btn">
                                        <el-icon>
                                            <Plus />
                                        </el-icon> 添加接待伙伴
                                    </el-button>
                                </div>
                            </el-form-item>
                        </el-form>
                    </el-tab-pane>

                    <el-tab-pane label="密码设置">
                        <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px">
                            <el-form-item label="当前密码" prop="oldPassword">
                                <el-input v-model="passwordForm.oldPassword" type="password" show-password />
                            </el-form-item>

                            <el-row :gutter="20">
                                <el-col :span="12">
                                    <el-form-item label="新密码" prop="newPassword">
                                        <el-input v-model="passwordForm.newPassword" type="password" show-password />
                                    </el-form-item>
                                </el-col>
                                <el-col :span="12">
                                    <el-form-item label="确认新密码" prop="confirmPassword">
                                        <el-input v-model="passwordForm.confirmPassword" type="password"
                                            show-password />
                                    </el-form-item>
                                </el-col>
                            </el-row>

                            <el-form-item>
                                <el-button type="primary" @click="handleChangePassword" :loading="passwordLoading">
                                    修改密码
                                </el-button>
                            </el-form-item>
                        </el-form>
                    </el-tab-pane>
                </el-tabs>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Delete, Upload, Calendar, House, Document, ChatDotRound, Star, Plus } from '@element-plus/icons-vue'
import { getHostInfo, updateHostInfo } from '@/api/host'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const loading = ref(false)
const passwordLoading = ref(false)
const hostSince = ref('')

// 统计数据
const statistics = reactive({
    homestayCount: 0,
    orderCount: 0,
    reviewCount: 0,
    rating: 0
})

interface Companion {
    name: string;
    avatar?: string;
}

// 表单数据
const formData = reactive({
    username: '',
    nickname: '',
    email: '',
    phone: '',
    realName: '',
    idCard: '',
    avatar: '',
    introduction: '',
    occupation: '',
    languages: [] as string[],
    companions: [] as Companion[]
})

// 表单验证规则
const rules = reactive<FormRules>({
    email: [
        { required: true, message: '请输入电子邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入有效的电子邮箱地址', trigger: 'blur' }
    ],
    phone: [
        { required: true, message: '请输入手机号码', trigger: 'blur' },
        { pattern: /^1[3456789]\d{9}$/, message: '请输入有效的手机号码', trigger: 'blur' }
    ],
    realName: [
        { required: true, message: '请输入真实姓名', trigger: 'blur' }
    ],
    idCard: [
        { required: true, message: '请输入身份证号', trigger: 'blur' },
        { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '请输入有效的身份证号', trigger: 'blur' }
    ],
    nickname: [
        { required: true, message: '请输入昵称', trigger: 'blur' }
    ]
})

// 密码表单数据
const passwordForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
})

// 密码表单验证规则
const passwordRules = reactive<FormRules>({
    oldPassword: [
        { required: true, message: '请输入当前密码', trigger: 'blur' }
    ],
    newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
    ],
    confirmPassword: [
        { required: true, message: '请确认新密码', trigger: 'blur' },
        {
            validator: (rule, value, callback) => {
                if (value !== passwordForm.newPassword) {
                    callback(new Error('两次输入的密码不一致'))
                } else {
                    callback()
                }
            },
            trigger: 'blur'
        }
    ]
})

// 获取房东信息
const fetchHostInfo = async () => {
    try {
        const response = await getHostInfo()
        if (response.data) {
            const hostData = response.data
            formData.username = hostData.username || ''
            formData.nickname = hostData.nickname || ''
            formData.email = hostData.email || ''
            formData.phone = hostData.phone || ''
            formData.realName = hostData.realName || ''
            formData.idCard = hostData.idCard || ''
            formData.avatar = hostData.avatar || ''
            formData.introduction = hostData.introduction || ''
            formData.occupation = hostData.occupation || ''
            formData.languages = hostData.languages || []
            formData.companions = hostData.companions || []

            hostSince.value = hostData.hostSince || '未知'

            // 统计数据
            statistics.homestayCount = hostData.homestayCount || 0
            statistics.orderCount = hostData.orderCount || 0
            statistics.reviewCount = hostData.reviewCount || 0
            statistics.rating = hostData.rating || 0
        }
    } catch (error) {
        console.error('获取房东信息失败:', error)
        ElMessage.error('获取房东信息失败')
    }
}

// 提交表单
const handleSubmit = async () => {
    if (!formRef.value) return

    await formRef.value.validate(async (valid) => {
        if (valid) {
            loading.value = true
            try {
                await updateHostInfo(formData)
                ElMessage.success('资料更新成功')
                // 更新用户存储中的用户信息
                if (userStore.userInfo) {
                    userStore.userInfo.nickname = formData.nickname
                    userStore.userInfo.avatar = formData.avatar
                }
            } catch (error) {
                console.error('更新资料失败:', error)
                ElMessage.error('更新资料失败')
            } finally {
                loading.value = false
            }
        } else {
            ElMessage.warning('请正确填写表单')
            return false
        }
    })
}

// 修改密码
const handleChangePassword = async () => {
    if (!passwordFormRef.value) return

    await passwordFormRef.value.validate(async (valid) => {
        if (valid) {
            passwordLoading.value = true
            try {
                await userStore.changePassword({
                    oldPassword: passwordForm.oldPassword,
                    newPassword: passwordForm.newPassword
                })
                ElMessage.success('密码修改成功')
                // 清空密码表单
                passwordForm.oldPassword = ''
                passwordForm.newPassword = ''
                passwordForm.confirmPassword = ''
            } catch (error) {
                console.error('修改密码失败:', error)
                ElMessage.error('修改密码失败')
            } finally {
                passwordLoading.value = false
            }
        } else {
            ElMessage.warning('请正确填写表单')
            return false
        }
    })
}

// 添加接待伙伴
const addCompanion = () => {
    formData.companions.push({ name: '' })
}

// 移除接待伙伴
const removeCompanion = (index: number) => {
    formData.companions.splice(index, 1)
}

// 头像上传相关
const handleAvatarSuccess = (response: any) => {
    formData.avatar = response.data.url
    ElMessage.success('头像上传成功')
}

const beforeAvatarUpload = (file: File) => {
    const isImage = file.type.startsWith('image/')
    const isLt2M = file.size / 1024 / 1024 < 2

    if (!isImage) {
        ElMessage.error('上传头像图片只能是图片格式!')
    }
    if (!isLt2M) {
        ElMessage.error('上传头像图片大小不能超过 2MB!')
    }
    return isImage && isLt2M
}

// 组件挂载时获取房东信息
onMounted(() => {
    fetchHostInfo()
})
</script>

<style scoped>
.profile-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 24px;
}

.profile-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
}

.profile-header h1 {
    margin: 0;
    font-size: 24px;
    font-weight: 600;
    color: #303133;
}

.profile-layout {
    display: grid;
    grid-template-columns: 300px 1fr;
    gap: 24px;
}

/* 左侧列样式 */
.profile-left-column {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.avatar-card {
    text-align: center;
    padding: 16px;
}

.avatar-wrapper {
    position: relative;
    width: 120px;
    height: 120px;
    margin: 0 auto 16px;
}

.avatar-container {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    overflow: hidden;
    border: 4px solid #f2f6fc;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    position: relative;
}

.avatar-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.avatar-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0);
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: all 0.3s;
    border-radius: 50%;
}

.avatar-container:hover .avatar-overlay {
    background: rgba(0, 0, 0, 0.3);
    opacity: 1;
}

.upload-btn {
    font-size: 18px;
}

.host-info {
    margin-top: 8px;
}

.host-info h2 {
    margin: 0 0 8px;
    font-size: 18px;
    font-weight: 600;
    color: #303133;
}

.host-since {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    font-size: 13px;
    color: #606266;
}

/* 统计卡片样式 */
.statistics-card {
    background-color: #fff;
}

.card-header h2 {
    font-size: 16px;
    font-weight: 600;
    margin: 0;
    color: #303133;
}

.stats-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
}

.stat-item {
    display: flex;
    align-items: center;
    padding: 12px;
    border-radius: 8px;
    background-color: #f8f9fa;
    transition: all 0.3s;
}

.stat-item:hover {
    transform: translateY(-3px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.stat-icon {
    width: 40px;
    height: 40px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 12px;
    color: white;
    font-size: 18px;
}

.homestay-icon {
    background-color: #67c23a;
}

.order-icon {
    background-color: #409eff;
}

.review-icon {
    background-color: #e6a23c;
}

.rating-icon {
    background-color: #f56c6c;
}

.stat-content {
    flex: 1;
}

.stat-value {
    font-size: 20px;
    font-weight: 600;
    line-height: 1.2;
    color: #303133;
}

.stat-label {
    font-size: 12px;
    color: #909399;
}

/* 右侧列样式 */
.profile-tabs {
    height: 100%;
}

.profile-form {
    padding: 16px 8px;
}

/* 伙伴列表样式 */
.companions-container {
    width: 100%;
}

.companions-list {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
    margin-bottom: 16px;
}

.companion-card {
    border: 1px solid #ebeef5;
    border-radius: 4px;
    padding: 12px;
    background-color: #f8f9fa;
}

.companion-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
}

.companion-title {
    font-weight: 600;
    font-size: 14px;
    color: #606266;
}

.add-companion-btn {
    width: 100%;
}

@media (max-width: 992px) {
    .profile-layout {
        grid-template-columns: 1fr;
    }

    .companions-list {
        grid-template-columns: 1fr;
    }
}
</style>