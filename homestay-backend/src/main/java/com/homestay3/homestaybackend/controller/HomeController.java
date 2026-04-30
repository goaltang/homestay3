package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.ApiResponse;
import com.homestay3.homestaybackend.dto.BannerDTO;
import com.homestay3.homestaybackend.dto.HomeStatsDTO;
import com.homestay3.homestaybackend.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<HomeStatsDTO>> getStats() {
        HomeStatsDTO stats = homeService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/banners")
    public ResponseEntity<ApiResponse<List<BannerDTO>>> getBanners() {
        List<BannerDTO> banners = homeService.getActiveBanners();
        return ResponseEntity.ok(ApiResponse.success(banners));
    }
}
