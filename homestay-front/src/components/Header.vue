<template>
    <el-header class="header">
        <div class="logo">民宿预订系统</div>
        <div class="nav">
            <el-menu mode="horizontal" :router="true">
                <el-menu-item index="/">首页</el-menu-item>
                <el-menu-item index="/homestays">民宿列表</el-menu-item>
            </el-menu>
        </div>
        <div class="user-area">
            <template v-if="userStore.isAuthenticated && userStore.userInfo">
                <el-dropdown @command="handleCommand">
                    <span class="user-info">
                        <el-avatar :size="32"
                            :src="userStore.userInfo.avatar ? `${baseUrl}${userStore.userInfo.avatar}` : ''">
                            {{ userStore.userInfo?.username?.charAt(0)?.toUpperCase() }}
                        </el-avatar>
                        <span class="username">{{ userStore.userInfo.username }}</span>
                    </span>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                            <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
            </template>
            <template v-else>
                <el-button type="text" @click="$router.push('/login')">登录</el-button>
                <el-button type="primary" @click="$router.push('/register')">注册</el-button>
            </template>
        </div>
    </el-header>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { onMounted } from 'vue';

const router = useRouter();
const userStore = useUserStore();
const baseUrl = 'http://localhost:8080';

onMounted(async () => {
    if (userStore.isAuthenticated && !userStore.userInfo) {
        try {
            await userStore.getUserInfo();
        } catch (error) {
            console.error('获取用户信息失败:', error);
        }
    }
});

const handleCommand = (command: string) => {
    switch (command) {
        case 'profile':
            router.push('/profile');
            break;
        case 'orders':
            router.push('/orders');
            break;
        case 'logout':
            userStore.logout();
            router.push('/login');
            break;
    }
};
</script>

<style scoped>
.header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
    background-color: #fff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 1000;
}

.logo {
    font-size: 20px;
    font-weight: bold;
    color: var(--el-color-primary);
}

.nav {
    flex: 1;
    margin: 0 40px;
}

.user-area {
    display: flex;
    align-items: center;
    gap: 16px;
}

.user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
}

.username {
    font-size: 14px;
    color: #333;
}

:deep(.el-menu) {
    border-bottom: none;
}

:deep(.el-menu--horizontal) {
    border-bottom: none;
}

:deep(.el-menu-item) {
    font-size: 15px;
}
</style>