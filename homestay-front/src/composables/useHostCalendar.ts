import { computed, ref, shallowRef, watch } from "vue";
import { ElMessage } from "element-plus";
import {
  getHostCalendar,
  updateHostCalendarAvailability,
  type CalendarAvailabilityUpdateRequest,
  type HostCalendarDay,
  type HostCalendarSummary,
} from "@/api/hostCalendar";
import { getHostHomestayOptions } from "@/api/host";

export interface HostHomestayOption {
  id: number;
  title: string;
}

const emptySummary: HostCalendarSummary = {
  availableCount: 0,
  bookedCount: 0,
  pendingCount: 0,
  unavailableCount: 0,
  checkInCount: 0,
  checkOutCount: 0,
  estimatedRevenue: 0,
};

function startOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

function addMonths(date: Date, amount: number) {
  return new Date(date.getFullYear(), date.getMonth() + amount, 1);
}

export function formatCalendarDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function addCalendarDays(value: string, amount: number) {
  const date = new Date(`${value}T00:00:00`);
  date.setDate(date.getDate() + amount);
  return formatCalendarDate(date);
}

export function useHostCalendar() {
  const currentMonth = shallowRef(startOfMonth(new Date()));
  const selectedHomestayId = shallowRef<number | null>(null);
  const homestayOptions = ref<HostHomestayOption[]>([]);
  const days = ref<HostCalendarDay[]>([]);
  const summary = ref<HostCalendarSummary>({ ...emptySummary });
  const loading = shallowRef(false);
  const saving = shallowRef(false);
  const initialized = shallowRef(false);

  const monthLabel = computed(() => {
    const year = currentMonth.value.getFullYear();
    const month = currentMonth.value.getMonth() + 1;
    return `${year}年${month}月`;
  });

  const queryRange = computed(() => {
    const startDate = startOfMonth(currentMonth.value);
    const endDate = addMonths(startDate, 1);
    return {
      startDate: formatCalendarDate(startDate),
      endDate: formatCalendarDate(endDate),
    };
  });

  const queryParams = computed(() => ({
    ...queryRange.value,
    ...(selectedHomestayId.value ? { homestayId: selectedHomestayId.value } : {}),
  }));

  async function loadHomestays() {
    homestayOptions.value = await getHostHomestayOptions();
  }

  async function loadCalendar() {
    loading.value = true;
    try {
      const response = await getHostCalendar(queryParams.value);
      const data = response.data;
      days.value = Array.isArray(data.days) ? data.days : [];
      summary.value = {
        ...emptySummary,
        ...(data.summary as HostCalendarSummary),
      };
    } catch {
      ElMessage.error("加载房东日历失败");
    } finally {
      loading.value = false;
    }
  }

  async function initialize() {
    await loadHomestays();
    initialized.value = true;
    await loadCalendar();
  }

  async function saveAvailability(payload: CalendarAvailabilityUpdateRequest) {
    saving.value = true;
    try {
      await updateHostCalendarAvailability(payload);
      ElMessage.success(payload.status === "UNAVAILABLE" ? "已设为不可订" : "已恢复可订");
      await loadCalendar();
      return true;
    } catch {
      // 错误消息已由响应拦截器展示，此处仅阻止继续执行
      return false;
    } finally {
      saving.value = false;
    }
  }

  function goPrevMonth() {
    currentMonth.value = addMonths(currentMonth.value, -1);
  }

  function goNextMonth() {
    currentMonth.value = addMonths(currentMonth.value, 1);
  }

  function goToday() {
    currentMonth.value = startOfMonth(new Date());
  }

  watch([currentMonth, selectedHomestayId], () => {
    if (initialized.value) {
      void loadCalendar();
    }
  });

  return {
    currentMonth,
    selectedHomestayId,
    homestayOptions,
    days,
    summary,
    loading,
    saving,
    monthLabel,
    queryRange,
    initialize,
    loadCalendar,
    loadHomestays,
    saveAvailability,
    goPrevMonth,
    goNextMonth,
    goToday,
  };
}
