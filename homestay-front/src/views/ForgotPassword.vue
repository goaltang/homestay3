<template>
  <div class="forgot-password-container">
    <div class="forgot-password-box">
      <h2>找回密码</h2>
      <p class="subtitle">请输入您的注册邮箱，我们将向您发送重置密码的链接</p>

      <el-form ref="formRef" :model="form" :rules="rules">
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="电子邮箱" prefix-icon="Message" />
        </el-form-item>

        <el-button type="primary" class="submit-btn" :loading="loading" @click="handleSubmit">
          发送重置链接
        </el-button>

        <div class="back-to-login">
          <el-button type="text" @click="goToLogin">返回登录</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";

const router = useRouter();
const loading = ref(false);

const form = reactive({
  email: "",
});

const rules = {
  email: [
    { required: true, message: "请输入邮箱地址", trigger: "blur" },
    { type: "email", message: "请输入正确的邮箱地址", trigger: "blur" },
  ],
};

const handleSubmit = async () => {
  try {
    loading.value = true;
    // TODO: 实现发送重置密码邮件的逻辑
    ElMessage.success("重置链接已发送到您的邮箱");
    router.push("/login");
  } catch (error: any) {
    ElMessage.error(error.message);
  } finally {
    loading.value = false;
  }
};

const goToLogin = () => {
  router.push("/login");
};
</script>

<style scoped>
.forgot-password-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f7fa;
}

.forgot-password-box {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

.forgot-password-box h2 {
  text-align: center;
  margin-bottom: 10px;
  color: #303133;
}

.subtitle {
  text-align: center;
  color: #606266;
  margin-bottom: 30px;
}

.submit-btn {
  width: 100%;
  margin-bottom: 20px;
}

.back-to-login {
  text-align: center;
}
</style> 