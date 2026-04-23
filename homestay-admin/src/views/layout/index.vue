<template>
  <div class="common-layout">
    <el-container>
      <el-aside width="200px">
        <div class="sidebar">
          <el-menu :default-active="route.path" class="el-menu-vertical" :collapse="isCollapse" router>
            <template v-for="item in menuData" :key="item.id">
              <el-sub-menu v-if="item.children" :index="item.index">
                <template #title>
                  <el-icon>
                    <component :is="item.icon" />
                  </el-icon>
                  <span>{{ item.title }}</span>
                </template>
                <template v-for="subItem in item.children" :key="subItem.id">
                  <el-sub-menu v-if="subItem.children" :index="subItem.index">
                    <template #title>{{ subItem.title }}</template>
                    <el-menu-item v-for="three in subItem.children" :key="three.id" :index="three.index">
                      {{ three.title }}
                    </el-menu-item>
                  </el-sub-menu>
                  <el-menu-item v-else :index="subItem.index">
                    {{ subItem.title }}
                  </el-menu-item>
                </template>
              </el-sub-menu>
              <el-menu-item v-else :index="item.index">
                <el-icon>
                  <component :is="item.icon" />
                </el-icon>
                <template #title>{{ item.title }}</template>
              </el-menu-item>
            </template>
          </el-menu>
        </div>
      </el-aside>
      <el-container>
        <el-header>
          <div class="header">
            <div class="left">
              <el-button type="text" @click="toggleSidebar">
                <el-icon>
                  <Fold v-if="!isCollapse" />
                  <Expand v-else />
                </el-icon>
              </el-button>
              <el-breadcrumb separator="/">
                <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
                <el-breadcrumb-item>{{ route.meta?.title }}</el-breadcrumb-item>
              </el-breadcrumb>
            </div>
            <div class="right">
              <el-dropdown>
                <span class="user-info">
                  {{ username }}
                  <el-icon>
                    <CaretBottom />
                  </el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </el-header>
        <el-main>
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Fold, Expand, CaretBottom } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { menuData } from '@/config/menu'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)

const username = computed(() => userStore.username || '管理员')

const toggleSidebar = () => {
  isCollapse.value = !isCollapse.value
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    router.push('/login')
  })
}
</script>

<style scoped lang="scss">
.common-layout {
  height: 100vh;
}

.sidebar {
  height: 100vh;
  background-color: #304156;

  .el-menu {
    height: 100%;
    border-right: none;
  }
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 20px;
  border-bottom: 1px solid #dcdfe6;

  .left {
    display: flex;
    align-items: center;
    gap: 20px;
  }

  .right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 4px;
      cursor: pointer;
    }
  }
}

.el-main {
  background-color: #f0f2f5;
  padding: 20px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
