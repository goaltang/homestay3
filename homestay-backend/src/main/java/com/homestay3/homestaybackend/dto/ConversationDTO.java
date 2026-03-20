package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {
    private Long id;
    private Long homestayId;
    private String homestayTitle;
    private Long hostId;
    private String hostUsername;
    private String hostAvatar;
    private Long guestId;
    private String guestUsername;
    private String guestAvatar;
    private String lastMessageContent;
    private LocalDateTime lastMessageAt;
    private Integer unreadCount;
    private LocalDateTime createdAt;
}
