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
            <div class="login-form-wrapper">
              <el-form ref="loginFormRef" :model="loginForm" :rules="rules" label-position="top"
                :validate-on-rule-change="false">
                <el-form-item label="用户名" prop="username">
                  <el-input ref="usernameInputRef" v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User"
                    :disabled="loading" @keyup.enter.stop.prevent="handleLogin" @blur="validateField('username')"
                    clearable />
                </el-form-item>

                <el-form-item label="密码" prop="password">
                  <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock"
                    show-password :disabled="loading" @keyup.enter.stop.prevent="handleLogin"
                    @blur="validateField('password')" clearable />
                </el-form-item>

                <div class="remember-forgot">
                  <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
                  <el-button type="text" @click.stop.prevent="goToForgotPassword" :disabled="loading">忘记密码？</el-button>
                </div>

                <el-button type="primary" class="submit-btn" :loading="loading" @click.stop.prevent="handleLogin"
                  :disabled="!isFormValid">
                  {{ loading ? '登录中...' : '登录' }}
                </el-button>

                <div class="fast-login-section">
                  <el-divider content-position="center">测试账号一键登录</el-divider>
                  <div class="fast-login-options">
                    <el-button size="small" type="success" plain @click.stop.prevent="fastLogin('user', '111111')" :loading="loading">
                      👤 普通用户
                    </el-button>
                    <el-button size="small" type="warning" plain @click.stop.prevent="fastLogin('host', '111111')" :loading="loading">
                      🏠 房东账号
                    </el-button>
                    <el-button size="small" type="danger" plain @click.stop.prevent="fastLogin('admin', '111111')" :loading="loading">
                      👑 管理员
                    </el-button>
                  </div>
                </div>

                <div class="register-link">
                  还没有账号？
                  <el-button type="text" @click.stop.prevent="goToRegister" :disabled="loading">立即注册</el-button>
                </div>
              </el-form>
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useUserStore } from "@/stores/user";
import type { FormInstance } from "element-plus";
import { ElMessage } from "element-plus";
import Logo from "@/components/common/Logo.vue";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const loginFormRef = ref<FormInstance>();
const usernameInputRef = ref();
const loading = ref(false);
const lastLoginAttempt = ref(0);

const loginForm = reactive({
  username: "",
  password: "",
  remember: false
});

// 表单验证规则
const rules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 3, max: 20, message: "用户名长度应在 3-20 个字符之间", trigger: "blur" },
    { pattern: /^[a-zA-Z0-9_\u4e00-\u9fa5]+$/, message: "用户名只能包含字母、数字、下划线和中文", trigger: "blur" }
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 20, message: "密码长度应在 6-20 个字符之间", trigger: "blur" },
  ],
};

// 计算表单是否有效
const isFormValid = computed(() => {
  return loginForm.username.length >= 3 &&
    loginForm.password.length >= 6 &&
    loginForm.username.length <= 20 &&
    loginForm.password.length <= 20;
});

// 单个字段验证
const validateField = async (prop: string) => {
  if (!loginFormRef.value) return;
  try {
    await loginFormRef.value.validateField(prop);
  } catch {
    // 验证失败时不做任何操作，错误信息已经显示
  }
};

