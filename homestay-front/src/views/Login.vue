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

          <el-card class="login-card">
            <el-form ref="loginFormRef" :model="loginForm" :rules="rules" label-position="top"
              @submit.prevent="handleLogin">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" :disabled="loading"
                  @keyup.enter="handleLogin" />
              </el-form-item>

              <el-form-item label="密码" prop="password">
                <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock"
                  show-password :disabled="loading" @keyup.enter="handleLogin" />
              </el-form-item>

              <div class="remember-forgot">
                <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
                <el-button type="text" @click="goToForgotPassword">忘记密码？</el-button>
              </div>

              <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">
                {{ loading ? '登录中...' : '登录' }}
              </el-button>

              <div class="register-link">
                还没有账号？
                <el-button type="text" @click="goToRegister">立即注册</el-button>
              </div>
            </el-form>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import type { FormInstance } from "element-plus";
import { ElMessage, ElLoading } from "element-plus";
import Logo from "@/components/Logo.vue";

const router = useRouter();
const userStore = useUserStore();
const loginFormRef = ref<FormInstance>();
const loading = ref(false);

const loginForm = reactive({
  username: "",
  password: "",
  remember: false
});

const rules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 3, max: 20, message: "长度在 3 到 20 个字符", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 20, message: "长度在 6 到 20 个字符", trigger: "blur" },
  ],
};

const handleLogin = async () => {
  if (!loginFormRef.value) return;

  let loadingInstance: any = null;

  try {
    await loginFormRef.value.validate();
    loadingInstance = ElLoading.service({
        lock: true,
        text: '正在登录...',
        background: 'rgba(0, 0, 0, 0.7)',
    });

    console.log("表单验证通过，正在尝试登录:", loginForm);

    const success = await userStore.login(loginForm.username, loginForm.password);

    if (success) {
      console.log("登录成功，准备获取用户信息:", userStore.userInfo);

      try {
        // 登录成功后立即获取完整的用户信息
        await userStore.fetchUserInfo();
        console.log("已成功获取完整用户信息:", userStore.userInfo);
        loadingInstance?.close();
        ElMessage.success("登录成功");

        // 获取最新的用户角色信息 (从 store 获取更可靠)
        const role = userStore.userInfo?.role || ''; // 使用 userStore 的最新数据
        const isLandlord = ['ROLE_LANDLORD', 'ROLE_HOST', 'LANDLORD', 'HOST'].includes(role.toUpperCase());

        console.log("登录后导航检查:", {
          role: role,
          isLandlord,
          redirectPath: router.currentRoute.value.query.redirect
        });

        // 使用 Vue Router 进行导航
        if (isLandlord) {
          console.log("导航到房东中心: /host");
          router.push('/host');
        } else {
          const redirectPath = router.currentRoute.value.query.redirect as string | undefined;
          if (redirectPath) {
            console.log("导航到重定向路径:", redirectPath);
            router.push(redirectPath);
          } else {
            console.log("导航到首页: /");
            router.push('/');
          }
        }

      } catch (fetchError) {
        // 获取用户信息失败，很可能是 token 问题
        console.error("登录后获取用户信息失败:", fetchError);
        loadingInstance?.close();
        ElMessage.error("获取用户信息失败，请重新登录");
        userStore.logout();
      }

    } else {
      // userStore.login 本身返回 false (例如后端返回 401 或其他错误)
      loadingInstance?.close();
      ElMessage.error("登录失败：无法连接到服务器或发生未知错误");
    }
  } catch (error: any) {
    loadingInstance?.close();
    console.error("登录出错:", error);
    if (error.message && error.message.includes("用户名或密码错误")) {
        ElMessage.error(error.message);
    } else {
        ElMessage.error(error.message || "登录失败，请检查输入或稍后重试");
    }
  } finally {
    if (loadingInstance) {
        loadingInstance.close();
    }
  }
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

.login-card {
  border: none;
  box-shadow: none;
}

.remember-forgot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.submit-btn {
  width: 100%;
  margin-bottom: 20px;
}

.register-link {
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
  .login-box {
    flex-direction: column;
    width: 100%;
    height: auto;
  }

  .login-left {
    display: none;
  }

  .login-right {
    padding: 20px;
  }
}
</style>