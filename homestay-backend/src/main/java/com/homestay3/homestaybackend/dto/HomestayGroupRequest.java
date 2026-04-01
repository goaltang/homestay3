package com.homestay3.homestaybackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomestayGroupRequest {

    @NotBlank(message = "分组名称不能为空")
    @Size(max = 100, message = "分组名称长度不能超过100个字符")
    private String name;

    @Size(max = 50, message = "分组编码长度不能超过50个字符")
    private String code;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    @Size(max = 100, message = "图标长度不能超过100个字符")
    private String icon;

    @Size(max = 20, message = "颜色值长度不能超过20个字符")
    private String color;

    private Integer sortOrder;

    private Boolean enabled;
}
