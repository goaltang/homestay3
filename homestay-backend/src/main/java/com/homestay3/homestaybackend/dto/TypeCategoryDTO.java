package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeCategoryDTO {
    private Long id;
    private String name;
    private String description;
    private int sortOrder;
    private List<HomestayTypeDTO> types = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 