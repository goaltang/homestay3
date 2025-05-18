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

    // 如果允许修改各项子评分，可以在这里添加相应字段和验证
    // private Integer cleanlinessRating;
    // private Integer accuracyRating;
    // ...
} 