<template>
    <div class="sidebar">
        <el-menu class="sidebar-el-menu" :default-active="route.path" :collapse="isCollapse" background-color="#324157"
            text-color="#bfcbd9" active-text-color="#20a0ff" unique-opened router>
            <template v-for="item in items">
                <template v-if="item.children">
                    <el-sub-menu :index="item.index" :key="item.index">
                        <template #title>
                            <el-icon>
                                <component :is="item.icon" />
                            </el-icon>
                            <span>{{ item.title }}</span>
                        </template>
                        <template v-for="subItem in item.children">
                            <el-menu-item :index="subItem.index" :key="subItem.index">
                                {{ subItem.title }}
                            </el-menu-item>
                        </template>
                    </el-sub-menu>
                </template>
                <template v-else>
                    <el-menu-item :index="item.index" :key="item.index">
                        <el-icon>
                            <component :is="item.icon" />
                        </el-icon>
                        <template #title>{{ item.title }}</template>
                    </el-menu-item>
                </template>
            </template>
        </el-menu>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRoute } from 'vue-router'
import {
    House,
    Document,
    Setting,
    User,
    List,
    ShoppingCart
} from '@element-plus/icons-vue'

const route = useRoute()
const isCollapse = defineProps<{
    collapse: boolean
}>()

const items = [
    {
        icon: House,
        index: '/dashboard',
        title: '系统首页'
    },
    {
        icon: List,
        index: '/homestay',
        title: '房源管理'
    },
    {
        icon: ShoppingCart,
        index: '/order',
        title: '订单管理'
    },
    {
        icon: User,
        index: '/user',
        title: '用户管理'
    },
    {
        icon: Setting,
        index: '/setting',
        title: '系统设置'
    }
]
</script>

<style scoped lang="scss">
.sidebar {
    display: block;
    position: absolute;
    left: 0;
    top: 70px;
    bottom: 0;
    overflow-y: scroll;
    background-color: #324157;

    &::-webkit-scrollbar {
        width: 0;
    }

    .sidebar-el-menu {
        width: 100%;
        border-right: none;

        &:not(.el-menu--collapse) {
            width: 250px;
        }
    }
}
</style>