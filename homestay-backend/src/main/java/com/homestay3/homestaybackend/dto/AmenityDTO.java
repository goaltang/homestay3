package com.homestay3.homestaybackend.dto;

import java.time.LocalDateTime;
import com.homestay3.homestaybackend.model.Amenity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmenityDTO {
    
    private String value;
    private String label;
    private String description;
    private String icon;
    private String categoryCode;
    private String categoryName;
    private String categoryIcon;
    private boolean active;
    private Long usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public AmenityDTO(Amenity amenity) {
        if (amenity != null) {
            this.setValue(amenity.getValue());
            this.setLabel(amenity.getLabel());
            this.setDescription(amenity.getDescription());
            this.setIcon(amenity.getIcon());
            this.setActive(amenity.isActive());
            this.setUsageCount(amenity.getUsageCount());
            this.setCreatedAt(amenity.getCreatedAt());
            this.setUpdatedAt(amenity.getUpdatedAt());
            this.setCreatedBy(amenity.getCreatedBy());
            this.setUpdatedBy(amenity.getUpdatedBy());
            
            if (amenity.getCategory() != null) {
                this.setCategoryCode(amenity.getCategory().getCode());
                this.setCategoryName(amenity.getCategory().getName());
                this.setCategoryIcon(amenity.getCategory().getIcon());
            }
        }
    }
} 