<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-left">
        <div class="register-overlay"></div>
        <div class="register-content">
          <h1>加入我们的民宿平台</h1>
          <p>开启您的旅程，发现更多精彩</p>
        </div>
      </div>
      <div class="register-right">
        <div class="register-form-container">
          <div class="register-logo">
            <Logo class="logo-icon" />
            <h2>民宿预订平台</h2>
          </div>
          <h3>创建账号</h3>
          <p class="register-subtitle">请填写以下信息完成注册</p>

          <el-card class="register-card">
            <el-form ref="registerFormRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
              <el-form-item label="用户名" prop="username">
                <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" :disabled="loading"
                  :suffix-icon="usernameValidating ? 'Loading' : ''" />
                <div class="form-item-tip" v-if="usernameValidating">
                  <span class="checking-text">正在检查用户名是否可用...</span>
                </div>
              </el-form-item>

              <el-form-item label="邮箱" prop="email">
                <el-input v-model="form.email" placeholder="请输入邮箱" prefix-icon="Message" :disabled="loading"
                  :suffix-icon="emailValidating ? 'Loading' : ''" />
                <div class="form-item-tip" v-if="emailValidating">
                  <span class="checking-text">正在检查邮箱是否可用...</span>
                </div>
              </el-form-item>

              <el-form-item label="手机号" prop="phone">
                <el-input v-model="form.phone" placeholder="请输入手机号" prefix-icon="Phone" :disabled="loading" />
              </el-form-item>

              <el-form-item label="密码" prop="password">
                <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password
                  :disabled="loading" />
              </el-form-item>

              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" prefix-icon="Lock"
                  show-password :disabled="loading" />
              </el-form-item>

              <el-form-item label="注册身份" prop="role">
                <el-radio-group v-model="form.role">
                  <el-radio label="ROLE_USER">普通用户</el-radio>
                  <el-radio label="ROLE_LANDLORD">房东</el-radio>
                </el-radio-group>
                <div class="role-tip" v-if="form.role === 'ROLE_LANDLORD'">
                  <el-alert title="提示：注册为房东后，您将可以发布和管理房源，接收订单并获得收益。" type="info" :closable="false" show-icon />
                </div>
                <div class="role-debug">
                  <p class="role-debug-text">当前选择角色: {{ form.role }}</p>
                </div>
              </el-form-item>

              <el-button type="primary" class="submit-btn" :loading="loading"
                :disabled="loading || usernameValidating || emailValidating" @click="handleRegister">
                {{ loading ? '注册中...' : '注册' }}
              </el-button>

              <div class="login-link">
                已有账号？
                <el-button type="text" @click="goToLogin">立即登录</el-button>
              </div>
            </el-form>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import type { FormInstance } from 'element-plus';
import { ElMessage } from 'element-plus';
import Logo from '@/components/Logo.vue';
import api from '@/api';
import { debounce } from 'lodash-es';

const router = useRouter();
const userStore = useUserStore();
const registerFormRef = ref<FormInstance>();
const loading = ref(false);

const form = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
  role: 'ROLE_USER',
});

// 添加验证状态跟踪
const usernameValidating = ref(false);
const emailValidating = ref(false);

// 检查用户名是否存在的函数
const checkUsernameExists = debounce(async (username: string, callback: Function) => {
  if (!username || username.length < 3) {
    callback();
    return;
  }

  try {
    usernameValidating.value = true;
    const response = await api.get(`/api/auth/check-username?username=${encodeURIComponent(username)}`);
    const exists = response.data?.exists;

    if (exists) {
      callback(new Error('该用户名已被使用'));
    } else {
      callback();
    }
  } catch (error) {
    console.error('检查用户名失败:', error);
    // 如果API请求失败，我们不阻止表单提交
    callback();
  } finally {
    usernameValidating.value = false;
  }
}, 500);

// 检查邮箱是否存在的函数
const checkEmailExists = debounce(async (email: string, callback: Function) => {
  if (!email || !/^[\w-]+(\.[\w-]+)*@([\w-]+\.)+[a-zA-Z]{2,7}$/.test(email)) {
    callback();
    return;
  }

  try {
    emailValidating.value = true;
    const response = await api.get(`/api/auth/check-email?email=${encodeURIComponent(email)}`);
    const exists = response.data?.exists;

    if (exists) {
      callback(new Error('该邮箱已被注册'));
    } else {
      callback();
    }
  } catch (error) {
    console.error('检查邮箱失败:', error);
    // 如果API请求失败，我们不阻止表单提交
    callback();
  } finally {
    emailValidating.value = false;
  }
}, 500);

// 自定义用户名验证
const validateUsername = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请输入用户名'));
    return;
  }

  if (value.length < 3 || value.length > 20) {
    callback(new Error('长度在 3 到 20 个字符'));
    return;
  }

  // 仅在生产环境或有后端API时检查用户名
  if (import.meta.env.PROD || import.meta.env.VITE_API_BASE_URL) {
    checkUsernameExists(value, callback);
  } else {
    callback();
  }
};

// 自定义邮箱验证
const validateEmail = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请输入邮箱'));
    return;
  }

  if (!/^[\w-]+(\.[\w-]+)*@([\w-]+\.)+[a-zA-Z]{2,7}$/.test(value)) {
    callback(new Error('请输入正确的邮箱地址'));
    return;
  }

  // 仅在生产环境或有后端API时检查邮箱
  if (import.meta.env.PROD || import.meta.env.VITE_API_BASE_URL) {
    checkEmailExists(value, callback);
  } else {
    callback();
  }
};

