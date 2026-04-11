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
            <div class="register-form-wrapper">
              <el-form ref="registerFormRef" :model="form" :rules="rules" label-position="top"
                :validate-on-rule-change="false">
                <el-form-item label="用户名" prop="username">
                  <el-input v-model="form.username" placeholder="请输入用户名（3-20个字符）" prefix-icon="User" :disabled="loading"
                    :suffix-icon="usernameValidating ? 'Loading' : ''" clearable @blur="validateField('username')" />
                  <div class="form-item-tip" v-if="usernameValidating">
                    <span class="checking-text">正在检查用户名是否可用...</span>
                  </div>
                  <div class="form-item-tip" v-else-if="form.username && form.username.length >= 3">
                    <span class="success-text">✓ 用户名可用</span>
                  </div>
                </el-form-item>

                <el-form-item label="邮箱" prop="email">
                  <el-autocomplete v-model="form.email" placeholder="请输入邮箱地址" :fetch-suggestions="queryEmailSearch"
                    :trigger-on-focus="false" clearable style="width: 100%;" prefix-icon="Message" :disabled="loading"
                    :suffix-icon="emailValidating ? 'Loading' : ''" @blur="validateField('email')" />
                  <div class="form-item-tip" v-if="emailValidating">
                    <span class="checking-text">正在检查邮箱是否可用...</span>
                  </div>
                  <div class="form-item-tip" v-else-if="form.email && isValidEmail(form.email)">
                    <span class="success-text">✓ 邮箱格式正确</span>
                  </div>
                </el-form-item>

                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="form.phone" placeholder="请输入手机号（可选）" prefix-icon="Phone" :disabled="loading"
                    clearable @blur="validateField('phone')" />
                  <div class="form-item-tip" v-if="form.phone && isValidPhone(form.phone)">
                    <span class="success-text">✓ 手机号格式正确</span>
                  </div>
                </el-form-item>

                <el-form-item label="密码" prop="password">
                  <el-input v-model="form.password" type="password" placeholder="请输入密码（6-20个字符）" prefix-icon="Lock"
                    show-password :disabled="loading" clearable @input="onPasswordInput"
                    @blur="validateField('password')" />
                  <!-- 密码强度指示器 -->
                  <div class="password-strength" v-if="form.password">
                    <div class="strength-bars">
                      <div class="strength-bar" :class="getPasswordStrengthClass(index)" v-for="index in 4"
                        :key="index"></div>
                    </div>
                    <span class="strength-text" :class="passwordStrength.class">
                      {{ passwordStrength.text }}
                    </span>
                  </div>
                  <!-- 密码要求提示 -->
                  <div class="password-tips" v-if="showPasswordTips">
                    <p class="tip-title">密码要求：</p>
                    <ul class="tip-list">
                      <li :class="{ 'tip-success': form.password.length >= 6 }">
                        ✓ 至少6个字符
                      </li>
                      <li :class="{ 'tip-success': /[A-Z]/.test(form.password) }">
                        ✓ 包含大写字母（推荐）
                      </li>
                      <li :class="{ 'tip-success': /[0-9]/.test(form.password) }">
                        ✓ 包含数字（推荐）
                      </li>
                      <li :class="{ 'tip-success': hasSpecialChar }">
                        ✓ 包含特殊字符（推荐）
                      </li>
                    </ul>
                  </div>
                </el-form-item>

                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" prefix-icon="Lock"
                    show-password :disabled="loading" clearable @blur="validateField('confirmPassword')" />
                  <div class="form-item-tip" v-if="form.confirmPassword && form.password === form.confirmPassword">
                    <span class="success-text">✓ 密码匹配</span>
                  </div>
                </el-form-item>

                <el-form-item label="注册身份" prop="role">
                  <el-radio-group v-model="form.role">
                    <el-radio label="ROLE_USER">
                      <div class="role-option">
                        <div class="role-title">普通用户</div>
                        <div class="role-desc">浏览和预订民宿</div>
                      </div>
                    </el-radio>
                    <el-radio label="ROLE_HOST">
                      <div class="role-option">
                        <div class="role-title">房东</div>
                        <div class="role-desc">发布房源，管理预订</div>
                      </div>
                    </el-radio>
                  </el-radio-group>
                  <div class="role-tip" v-if="form.role === 'ROLE_HOST'">
                    <el-alert title="房东特权：发布房源、管理订单、获得收益、专属客服支持" type="info" :closable="false" show-icon />
                  </div>
                </el-form-item>

                <!-- 用户协议和隐私政策 -->
                <el-form-item prop="agreement">
                  <el-checkbox v-model="form.agreement" :disabled="loading">
                    我已阅读并同意
                    <el-button type="text" @click="showUserAgreement">《用户服务协议》</el-button>
                    和
                    <el-button type="text" @click="showPrivacyPolicy">《隐私政策》</el-button>
                  </el-checkbox>
                </el-form-item>

                <el-button type="primary" class="submit-btn" :loading="loading"
                  :disabled="!isFormValid || loading || usernameValidating || emailValidating"
                  @click.prevent="handleRegister">
                  {{ loading ? '注册中...' : '立即注册' }}
                </el-button>

                <div class="login-link">
                  已有账号？
                  <el-button type="text" @click="goToLogin" :disabled="loading">立即登录</el-button>
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
import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import type { FormInstance } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import Logo from '@/components/common/Logo.vue';
import api from '@/api';
import { debounce } from 'lodash-es';

