<template>
    <div class="host-layout">
        <el-container class="layout-container">
            <el-aside width="220px" class="aside">
                <div class="logo">
                    <h2>房东中心</h2>
                </div>
                <el-menu router :default-active="activeMenu" class="el-menu-vertical" background-color="#304156"
                    text-color="#bfcbd9" active-text-color="#409EFF">
                    <el-menu-item index="/host">
                        <el-icon>
                            <Monitor />
                        </el-icon>
                        <span>控制台</span>
                    </el-menu-item>

                    <el-menu-item index="/host/homestay">
                        <el-icon>
                            <House />
                        </el-icon>
                        <span>房源管理</span>
                    </el-menu-item>

                    <el-menu-item index="/host/orders">
                        <el-icon>
                            <List />
                        </el-icon>
                        <span>订单管理</span>
                    </el-menu-item>

                    <el-menu-item index="/host/earnings">
                        <el-icon>
                            <Money />
                        </el-icon>
                        <span>收益管理</span>
                    </el-menu-item>

                    <el-menu-item index="/host/reviews">
                        <el-icon>
                            <StarFilled />
                        </el-icon>
                        <span>评价管理</span>
                    </el-menu-item>

                    <el-menu-item index="/host/profile">
                        <el-icon>
                            <User />
                        </el-icon>
                        <span>个人资料</span>
                    </el-menu-item>
                </el-menu>
            </el-aside>

            <el-container>
                <el-header class="header">
                    <div class="header-left">
                        <el-breadcrumb separator="/">
                            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
                            <el-breadcrumb-item>房东中心</el-breadcrumb-item>
                            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
                        </el-breadcrumb>
                    </div>
                    <div class="header-right">
                        <el-dropdown>
                            <span class="user-dropdown">
                                {{ userName }} <el-icon>
                                    <ArrowDown />
                                </el-icon>
                            </span>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <el-dropdown-item @click="goToProfile">个人资料</el-dropdown-item>
                                    <el-dropdown-item @click="goToHomePage">返回首页</el-dropdown-item>
                                    <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
                                </el-dropdown-menu>
                            </template>
                        </el-dropdown>
                    </div>
                </el-header>

                <el-main class="main">
                    <router-view />
                </el-main>
            </el-container>
        </el-container>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
    Monitor,
    House,
    List,
    Money,
    StarFilled,
    User,
    ArrowDown
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useAuthStore } from '@/stores/auth'
import { ElMessageBox, ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const authStore = useAuthStore()

// 从userInfo中获取用户名
const userName = computed(() => userStore.userInfo?.username || '房东')

// 计算当前激活的菜单项
const activeMenu = computed(() => {
    const { meta, path } = route
    if (meta.activeMenu) {
        return meta.activeMenu
    }
    return path
})

// 获取当前页面标题
const currentPageTitle = computed(() => {
    return route.meta.title || '房东中心'
})

// 前往个人资料页
const goToProfile = () => {
    router.push('/host/profile')
}

// 返回首页
const goToHomePage = () => {
    router.push('/')
}

// 处理退出登录
const handleLogout = () => {
    ElMessageBox.confirm('确定要退出登录吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        userStore.logout()
        router.push('/login')
        ElMessage.success('已成功退出登录')
    }).catch(() => { })
}

onMounted(() => {
    console.log('房东中心组件已挂载')
    console.log('当前用户信息:', {
        userStore: userStore.userInfo,
        authStore: authStore.user
    })
    console.log('当前用户角色:', {
        userStoreRole: userStore.userInfo?.role,
        authStoreRole: authStore.userRole,
        authStoreIsLandlord: authStore.isLandlord
    })

    // 如果用户不是房东，重定向到首页
    const isUserAuthenticated = userStore.token !== null || authStore.isAuthenticated
    const isLandlord = authStore.isLandlord || userStore.userInfo?.role === 'ROLE_LANDLORD'

    if (!isUserAuthenticated) {
        console.error('用户未登录，重定向至登录页')
        ElMessage.error('请先登录')
        router.push('/login')
    } else if (!isLandlord) {
        console.error('用户角色不是房东:', {
            userStoreRole: userStore.userInfo?.role,
            authStoreRole: authStore.userRole
        })
        ElMessage.error('您没有访问房东中心的权限')
        router.push('/')
    } else {
        console.log('用户有权限访问房东中心')
    }
})
</script>

<style scoped>
.host-layout {
    min-height: 100vh;
}

.layout-container {
    height: 100vh;
}

.aside {
    background-color: #304156;
    color: #bfcbd9;
    overflow-x: hidden;
}

.logo {
    height: 60px;
    line-height: 60px;
    text-align: center;
    color: #fff;
    font-size: 18px;
    border-bottom: 1px solid #1f2d3d;
}

.header {
    background-color: #fff;
    color: #333;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-right {
    display: flex;
    align-items: center;
}

.user-dropdown {
    cursor: pointer;
    display: flex;
    align-items: center;
}

.main {
    background-color: #f0f2f5;
    padding: 20px;
}

.el-menu-vertical {
    border-right: none;
}

.el-menu-item {
    display: flex;
    align-items: center;
}

.el-icon {
    margin-right: 5px;
}
</style>