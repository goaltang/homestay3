<template>
    <div class="common-layout">
        <v-header @collapse="handleCollapse"></v-header>
        <v-sidebar :collapse="isCollapse"></v-sidebar>
        <div class="content-box" :class="{ 'content-collapse': isCollapse }">
            <div class="content">
                <router-view v-slot="{ Component }">
                    <transition name="fade" mode="out-in">
                        <component :is="Component" />
                    </transition>
                </router-view>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import vHeader from '@/components/header.vue'
import vSidebar from '@/components/sidebar.vue'

const isCollapse = ref(false)
const handleCollapse = (val: boolean) => {
    isCollapse.value = val
}
</script>

<style scoped>
.common-layout {
    position: relative;
    width: 100%;
    height: 100%;
}

.content-box {
    position: absolute;
    left: 250px;
    right: 0;
    top: 70px;
    bottom: 0;
    padding: 20px;
    transition: left 0.3s ease-in-out;
    background: #f0f2f5;
}

.content {
    width: 100%;
    height: 100%;
    padding: 20px;
    box-sizing: border-box;
    background: #fff;
    border-radius: 4px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.content-collapse {
    left: 65px;
}

/* 路由切换动画 */
.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}
</style>