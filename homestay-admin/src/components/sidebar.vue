<template>
    <div class="sidebar">
        <el-menu class="sidebar-el-menu" :default-active="onRoutes" :collapse="sidebar.collapse"
            :background-color="sidebar.bgColor" :text-color="sidebar.textColor" @select="handleMenuSelect">
            <template v-for="item in menuData">
                <template v-if="item.children">
                    <el-sub-menu :index="item.index" :key="item.index">
                        <template #title>
                            <el-icon>
                                <component :is="item.icon"></component>
                            </el-icon>
                            <span>{{ item.title }}</span>
                        </template>
                        <template v-for="subItem in item.children">
                            <el-sub-menu v-if="subItem.children" :index="subItem.index" :key="subItem.index">
                                <template #title>
                                    <span>{{ subItem.title }}</span>
                                </template>
                                <el-menu-item v-for="(threeItem, i) in subItem.children" :key="i"
                                    :index="threeItem.index">
                                    {{ threeItem.title }}
                                </el-menu-item>
                            </el-sub-menu>
                            <el-menu-item v-else :index="subItem.index">
                                {{ subItem.title }}
                            </el-menu-item>
                        </template>
                    </el-sub-menu>
                </template>
                <template v-else>
                    <el-menu-item :index="item.index" :key="item.index">
                        <el-icon>
                            <component :is="item.icon"></component>
                        </el-icon>
                        <template #title>{{ item.title }}</template>
                    </el-menu-item>
                </template>
            </template>
        </el-menu>
    </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useSidebarStore } from '@/stores/sidebar';
import { useRoute, useRouter } from 'vue-router';
import { menuData } from '../config/menu';

const route = useRoute();
const router = useRouter();
const onRoutes = computed(() => {
    return route.path;
});

const sidebar = useSidebarStore();

// 手动处理菜单选择，有子菜单的父级不导航
const handleMenuSelect = (index: string) => {
    if (index && index !== '/') {
        router.push(index);
    }
};
</script>

<style scoped>
.sidebar {
    display: block !important;
    position: absolute !important;
    left: 0 !important;
    top: 70px !important;
    bottom: 0 !important;
    overflow-y: scroll !important;
    z-index: 999 !important;
    width: 250px !important;
}

.sidebar::-webkit-scrollbar {
    width: 0;
}

.sidebar-el-menu:not(.el-menu--collapse) {
    width: 250px !important;
}

.sidebar-el-menu {
    min-height: 100% !important;
}
</style>
