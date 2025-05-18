<template>
    <el-container class="user-layout-container h-screen">
        <!-- 侧边栏 -->
        <el-aside width="220px" class="user-sidebar bg-white shadow-sm">
            <div class="sidebar-header p-4 border-b">
                <h2 class="text-lg font-semibold text-gray-700">用户中心</h2>
            </div>
            <el-menu :default-active="activeMenu" class="el-menu-vertical-user" router>
                <el-menu-item index="/user/profile">
                    <el-icon>
                        <User />
                    </el-icon>
                    <span>个人资料</span>
                </el-menu-item>
                <el-menu-item index="/user/notifications">
                    <el-icon>
                        <Bell />
                    </el-icon>
                    <span>通知中心</span>
                </el-menu-item>
                <el-menu-item index="/user/bookings">
                    <el-icon>
                        <List />
                    </el-icon>
                    <span>我的订单</span>
                </el-menu-item>
                <el-menu-item index="/user/favorites">
                    <el-icon>
                        <Star />
                    </el-icon>
                    <span>我的收藏</span>
                </el-menu-item>
                <!-- 添加我的评价菜单项 -->
                <el-menu-item index="/user/reviews">
                    <el-icon>
                        <Comment />
                    </el-icon>
                    <span>我的评价</span>
                </el-menu-item>
                <!-- 添加更多导航项 -->
            </el-menu>
        </el-aside>

        <!-- 主内容区 -->
        <el-main class="user-main-content bg-white p-4 md:p-6 overflow-y-auto">
            <!-- 添加内容容器，限制最大宽度并居中 -->
            <div class="main-content-wrapper max-w-5xl mx-auto">
                <router-view v-slot="{ Component }">
                    <transition name="fade" mode="out-in">
                        <component :is="Component" />
                    </transition>
                </router-view>
            </div>
        </el-main>
    </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, RouterView } from 'vue-router';
import {
    ElContainer,
    ElAside,
    ElMain,
    ElMenu,
    ElMenuItem,
    ElIcon,
} from 'element-plus';
import {
    User,
    Bell,
    List,
    Star,
    Comment,
} from '@element-plus/icons-vue';

const route = useRoute();

// 计算当前激活的菜单项路径
const activeMenu = computed(() => route.path);

</script>

<style scoped>
.user-layout-container {
    /* 可以覆盖或添加样式 */
}

.user-sidebar {
    border-right: 1px solid var(--el-border-color-lighter);
}

/* 移除 el-menu 的默认右边框 */
.el-menu-vertical-user {
    border-right: none;
}

/* 激活菜单项的样式 (可以根据需要调整) */
.el-menu-item.is-active {
    background-color: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
}

.el-menu-item:hover {
    background-color: var(--el-fill-color-light);
}

/* 内容区域过渡动画 */
.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}
</style>