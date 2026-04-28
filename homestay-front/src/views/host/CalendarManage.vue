<script setup lang="ts">
import { computed, onMounted, reactive, ref, shallowRef } from "vue";
import { ElMessage } from "element-plus";
import CalendarStats from "@/components/host-calendar/CalendarStats.vue";
import CalendarToolbar from "@/components/host-calendar/CalendarToolbar.vue";
import HostCalendarGrid from "@/components/host-calendar/HostCalendarGrid.vue";
import {
  addCalendarDays,
  useHostCalendar,
} from "@/composables/useHostCalendar";
import type { HostCalendarDay, HostCalendarStatus } from "@/api/hostCalendar";

type EditableStatus = "AVAILABLE" | "UNAVAILABLE";

const {
  currentMonth,
  selectedHomestayId,
  homestayOptions,
  days,
  summary,
  loading,
  saving,
  monthLabel,
  initialize,
  loadCalendar,
  saveAvailability,
  goPrevMonth,
  goNextMonth,
  goToday,
} = useHostCalendar();

const drawerVisible = shallowRef(false);
const selectedDate = shallowRef("");
const selectedEntries = ref<HostCalendarDay[]>([]);

const form = reactive<{
  homestayId?: number;
  dateRange: string[];
  status: EditableStatus;
  reason: string;
  note: string;
}>({
  homestayId: undefined,
  dateRange: [],
  status: "UNAVAILABLE",
  reason: "",
  note: "",
});

const statusLabels: Record<HostCalendarStatus, string> = {
  AVAILABLE: "可订",
  PENDING_CONFIRM: "待确认",
  BOOKED: "已订",
  CHECKED_IN: "入住中",
  CHECKED_OUT: "已退房",
  UNAVAILABLE: "不可订",
  LOCKED: "锁定中",
};

const drawerTitle = computed(() => {
  return selectedDate.value ? `${selectedDate.value} 日历调整` : "日历调整";
});

const selectedEntry = computed(() => {
  return selectedEntries.value.find((entry) => entry.homestayId === form.homestayId);
});

const selectedHomestayName = computed(() => {
  const option = homestayOptions.value.find((item) => item.id === form.homestayId);
  return option?.title ?? selectedEntry.value?.homestayTitle ?? "";
});

function getStatusLabel(status: HostCalendarStatus) {
  return statusLabels[status] ?? status;
}

function handleDayClick(payload: { date: string; entries: HostCalendarDay[] }) {
  if (homestayOptions.value.length === 0) {
    ElMessage.warning("暂无可管理房源");
    return;
  }

  selectedDate.value = payload.date;
  selectedEntries.value = payload.entries;
  form.homestayId =
    selectedHomestayId.value ?? payload.entries[0]?.homestayId ?? homestayOptions.value[0]?.id;
  form.dateRange = [payload.date, payload.date];
  form.status = "UNAVAILABLE";
  form.reason = selectedEntry.value?.reason ?? "";
  form.note = selectedEntry.value?.note ?? "";
  drawerVisible.value = true;
}

async function submitAvailability() {
  if (!form.homestayId) {
    ElMessage.warning("请选择房源");
    return;
  }

  if (form.dateRange.length !== 2) {
    ElMessage.warning("请选择日期范围");
    return;
  }

  const ok = await saveAvailability({
    homestayId: form.homestayId,
    startDate: form.dateRange[0],
    endDate: addCalendarDays(form.dateRange[1], 1),
    status: form.status,
    reason: form.status === "UNAVAILABLE" ? form.reason.trim() : undefined,
    note: form.status === "UNAVAILABLE" ? form.note.trim() : undefined,
  });
  if (ok) {
    drawerVisible.value = false;
  }
}

onMounted(() => {
  void initialize();
});
</script>

<template>
  <div class="calendar-page">
    <div class="page-heading">
      <h1>房东日历</h1>
    </div>

    <section class="calendar-shell">
      <CalendarToolbar
        v-model:selected-homestay-id="selectedHomestayId"
        :month-label="monthLabel"
        :homestay-options="homestayOptions"
        :loading="loading"
        @prev="goPrevMonth"
        @next="goNextMonth"
        @today="goToday"
        @refresh="loadCalendar"
      />

      <CalendarStats :summary="summary" />

      <div class="legend-row">
        <span class="legend available">可订</span>
        <span class="legend pending">待确认</span>
        <span class="legend booked">已订</span>
        <span class="legend unavailable">不可订</span>
        <span class="legend locked">锁定中</span>
      </div>

      <div v-if="homestayOptions.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无可管理房源" />
      </div>
      <div v-else class="grid-scroll">
        <HostCalendarGrid
          :current-month="currentMonth"
          :days="days"
          :loading="loading"
          @day-click="handleDayClick"
        />
      </div>
    </section>

    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="430px">
      <div class="drawer-content">
        <div class="selected-panel">
          <div class="selected-title">{{ selectedHomestayName || "未选择房源" }}</div>
          <div v-if="selectedEntries.length > 0" class="entry-list">
            <div v-for="entry in selectedEntries" :key="entry.homestayId" class="entry-item">
              <span class="entry-name">{{ entry.homestayTitle }}</span>
              <span class="entry-status">{{ getStatusLabel(entry.status) }}</span>
              <span v-if="entry.guestName" class="entry-meta">{{ entry.guestName }}</span>
            </div>
          </div>
        </div>

        <el-form label-position="top" class="availability-form">
          <el-form-item label="房源">
            <el-select v-model="form.homestayId" filterable placeholder="请选择房源">
              <el-option
                v-for="item in homestayOptions"
                :key="item.id"
                :label="item.title"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="日期范围">
            <el-date-picker
              v-model="form.dateRange"
              type="daterange"
              unlink-panels
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              class="range-picker"
            />
          </el-form-item>

          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio-button label="UNAVAILABLE">不可订</el-radio-button>
              <el-radio-button label="AVAILABLE">恢复可订</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <template v-if="form.status === 'UNAVAILABLE'">
            <el-form-item label="原因">
              <el-input v-model="form.reason" maxlength="80" show-word-limit />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="form.note" type="textarea" :rows="4" maxlength="300" show-word-limit />
            </el-form-item>
          </template>
        </el-form>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitAvailability">保存</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.calendar-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-heading h1 {
  margin: 0;
  color: #111827;
  font-size: 24px;
  font-weight: 700;
}

.calendar-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.legend-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.legend {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #475569;
  font-size: 13px;
}

.legend::before {
  content: "";
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #94a3b8;
}

.legend.available::before {
  background: #16a34a;
}

.legend.pending::before {
  background: #d97706;
}

.legend.booked::before {
  background: #2563eb;
}

.legend.unavailable::before {
  background: #dc2626;
}

.legend.locked::before {
  background: #64748b;
}

.grid-scroll {
  overflow-x: auto;
}

.empty-state {
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
}

.drawer-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.selected-panel {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.selected-title {
  color: #111827;
  font-weight: 600;
}

.entry-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.entry-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 4px 8px;
  color: #334155;
  font-size: 13px;
}

.entry-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entry-status {
  color: #475569;
}

.entry-meta {
  grid-column: 1 / -1;
  color: #64748b;
}

.availability-form :deep(.el-select),
.range-picker {
  width: 100%;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
