package com.homestay3.homestaybackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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
    private Long orderId;
    private String homestayTitle;

    @Min(value = 1, message = "评分不能低于1")
    @Max(value = 5, message = "评分不能高于5")
    private Integer rating;

    @Size(max = 1000, message = "评价内容不能超过1000字")
    private String content;

    @Size(max = 500, message = "回复内容不能超过500字")
    private String response;

    // 评价图片
    private List<String> images;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime responseTime;

    // 各项评分 (1-5)
    @Min(value = 1, message = "清洁度评分不能低于1")
    @Max(value = 5, message = "清洁度评分不能高于5")
    private Integer cleanlinessRating;

    @Min(value = 1, message = "准确性评分不能低于1")
    @Max(value = 5, message = "准确性评分不能高于5")
    private Integer accuracyRating;

    @Min(value = 1, message = "沟通评分不能低于1")
    @Max(value = 5, message = "沟通评分不能高于5")
    private Integer communicationRating;

    @Min(value = 1, message = "位置评分不能低于1")
    @Max(value = 5, message = "位置评分不能高于5")
    private Integer locationRating;

    @Min(value = 1, message = "入住评分不能低于1")
    @Max(value = 5, message = "入住评分不能高于5")
    private Integer checkInRating;

    @Min(value = 1, message = "性价比评分不能低于1")
    @Max(value = 5, message = "性价比评分不能高于5")
    private Integer valueRating;

    // 额外信息
    private Boolean isOwnerResponse; // 是否房东回复
    private Boolean isPublic; // 是否公开
}
