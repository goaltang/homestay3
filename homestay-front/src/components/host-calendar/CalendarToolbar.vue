<script setup lang="ts">
import { computed } from "vue";
import { ArrowLeft, ArrowRight, Calendar, Refresh, Download } from "@element-plus/icons-vue";
import type { HostHomestayOption } from "@/composables/useHostCalendar";

const props = defineProps<{
  monthLabel: string;
  selectedHomestayId: number | null;
  homestayOptions: HostHomestayOption[];
  loading: boolean;
}>();

const emit = defineEmits<{
  "update:selectedHomestayId": [value: number | null];
  prev: [];
  next: [];
  today: [];
  refresh: [];
  export: [];
}>();

const selectedValue = computed<number | null>({
  get: () => props.selectedHomestayId,
  set: (value) => emit("update:selectedHomestayId", value ?? null),
});

const selectOptions = computed(() =>
  props.homestayOptions.map((item) => ({ label: item.title, value: item.id }))
);
</script>

<template>
  <div class="calendar-toolbar">
    <div class="month-controls">
      <el-button :icon="ArrowLeft" circle aria-label="上个月" @click="emit('prev')" />
      <div class="month-title">{{ monthLabel }}</div>
      <el-button :icon="ArrowRight" circle aria-label="下个月" @click="emit('next')" />
      <el-button :icon="Calendar" @click="emit('today')">本月</el-button>
    </div>

    <div class="toolbar-actions">
      <el-select-v2
        v-model="selectedValue"
        :options="selectOptions"
        clearable
        filterable
        placeholder="搜索并选择房源"
        class="homestay-select"
      />
      <el-button :icon="Refresh" :loading="loading" @click="emit('refresh')">刷新</el-button>
      <el-button :icon="Download" @click="emit('export')">导出</el-button>
    </div>
  </div>
</template>

<style scoped>
.calendar-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.month-controls,
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.month-title {
  min-width: 112px;
  text-align: center;
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
}

.homestay-select {
  width: 240px;
}

@media (max-width: 720px) {
  .calendar-toolbar,
  .month-controls,
  .toolbar-actions {
    width: 100%;
  }

  .toolbar-actions {
    align-items: stretch;
  }

  .homestay-select {
    flex: 1;
    min-width: 0;
  }
}
</style>
