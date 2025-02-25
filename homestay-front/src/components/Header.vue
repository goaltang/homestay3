<template>
    <el-header class="header">
        <div class="header-left">
            <router-link to="/" class="logo">
                <Logo class="logo-icon" />
                <span class="logo-text">民宿预订平台</span>
            </router-link>
        </div>

        <div class="header-right">
            <template v-if="userStore.isAuthenticated">
                <el-dropdown>
                    <span class="user-menu">
                        {{ userStore.userInfo.username }}
                        <el-icon>
                            <ArrowDown />
                        </el-icon>
                    </span>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item>
                                <router-link to="/profile">个人中心</router-link>
                            </el-dropdown-item>
                            <el-dropdown-item divided @click="handleLogout">
                                退出登录
                            </el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
            </template>
            <template v-else>
                <router-link to="/login">登录</router-link>
                <router-link to="/register" class="register-link">注册</router-link>
            </template>
        </div>
    </el-header>
</template>

<script setup lang="ts">
import { useUserStore } from "@/stores/user";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { ArrowDown } from "@element-plus/icons-vue";
import Logo from "./Logo.vue";

const userStore = useUserStore();
const router = useRouter();

const handleLogout = () => {
    userStore.logout();
    ElMessage.success("已退出登录");
    router.push({ name: "Login" });
};
</script>

<style scoped>
.header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 20px;
    height: 60px;
    background-color: #fff;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header-left {
    display: flex;
    align-items: center;
}

.logo {
    display: flex;
    align-items: center;
    text-decoration: none;
    color: #333;
}

.logo-icon {
    width: 32px;
    height: 32px;
    margin-right: 8px;
}

.logo-text {
    font-size: 18px;
    font-weight: bold;
}

.header-right {
    display: flex;
    align-items: center;
    gap: 20px;
}

.header-right a {
    text-decoration: none;
    color: #333;
}

.register-link {
    color: #409eff;
}

.user-menu {
    display: flex;
    align-items: center;
    gap: 4px;
    cursor: pointer;
}

.el-dropdown-menu a {
    text-decoration: none;
    color: inherit;
    display: block;
    width: 100%;
}
</style>