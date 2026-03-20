package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.ConversationDTO;
import com.homestay3.homestaybackend.dto.MessageDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChatService {
    ConversationDTO createOrGetConversation(Long homestayId, Long hostId, Long guestId);
    Page<ConversationDTO> getConversations(Long userId, Pageable pageable);
    ConversationDTO getConversation(Long conversationId, Long userId);
    MessageDTO sendMessage(Long conversationId, Long senderId, String content);
    Page<MessageDTO> getMessages(Long conversationId, Long userId, Pageable pageable);
    void markMessagesAsRead(Long conversationId, Long userId);
    long getUnreadConversationCount(Long userId);
}
