<script setup lang="ts">
import { computed } from "vue";
import type { HostCalendarDay, HostCalendarStatus } from "@/api/hostCalendar";
import { formatCalendarDate } from "@/composables/useHostCalendar";

interface CalendarCell {
  date: string;
  dayNumber: number;
  inMonth: boolean;
  isToday: boolean;
  entries: HostCalendarDay[];
}

const props = defineProps<{
  currentMonth: Date;
  days: HostCalendarDay[];
  loading: boolean;
}>();

const emit = defineEmits<{
  "day-click": [payload: { date: string; entries: HostCalendarDay[] }];
}>();

const weekdays = ["一", "二", "三", "四", "五", "六", "日"];

const statusLabels: Record<HostCalendarStatus, string> = {
  AVAILABLE: "可订",
  PENDING_CONFIRM: "待确认",
  BOOKED: "已订",
  CHECKED_IN: "入住中",
  CHECKED_OUT: "已退房",
  UNAVAILABLE: "不可订",
  LOCKED: "锁定中",
};

const statusPriority: HostCalendarStatus[] = [
  "CHECKED_IN",
  "CHECKED_OUT",
  "BOOKED",
  "PENDING_CONFIRM",
  "LOCKED",
  "UNAVAILABLE",
  "AVAILABLE",
];

const today = formatCalendarDate(new Date());

const daysByDate = computed(() => {
  const map = new Map<string, HostCalendarDay[]>();
  for (const day of props.days) {
    const entries = map.get(day.date) ?? [];
    entries.push(day);
    map.set(day.date, entries);
  }
  return map;
});

const cells = computed<CalendarCell[]>(() => {
  const monthStart = new Date(
    props.currentMonth.getFullYear(),
    props.currentMonth.getMonth(),
    1
  );
  const leadingDays = (monthStart.getDay() + 6) % 7;
  const gridStart = new Date(monthStart);
  gridStart.setDate(monthStart.getDate() - leadingDays);

  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(gridStart);
    date.setDate(gridStart.getDate() + index);
    const dateKey = formatCalendarDate(date);
    return {
      date: dateKey,
      dayNumber: date.getDate(),
      inMonth: date.getMonth() === props.currentMonth.getMonth(),
      isToday: dateKey === today,
      entries: daysByDate.value.get(dateKey) ?? [],
    };
  });
});

function getDominantStatus(entries: HostCalendarDay[]): HostCalendarStatus {
  if (entries.length === 0) {
    return "AVAILABLE";
  }
  return (
    statusPriority.find((status) => entries.some((entry) => entry.status === status)) ??
    "AVAILABLE"
  );
}

function getStatusClass(status: HostCalendarStatus) {
  return `status-${status.toLowerCase().replace(/_/g, "-")}`;
}

function countByStatus(entries: HostCalendarDay[], status: HostCalendarStatus) {
  return entries.filter((entry) => entry.status === status).length;
}

function getCellSummary(entries: HostCalendarDay[]) {
  if (entries.length === 0) {
    return "暂无房源";
  }

  const booked = entries.filter((entry) =>
    ["BOOKED", "CHECKED_IN", "CHECKED_OUT"].includes(entry.status)
  ).length;
  const unavailable = countByStatus(entries, "UNAVAILABLE");
  const pending = countByStatus(entries, "PENDING_CONFIRM");
  const available = countByStatus(entries, "AVAILABLE");

  if (entries.length === 1) {
    return statusLabels[entries[0].status];
  }

  return `${available}可订 / ${booked}已订 / ${pending}待确认 / ${unavailable}不可订`;
}

function handleClick(cell: CalendarCell) {
  if (!cell.inMonth) {
    return;
  }
  emit("day-click", { date: cell.date, entries: cell.entries });
}
</script>

<template>
  <div v-loading="loading" class="calendar-grid">
    <div v-for="weekday in weekdays" :key="weekday" class="weekday">
      {{ weekday }}
    </div>

    <button
      v-for="cell in cells"
      :key="cell.date"
      type="button"
      class="calendar-cell"
      :class="[
        { muted: !cell.inMonth, today: cell.isToday },
        getStatusClass(getDominantStatus(cell.entries)),
      ]"
      :disabled="!cell.inMonth"
      @click="handleClick(cell)"
    >
      <span class="cell-header">
        <span class="day-number">{{ cell.dayNumber }}</span>
        <span v-if="cell.entries.some((entry) => entry.checkIn)" class="flag checkin">入住</span>
        <span v-if="cell.entries.some((entry) => entry.checkOut)" class="flag checkout">退房</span>
      </span>

      <span class="cell-status">{{ getCellSummary(cell.entries) }}</span>

      <span v-if="cell.entries.length === 1" class="cell-detail">
        <span v-if="cell.entries[0].guestName">{{ cell.entries[0].guestName }}</span>
        <span v-else-if="cell.entries[0].orderNumber">{{ cell.entries[0].orderNumber }}</span>
        <span v-else-if="cell.entries[0].finalPrice">¥{{ cell.entries[0].finalPrice }}</span>
      </span>

      <span v-else-if="cell.entries.length > 1" class="cell-detail">
        {{ cell.entries.length }} 套房源
      </span>
    </button>
  </div>
</template>

<style scoped>
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  border: 1px solid #d8dee9;
  border-radius: 8px;
  background: #d8dee9;
}

.weekday {
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}

.calendar-cell {
  min-height: 118px;
  padding: 10px;
  border: none;
  background: #fff;
  text-align: left;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: background 0.16s ease, box-shadow 0.16s ease;
}

.calendar-cell:hover:not(:disabled) {
  background: #f8fbff;
  box-shadow: inset 0 0 0 1px #93c5fd;
}

.calendar-cell:disabled {
  cursor: default;
}

.muted {
  background: #f8fafc;
  color: #94a3b8;
}

.today {
  box-shadow: inset 0 0 0 2px #2563eb;
}

.cell-header {
  min-height: 22px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.day-number {
  color: #111827;
  font-weight: 700;
}

.flag {
  padding: 2px 6px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1.2;
}

.checkin {
  color: #075985;
  background: #e0f2fe;
}

.checkout {
  color: #5b21b6;
  background: #ede9fe;
}

.cell-status {
  color: #334155;
  font-size: 13px;
  line-height: 1.35;
}

.cell-detail {
  margin-top: auto;
  color: #64748b;
  font-size: 12px;
  line-height: 1.3;
  word-break: break-word;
}

.status-available {
  border-top: 3px solid #16a34a;
}

.status-pending-confirm {
  border-top: 3px solid #d97706;
}

.status-booked,
.status-checked-in,
.status-checked-out {
  border-top: 3px solid #2563eb;
}

.status-unavailable {
  border-top: 3px solid #dc2626;
}

.status-locked {
  border-top: 3px solid #64748b;
}

@media (max-width: 900px) {
  .calendar-grid {
    min-width: 760px;
  }
}
</style>
