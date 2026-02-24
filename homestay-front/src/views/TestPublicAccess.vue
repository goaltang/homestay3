<template>
    <div class="test-page">
        <el-card>
            <template #header>
                <span>未登录用户公共API访问测试</span>
            </template>

            <div class="test-section">
                <h3>API测试结果</h3>
                <div class="api-test" v-for="test in apiTests" :key="test.name">
                    <el-row :gutter="10" style="margin-bottom: 10px;">
                        <el-col :span="8">
                            <strong>{{ test.name }}</strong>
                        </el-col>
                        <el-col :span="4">
                            <el-tag
                                :type="test.status === 'success' ? 'success' : test.status === 'error' ? 'danger' : 'info'">
                                {{ test.status === 'success' ? '成功' : test.status === 'error' ? '失败' : '测试中' }}
                            </el-tag>
                        </el-col>
                        <el-col :span="12">
                            <span class="test-message">{{ test.message }}</span>
                        </el-col>
                    </el-row>
                </div>
            </div>

            <div class="test-section" style="margin-top: 20px;">
                <el-button type="primary" @click="runAllTests">重新测试所有API</el-button>
                <el-button @click="clearResults">清空结果</el-button>
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getRecommendedHomestays, getPopularHomestays } from '@/api/recommendation'
import { getHomestayTypes, getAvailableAmenitiesGrouped, getProvinces } from '@/api/homestay'
import { ElMessage } from 'element-plus'

interface ApiTestResult {
    success: boolean
    data: any
}

interface ApiTest {
    name: string
    status: 'pending' | 'success' | 'error'
    message: string
    testFunction: () => Promise<ApiTestResult>
}

const apiTests = ref<ApiTest[]>([
    {
        name: '推荐民宿API',
        status: 'pending',
        message: '等待测试...',
        testFunction: async (): Promise<ApiTestResult> => {
            const result = await getRecommendedHomestays(3)
            return { success: true, data: result.data }
        }
    },
    {
        name: '热门民宿API',
        status: 'pending',
        message: '等待测试...',
        testFunction: async (): Promise<ApiTestResult> => {
            const result = await getPopularHomestays(3)
            return { success: true, data: result.data }
        }
    },
    {
        name: '房源类型API',
        status: 'pending',
        message: '等待测试...',
        testFunction: async (): Promise<ApiTestResult> => {
            const result = await getHomestayTypes()
            return { success: true, data: result }
        }
    },
    {
        name: '设施分组API',
        status: 'pending',
        message: '等待测试...',
        testFunction: async (): Promise<ApiTestResult> => {
            const result = await getAvailableAmenitiesGrouped()
            return { success: true, data: result }
        }
    },
    {
        name: '省份信息API',
        status: 'pending',
        message: '等待测试...',
        testFunction: async (): Promise<ApiTestResult> => {
            const result = await getProvinces()
            return { success: true, data: result.data }
        }
    }
])

const runTest = async (test: ApiTest) => {
    test.status = 'pending'
    test.message = '测试中...'

    try {
        const result = await test.testFunction()
        test.status = 'success'
        test.message = `成功获取 ${Array.isArray(result.data) ? result.data.length : '1'} 条数据`
    } catch (error: any) {
        test.status = 'error'
        test.message = `失败: ${error.message || '未知错误'}`
        console.error(`${test.name} 测试失败:`, error)
    }
}

const runAllTests = async () => {
    ElMessage.info('开始测试所有API...')

    for (const test of apiTests.value) {
        await runTest(test)
    }

    const successCount = apiTests.value.filter(t => t.status === 'success').length
    const totalCount = apiTests.value.length

    if (successCount === totalCount) {
        ElMessage.success(`所有API测试通过！(${successCount}/${totalCount})`)
    } else {
        ElMessage.warning(`API测试完成: ${successCount}/${totalCount} 成功`)
    }
}

const clearResults = () => {
    apiTests.value.forEach(test => {
        test.status = 'pending'
        test.message = '等待测试...'
    })
}

onMounted(() => {
    // 自动运行测试
    setTimeout(() => {
        runAllTests()
    }, 1000)
})
</script>

<style scoped>
.test-page {
    max-width: 800px;
    margin: 20px auto;
    padding: 20px;
}

.test-section {
    margin-bottom: 20px;
}

.api-test {
    padding: 8px;
    border-bottom: 1px solid #f0f0f0;
}

.test-message {
    color: #666;
    font-size: 14px;
}
</style>