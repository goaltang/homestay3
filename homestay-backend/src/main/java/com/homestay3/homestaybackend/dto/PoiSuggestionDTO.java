package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoiSuggestionDTO {
    private String id;
    private String name;
    private String address;
    private String district;
    private Double latitude;
    private Double longitude;
}