const validatePass2 = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'));
  } else if (value !== form.password) {
    callback(new Error('两次输入密码不一致!'));
  } else {
    callback();
  }
};

const validatePhone = (rule: any, value: string, callback: any) => {
  if (!value) {
    callback();
    return;
  }
  const phoneRegex = /^1[3-9]\d{9}$/;
  if (!phoneRegex.test(value)) {
    callback(new Error('请输入正确的手机号'));
  } else {
    callback();
  }
};

// 更新验证规则
const rules = {
  username: [
    { validator: validateUsername, trigger: 'blur' }
  ],
  email: [
    { validator: validateEmail, trigger: 'blur' }
  ],
  phone: [
    { validator: validatePhone, trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, validator: validatePass2, trigger: 'blur' },
  ],
  role: [
    { required: true, message: '请选择注册身份', trigger: 'change' },
  ],
};

const handleRegister = async () => {
  if (!registerFormRef.value) return;

  try {
    await registerFormRef.value.validate();
    loading.value = true;

    const { confirmPassword, ...registerData } = form;
    console.log('提交注册信息:', registerData);

    try {
      // 注册
      const registerSuccess = await userStore.register(registerData);

      if (registerSuccess) {
        // 注册成功，根据角色提供不同的成功提示
        if (form.role === 'ROLE_LANDLORD') {
          ElMessage.success('恭喜您！已成功注册为房东。正在为您跳转...');
          // 直接跳转到房东中心
          router.push('/host');
        } else {
          ElMessage.success('注册成功！正在为您跳转到首页...');
          // 普通用户跳转到首页
          router.push('/');
        }
      } else {
        ElMessage.error('注册处理失败，请稍后重试');
      }
    } catch (error: any) {
      console.error('注册失败详情:', error);
      if (error.response) {
        console.error('错误状态码:', error.response.status);
        console.error('错误响应数据:', error.response.data);

        // 使用API拦截器中提取的错误消息
        let errorMessage = error.displayMessage || '注册失败，请重试';

        // 针对特定错误进行友好提示
        if (errorMessage.includes('邮箱已存在')) {
          errorMessage = '该邮箱已被注册，请使用其他邮箱或直接登录';
        } else if (errorMessage.includes('用户名已存在')) {
          errorMessage = '该用户名已被使用，请选择其他用户名';
        } else if (error.response.status === 500 && !errorMessage.includes('已存在')) {
          errorMessage = '服务器内部错误，请联系管理员或稍后重试';
        }

        ElMessage.error(errorMessage);
      } else {
        ElMessage.error(error.displayMessage || error.message || '网络错误，请稍后重试');
      }
    }
  } catch (formError: any) {
    console.error('表单验证失败:', formError);
    ElMessage.error('请正确填写所有必填信息');
  } finally {
    loading.value = false;
  }
};

const goToLogin = () => {
  router.push('/login');
};
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f7fa;
  padding: 20px;
}

.register-box {
  display: flex;
  width: 1000px;
  min-height: 700px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.register-left {
  position: relative;
  flex: 1;
  background: linear-gradient(135deg, #1E88E5 0%, #64B5F6 100%);
  color: white;
}

.register-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(32, 72, 142, 0.7) 0%, rgba(62, 136, 180, 0.6) 100%);
}

.register-content {
  position: relative;
  z-index: 1;
  padding: 40px;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.register-content h1 {
  font-size: 2.5rem;
  margin-bottom: 16px;
  font-weight: 700;
  line-height: 1.2;
}

.register-content p {
  font-size: 1.1rem;
  opacity: 0.9;
}

.register-right {
  flex: 1;
  background-color: white;
  padding: 40px;
  display: flex;
  align-items: center;
}

.register-form-container {
  width: 100%;
}

.register-logo {
  display: flex;
  align-items: center;
  margin-bottom: 30px;
}

.logo-icon {
  color: var(--el-color-primary);
  margin-right: 12px;
}

.register-logo h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.register-form-container h3 {
  font-size: 1.8rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.register-subtitle {
  color: #666;
  margin-bottom: 30px;
}

.register-card {
  border: none;
  box-shadow: none;
}

.submit-btn {
  width: 100%;
  margin: 20px 0;
}

.login-link {
  text-align: center;
  color: #606266;
}

:deep(.el-form-item__label) {
  padding-bottom: 8px;
}

:deep(.el-input__wrapper) {
  padding: 1px 15px;
}

:deep(.el-input__prefix) {
  margin-right: 8px;
}

.role-tip {
  margin-top: 8px;
}

.role-debug {
  margin-top: 8px;
}

.role-debug-text {
  font-size: 0.8rem;
  color: #909399;
}

.form-item-tip {
  margin-top: 8px;
  text-align: right;
}

.checking-text {
  font-size: 0.8rem;
  color: #909399;
}

@media (max-width: 768px) {
  .register-box {
    flex-direction: column;
    width: 100%;
    min-height: auto;
  }

  .register-left {
    display: none;
  }

  .register-right {
    padding: 20px;
  }
}
</style>