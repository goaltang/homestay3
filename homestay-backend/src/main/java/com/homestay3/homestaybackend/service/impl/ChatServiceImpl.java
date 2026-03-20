package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.ChatMessageDTO;
import com.homestay3.homestaybackend.dto.ConversationDTO;
import com.homestay3.homestaybackend.dto.MessageDTO;
import com.homestay3.homestaybackend.entity.Conversation;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.Message;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.repository.ConversationRepository;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.MessageRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.ChatService;
import com.homestay3.homestaybackend.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final HomestayRepository homestayRepository;
    private final WebSocketService webSocketService;

    @Autowired
    public ChatServiceImpl(ConversationRepository conversationRepository,
                          MessageRepository messageRepository,
                          UserRepository userRepository,
                          HomestayRepository homestayRepository,
                          WebSocketService webSocketService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.homestayRepository = homestayRepository;
        this.webSocketService = webSocketService;
    }

    @Override
    @Transactional
    public ConversationDTO createOrGetConversation(Long homestayId, Long hostId, Long guestId) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new IllegalArgumentException("房东不存在"));
        User guest = userRepository.findById(guestId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        final Homestay homestay = homestayId != null
                ? homestayRepository.findById(homestayId).orElse(null)
                : null;

        Conversation conversation = conversationRepository
                .findByHomestayIdAndHostIdAndGuestId(homestayId, hostId, guestId)
                .orElseGet(() -> {
                    Conversation newConv = Conversation.builder()
                            .homestay(homestay)
                            .host(host)
                            .guest(guest)
                            .unreadHostCount(0)
                            .unreadGuestCount(0)
                            .build();
                    return conversationRepository.save(newConv);
                });

        return convertToDTO(conversation, guestId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationDTO> getConversations(Long userId, Pageable pageable) {
        return conversationRepository.findByUserIdOrderByLastMessageAtDesc(userId, pageable)
                .map(conv -> convertToDTO(conv, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDTO getConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("对话不存在"));
        return convertToDTO(conversation, userId);
    }

    @Override
    @Transactional
    public MessageDTO sendMessage(Long conversationId, Long senderId, String content) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("对话不存在"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(content)
                .isRead(false)
                .build();
        Message savedMessage = messageRepository.save(message);

        conversation.setLastMessageContent(content.length() > 50 ? content.substring(0, 50) + "..." : content);
        conversation.setLastMessageAt(savedMessage.getCreatedAt());

        Long receiverId;
        if (senderId.equals(conversation.getHost().getId())) {
            receiverId = conversation.getGuest().getId();
            conversation.setUnreadGuestCount(conversation.getUnreadGuestCount() + 1);
        } else {
            receiverId = conversation.getHost().getId();
            conversation.setUnreadHostCount(conversation.getUnreadHostCount() + 1);
        }
        conversationRepository.save(conversation);

        MessageDTO messageDTO = convertToMessageDTO(savedMessage);
        ChatMessageDTO chatMessageDTO = ChatMessageDTO.builder()
                .conversationId(conversationId)
                .message(messageDTO)
                .receiverId(receiverId)
                .unreadCount(senderId.equals(conversation.getHost().getId())
                        ? conversation.getUnreadGuestCount()
                        : conversation.getUnreadHostCount())
                .build();

        webSocketService.sendChatMessage(conversationId, chatMessageDTO);
        webSocketService.sendChatMessageToUser(receiverId, chatMessageDTO);

        log.info("发送消息成功: conversationId={}, senderId={}, receiverId={}", conversationId, senderId, receiverId);
        return messageDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageDTO> getMessages(Long conversationId, Long userId, Pageable pageable) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("对话不存在"));
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(this::convertToMessageDTO);
    }

    @Override
    @Transactional
    public void markMessagesAsRead(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("对话不存在"));

        Long senderId = userId.equals(conversation.getHost().getId())
                ? conversation.getGuest().getId()
                : conversation.getHost().getId();

        int updated = messageRepository.markMessagesAsReadByConversationAndSender(conversationId, senderId);

        if (userId.equals(conversation.getHost().getId())) {
            conversation.setUnreadHostCount(0);
        } else {
            conversation.setUnreadGuestCount(0);
        }
        conversationRepository.save(conversation);

        log.info("标记消息已读: conversationId={}, userId={}, updatedCount={}", conversationId, userId, updated);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadConversationCount(Long userId) {
        return conversationRepository.countUnreadByUserId(userId);
    }

    private ConversationDTO convertToDTO(Conversation conversation, Long currentUserId) {
        boolean isHost = currentUserId.equals(conversation.getHost().getId());

        return ConversationDTO.builder()
                .id(conversation.getId())
                .homestayId(conversation.getHomestay() != null ? conversation.getHomestay().getId() : null)
                .homestayTitle(conversation.getHomestay() != null ? conversation.getHomestay().getTitle() : null)
                .hostId(conversation.getHost().getId())
                .hostUsername(conversation.getHost().getUsername())
                .hostAvatar(conversation.getHost().getAvatar())
                .guestId(conversation.getGuest().getId())
                .guestUsername(conversation.getGuest().getUsername())
                .guestAvatar(conversation.getGuest().getAvatar())
                .lastMessageContent(conversation.getLastMessageContent())
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount(isHost ? conversation.getUnreadHostCount() : conversation.getUnreadGuestCount())
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    private MessageDTO convertToMessageDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId())
                .senderUsername(message.getSender().getUsername())
                .senderAvatar(message.getSender().getAvatar())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .readAt(message.getReadAt())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
