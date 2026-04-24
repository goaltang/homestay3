package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomestayWriteRequest {

    private String title;
    private String type;
    private String price;
    private String status;
    private Integer maxGuests;
    private Integer minNights;
    private Integer maxNights;
    private String provinceText;
    private String cityText;
    private String districtText;
    private String addressDetail;
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String description;
    private String coverImage;

    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Builder.Default
    private List<Object> amenities = new ArrayList<>();

    private String ownerUsername;
    private Boolean featured;
    private Boolean autoConfirm;
    private String checkInTime;
    private String checkOutTime;
    private Integer cancelPolicyType;
    private String houseRules;
    private Double latitude;
    private Double longitude;
}
