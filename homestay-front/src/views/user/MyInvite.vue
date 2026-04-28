<template>
  <div class="my-invite-container">
    <h2 class="page-title">我的邀请</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-value">{{ stats.totalCodes || 0 }}</div>
          <div class="stat-label">我的邀请码</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-value">{{ stats.totalUsed || 0 }}</div>
          <div class="stat-label">已被使用</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-value">{{ stats.activeCodes || 0 }}</div>
          <div class="stat-label">活跃中</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 使用邀请码 -->
    <el-card class="action-card">
      <template #header>
        <span>使用邀请码</span>
      </template>
      <p class="tip-text">如果你有朋友分享的邀请码，可以在此输入领取奖励。</p>
      <el-input v-model="referralCode" placeholder="请输入邀请码" clearable style="width: 280px" />
      <el-button type="primary" @click="claimReferral" :loading="claiming" style="margin-left: 12px">领取奖励</el-button>
    </el-card>

    <!-- 邀请说明 -->
    <el-card class="info-card">
      <template #header>
        <span>邀请奖励说明</span>
      </template>
      <ul class="info-list">
        <li>邀请好友注册，双方均可获得优惠券奖励。</li>
        <li>每个邀请码有使用次数上限，请在有效期内使用。</li>
        <li>奖励优惠券将自动发放到您的账户中，可在下单时使用。</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'

const stats = ref<any>({})
const referralCode = ref('')
const claiming = ref(false)
const loading = ref(false)

const fetchStats = async () => {
  loading.value = true
  try {
    const res = await api.get('/api/coupons/referral/stats')
    stats.value = res.data || {}
  } catch (e) {
    console.error('获取邀请统计失败', e)
  } finally {
    loading.value = false
  }
}

const claimReferral = async () => {
  if (!referralCode.value.trim()) {
    ElMessage.warning('请输入邀请码')
    return
  }
  claiming.value = true
  try {
    const res = await api.post(`/api/coupons/referral/${referralCode.value.trim()}/claim`)
    ElMessage.success(res.data?.message || '领取成功')
    referralCode.value = ''
    fetchStats()
  } catch (e: any) {
    const msg = e?.response?.data?.error || '领取失败，请检查邀请码是否有效'
    ElMessage.error(msg)
  } finally {
    claiming.value = false
  }
}

onMounted(fetchStats)
</script>

<style scoped>
.my-invite-container {
  padding: 8px;
}
.page-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 20px;
  color: #303133;
}
.stats-row {
  margin-bottom: 20px;
}
.stat-card {
  text-align: center;
}
.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--el-color-primary);
  margin-bottom: 8px;
}
.stat-label {
  font-size: 14px;
  color: #606266;
}
.action-card {
  margin-bottom: 20px;
}
.tip-text {
  color: #606266;
  margin-bottom: 16px;
  font-size: 14px;
}
.info-card {
  margin-bottom: 20px;
}
.info-list {
  padding-left: 20px;
  color: #606266;
  line-height: 2;
  font-size: 14px;
}
</style>
