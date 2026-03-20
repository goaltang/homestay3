package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.ApiResponse;
import com.homestay3.homestaybackend.dto.ConversationDTO;
import com.homestay3.homestaybackend.dto.MessageDTO;
import com.homestay3.homestaybackend.service.ChatService;
import com.homestay3.homestaybackend.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
@PreAuthorize("isAuthenticated()")
public class ChatController {

    private final ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<Page<ConversationDTO>>> getConversations(
            @PageableDefault(size = 20, sort = "lastMessageAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        Long userId = UserUtil.getCurrentUserId(authentication);
        Page<ConversationDTO> conversations = chatService.getConversations(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<ApiResponse<ConversationDTO>> getConversation(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = UserUtil.getCurrentUserId(authentication);
        ConversationDTO conversation = chatService.getConversation(id, userId);
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<ConversationDTO>> createConversation(
            @RequestBody Map<String, Long> request,
            Authentication authentication) {
        Long userId = UserUtil.getCurrentUserId(authentication);
        Long homestayId = request.get("homestayId");
        Long hostId = request.get("hostId");
        ConversationDTO conversation = chatService.createOrGetConversation(homestayId, hostId, userId);
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<Page<MessageDTO>>> getMessages(
            @PathVariable Long id,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        Long userId = UserUtil.getCurrentUserId(authentication);
        Page<MessageDTO> messages = chatService.getMessages(id, userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<MessageDTO>> sendMessage(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        Long userId = UserUtil.getCurrentUserId(authentication);
        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "消息内容不能为空"));
        }
        MessageDTO message = chatService.sendMessage(id, userId, content.trim());
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @PutMapping("/conversations/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = UserUtil.getCurrentUserId(authentication);
        chatService.markMessagesAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            Authentication authentication) {
        Long userId = UserUtil.getCurrentUserId(authentication);
        Map<String, Long> result = new HashMap<>();
        result.put("unreadCount", chatService.getUnreadConversationCount(userId));
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
