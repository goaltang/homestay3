<template>
    <div class="header">
        <div class="collapse-btn" @click="handleCollapse">
            <el-icon v-if="sidebar.collapse">
                <Expand />
            </el-icon>
            <el-icon v-else>
                <Fold />
            </el-icon>
        </div>
        <div class="logo">民宿管理系统</div>
        <div class="header-right">
            <div class="header-user-con">
                <el-dropdown class="user-name" trigger="click" @command="handleCommand">
                    <span class="el-dropdown-link">
                        {{ username }}
                        <el-icon class="el-icon--right">
                            <CaretBottom />
                        </el-icon>
                    </span>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item command="user">个人中心</el-dropdown-item>
                            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CaretBottom, Expand, Fold } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const username = ref(userStore.username || '管理员')

const sidebar = reactive({
    collapse: false
})

const handleCollapse = () => {
    sidebar.collapse = !sidebar.collapse
    emit('collapse', sidebar.collapse)
}

const emit = defineEmits(['collapse'])

const handleCommand = async (command: string) => {
    if (command === 'logout') {
        userStore.logout()
        ElMessage.success('退出成功')
        router.push('/login')
    } else if (command === 'user') {
        router.push('/user')
    }
}
</script>

<style scoped lang="scss">
.header {
    position: relative;
    box-sizing: border-box;
    width: 100%;
    height: 70px;
    font-size: 22px;
    color: #fff;
    background-color: #242f42;

    .collapse-btn {
        float: left;
        width: 60px;
        height: 70px;
        line-height: 70px;
        text-align: center;
        cursor: pointer;

        &:hover {
            background: rgba(255, 255, 255, 0.1);
        }
    }

    .logo {
        float: left;
        line-height: 70px;
    }

    .header-right {
        float: right;
        padding-right: 50px;

        .header-user-con {
            display: flex;
            height: 70px;
            align-items: center;

            .user-name {
                margin-right: 20px;

                .el-dropdown-link {
                    color: #fff;
                    cursor: pointer;
                    display: flex;
                    align-items: center;
                }
            }
        }
    }
}
</style>