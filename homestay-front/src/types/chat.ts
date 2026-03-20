// 对话类型
export interface Conversation {
  id: number;
  homestayId: number | null;
  homestayTitle: string | null;
  hostId: number;
  hostUsername: string;
  hostAvatar: string | null;
  guestId: number;
  guestUsername: string;
  guestAvatar: string | null;
  lastMessageContent: string | null;
  lastMessageAt: string | null;
  unreadCount: number;
  createdAt: string;
}

// 消息类型
export interface Message {
  id: number;
  conversationId: number;
  senderId: number;
  senderUsername: string;
  senderAvatar: string | null;
  content: string;
  isRead: boolean;
  readAt: string | null;
  createdAt: string;
}

// WebSocket聊天消息
export interface ChatMessage {
  conversationId: number;
  message: Message;
  receiverId: number;
  unreadCount: number;
}

// 分页数据
export interface ConversationPageData {
  content: Conversation[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface MessagePageData {
  content: Message[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
