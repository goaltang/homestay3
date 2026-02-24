<template>
    <div class="auth-guard">
        <el-card class="login-prompt-card" shadow="always">
            <div class="login-prompt-content">
                <el-icon class="login-icon" :size="48" color="#409EFF">
                    <User />
                </el-icon>
                <h3>需要登录</h3>
                <p>{{ message || '请先登录后再进行此操作' }}</p>
                <div class="login-actions">
                    <el-button type="primary" @click="goToLogin">
                        立即登录
                    </el-button>
                    <el-button @click="goToRegister">
                        注册账号
                    </el-button>
                </div>
                <el-button type="text" class="close-btn" @click="$emit('close')">
                    <el-icon>
                        <Close />
                    </el-icon>
                </el-button>
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { User, Close } from '@element-plus/icons-vue'

interface Props {
    message?: string
    redirectAfterLogin?: boolean
}

const props = withDefaults(defineProps<Props>(), {
    redirectAfterLogin: true
})

const emit = defineEmits<{
    close: []
}>()

const router = useRouter()
const route = useRoute()

const goToLogin = () => {
    const loginRoute = {
        name: 'login'
    } as any

    if (props.redirectAfterLogin) {
        loginRoute.query = { redirect: route.fullPath }
    }

    router.push(loginRoute)
    emit('close')
}

const goToRegister = () => {
    const registerRoute = {
        name: 'register'
    } as any

    if (props.redirectAfterLogin) {
        registerRoute.query = { redirect: route.fullPath }
    }

    router.push(registerRoute)
    emit('close')
}
</script>

<style scoped>
.auth-guard {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 2000;
}

.login-prompt-card {
    position: relative;
    width: 400px;
    max-width: 90vw;
}

.login-prompt-content {
    text-align: center;
    padding: 20px;
}

.login-icon {
    margin-bottom: 16px;
}

.login-prompt-content h3 {
    margin: 0 0 8px 0;
    font-size: 20px;
    color: #303133;
}

.login-prompt-content p {
    margin: 0 0 20px 0;
    color: #606266;
    line-height: 1.5;
}

.login-actions {
    display: flex;
    gap: 12px;
    justify-content: center;
}

.close-btn {
    position: absolute;
    top: 8px;
    right: 8px;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
}

@media (max-width: 768px) {
    .login-actions {
        flex-direction: column;
    }

    .login-actions .el-button {
        width: 100%;
    }
}
</style>