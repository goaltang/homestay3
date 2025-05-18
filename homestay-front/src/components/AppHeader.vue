<template>
    <header class="app-header">
        <div class="header-content">
            <div class="logo" @click="goToHome">
                <img src="/logo.svg" alt="Homestay Logo" class="logo-image" />
                <span class="logo-text">民宿预订</span>
            </div>

            <div class="user-menu">
                <NotificationBell v-if="isLoggedIn" />
                <el-dropdown trigger="click" @command="handleCommand">
                    <div class="user-avatar">
                        <el-icon>
                            <Menu />
                        </el-icon>
                        <el-avatar :size="32" :src="userAvatar" @error="handleAvatarError" />
                    </div>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item v-if="isLoggedIn" command="profile">个人中心</el-dropdown-item>
                            <el-dropdown-item v-if="isLoggedIn" command="host">房东中心</el-dropdown-item>
                            <el-dropdown-item command="favorites">我的收藏</el-dropdown-item>
                            <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                            <el-dropdown-item divided v-if="isLoggedIn" command="logout">退出登录</el-dropdown-item>
                            <el-dropdown-item v-else command="login">登录</el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
            </div>
        </div>
    </header>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar, ElIcon } from 'element-plus';
import { Menu } from '@element-plus/icons-vue';
import { useUserStore } from '@/stores/user';
import { useAuthStore } from '@/stores/auth';
import { getAvatarUrl, handleImageError } from '@/utils/image';
import NotificationBell from '@/components/NotificationBell.vue';

const router = useRouter();
const userStore = useUserStore();
const authStore = useAuthStore();

const isLoggedIn = computed(() => userStore.token !== null || authStore.isAuthenticated);
const userAvatar = computed(() => {
    const avatarUrl = userStore.userInfo?.avatar;
    if (!avatarUrl) {
        const seed = userStore.userInfo?.username || "default" + Date.now();
        return `https://api.dicebear.com/7.x/avataaars/svg?seed=${seed}`;
    }
    return getAvatarUrl(avatarUrl);
});

const goToHome = () => {
    router.push('/');
};

const handleCommand = (command: string) => {
    console.log('Header 命令执行:', command);
    switch (command) {
        case 'profile':
            router.push('/user/profile');
            break;
        case 'host':
            if (!isLoggedIn.value) {
                ElMessage.warning('请先登录');
                router.push('/login');
                return;
            }
            if (authStore.isLandlord || userStore.isLandlord) {
                router.push('/host').catch(err => {
                    console.error('跳转到房东中心失败:', err);
                    ElMessage.error('跳转到房东中心失败');
                });
            } else {
                ElMessage.warning('您不是房东，无法访问房东中心');
            }
            break;
        case 'favorites':
            router.push('/user/favorites');
            break;
        case 'orders':
            router.push('/user/orders');
            break;
        case 'login':
            router.push('/login');
            break;
        case 'logout':
            userStore.logout();
            ElMessage.success('已退出登录');
            router.push('/');
            break;
    }
};

const handleAvatarError = (e: Event) => {
    handleImageError(e, 'avatar');
};
</script>

<style scoped>
.app-header {
    position: sticky;
    top: 0;
    z-index: 100;
    background-color: white;
    border-bottom: 1px solid var(--border-color);
    padding: 16px 0;
}

.header-content {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.logo {
    display: flex;
    align-items: center;
    cursor: pointer;
}

.logo-image {
    height: 32px;
    margin-right: 8px;
}

.logo-text {
    font-size: 20px;
    font-weight: bold;
    color: var(--primary-color);
}

.user-menu {
    display: flex;
    align-items: center;
}

.user-avatar {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    padding: 8px;
    border: 1px solid var(--border-color);
    border-radius: 24px;
}
</style>
