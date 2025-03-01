<template>
  <div class="app-container">
    <!-- 导航栏 -->
    <header class="app-header">
      <div class="header-content">
        <!-- Logo -->
        <div class="logo" @click="goToHome">
          <img src="/logo.svg" alt="Homestay Logo" class="logo-image" />
          <span class="logo-text">民宿预订</span>
        </div>

        <!-- 用户菜单 -->
        <div class="user-menu">
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-avatar">
              <el-icon>
                <Menu />
              </el-icon>
              <el-avatar :size="32" :src="userAvatar" />
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="isLoggedIn" command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="favorites">我的收藏</el-dropdown-item>
                <el-dropdown-item command="bookings">我的预订</el-dropdown-item>
                <el-dropdown-item divided v-if="isLoggedIn" command="logout">退出登录</el-dropdown-item>
                <el-dropdown-item v-else command="login">登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="app-main">
      <router-view />
    </main>

    <!-- 页脚 -->
    <footer class="app-footer">
      <div class="footer-content">
        <div class="footer-section">
          <h3>关于我们</h3>
          <ul>
            <li>公司简介</li>
            <li>加入我们</li>
            <li>新闻中心</li>
          </ul>
        </div>
        <div class="footer-section">
          <h3>帮助中心</h3>
          <ul>
            <li>预订指南</li>
            <li>常见问题</li>
            <li>联系客服</li>
          </ul>
        </div>
        <div class="footer-section">
          <h3>商务合作</h3>
          <ul>
            <li>房东申请</li>
            <li>广告合作</li>
            <li>战略合作</li>
          </ul>
        </div>
        <div class="footer-section">
          <h3>法律声明</h3>
          <ul>
            <li>隐私政策</li>
            <li>用户协议</li>
            <li>Cookie政策</li>
          </ul>
        </div>
      </div>
      <div class="footer-bottom">
        <p>© 2025 民宿预订平台 版权所有</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Menu } from '@element-plus/icons-vue'
import { useUserStore } from './stores/user'

const router = useRouter()
const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.token !== null)
const userAvatar = computed(() => {
  return userStore.user?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
})

const goToHome = () => {
  router.push('/')
}

const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/user/profile')
      break
    case 'favorites':
      router.push('/user/favorites')
      break
    case 'bookings':
      router.push('/user/bookings')
      break
    case 'login':
      router.push('/login')
      break
    case 'logout':
      userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/')
      break
  }
}
</script>

<style>
/* 全局样式 */
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

.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* 头部样式 */
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

.user-avatar {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px;
  border: 1px solid var(--border-color);
  border-radius: 24px;
}

/* 主内容区样式 */
.app-main {
  flex: 1;
}

/* 页脚样式 */
.app-footer {
  background-color: var(--background-light);
  padding: 48px 0 24px;
  margin-top: 48px;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 32px;
}

.footer-section h3 {
  margin-bottom: 16px;
  font-size: 16px;
}

.footer-section ul {
  list-style: none;
}

.footer-section li {
  margin-bottom: 8px;
  color: var(--text-light);
  cursor: pointer;
}

.footer-section li:hover {
  text-decoration: underline;
}

.footer-bottom {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  text-align: center;
  border-top: 1px solid var(--border-color);
  margin-top: 32px;
}

.footer-bottom p {
  color: var(--text-light);
  font-size: 14px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .footer-content {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .footer-content {
    grid-template-columns: 1fr;
  }
}
</style>