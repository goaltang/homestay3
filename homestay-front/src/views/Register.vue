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
                <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" :disabled="loading" />
              </el-form-item>

              <el-form-item label="邮箱" prop="email">
                <el-input v-model="form.email" placeholder="请输入邮箱" prefix-icon="Message" :disabled="loading" />
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

              <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegister">
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
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import type { FormInstance } from 'element-plus';
import { ElMessage } from 'element-plus';
import Logo from '@/components/Logo.vue';

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
});

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

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
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
};

const handleRegister = async () => {
  if (!registerFormRef.value) return;

  try {
    await registerFormRef.value.validate();
    loading.value = true;

    const { confirmPassword, ...registerData } = form;
    await userStore.register(registerData);

    ElMessage.success('注册成功');
    // 注册成功后延迟跳转，让用户看到成功提示
    setTimeout(() => {
      router.push('/login');
    }, 1500);
  } catch (error: any) {
    console.error('注册失败:', error);
    ElMessage.error(error.response?.data?.message || '注册失败，请重试');
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