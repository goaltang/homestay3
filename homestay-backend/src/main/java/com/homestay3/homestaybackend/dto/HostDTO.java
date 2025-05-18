package com.homestay3.homestaybackend.dto;

import com.homestay3.homestaybackend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostDTO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private String phone;
    private String realName;
    private String idCard;
    private String occupation;
    private String introduction;
    private List<String> languages;
    private List<Map<String, String>> companions;
    private String hostSince; // 格式化后的日期字符串
    private String hostRating; // 转换为字符串，保留一位小数
    private String hostAccommodations;
    private String hostYears;
    private String hostResponseRate;
    private String hostResponseTime;
    
    // 统计数据
    private Integer homestayCount;
    private Integer orderCount;
    private Integer reviewCount;
    private Double rating;
    
    // 从User实体构建DTO，必要的转换在Service层完成
    @JsonIgnore
    public static HostDTO fromUser(User user) {
        return HostDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .realName(user.getRealName())
                .idCard(user.getIdCard())
                .occupation(user.getOccupation())
                .introduction(user.getIntroduction())
                // languages和companions需要JSON解析，在Service层处理
                .hostSince(user.getHostSince() != null ? user.getHostSince().toString() : null)
                .hostRating(user.getHostRating() != null ? String.format("%.1f", user.getHostRating()) : null)
                .hostAccommodations(user.getHostAccommodations() != null ? user.getHostAccommodations().toString() : null)
                .hostYears(user.getHostYears() != null ? user.getHostYears().toString() : null)
                .hostResponseRate(user.getHostResponseRate())
                .hostResponseTime(user.getHostResponseTime())
                .build();
    }
} 