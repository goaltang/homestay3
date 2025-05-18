<template>
    <footer class="app-footer">
        <div class="footer-content">
            <div class="footer-section">
                <h3>关于我们</h3>
                <ul>
                    <li>公司简介</li>
                    <li>加入我们</li>
                    <li>新闻中心</li>
                </ul>
            </div>
            <div class="footer-section">
                <h3>帮助中心</h3>
                <ul>
                    <li>预订指南</li>
                    <li>常见问题</li>
                    <li>联系客服</li>
                </ul>
            </div>
            <div class="footer-section">
                <h3>商务合作</h3>
                <ul>
                    <li>房东申请</li>
                    <li>广告合作</li>
                    <li>战略合作</li>
                </ul>
            </div>
            <div class="footer-section">
                <h3>法律声明</h3>
                <ul>
                    <li>隐私政策</li>
                    <li>用户协议</li>
                    <li>Cookie政策</li>
                </ul>
            </div>
        </div>
        <div class="footer-bottom">
            <p>© 2025 民宿预订平台 版权所有</p>
        </div>
    </footer>
</template>

<script setup lang="ts">
// 页脚通常是静态的，不需要太多逻辑
</script>

<style scoped>
/* 从 App.vue 移过来的样式 */
.app-footer {
    background-color: var(--background-light);
    padding: 48px 0 24px;
    margin-top: 48px;
}

.footer-content {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 24px;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 32px;
}

.footer-section h3 {
    margin-bottom: 16px;
    font-size: 16px;
}

.footer-section ul {
    list-style: none;
    padding: 0;
    /* 重置可能继承的 padding */
}

.footer-section li {
    margin-bottom: 8px;
    color: var(--text-light);
    cursor: pointer;
}

.footer-section li:hover {
    text-decoration: underline;
}

.footer-bottom {
    max-width: 1200px;
    margin: 0 auto;
    padding: 24px;
    text-align: center;
    border-top: 1px solid var(--border-color);
    margin-top: 32px;
}

.footer-bottom p {
    color: var(--text-light);
    font-size: 14px;
}

/* 响应式调整 */
@media (max-width: 768px) {
    .footer-content {
        grid-template-columns: repeat(2, 1fr);
    }
}

@media (max-width: 480px) {
    .footer-content {
        grid-template-columns: 1fr;
    }
}
</style>