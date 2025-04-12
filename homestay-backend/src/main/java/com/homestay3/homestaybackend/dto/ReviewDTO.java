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
public class ReviewDTO {
    private Long id;
    private Long userId;
    private String userName;
    private String userAvatar;
    private Long homestayId;
    private String homestayTitle;
    private Integer rating;
    private String content;
    private String response;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime responseTime;
    
    // 各项评分
    private Integer cleanlinessRating;
    private Integer accuracyRating;
    private Integer communicationRating;
    private Integer locationRating;
    private Integer checkInRating;
    private Integer valueRating;
    
    // 额外信息
    private Boolean isOwnerResponse; // 是否房东回复
    private Boolean isPublic; // 是否公开
} 