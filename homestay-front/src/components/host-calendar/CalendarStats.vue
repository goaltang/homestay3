<script setup lang="ts">
import type { HostCalendarSummary } from "@/api/hostCalendar";

const props = defineProps<{
  summary: HostCalendarSummary;
}>();

const stats = [
  { key: "availableCount", label: "可订", tone: "available" },
  { key: "bookedCount", label: "已订", tone: "booked" },
  { key: "pendingCount", label: "待确认", tone: "pending" },
  { key: "unavailableCount", label: "不可订", tone: "unavailable" },
  { key: "checkInCount", label: "入住", tone: "checkin" },
  { key: "checkOutCount", label: "退房", tone: "checkout" },
] as const;

function formatCurrency(value: number | null | undefined) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    maximumFractionDigits: 0,
  }).format(value ?? 0);
}
</script>

<template>
  <div class="stats-grid">
    <div v-for="item in stats" :key="item.key" class="stat-item" :class="item.tone">
      <span class="stat-label">{{ item.label }}</span>
      <strong class="stat-value">{{ props.summary[item.key] ?? 0 }}</strong>
    </div>
    <div class="stat-item revenue">
      <span class="stat-label">预计收入</span>
      <strong class="stat-value">{{ formatCurrency(props.summary.estimatedRevenue) }}</strong>
    </div>
  </div>
</template>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(112px, 1fr));
  gap: 10px;
}

.stat-item {
  min-height: 76px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-left-width: 4px;
  border-radius: 8px;
  background: #fff;
}

.stat-label {
  display: block;
  color: #6b7280;
  font-size: 13px;
}

.stat-value {
  display: block;
  margin-top: 8px;
  color: #111827;
  font-size: 22px;
  line-height: 1.2;
}

.available {
  border-left-color: #16a34a;
}

.booked {
  border-left-color: #2563eb;
}

.pending {
  border-left-color: #d97706;
}

.unavailable {
  border-left-color: #dc2626;
}

.checkin {
  border-left-color: #0891b2;
}

.checkout {
  border-left-color: #7c3aed;
}

.revenue {
  border-left-color: #475569;
}

@media (max-width: 1180px) {
  .stats-grid {
    grid-template-columns: repeat(4, minmax(112px, 1fr));
  }
}

@media (max-width: 720px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
