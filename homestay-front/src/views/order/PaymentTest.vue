<template>
    <div class="payment-test" v-if="import.meta.env.DEV">
        <el-card class="test-card">
            <template #header>
                <h3>🔧 支付API测试工具</h3>
                <el-alert type="warning" :closable="false">
                    仅在开发环境显示，用于测试支付宝支付功能
                </el-alert>
            </template>

            <el-form :model="testForm" label-width="120px">
                <el-form-item label="订单ID">
                    <el-input-number v-model="testForm.orderId" :min="1" placeholder="请输入订单ID" />
                </el-form-item>

                <el-form-item label="支付方式">
                    <el-radio-group v-model="testForm.method">
                        <el-radio-button label="alipay">支付宝</el-radio-button>
                        <el-radio-button label="wechat" disabled>微信支付</el-radio-button>
                    </el-radio-group>
                </el-form-item>

                <el-form-item>
                    <el-button type="primary" @click="testCreatePayment" :loading="loading">
                        生成支付二维码
                    </el-button>
                    <el-button @click="testCheckStatus" :loading="statusLoading">
                        查询支付状态
                    </el-button>
                    <el-button type="success" @click="testMockPayment" :loading="mockLoading">
                        模拟支付成功
                    </el-button>
                </el-form-item>
            </el-form>

            <div v-if="qrCode" class="qr-result">
                <h4>二维码生成结果：</h4>
                <el-input v-model="qrCode" readonly type="textarea" :rows="3" />
                <p class="tip">可以复制二维码URL到浏览器查看</p>
            </div>

            <div v-if="statusResult" class="status-result">
                <h4>支付状态查询结果：</h4>
                <pre>{{ statusResult }}</pre>
            </div>

            <div class="logs" v-if="logs.length > 0">
                <h4>测试日志：</h4>
                <div class="log-item" v-for="(log, index) in logs" :key="index">
                    <span class="log-time">{{ log.time }}</span>
                    <span :class="['log-type', log.type]">{{ log.type.toUpperCase() }}</span>
                    <span class="log-message">{{ log.message }}</span>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { generatePaymentQRCode, checkPayment, mockPaymentSuccess } from '../../api/order'

const testForm = reactive({
    orderId: 1,
    method: 'alipay'
})

const loading = ref(false)
const statusLoading = ref(false)
const mockLoading = ref(false)
const qrCode = ref('')
const statusResult = ref('')
const logs = ref<Array<{ time: string, type: string, message: string }>>([])

const addLog = (type: string, message: string) => {
    logs.value.unshift({
        time: new Date().toLocaleTimeString(),
        type,
        message
    })
    // 保留最新20条日志
    if (logs.value.length > 20) {
        logs.value = logs.value.slice(0, 20)
    }
}

// 测试生成支付二维码
const testCreatePayment = async () => {
    if (!testForm.orderId) {
        ElMessage.error('请输入订单ID')
        return
    }

    loading.value = true
    addLog('info', `开始测试生成支付二维码: 订单${testForm.orderId}, 方式${testForm.method}`)

    try {
        const response = await generatePaymentQRCode({
            orderId: testForm.orderId,
            method: testForm.method
        })

        if (response.data.success) {
            qrCode.value = response.data.qrCode
            ElMessage.success('二维码生成成功')
            addLog('success', `二维码生成成功: ${response.data.message}`)
        } else {
            ElMessage.error(response.data.message)
            addLog('error', `二维码生成失败: ${response.data.message}`)
        }
    } catch (error: any) {
        ElMessage.error('API调用失败')
        addLog('error', `API调用异常: ${error.message}`)
    } finally {
        loading.value = false
    }
}

// 测试查询支付状态
const testCheckStatus = async () => {
    if (!testForm.orderId) {
        ElMessage.error('请输入订单ID')
        return
    }

    statusLoading.value = true
    addLog('info', `查询支付状态: 订单${testForm.orderId}`)

    try {
        const response = await checkPayment(testForm.orderId)
        statusResult.value = JSON.stringify(response.data, null, 2)

        if (response.data.success) {
            const status = response.data.isPaid ? '已支付' : '未支付'
            ElMessage.success(`查询成功: ${status}`)
            addLog('success', `状态查询成功: ${status}`)
        } else {
            ElMessage.error(response.data.message)
            addLog('error', `状态查询失败: ${response.data.message}`)
        }
    } catch (error: any) {
        ElMessage.error('查询失败')
        addLog('error', `状态查询异常: ${error.message}`)
    } finally {
        statusLoading.value = false
    }
}

// 测试模拟支付成功
const testMockPayment = async () => {
    if (!testForm.orderId) {
        ElMessage.error('请输入订单ID')
        return
    }

    mockLoading.value = true
    addLog('info', `模拟支付成功: 订单${testForm.orderId}`)

    try {
        const response = await mockPaymentSuccess(testForm.orderId)

        if (response.data.success) {
            ElMessage.success('模拟支付成功')
            addLog('success', `模拟支付成功: ${response.data.message}`)
            // 自动查询一次状态确认
            setTimeout(() => {
                testCheckStatus()
            }, 1000)
        } else {
            ElMessage.error(response.data.message)
            addLog('error', `模拟支付失败: ${response.data.message}`)
        }
    } catch (error: any) {
        ElMessage.error('模拟支付失败')
        addLog('error', `模拟支付异常: ${error.message}`)
    } finally {
        mockLoading.value = false
    }
}
</script>

<style scoped>
.payment-test {
    max-width: 800px;
    margin: 20px auto;
    padding: 20px;
}

.test-card {
    margin-bottom: 20px;
}

.qr-result,
.status-result {
    margin-top: 20px;
    padding: 15px;
    background-color: #f5f7fa;
    border-radius: 4px;
}

.tip {
    color: #909399;
    font-size: 12px;
    margin-top: 5px;
}

.logs {
    margin-top: 20px;
    padding: 15px;
    background-color: #f8f9fa;
    border-radius: 4px;
    max-height: 300px;
    overflow-y: auto;
}

.log-item {
    display: flex;
    gap: 10px;
    margin-bottom: 5px;
    font-family: monospace;
    font-size: 12px;
}

.log-time {
    color: #909399;
    min-width: 80px;
}

.log-type {
    min-width: 60px;
    font-weight: bold;
}

.log-type.success {
    color: #67c23a;
}

.log-type.error {
    color: #f56c6c;
}

.log-type.info {
    color: #409eff;
}

.log-message {
    flex: 1;
}

pre {
    background-color: #f5f7fa;
    padding: 10px;
    border-radius: 4px;
    overflow-x: auto;
    font-size: 12px;
}
</style>