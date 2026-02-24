import { ref, reactive, computed, type Ref } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { useAuthStore } from "@/stores/auth";
import { useUserStore } from "@/stores/user";
import {
  formatDateString,
  calculateNights,
  calculateFees,
} from "@/utils/homestayUtils";
import type { BookingDates, HomestayDetail } from "@/types/homestay";

export function useBooking(
  homestay: Ref<HomestayDetail | null>,
  pricePerNight: Ref<number>
) {
  const router = useRouter();
  const route = useRoute();
  const authStore = useAuthStore();
  const userStore = useUserStore();

  const bookingDates = reactive<BookingDates>({
    checkIn: null,
    checkOut: null,
    guests: 1,
  });
  const bookingDateRange = ref<[Date, Date] | null>(null);

  const totalNights = computed(() => {
    return calculateNights(bookingDates.checkIn, bookingDates.checkOut);
  });

  const fees = computed(() => {
    return calculateFees(pricePerNight.value, totalNights.value);
  });

  const disablePastDates = (date: Date) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return date.getTime() < today.getTime();
  };

  const handleDateRangeChange = (dates: [Date, Date] | null) => {
    if (dates && dates.length === 2 && dates[0] && dates[1]) {
      if (dates[1].getTime() <= dates[0].getTime()) {
        ElMessage.warning("退房日期必须在入住日期之后");
        bookingDateRange.value = null;
        bookingDates.checkIn = null;
        bookingDates.checkOut = null;
      } else {
        bookingDates.checkIn = dates[0];
        bookingDates.checkOut = dates[1];
      }
    } else {
      bookingDates.checkIn = null;
      bookingDates.checkOut = null;
    }
  };

  const bookHomestay = async () => {
    // 检查登录状态
    if (!authStore.isAuthenticated) {
      ElMessageBox.confirm("您需要登录才能预订，是否现在登录？", "提示", {
        confirmButtonText: "去登录",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          router.push({ path: "/login", query: { redirect: route.fullPath } });
        })
        .catch(() => {
          ElMessage.info("取消预订");
        });
      return;
    }

    // 检查日期和民宿信息
    if (
      !bookingDates.checkIn ||
      !bookingDates.checkOut ||
      !homestay.value ||
      totalNights.value <= 0
    ) {
      ElMessage.warning("请先选择有效的入住和退房日期");
      return;
    }

    // 准备传递给确认页的数据
    const bookingDetails = {
      homestayId: homestay.value.id!,
      checkInDate: formatDateString(bookingDates.checkIn),
      checkOutDate: formatDateString(bookingDates.checkOut),
      guestCount: bookingDates.guests,
      totalPrice: fees.value.totalPrice,
      // 完整的订单数据
      homestayData: {
        id: homestay.value.id!,
        title: homestay.value.title,
        coverImage: homestay.value.coverImage || "",
        addressDetail: homestay.value.addressDetail || "",
        ownerId: homestay.value.ownerId || 0,
        ownerName: homestay.value.ownerName || "",
        autoConfirm: homestay.value.autoConfirm || false,
        price: pricePerNight.value,
      },
      nights: totalNights.value,
      cleaningFee: fees.value.cleaningFee,
      serviceFee: fees.value.serviceFee,
    };

    console.log("准备跳转到订单确认页，传递数据:", bookingDetails);

    // 将详细数据存储到session storage，避免URL过长
    sessionStorage.setItem("booking-details", JSON.stringify(bookingDetails));

    // 跳转到订单确认页，只传递必要的核心参数
    router.push({
      path: "/order/confirm",
      query: {
        homestayId: bookingDetails.homestayId.toString(),
        checkIn: bookingDetails.checkInDate,
        checkOut: bookingDetails.checkOutDate,
        guests: bookingDetails.guestCount.toString(),
      },
    });
  };

  const resetBooking = () => {
    bookingDates.checkIn = null;
    bookingDates.checkOut = null;
    bookingDates.guests = 1;
    bookingDateRange.value = null;
  };

  return {
    bookingDates,
    bookingDateRange,
    totalNights,
    fees,
    disablePastDates,
    handleDateRangeChange,
    bookHomestay,
    resetBooking,
  };
}
