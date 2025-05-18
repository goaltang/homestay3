package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomestayRequest {
    
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100个字符")
    private String title;
    
    @NotBlank(message = "类型不能为空")
    private String type;
    
    @NotBlank(message = "价格不能为空")
    private String price;
    
    private String status;
    
    @NotNull(message = "最大入住人数不能为空")
    @Positive(message = "最大入住人数必须大于0")
    private Integer maxGuests;
    
    @NotNull(message = "最少入住晚数不能为空")
    @Positive(message = "最少入住晚数必须大于0")
    private Integer minNights;
    
    @NotBlank(message = "省份编码不能为空")
    private String provinceCode;
    
    @NotBlank(message = "城市编码不能为空")
    private String cityCode;
    
    private String districtCode;
    
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "详细地址长度不能超过255个字符")
    private String addressDetail;
    
    @Builder.Default
    private List<String> amenities = new ArrayList<>();
    
    @Size(max = 1000, message = "描述长度不能超过1000个字符")
    private String description;
    
    private String coverImage;
    
    @Builder.Default
    private List<String> images = new ArrayList<>();
} 