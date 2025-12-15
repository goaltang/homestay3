<template>
    <div class="order-debug" v-if="import.meta.env.DEV">
        <el-card class="debug-card">
            <template #header>
                <h3>🔧 订单创建调试工具</h3>
                <el-alert type="warning" :closable="false">
                    仅在开发环境显示，用于调试订单创建问题
                </el-alert>
            </template>

            <el-form :model="debugForm" label-width="120px">
                <el-form-item label="房源ID">
                    <el-input-number v-model="debugForm.homestayId" :min="1" placeholder="请输入房源ID" />
                </el-form-item>

                <el-form-item label="入住日期">
                    <el-date-picker v-model="debugForm.checkInDate" type="date" placeholder="选择入住日期" format="YYYY-MM-DD"
                        value-format="YYYY-MM-DD" />
                </el-form-item>

                <el-form-item label="退房日期">
                    <el-date-picker v-model="debugForm.checkOutDate" type="date" placeholder="选择退房日期"
                        format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
                </el-form-item>

                <el-form-item label="客人数量">
                    <el-input-number v-model="debugForm.guestCount" :min="1" :max="10" />
                </el-form-item>

                <el-form-item>
                    <el-button type="primary" @click="checkAvailability" :loading="checking">
                        检查日期可用性
                    </el-button>
                    <el-button type="success" @click="createOrder" :loading="creating">
                        创建订单
                    </el-button>
                    <el-button @click="clearResults">清空结果</el-button>
                </el-form-item>
            </el-form>

            <!-- 检查结果 -->
            <div v-if="availabilityResult" class="result-section">
                <h4>日期可用性检查结果</h4>
                <el-descriptions :column="2" border>
                    <el-descriptions-item label="房源ID">{{ availabilityResult.homestayId }}</el-descriptions-item>
                    <el-descriptions-item label="入住日期">{{ availabilityResult.checkInDate }}</el-descriptions-item>
                    <el-descriptions-item label="退房日期">{{ availabilityResult.checkOutDate }}</el-descriptions-item>
                    <el-descriptions-item label="总订单数">{{ availabilityResult.totalOrders }}</el-descriptions-item>
                    <el-descriptions-item label="是否冲突" :span="2">
                        <el-tag :type="availabilityResult.hasConflict ? 'danger' : 'success'">
                            {{ availabilityResult.hasConflict ? '有冲突' : '可预订' }}
                        </el-tag>
                    </el-descriptions-item>
                </el-descriptions>

                <div v-if="availabilityResult.conflictOrders?.length > 0" class="conflict-orders">
                    <h5>冲突订单：</h5>
                    <el-tag v-for="order in availabilityResult.conflictOrders" :key="order" type="danger"
                        class="conflict-tag">
                        {{ order }}
                    </el-tag>
                </div>

                <div v-if="availabilityResult.existingOrders?.length > 0" class="existing-orders">
                    <h5>现有订单：</h5>
                    <el-table :data="availabilityResult.existingOrders" size="small">
                        <el-table-column prop="orderNumber" label="订单号" width="150" />
                        <el-table-column prop="status" label="状态" width="100">
                            <template #default="scope">
                                <el-tag :type="getStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="checkInDate" label="入住日期" width="120" />
                        <el-table-column prop="checkOutDate" label="退房日期" width="120" />
                        <el-table-column prop="guestCount" label="客人数" width="80" />
                    </el-table>
                </div>
            </div>

            <!-- 创建订单结果 -->
            <div v-if="createResult" class="result-section">
                <h4>订单创建结果</h4>
                <el-alert :type="createResult.success ? 'success' : 'error'"
                    :title="createResult.success ? '订单创建成功' : '订单创建失败'" :description="createResult.message" show-icon />
                <div v-if="createResult.success && createResult.order" class="order-info">
                    <p><strong>订单号：</strong>{{ createResult.order.orderNumber }}</p>
                    <p><strong>订单ID：</strong>{{ createResult.order.id }}</p>
                    <p><strong>总金额：</strong>¥{{ createResult.order.totalAmount }}</p>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const debugForm = reactive({
    homestayId: 1,
    checkInDate: '',
    checkOutDate: '',
    guestCount: 2
})

const checking = ref(false)
const creating = ref(false)
const availabilityResult = ref(null)
const createResult = ref(null)

const checkAvailability = async () => {
    if (!debugForm.homestayId || !debugForm.checkInDate || !debugForm.checkOutDate) {
        ElMessage.warning('请填写完整信息')
        return
    }

    checking.value = true
    try {
        const response = await request({
            url: '/api/orders/check-availability',
            method: 'post',
            data: {
                homestayId: debugForm.homestayId,
                checkInDate: debugForm.checkInDate,
                checkOutDate: debugForm.checkOutDate
            }
        })
        availabilityResult.value = response.data
        ElMessage.success('检查完成')
    } catch (error: any) {
        ElMessage.error('检查失败：' + (error.response?.data?.error || error.message))
    } finally {
        checking.value = false
    }
}

const createOrder = async () => {
    if (!debugForm.homestayId || !debugForm.checkInDate || !debugForm.checkOutDate) {
        ElMessage.warning('请填写完整信息')
        return
    }

    creating.value = true
    try {
        const response = await request({
            url: '/api/orders',
            method: 'post',
            data: {
                homestayId: debugForm.homestayId,
                checkInDate: debugForm.checkInDate,
                checkOutDate: debugForm.checkOutDate,
                guestCount: debugForm.guestCount
            }
        })
        createResult.value = {
            success: true,
            message: '订单创建成功',
            order: response.data
        }
        ElMessage.success('订单创建成功')
    } catch (error: any) {
        createResult.value = {
            success: false,
            message: error.response?.data?.error || error.message
        }
        ElMessage.error('订单创建失败：' + (error.response?.data?.error || error.message))
    } finally {
        creating.value = false
    }
}

const clearResults = () => {
    availabilityResult.value = null
    createResult.value = null
}

const getStatusType = (status: string) => {
    const statusMap: Record<string, string> = {
        'PENDING': 'warning',
        'CONFIRMED': 'primary',
        'PAID': 'success',
        'CHECKED_IN': 'info',
        'COMPLETED': 'success',
        'CANCELLED': 'danger'
    }
    return statusMap[status] || 'info'
}
</script>

<style scoped>
.order-debug {
    max-width: 1000px;
    margin: 20px auto;
    padding: 20px;
}

.debug-card {
    margin-bottom: 20px;
}

.result-section {
    margin-top: 20px;
    padding: 15px;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    background: #fafafa;
}

.conflict-orders,
.existing-orders {
    margin-top: 15px;
}

.conflict-tag {
    margin-right: 8px;
    margin-bottom: 4px;
}

.order-info {
    margin-top: 10px;
    padding: 10px;
    background: #f0f9ff;
    border-radius: 4px;
}
</style>