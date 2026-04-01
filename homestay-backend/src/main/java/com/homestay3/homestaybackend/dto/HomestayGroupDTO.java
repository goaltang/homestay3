package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomestayGroupDTO {

    private Long id;

    private String name;

    private String code;

    private String description;

    private String icon;

    private String color;

    private Long ownerId;

    private String ownerUsername;

    private Integer sortOrder;

    private Boolean isDefault;

    private Boolean enabled;

    private Long homestayCount;

    private String createdAt;

    private String updatedAt;
}
