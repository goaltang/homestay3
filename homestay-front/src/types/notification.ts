import type { AxiosResponse } from "axios";

export interface NotificationDto {
  id: number;
  userId: number;
  actorId: number | null;
  type: string;
  rawType?: string | null;
  entityType: string | null;
  rawEntityType?: string | null;
  entityId: string | null;
  content: string;
  read: boolean;
  isRead?: boolean;
  readAt: string | null;
  createdAt: string;
  actorUsername?: string;
  entityTitle?: string;
}

export interface NotificationPageData {
  content: NotificationDto[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export type NotificationApiResponse = AxiosResponse<NotificationPageData>;

export const normalizeNotification = (notification: NotificationDto): NotificationDto => {
  const read = notification.read ?? notification.isRead ?? false;
  const type = notification.type || notification.rawType || "UNKNOWN";
  const entityType = notification.entityType || notification.rawEntityType || null;

  return {
    ...notification,
    type,
    entityType,
    read,
    isRead: read,
  };
};
