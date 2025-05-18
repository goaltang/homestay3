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
public class HomestayTypeDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String icon;
    private boolean active;
    private int sortOrder;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 