<template>
    <header class="bg-white shadow-md">
        <nav class="container mx-auto px-4 py-3 flex justify-between items-center">
            <div class="flex items-center">
                <router-link to="/" class="text-2xl font-bold text-primary">
                    <img src="@/assets/logo.png" alt="Homestay Logo" class="h-10" />
                </router-link>
            </div>

            <div class="hidden md:flex space-x-6">
                <router-link to="/" class="nav-link">首页</router-link>
                <router-link to="/homestays" class="nav-link">浏览房源</router-link>
                <router-link to="/about" class="nav-link">关于我们</router-link>
            </div>

            <div class="flex items-center space-x-4">
                <template v-if="authStore.isAuthenticated">
                    <div class="relative" ref="userMenuContainer">
                        <button @click="toggleUserMenu" class="flex items-center space-x-2">
                            <img :src="authStore.user?.avatar ? `${baseUrl}${authStore.user.avatar}` : '/default-avatar.png'"
                                alt="User Avatar" class="w-8 h-8 rounded-full object-cover" />
                            <span class="hidden md:inline">{{ authStore.user?.username }}</span>
                            <i class="fas fa-chevron-down text-xs"></i>
                        </button>

                        <div v-if="showUserMenu"
                            class="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg py-1 z-10">
                            <router-link to="/profile" class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
                                个人资料
                            </router-link>

                            <router-link to="/orders" class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
                                我的订单
                            </router-link>

                            <template v-if="authStore.isLandlord">
                                <router-link to="/landlord"
                                    class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
                                    房东中心
                                </router-link>
                                <router-link to="/landlord/homestays"
                                    class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
                                    我的房源
                                </router-link>
                            </template>

                            <button @click="logout"
                                class="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
                                退出登录
                            </button>
                        </div>
                    </div>
                </template>

                <template v-else>
                    <router-link to="/login" class="btn-secondary">登录</router-link>
                    <router-link to="/register" class="btn-primary hidden md:block">注册</router-link>
                </template>
            </div>
        </nav>
    </header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const showUserMenu = ref(false);
const userMenuContainer = ref<HTMLElement | null>(null);
const baseUrl = import.meta.env.VITE_API_BASE_URL || '';

function toggleUserMenu() {
    showUserMenu.value = !showUserMenu.value;
}

function handleClickOutside(event: MouseEvent) {
    if (userMenuContainer.value && !userMenuContainer.value.contains(event.target as Node)) {
        showUserMenu.value = false;
    }
}

function logout() {
    authStore.logout();
    showUserMenu.value = false;
}

onMounted(() => {
    document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
    document.removeEventListener('click', handleClickOutside);
});
</script>

<style scoped>
.nav-link {
    @apply text-gray-700 hover:text-primary transition-colors;
}

.btn-primary {
    @apply bg-primary text-white px-4 py-2 rounded-md hover:bg-primary-dark transition-colors;
}

.btn-secondary {
    @apply bg-white text-primary border border-primary px-4 py-2 rounded-md hover:bg-gray-50 transition-colors;
}
</style>
