<template>
  <div class="register-container">
    <div class="register-box">
      <div class="login-left">
        <div class="login-overlay"></div>
        <div class="login-content">
          <h1>加入我们的民宿平台</h1>
          <p>开启您的住宿之旅</p>
        </div>
      </div>
      <div class="register-right">
        <div class="register-form-container">
          <div class="login-logo">
            <Logo class="logo-icon" />
            <h2>民宿预订平台</h2>
          </div>
          <h3>创建账号</h3>
          <p class="register-subtitle">请填写以下信息完成注册</p>

          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" class="register-form">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="用户名" prefix-icon="User" />
            </el-form-item>

            <el-form-item prop="email">
              <el-input v-model="registerForm.email" placeholder="电子邮箱" prefix-icon="Message" />
            </el-form-item>

            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码" prefix-icon="Lock"
                show-password />
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock"
                show-password />
            </el-form-item>

            <el-form-item prop="phone">
              <el-input v-model="registerForm.phone" placeholder="手机号码（选填）" prefix-icon="Phone" />
            </el-form-item>

            <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegister">
              注册
            </el-button>

            <div class="login-link">
              已有账号？ <el-button type="text" @click="goToLogin">立即登录</el-button>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { User, Lock, Message, Phone } from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";
import Logo from "@/components/Logo.vue";
import type { RegisterRequest } from "@/types/auth";

const router = useRouter();
const userStore = useUserStore();
const registerFormRef = ref();
const loading = ref(false);

const registerForm = reactive({
  username: "",
  email: "",
  password: "",
  confirmPassword: "",
  phone: "",
});

const registerRules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 3, message: "用户名长度不能小于3个字符", trigger: "blur" },
  ],
  email: [
    { required: true, message: "请输入邮箱地址", trigger: "blur" },
    { type: "email", message: "请输入正确的邮箱地址", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, message: "密码长度不能小于6个字符", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请确认密码", trigger: "blur" },
    {
      validator: (rule: any, value: string, callback: Function) => {
        if (value !== registerForm.password) {
          callback(new Error("两次输入的密码不一致"));
        } else {
          callback();
        }
      },
      trigger: "blur",
    },
  ],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: "请输入正确的手机号码",
      trigger: "blur",
    },
  ],
};

const handleRegister = async () => {
  if (!registerFormRef.value) return;

  await registerFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        loading.value = true;
        const registerData: RegisterRequest = {
          username: registerForm.username,
          email: registerForm.email,
          password: registerForm.password,
          phone: registerForm.phone || undefined,
        };
        await userStore.register(registerData);
        ElMessage.success("注册成功");
        await router.push({ name: "Home" });
      } catch (error: any) {
        ElMessage.error(error.message || "注册失败");
      } finally {
        loading.value = false;
      }
    }
  });
};

const goToLogin = () => {
  router.push("/login");
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
  width: 900px;
  height: 700px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.login-left {
  position: relative;
  flex: 1;
  background: linear-gradient(135deg, #1E88E5 0%, #64B5F6 100%);
  color: white;
}

.login-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(32, 72, 142, 0.7) 0%, rgba(62, 136, 180, 0.6) 100%);
}

.login-content {
  position: relative;
  z-index: 1;
  padding: 40px;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.login-content h1 {
  font-size: 2.5rem;
  margin-bottom: 16px;
  font-weight: 700;
  line-height: 1.2;
}

.login-content p {
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

.login-logo {
  display: flex;
  align-items: center;
  margin-bottom: 30px;
}

.logo-icon {
  color: var(--el-color-primary);
  margin-right: 12px;
}

.login-logo h2 {
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

.register-form {
  margin-top: 20px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
  margin: 20px 0;
}

.login-link {
  text-align: center;
  color: #666;
}

@media (max-width: 768px) {
  .register-box {
    flex-direction: column;
    width: 100%;
    height: auto;
  }

  .login-left {
    display: none;
  }
}
</style>