<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-left">
        <div class="login-overlay"></div>
        <div class="login-content">
          <h1>探索世界各地的精品民宿</h1>
          <p>开启您的旅行，体验如家般的温馨</p>
        </div>
      </div>
      <div class="login-right">
        <div class="login-form-container">
          <div class="login-logo">
            <Logo class="logo-icon" />
            <h2>民宿预订平台</h2>
          </div>
          <h3>欢迎回来</h3>
          <p class="login-subtitle">请登录您的账号</p>

          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名/邮箱" prefix-icon="User" />
            </el-form-item>

            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock"
                show-password />
            </el-form-item>

            <div class="login-options">
              <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
              <el-button type="text" @click="goToForgotPassword">忘记密码？</el-button>
            </div>

            <el-button type="primary" class="login-button" :loading="loading" @click="handleLogin">
              登录
            </el-button>

            <div class="register-link">
              还没有账号？ <el-button type="text" @click="goToRegister">立即注册</el-button>
            </div>

            <div class="social-login">
              <p>其他登录方式</p>
              <div class="social-icons">
                <el-button circle><el-icon>
                    <ChatRound />
                  </el-icon></el-button>
                <el-button circle><el-icon>
                    <Apple />
                  </el-icon></el-button>
                <el-button circle><el-icon>
                    <Iphone />
                  </el-icon></el-button>
              </div>
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
import {
  User,
  Lock,
  ChatRound,
  Apple,
  Iphone,
} from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";
import Logo from "@/components/Logo.vue";

const router = useRouter();
const userStore = useUserStore();
const loginFormRef = ref();
const loading = ref(false);

const loginForm = reactive({
  username: "",
  password: "",
  remember: false,
});

const loginRules = {
  username: [
    { required: true, message: "请输入用户名或邮箱", trigger: "blur" },
    { min: 3, message: "用户名长度不能小于3个字符", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, message: "密码长度不能小于6个字符", trigger: "blur" },
  ],
};

const handleLogin = async () => {
  if (!loginFormRef.value) return;

  await loginFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        loading.value = true;
        await userStore.login(loginForm);
        ElMessage.success("登录成功");
        await router.push({ name: "Home" });
      } catch (error: any) {
        console.error("登录失败:", error);
        ElMessage.error(error.message || "登录失败，请检查您的用户名和密码");
      } finally {
        loading.value = false;
      }
    }
  });
};

const goToRegister = () => {
  router.push("/register");
};

const goToForgotPassword = () => {
  router.push("/forgot-password");
};
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f7fa;
  padding: 20px;
}

.login-box {
  display: flex;
  width: 900px;
  height: 600px;
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

.login-right {
  flex: 1;
  background-color: white;
  padding: 40px;
  display: flex;
  align-items: center;
}

.login-form-container {
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

.login-form-container h3 {
  font-size: 1.8rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.login-subtitle {
  color: #666;
  margin-bottom: 30px;
}

.login-form {
  margin-top: 20px;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.login-button {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.register-link {
  text-align: center;
  margin-bottom: 24px;
  color: #666;
}

.social-login {
  margin-top: 30px;
  text-align: center;
}

.social-login p {
  color: #999;
  margin-bottom: 16px;
  position: relative;
}

.social-login p::before,
.social-login p::after {
  content: "";
  position: absolute;
  top: 50%;
  width: 25%;
  height: 1px;
  background-color: #eee;
}

.social-login p::before {
  left: 0;
}

.social-login p::after {
  right: 0;
}

.social-icons {
  display: flex;
  justify-content: center;
  gap: 16px;
}

@media (max-width: 768px) {
  .login-box {
    flex-direction: column;
    width: 100%;
    height: auto;
  }

  .login-left {
    display: none;
  }
}
</style>