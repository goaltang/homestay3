package com.homestay3.homestaybackend.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "homestay_images")
public class HomestayImage implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "homestay_id")
    private Long homestayId;
    
    @Column(name = "image")
    private String image;
    
    @Column(name = "image_url")
    private String imageUrl;
} 