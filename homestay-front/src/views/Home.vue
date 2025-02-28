<template>
  <div class="home-container">
    <el-row :gutter="20" class="welcome-section">
      <el-col :span="24">
        <h1>欢迎来到民宿预订系统</h1>
        <p>探索世界各地的精品民宿，开启您的旅行</p>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.welcome-section {
  text-align: center;
  margin: 40px 0;
}

h1 {
  color: var(--el-color-primary);
  margin-bottom: 20px;
}

p {
  color: var(--el-text-color-secondary);
  font-size: 18px;
}
</style>