const router = useRouter();
const userStore = useUserStore();
const registerFormRef = ref<FormInstance>();
const loading = ref(false);
const showPasswordTips = ref(false);

const form = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
  role: 'ROLE_USER',
  agreement: false,
});

// 添加验证状态跟踪
const usernameValidating = ref(false);
const emailValidating = ref(false);

const hasSpecialChar = computed(() => /[!@#$%^&*(),.?":{}|<>]/.test(form.password));

// 计算表单是否有效
const isFormValid = computed(() => {
  return form.username.length >= 3 &&
    form.email.length > 0 &&
    form.password.length >= 6 &&
    form.confirmPassword === form.password &&
    form.agreement &&
    !usernameValidating.value &&
    !emailValidating.value;
});

// 密码强度计算
const passwordStrength = computed(() => {
  const password = form.password;
  if (!password) return { score: 0, text: '', class: '' };

  let score = 0;

  // 长度检查
  if (password.length >= 6) score++;
  if (password.length >= 8) score++;

  // 复杂度检查
  if (/[a-z]/.test(password)) score++;
  if (/[A-Z]/.test(password)) score++;
  if (/[0-9]/.test(password)) score++;
  if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) score++;

  // 根据分数确定强度
  if (score <= 2) return { score, text: '弱', class: 'weak' };
  if (score <= 4) return { score, text: '中等', class: 'medium' };
  return { score, text: '强', class: 'strong' };
});

const getPasswordStrengthClass = (index: number) => {
  const score = passwordStrength.value.score;
  if (index <= score) {
    if (score <= 2) return 'weak';
    if (score <= 4) return 'medium';
    return 'strong';
  }
  return '';
};

// 邮箱验证
const isValidEmail = (email: string) => {
  return /^[\w-]+(\.[\w-]+)*@([\w-]+\.)+[a-zA-Z]{2,7}$/.test(email);
};

// 手机号验证
const isValidPhone = (phone: string) => {
  return /^1[3-9]\d{9}$/.test(phone);
};

// 密码输入处理
const onPasswordInput = () => {
  showPasswordTips.value = form.password.length > 0 && form.password.length < 6;
};

// 单个字段验证
const validateField = async (prop: string) => {
  if (!registerFormRef.value) return;
  try {
    await registerFormRef.value.validateField(prop);
  } catch {
    // 验证失败时不做任何操作，错误信息已经显示
  }
};

const emailSuffixes = ['@qq.com', '@gmail.com', '@163.com', '@126.com', '@sina.com', '@hotmail.com', '@outlook.com', '@live.com', '@icloud.com'];

interface EmailSuggestion { value: string; }

const queryEmailSearch = (queryString: string, cb: (suggestions: EmailSuggestion[]) => void) => {
  let results: EmailSuggestion[] = [];
  if (queryString) {
    const atIndex = queryString.indexOf('@');
    if (atIndex === -1) { // 用户尚未输入 @
      results = emailSuffixes.map(suffix => ({ value: queryString + suffix }));
    } else { // 用户已输入 @
      const prefix = queryString.substring(0, atIndex);
      const domainInput = queryString.substring(atIndex);
      results = emailSuffixes
        .filter(suffix => suffix.startsWith(domainInput))
        .map(suffix => ({ value: prefix + suffix }));

      // 如果用户输入的@xxx.com 不在预设后缀中，也允许其作为选项，但优先显示匹配的预设后缀
      if (!emailSuffixes.some(s => s === domainInput) && domainInput.length > 1 && domainInput.includes('.')) {
        results.unshift({ value: queryString });
      }
    }
  }
  cb(results);
};

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
  if (!email || !isValidEmail(email)) {
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
    callback(new Error('用户名长度应在 3-20 个字符之间'));
    return;
  }

  if (!/^[a-zA-Z0-9_\u4e00-\u9fa5]+$/.test(value)) {
    callback(new Error('用户名只能包含字母、数字、下划线和中文'));
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
  if (!value) {
    callback(new Error('请输入邮箱'));
    return;
  }

  if (!isValidEmail(value)) {
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
    callback(new Error('两次输入密码不一致'));
  } else {
    callback();
  }
};

const validatePhone = (rule: any, value: string, callback: any) => {
  if (!value) {
    callback();
    return;
  }
  if (!isValidPhone(value)) {
    callback(new Error('请输入正确的手机号'));
  } else {
    callback();
  }
};

const validateAgreement = (rule: any, value: boolean, callback: any) => {
  if (!value) {
    callback(new Error('请阅读并同意用户协议和隐私政策'));
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
    { min: 6, max: 20, message: '密码长度应在 6-20 个字符之间', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, validator: validatePass2, trigger: 'blur' },
  ],
  role: [
    { required: true, message: '请选择注册身份', trigger: 'change' },
  ],
  agreement: [
    { validator: validateAgreement, trigger: 'change' },
  ],
};

const handleRegister = async () => {
  if (!registerFormRef.value) return;

  try {
    await registerFormRef.value.validate();
    loading.value = true;

    const { confirmPassword, agreement, ...registerData } = form;
    console.log('提交注册信息:', registerData);

    try {
      // 注册
      const registerSuccess = await userStore.register(registerData);

      if (registerSuccess) {
        // 注册成功，根据角色提供不同的成功提示
        if (form.role === 'ROLE_HOST') {
          ElMessage.success({
            message: '恭喜您！已成功注册为房东。',
            duration: 3000
          });

          // 延迟跳转提示
          setTimeout(() => {
            ElMessage.info({
              message: '即将跳转到房东信息完善页面...',
              duration: 2000
            });
          }, 1000);

          // 跳转到房东信息完善引导页面
          setTimeout(() => {
            router.push('/host/onboarding');
          }, 3000);
        } else {
          ElMessage.success({
            message: '注册成功！欢迎加入我们的平台！',
            duration: 3000
          });

          // 普通用户跳转到首页
          setTimeout(() => {
            router.push('/');
          }, 2000);
        }
      } else {
        ElMessage.error('注册处理失败，请稍后重试');
      }
    } catch (error: any) {
      console.error('注册失败详情:', error);

      // 优先使用后端返回的具体错误信息
      let errorMessage = "注册失败，请重试";

      if (error.message) {
        errorMessage = error.message;
      } else if (error.response?.data) {
        if (typeof error.response.data === "string") {
          errorMessage = error.response.data;
        } else if (error.response.data.message) {
          errorMessage = error.response.data.message;
        } else if (error.response.data.error) {
          errorMessage = error.response.data.error;
        } else if (error.displayMessage) {
          errorMessage = error.displayMessage;
        }
      }

      // 针对特定错误进行友好提示
      if (errorMessage.includes('邮箱已存在') || errorMessage.includes('邮箱已被注册')) {
        ElMessage.error('该邮箱已被注册，请使用其他邮箱或直接登录');
      } else if (errorMessage.includes('用户名已存在') || errorMessage.includes('用户名已被使用')) {
        ElMessage.error('该用户名已被使用，请选择其他用户名');
      } else {
        ElMessage.error(errorMessage);
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

// 显示用户协议
const showUserAgreement = () => {
  ElMessageBox.alert(
    '这里是用户服务协议的内容...\n\n1. 用户权利和义务\n2. 平台服务内容\n3. 隐私保护\n4. 免责声明\n\n详细内容请访问我们的官方网站。',
    '用户服务协议',
    {
      confirmButtonText: '我知道了',
      type: 'info'
    }
  );
};

// 显示隐私政策
const showPrivacyPolicy = () => {
  ElMessageBox.alert(
    '这里是隐私政策的内容...\n\n1. 信息收集\n2. 信息使用\n3. 信息保护\n4. 第三方服务\n\n我们承诺保护您的隐私安全。',
    '隐私政策',
    {
      confirmButtonText: '我知道了',
      type: 'info'
    }
  );
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

.success-text {
  font-size: 0.8rem;
  color: #67C23A;
}

.password-strength {
  margin-top: 8px;
  margin-bottom: 8px;
}

.strength-bars {
  display: flex;
  height: 4px;
  background-color: #f0f0f0;
  border-radius: 2px;
  gap: 2px;
  margin-bottom: 4px;
}

.strength-bar {
  flex: 1;
  height: 100%;
  background-color: #e0e0e0;
  border-radius: 1px;
}

.strength-bar.weak {
  background-color: #F56C6C;
}

.strength-bar.medium {
  background-color: #E6A23C;
}

.strength-bar.strong {
  background-color: #67C23A;
}

.strength-text {
  font-size: 0.8rem;
  color: #333;
  margin-left: 8px;
}

.strength-text.weak {
  color: #F56C6C;
}

.strength-text.medium {
  color: #E6A23C;
}

.strength-text.strong {
  color: #67C23A;
}

.password-tips {
  margin-top: 8px;
  padding: 12px;
  background-color: #f8f9fa;
  border-radius: 4px;
  border-left: 3px solid #409EFF;
}

.tip-title {
  font-size: 0.9rem;
  font-weight: 600;
  margin-bottom: 8px;
  color: #333;
}

.tip-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tip-list li {
  font-size: 0.8rem;
  margin-bottom: 4px;
  color: #666;
}

.tip-list li.tip-success {
  color: #67C23A;
}

.role-option {
  display: flex;
  flex-direction: column;
}

.role-title {
  font-weight: 600;
  color: #333;
}

.role-desc {
  font-size: 0.8rem;
  color: #999;
  margin-top: 2px;
}

.password-tips {
  margin-top: 8px;
}

.tip-title {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 8px;
}

.tip-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tip-list li {
  margin-bottom: 4px;
}

.tip-list li.tip-success {
  color: #67C23A;
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
