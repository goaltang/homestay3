<template>
  <div class="common-layout">
    <el-container>
      <el-aside width="200px">
        <div class="sidebar">
          <el-menu :default-active="route.path" class="el-menu-vertical" :collapse="isCollapse" router>
            <el-menu-item index="/dashboard">
              <el-icon>
                <DataLine />
              </el-icon>
              <template #title>系统首页</template>
            </el-menu-item>

            <!-- 房源管理 Sub Menu -->
            <el-sub-menu index="/homestay-management">
              <template #title>
                <el-icon>
                  <House />
                </el-icon>
                <span>房源管理</span>
              </template>
              <el-menu-item index="/homestays">房源列表</el-menu-item>
              <el-menu-item index="/types">房源类型管理</el-menu-item>
              <!-- 设施管理 Sub Menu (Nested) -->
              <el-sub-menu index="/amenities">
                <template #title>
                  <el-icon>
                    <SetUp />
                  </el-icon>
                  <span>设施管理</span>
                </template>
                <el-menu-item index="/amenities/manage">设施列表</el-menu-item>
                <el-menu-item index="/amenities/categories">设施分类</el-menu-item>
              </el-sub-menu>
            </el-sub-menu>

            <el-menu-item index="/orders">
              <el-icon>
                <List />
              </el-icon>
              <template #title>订单管理</template>
            </el-menu-item>

            <!-- 用户管理 Sub Menu -->
            <el-sub-menu index="/user-management">
              <template #title>
                <el-icon>
                  <User />
                </el-icon>
                <span>用户管理</span>
              </template>
              <el-menu-item index="/users">用户列表</el-menu-item>
              <el-menu-item index="/verifications">身份认证审核</el-menu-item>
            </el-sub-menu>

            <!-- Add Review Management Menu Item -->
            <el-menu-item index="/reviews">
              <el-icon>
                <ChatDotSquare />
              </el-icon>
              <template #title>评价管理</template>
            </el-menu-item>

            <!-- Keep other top-level items if needed, e.g., Reviews, Statistics -->
            <!-- Example:
            <el-menu-item index="/reviews">
              <el-icon>...</el-icon>
              <template #title>评价管理</template>
            </el-menu-item>
            <el-menu-item index="/statistics">
              <el-icon>...</el-icon>
              <template #title>统计分析</template>
            </el-menu-item>
            -->
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
import { DataLine, House, List, User, Fold, Expand, CaretBottom, Tickets, Collection, SetUp, ChatDotSquare } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)

// 使用计算属性从store获取用户名
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