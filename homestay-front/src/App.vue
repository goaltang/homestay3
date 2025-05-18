<template>
  <div class="app-container">
    <!-- 修改 v-if 条件，排除登录和注册页 -->
    <AppHeader v-if="!isHostCenterRoute && route.path !== '/login' && route.path !== '/register'" />

    <!-- 主内容区 -->
    <main class="app-main" :class="{ 'host-center-main': isHostCenterRoute }">
      <router-view />
    </main>

    <!-- 修改 v-if 条件，排除登录和注册页 -->
    <AppFooter v-if="!isHostCenterRoute && route.path !== '/login' && route.path !== '/register'" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router' // 移除 useRouter, ElMessage, Menu 等 Header 相关的导入
import { useUserStore } from './stores/user'
// import { useAuthStore } from './stores/auth' // authStore 现在由 AppHeader 使用
// import { getAvatarUrl, handleImageError } from './utils/image' // 这些现在由 AppHeader 处理
import AppHeader from '@/components/AppHeader.vue'; // 导入新的 Header
import AppFooter from '@/components/AppFooter.vue'; // 导入新的 Footer

// const router = useRouter() // router 现在由 AppHeader 使用
const route = useRoute()
const userStore = useUserStore()
// const authStore = useAuthStore() // authStore 现在由 AppHeader 使用

// onMounted 获取用户信息的逻辑可以保留在 App.vue，因为它关系到整个应用的状态
onMounted(async () => {
  if (userStore.token && (!userStore.userInfo || !userStore.userInfo.avatar)) {
    console.log('App.vue: 已登录但信息不完整，尝试获取完整用户信息');
    try {
      await userStore.fetchUserInfo();
      console.log('App.vue: 成功获取用户信息:', userStore.userInfo);
    } catch (error) {
      console.error('App.vue: 获取用户信息失败:', error);
    }
  }
});

// isLoggedIn, userAvatar, goToHome, handleCommand, handleAvatarError 这些逻辑都移到了 AppHeader.vue

// isHostCenterRoute 仍然需要，因为它控制 Header 和 Footer 的显示
const isHostCenterRoute = computed(() => {
  return route.path.startsWith('/host');
})

</script>

<style>
/* 全局样式可以保留 */
:root {
  --primary-color: #ff385c;
  --secondary-color: #00a699;
  --text-color: #222222;
  --text-light: #717171;
  --border-color: #dddddd;
  --background-light: #f7f7f7;
}

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  color: var(--text-color);
  line-height: 1.5;
}

/* 全局数字输入框样式优化 */
.el-input-number {
  width: 100px !important;
}

.el-input-number .el-input__inner {
  text-align: center !important;
  padding-left: 8px !important;
  padding-right: 8px !important;
}

/* 隐藏筛选面板中的数字输入框控制按钮 */
.filter-card .el-input-number__decrease,
.filter-card .el-input-number__increase {
  display: none !important;
}

.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* 主内容区样式 */
.app-main {
  flex: 1;
}

.host-center-main {
  padding-top: 0 !important;
}

/* 移除 App.vue 中与 Header 和 Footer 相关的局部样式 */
/* .app-header { ... } */
/* .header-content { ... } */
/* .logo { ... } */
/* ... */
/* .app-footer { ... } */
/* .footer-content { ... } */
/* ... */
</style>