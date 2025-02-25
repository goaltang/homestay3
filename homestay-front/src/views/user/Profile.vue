<template>
    <div class="profile-container">
        <el-card class="profile-card">
            <template #header>
                <div class="card-header">
                    <span>个人信息</span>
                    <el-button type="primary" @click="isEditing = true" v-if="!isEditing">
                        编辑资料
                    </el-button>
                    <div v-else>
                        <el-button type="success" @click="handleSave">保存</el-button>
                        <el-button @click="handleCancel">取消</el-button>
                    </div>
                </div>
            </template>

            <el-form ref="formRef" :model="userForm" :rules="rules" label-width="100px" :disabled="!isEditing">
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="userForm.username" />
                </el-form-item>

                <el-form-item label="邮箱" prop="email">
                    <el-input v-model="userForm.email" />
                </el-form-item>

                <el-form-item label="手机号码" prop="phone">
                    <el-input v-model="userForm.phone" />
                </el-form-item>

                <el-form-item label="真实姓名" prop="realName">
                    <el-input v-model="userForm.realName" />
                </el-form-item>

                <el-form-item label="身份证号" prop="idCard">
                    <el-input v-model="userForm.idCard" />
                </el-form-item>
            </el-form>
        </el-card>

        <el-card class="security-card">
            <template #header>
                <div class="card-header">
                    <span>账号安全</span>
                </div>
            </template>

            <div class="security-items">
                <div class="security-item">
                    <span>修改密码</span>
                    <el-button type="primary" text @click="showChangePassword = true">
                        修改
                    </el-button>
                </div>
            </div>
        </el-card>

        <!-- 修改密码对话框 -->
        <el-dialog v-model="showChangePassword" title="修改密码" width="400px">
            <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px">
                <el-form-item label="原密码" prop="oldPassword">
                    <el-input v-model="passwordForm.oldPassword" type="password" show-password />
                </el-form-item>

                <el-form-item label="新密码" prop="newPassword">
                    <el-input v-model="passwordForm.newPassword" type="password" show-password />
                </el-form-item>

                <el-form-item label="确认密码" prop="confirmPassword">
                    <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="showChangePassword = false">取消</el-button>
                    <el-button type="primary" @click="handleChangePassword">
                        确认修改
                    </el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import type { FormInstance } from "element-plus";

const userStore = useUserStore();
const isEditing = ref(false);
const showChangePassword = ref(false);

// 表单引用
const formRef = ref<FormInstance>();
const passwordFormRef = ref<FormInstance>();

// 用户表单数据
const userForm = reactive({
    username: userStore.userInfo.username,
    email: userStore.userInfo.email,
    phone: userStore.userInfo.phone || "",
    realName: userStore.userInfo.realName || "",
    idCard: userStore.userInfo.idCard || "",
});

// 密码表单数据
const passwordForm = reactive({
    oldPassword: "",
    newPassword: "",
    confirmPassword: "",
});

// 表单验证规则
const rules = {
    username: [
        { required: true, message: "请输入用户名", trigger: "blur" },
        { min: 3, message: "用户名长度不能小于3个字符", trigger: "blur" },
    ],
    email: [
        { required: true, message: "请输入邮箱地址", trigger: "blur" },
        { type: "email", message: "请输入正确的邮箱地址", trigger: "blur" },
    ],
    phone: [
        {
            pattern: /^1[3-9]\d{9}$/,
            message: "请输入正确的手机号码",
            trigger: "blur",
        },
    ],
    idCard: [
        {
            pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/,
            message: "请输入正确的身份证号码",
            trigger: "blur",
        },
    ],
};

const passwordRules = {
    oldPassword: [
        { required: true, message: "请输入原密码", trigger: "blur" },
        { min: 6, message: "密码长度不能小于6个字符", trigger: "blur" },
    ],
    newPassword: [
        { required: true, message: "请输入新密码", trigger: "blur" },
        { min: 6, message: "密码长度不能小于6个字符", trigger: "blur" },
    ],
    confirmPassword: [
        { required: true, message: "请确认密码", trigger: "blur" },
        {
            validator: (rule: any, value: string, callback: Function) => {
                if (value !== passwordForm.newPassword) {
                    callback(new Error("两次输入的密码不一致"));
                } else {
                    callback();
                }
            },
            trigger: "blur",
        },
    ],
};

// 保存个人信息
const handleSave = async () => {
    if (!formRef.value) return;

    await formRef.value.validate(async (valid) => {
        if (valid) {
            try {
                await userStore.updateProfile(userForm);
                ElMessage.success("保存成功");
                isEditing.value = false;
            } catch (error: any) {
                ElMessage.error(error.message || "保存失败");
            }
        }
    });
};

// 取消编辑
const handleCancel = () => {
    userForm.username = userStore.userInfo.username;
    userForm.email = userStore.userInfo.email;
    userForm.phone = userStore.userInfo.phone || "";
    userForm.realName = userStore.userInfo.realName || "";
    userForm.idCard = userStore.userInfo.idCard || "";
    isEditing.value = false;
};

// 修改密码
const handleChangePassword = async () => {
    if (!passwordFormRef.value) return;

    await passwordFormRef.value.validate(async (valid) => {
        if (valid) {
            try {
                await userStore.changePassword(passwordForm);
                ElMessage.success("密码修改成功");
                showChangePassword.value = false;
                passwordForm.oldPassword = "";
                passwordForm.newPassword = "";
                passwordForm.confirmPassword = "";
            } catch (error: any) {
                ElMessage.error(error.message || "密码修改失败");
            }
        }
    });
};
</script>

<style scoped>
.profile-container {
    max-width: 800px;
    margin: 20px auto;
    padding: 0 20px;
}

.profile-card,
.security-card {
    margin-bottom: 20px;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.security-items {
    padding: 10px 0;
}

.security-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 0;
    border-bottom: 1px solid #eee;
}

.security-item:last-child {
    border-bottom: none;
}
</style>