// 处理登录
const handleLogin = async (event?: Event) => {
  // 阻止任何可能的默认行为
  if (event) {
    event.preventDefault();
    event.stopPropagation();
  }

  // 防抖机制：防止快速重复点击
  const now = Date.now();
  if (now - lastLoginAttempt.value < 1000) {
    return false;
  }
  lastLoginAttempt.value = now;

  if (!loginFormRef.value || loading.value) {
    return false;
  }

  try {
    // 验证表单
    await loginFormRef.value.validate();

    loading.value = true;
    console.log("开始登录验证:", { username: loginForm.username });

    const success = await userStore.login(loginForm.username, loginForm.password);

    if (success) {
      console.log("登录成功，获取用户信息中...");

      try {
        // 获取完整用户信息
        await userStore.fetchUserInfo();
        console.log("用户信息获取成功:", userStore.userInfo);

        ElMessage.success({
          message: `欢迎回来，${userStore.userInfo?.realName || userStore.userInfo?.username || '用户'}！`,
          duration: 2000
        });

        // 如果选择了记住我，保存用户名
        if (loginForm.remember) {
          localStorage.setItem('rememberedUsername', loginForm.username);
        } else {
          localStorage.removeItem('rememberedUsername');
        }

        // 导航逻辑
        await handleLoginSuccess();

      } catch (fetchError) {
        console.error("获取用户信息失败:", fetchError);
        ElMessage.error("获取用户信息失败，请重新登录");
        userStore.logout();
      }

    } else {
      ElMessage.error("登录失败，请检查用户名和密码");
    }

  } catch (error: any) {
    console.error("登录过程中出错:", error);

    // 确保即使发生错误也阻止页面刷新
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }

    // 提取错误信息
    let errorMessage = "登录失败，请重试";

    // 优先使用后端返回的 message 字段（ApiResponse 格式）
    if (error.response?.data?.message) {
      errorMessage = error.response.data.message;
    }
    // 处理字符串格式的响应
    else if (typeof error.response?.data === "string" && error.response.data) {
      errorMessage = error.response.data;
    }
    // 根据 HTTP 状态码判断
    else if (error.response?.status === 401) {
      errorMessage = "用户名或密码错误，请重新输入";
    } else if (error.response?.status === 404) {
      errorMessage = "用户不存在，请检查用户名或先注册账号";
    } else if (error.response?.status === 429) {
      errorMessage = "登录尝试过于频繁，请稍后再试";
    } else if (error.response?.status >= 500) {
      errorMessage = "服务器暂时无法连接，请稍后重试";
    } else if (error.message?.includes("Network Error")) {
      errorMessage = "网络连接失败，请检查网络设置";
    }

    // 显示错误信息
    ElMessage.error(errorMessage);

    // 如果是用户不存在的错误，延迟显示注册提示
    if (errorMessage.includes("用户不存在")) {
      setTimeout(() => {
        ElMessage.info('还没有账号？点击下方"立即注册"创建新账号');
      }, 1500);
    }
  } finally {
    loading.value = false;
  }

  return false; // 确保不会触发任何默认提交行为
};

// 处理登录成功后的导航
const handleLoginSuccess = async () => {
  await nextTick(); // 确保 DOM 更新完成

  // 从最新的 userInfo 中获取角色，确保使用 fetchUserInfo 后的最新数据
  const role = userStore.userInfo?.role?.toUpperCase() || '';

  console.log("导航决策:", {
    role: userStore.userInfo?.role,
    normalizedRole: role,
    isAdmin: userStore.isAdmin,
    isLandlord: userStore.isLandlord
  });

  // 管理员 → 管理后台
  if (userStore.isAdmin) {
    await router.push('/admin');
    return;
  }

  // 房东 → 房东管理页
  if (userStore.isLandlord) {
    await router.push('/host');
    return;
  }

  // 普通用户 → 检查 redirect 参数，否则去首页
  const redirectPath = route.query.redirect as string;
  if (redirectPath && !redirectPath.includes('login') && !redirectPath.includes('register')) {
    await router.push(redirectPath);
  } else {
    await router.push('/');
  }
};

// 跳转到注册页
const goToRegister = () => {
  if (loading.value) return;
  router.push("/register");
};

// 跳转到忘记密码页
const goToForgotPassword = () => {
  if (loading.value) return;
  router.push("/forgot-password");
};

// 一键快速登录
const fastLogin = async (username: string, password: string = '111111') => {
  if (loading.value) return;
  loginForm.username = username;
  loginForm.password = password;
  await nextTick();
  handleLogin();
};

// 页面加载完成后的初始化
onMounted(async () => {
  // 处理路由消息
  if (route.query.message) {
    ElMessage.info(route.query.message as string);
  }

  // 自动聚焦到用户名输入框
  await nextTick();
  if (usernameInputRef.value) {
    usernameInputRef.value.focus();
  }

  // 恢复记住的用户名
  const rememberedUsername = localStorage.getItem('rememberedUsername');
  if (rememberedUsername) {
    loginForm.username = rememberedUsername;
    loginForm.remember = true;
  }


});
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

.fast-login-section {
  margin-bottom: 20px;
}

:deep(.el-divider__text) {
  color: #909399;
  font-size: 13px;
  background-color: var(--el-bg-color);
}

.fast-login-options {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.fast-login-options .el-button {
  flex: 1;
  padding: 8px 0;
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