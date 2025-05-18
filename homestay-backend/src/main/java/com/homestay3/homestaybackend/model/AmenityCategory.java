package com.homestay3.homestaybackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "amenity_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmenityCategory {
    
    @Id
    @Column(name = "code", nullable = false, length = 50)
    private String code;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "icon", length = 100)
    private String icon;
    
    @Column(name = "sort_order")
    private Integer sortOrder;
} 