<template>
    <el-container class="app-container">
        <Header />
        <el-main class="main-content">
            <router-view></router-view>
        </el-main>
        <el-footer>
            <div class="footer-content">
                © 2024 民宿预订系统 All Rights Reserved.
            </div>
        </el-footer>
    </el-container>
</template>

<script setup lang="ts">
import Header from '@/components/Header.vue'
</script>

<style scoped>
.app-container {
    min-height: 100%;
    display: flex;
    flex-direction: column;
}

.main-content {
    padding-top: 80px;
    flex: 1;
    background-color: #f5f7fa;
}

.el-footer {
    padding: 20px 0;
    background-color: #fff;
    border-top: 1px solid #e6e6e6;
}

.footer-content {
    text-align: center;
    color: #909399;
    font-size: 14px;
}
</style>