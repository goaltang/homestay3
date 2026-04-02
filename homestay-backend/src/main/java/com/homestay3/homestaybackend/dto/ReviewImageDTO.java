package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewImageDTO {
    private Long id;
    private Long reviewId;
    private String imageUrl;
    private Integer sortOrder;
}
