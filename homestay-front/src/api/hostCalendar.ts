import request from "@/utils/request";

export type HostCalendarStatus =
  | "AVAILABLE"
  | "PENDING_CONFIRM"
  | "BOOKED"
  | "CHECKED_IN"
  | "CHECKED_OUT"
  | "UNAVAILABLE"
  | "LOCKED";

export interface HostCalendarDay {
  date: string;
  homestayId: number;
  homestayTitle: string;
  status: HostCalendarStatus;
  source?: string | null;
  reason?: string | null;
  note?: string | null;
  orderId?: number | null;
  orderNumber?: string | null;
  guestName?: string | null;
  checkIn: boolean;
  checkOut: boolean;
  basePrice?: number | null;
  finalPrice?: number | null;
}

export interface HostCalendarSummary {
  availableCount: number;
  bookedCount: number;
  pendingCount: number;
  unavailableCount: number;
  checkInCount: number;
  checkOutCount: number;
  estimatedRevenue: number;
}

export interface HostCalendarQuery {
  homestayId?: number;
  startDate: string;
  endDate: string;
}

export interface CalendarAvailabilityUpdateRequest {
  homestayId: number;
  startDate: string;
  endDate: string;
  status: "AVAILABLE" | "UNAVAILABLE";
  reason?: string;
  note?: string;
}

export interface HostCalendarResponse {
  days: HostCalendarDay[];
  summary: HostCalendarSummary;
}

export function getHostCalendar(params: HostCalendarQuery) {
  return request<HostCalendarResponse>({
    url: "/api/host/calendar",
    method: "get",
    params,
  });
}

export function updateHostCalendarAvailability(data: CalendarAvailabilityUpdateRequest) {
  return request({
    url: "/api/host/calendar/availability",
    method: "patch",
    data,
  });
}
