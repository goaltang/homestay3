package com.homestay3.homestaybackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateReviewRequest {

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分不能低于1")
    @Max(value = 5, message = "评分不能高于5")
    private Integer rating;

    @NotBlank(message = "评价内容不能为空")
    @Size(max = 1000, message = "评价内容不能超过1000字")
    private String content;

    // 子评分（可选）
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
} 