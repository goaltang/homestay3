<template>
  <el-container class="app-container">
    <Header />
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import Header from "@/components/Header.vue";
</script>

<style>
html,
body {
  margin: 0;
  padding: 0;
  height: 100%;
}

#app {
  height: 100%;
}

.app-container {
  min-height: 100vh;
}

.el-main {
  padding: 20px;
  background-color: #f5f5f5;
}
</style>