<template>
    <div class="error-page">
        <div class="error-content">
            <div class="error-image">
                <img src="@/assets/404.png" alt="404"
                    onerror="this.src='data:image/svg+xml;charset=UTF-8,%3Csvg%20width%3D%22800%22%20height%3D%22400%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%20800%20400%22%20preserveAspectRatio%3D%22none%22%3E%3Cdefs%3E%3Cstyle%20type%3D%22text%2Fcss%22%3E%23holder_15ba800aa20%20text%20%7B%20fill%3A%23444%3Bfont-weight%3Anormal%3Bfont-family%3AHelvetica%2C%20monospace%3Bfont-size%3A40pt%20%7D%20%3C%2Fstyle%3E%3C%2Fdefs%3E%3Cg%20id%3D%22holder_15ba800aa20%22%3E%3Crect%20width%3D%22800%22%20height%3D%22400%22%20fill%3D%22%23666%22%3E%3C%2Frect%3E%3Cg%3E%3Ctext%20x%3D%22247.3203125%22%20y%3D%22218.3%22%3E404%3C%2Ftext%3E%3C%2Fg%3E%3C%2Fg%3E%3C%2Fsvg%3E'" />
            </div>
            <h1>404</h1>
            <h2>页面不存在</h2>
            <p>抱歉，您访问的页面不存在或已被删除</p>
            <el-button type="primary" @click="goHome">返回首页</el-button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

const goHome = () => {
    router.push('/');
};
</script>

<style scoped>
.error-page {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
    background-color: #f5f7fa;
}

.error-content {
    text-align: center;
    padding: 30px;
    max-width: 500px;
    background-color: white;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.error-image {
    margin-bottom: 20px;
}

.error-image img {
    max-width: 200px;
    height: auto;
}

h1 {
    font-size: 72px;
    margin: 0;
    color: #1E88E5;
    line-height: 1;
}

h2 {
    margin: 10px 0 20px;
    color: #333;
    font-weight: 500;
}

p {
    margin-bottom: 30px;
    color: #666;
}
</style>