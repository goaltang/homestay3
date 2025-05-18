package com.homestay3.homestaybackend.model;

import com.homestay3.homestaybackend.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "homestays")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Homestay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private String status;

    @Column(name = "max_guests", nullable = false)
    private Integer maxGuests;

    @Column(name = "min_nights", nullable = false)
    private Integer minNights;

    @Column(name = "province_text")
    private String provinceText;

    @Column(name = "city_text")
    private String cityText;

    @Column(name = "district_text")
    private String districtText;

    @Column(name = "address_detail", nullable = false)
    private String addressDetail;

    @Column(name = "province_code", length = 10, nullable = false)
    private String provinceCode;

    @Column(name = "city_code", length = 10, nullable = false)
    private String cityCode;

    @Column(name = "district_code", length = 10)
    private String districtCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image")
    private String coverImage;

    @ElementCollection
    @CollectionTable(name = "homestay_images", joinColumns = @JoinColumn(name = "homestay_id"))
    @Column(name = "image_url")
    @Builder.Default
    private List<String> images = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column
    @Builder.Default
    private Boolean featured = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "homestay_amenity",
        joinColumns = @JoinColumn(name = "homestay_id"),
        inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Amenity> amenities = new HashSet<>();
}
